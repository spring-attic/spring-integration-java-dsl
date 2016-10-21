/*
 * Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.dsl.test.kafka;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.stream.Stream;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.kafka.Kafka;
import org.springframework.integration.dsl.kafka.KafkaProducerMessageHandlerSpec;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter;
import org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Artem Bilan
 * @author Nasko Vasilev
 * @since 1.1
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class KafkaTests {

	private static final String TEST_TOPIC = "test-topic";

	private static final String TEST_TOPIC2 = "test-topic2";

	private static final String TEST_TOPIC3 = "test-topic3";

	@ClassRule
	public static KafkaEmbedded embeddedKafka = new KafkaEmbedded(1, true, TEST_TOPIC, TEST_TOPIC2, TEST_TOPIC3);

	@Autowired
	@Qualifier("sendToKafkaFlow.input")
	private MessageChannel sendToKafkaFlowInput;

	@Autowired
	private PollableChannel listeningFromKafkaResults;

	@Autowired
	@Qualifier("kafkaProducer.handler")
	private KafkaProducerMessageHandler<?, ?> kafkaProducer;

	@Autowired
	@Qualifier("kafkaProducer3.handler")
	private KafkaProducerMessageHandler<?, ?> kafkaProducer3;

	@Autowired
	private PollableChannel errorChannel;

	@Test
	public void testKafkaAdapters() {
		try {
			this.sendToKafkaFlowInput.send(new GenericMessage<>("foo"));
			fail("IllegalArgumentException expected");
		}
		catch (Exception e) {
			assertThat(e.getMessage(), containsString("10 is not in the range"));
		}

		this.kafkaProducer.setPartitionIdExpression(new ValueExpression<>(0));
		this.kafkaProducer3.setPartitionIdExpression(new ValueExpression<>(0));
		this.sendToKafkaFlowInput.send(new GenericMessage<>("foo"));

		for (int i = 0; i < 100; i++) {
			Message<?> receive = this.listeningFromKafkaResults.receive(10000);
			assertNotNull(receive);
			assertEquals("FOO", receive.getPayload());
			MessageHeaders headers = receive.getHeaders();
			assertTrue(headers.containsKey(KafkaHeaders.ACKNOWLEDGMENT));
			Acknowledgment acknowledgment = headers.get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment.class);
			acknowledgment.acknowledge();
			assertEquals(TEST_TOPIC, headers.get(KafkaHeaders.RECEIVED_TOPIC));
			assertEquals(i + 1, headers.get(KafkaHeaders.RECEIVED_MESSAGE_KEY));
			assertEquals(0, headers.get(KafkaHeaders.RECEIVED_PARTITION_ID));
			assertEquals((long) i, headers.get(KafkaHeaders.OFFSET));
		}

		Message<String> message = MessageBuilder.withPayload("BAR").setHeader(KafkaHeaders.TOPIC, TEST_TOPIC2).build();

		this.sendToKafkaFlowInput.send(message);

		assertNull(this.listeningFromKafkaResults.receive(10));

		Message<?> error = this.errorChannel.receive(10000);
		assertNotNull(error);
		assertThat(error, instanceOf(ErrorMessage.class));
		assertThat(error.getPayload(), instanceOf(MessageRejectedException.class));
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {


		@Bean
		public ConsumerFactory<Integer, String> consumerFactory() {
			Map<String, Object> props = KafkaTestUtils.consumerProps("test1", "false", embeddedKafka);
			props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
			return new DefaultKafkaConsumerFactory<>(props);
		}

		@Bean
		public PollableChannel errorChannel() {
			return new QueueChannel();
		}

		@Bean
		public IntegrationFlow listeningFromKafkaFlow() {
			return IntegrationFlows
					.from(Kafka.messageDrivenChannelAdapter(consumerFactory(),
							KafkaMessageDrivenChannelAdapter.ListenerMode.record, TEST_TOPIC)
							.configureListenerContainer(c ->
									c.ackMode(AbstractMessageListenerContainer.AckMode.MANUAL))
							.errorChannel("errorChannel")
							.retryTemplate(new RetryTemplate())
							.filterInRetry(true))
					.filter(Message.class, m ->
									m.getHeaders().get(KafkaHeaders.RECEIVED_MESSAGE_KEY, Integer.class) < 101,
							f -> f.throwExceptionOnRejection(true))
					.<String, String>transform(String::toUpperCase)
					.channel(c -> c.queue("listeningFromKafkaResults"))
					.get();
		}

		@Bean
		public ProducerFactory<Integer, String> producerFactory() {
			return new DefaultKafkaProducerFactory<>(KafkaTestUtils.producerProps(embeddedKafka));
		}

		@Bean
		public IntegrationFlow sendToKafkaFlow() {
			return f -> f.<String>split(p -> Stream.generate(() -> p).limit(101).iterator(), null)
					.publishSubscribeChannel(c -> c
							.subscribe(sf -> sf.handle(kafkaMessageHandler(producerFactory(), TEST_TOPIC),
									e -> e.id("kafkaProducer")))
							.subscribe(sf -> sf.handle(kafkaMessageHandler(producerFactory(), TEST_TOPIC3),
									e -> e.id("kafkaProducer3")))
					);
		}

		private KafkaProducerMessageHandlerSpec<Integer, String>
		kafkaMessageHandler(ProducerFactory<Integer, String> producerFactory, String topic) {
			return Kafka
					.outboundChannelAdapter(producerFactory)
					.messageKey(m -> m
							.getHeaders()
							.get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER))
					.partitionId(m -> 10)
					.topicExpression("headers[kafka_topic] ?: '" + topic + "'");
		}

	}

}
