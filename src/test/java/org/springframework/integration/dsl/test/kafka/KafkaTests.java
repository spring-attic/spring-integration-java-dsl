/*
 * Copyright 2015 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.dsl.test.kafka;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static scala.collection.JavaConversions.asScalaBuffer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.I0Itec.zkclient.ZkClient;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.kafka.Kafka;
import org.springframework.integration.dsl.kafka.KafkaProducerMessageHandlerSpec;
import org.springframework.integration.kafka.core.ConnectionFactory;
import org.springframework.integration.kafka.core.DefaultConnectionFactory;
import org.springframework.integration.kafka.core.ZookeeperConfiguration;
import org.springframework.integration.kafka.listener.Acknowledgment;
import org.springframework.integration.kafka.listener.MetadataStoreOffsetManager;
import org.springframework.integration.kafka.support.KafkaHeaders;
import org.springframework.integration.kafka.support.ProducerMetadata;
import org.springframework.integration.kafka.support.ZookeeperConnect;
import org.springframework.integration.kafka.util.EncoderAdaptingSerializer;
import org.springframework.integration.kafka.util.TopicUtils;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.gs.collections.impl.list.mutable.FastList;

import kafka.admin.AdminUtils;
import kafka.api.OffsetRequest;
import kafka.serializer.Encoder;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import kafka.utils.IntEncoder;
import kafka.utils.SystemTime$;
import kafka.utils.TestUtils;
import kafka.utils.TestZKUtils;
import kafka.utils.Utils;
import kafka.utils.ZKStringSerializer$;
import kafka.zk.EmbeddedZookeeper;

/**
 * @author Artem Bilan
 * @since 1.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class KafkaTests {

	private static final String TEST_TOPIC = "test-topic";

	private static final String TEST_TOPIC2 = "test-topic2";

	@Autowired
	@Qualifier("sendToKafkaFlow.input")
	private MessageChannel sendToKafkaFlowInput;

	@Autowired
	private PollableChannel listeningFromKafkaResults;

	@Autowired
	private PollableChannel pollKafkaResults;


	@Test
	public void testKafkaListeningAdapter() {
		this.sendToKafkaFlowInput.send(new GenericMessage<>("foo"));
		for (int i = 0; i < 100; i++) {
			Message<?> receive = this.listeningFromKafkaResults.receive(10000);
			assertNotNull(receive);
			assertEquals("FOO", receive.getPayload());
			MessageHeaders headers = receive.getHeaders();
			assertTrue(headers.containsKey(KafkaHeaders.ACKNOWLEDGMENT));
			Acknowledgment acknowledgment = headers.get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment.class);
			acknowledgment.acknowledge();
			assertEquals(TEST_TOPIC, headers.get(KafkaHeaders.TOPIC));
			assertEquals(i + 1, headers.get(KafkaHeaders.MESSAGE_KEY));
			assertEquals((long) (i + 1), headers.get(KafkaHeaders.NEXT_OFFSET));
		}
		assertNull(this.pollKafkaResults.receive(10));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testKafkaPollingAdapter() {
		Message<String> message = MessageBuilder.withPayload("BAR").setHeader(KafkaHeaders.TOPIC, TEST_TOPIC2).build();
		this.sendToKafkaFlowInput.send(message);
		for (int i = 0; i < 10; i++) {
			Message<?> receive = this.pollKafkaResults.receive(10000);
			assertNotNull(receive);
			assertThat(receive.getPayload(), instanceOf(List.class));
			List<String> messages = (List<String>) receive.getPayload();
			assertEquals(10, messages.size());
			messages.forEach(m -> assertEquals("bar", m));
		}
		assertNull(this.listeningFromKafkaResults.receive(10));
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration implements DisposableBean {

		@Bean
		public EmbeddedZookeeper zookeeper() {
			return new EmbeddedZookeeper(TestZKUtils.zookeeperConnect());
		}

		@Bean(destroyMethod = "close")
		public ZkClient zookeeperClient(EmbeddedZookeeper zookeeper) {
			return new ZkClient(zookeeper.connectString(), 6000, 6000, ZKStringSerializer$.MODULE$);
		}

		@Bean
		@DependsOn("zookeeper")
		public KafkaServer kafkaServer() {
			Properties brokerConfigProperties = TestUtils.createBrokerConfig(1, TestUtils.choosePort(), false);
			return TestUtils.createServer(new KafkaConfig(brokerConfigProperties), SystemTime$.MODULE$);
		}

		@Bean
		public String serverAddress(KafkaServer kafkaServer) {
			return kafkaServer.config().hostName() + ":" + kafkaServer.config().port();
		}

		@Bean(destroyMethod = "destroy")
		public InitializingBean topicManager(EmbeddedZookeeper zookeeper, ZkClient zookeeperClient,
				List<KafkaServer> kafkaServers) {
			return new InitializingBean() {

				private final List<String> topics = Arrays.asList(TEST_TOPIC, TEST_TOPIC2);

				@Override
				public void afterPropertiesSet() throws Exception {
					this.topics.forEach(t -> TopicUtils.ensureTopicCreated(zookeeper.connectString(), t, 1, 1));
				}

				public void destroy() {
					this.topics.forEach(t -> {
						AdminUtils.deleteTopic(zookeeperClient, t);
						TestUtils.waitUntilMetadataIsPropagated(asScalaBuffer(kafkaServers), t, 0, 5000L);
					});
				}

			};
		}

		@Bean
		@DependsOn("topicManager")
		public ConnectionFactory connectionFactory(EmbeddedZookeeper zookeeper) {
			return new DefaultConnectionFactory(new ZookeeperConfiguration(zookeeper.connectString()));
		}

		@Bean
		public MetadataStoreOffsetManager offsetManager(ConnectionFactory connectionFactory) {
			MetadataStoreOffsetManager offsetManager = new MetadataStoreOffsetManager(connectionFactory);
			// start reading at the end of the
			offsetManager.setReferenceTimestamp(OffsetRequest.LatestTime());
			return offsetManager;
		}

		@Bean
		public IntegrationFlow listeningFromKafkaFlow(ConnectionFactory connectionFactory,
				MetadataStoreOffsetManager offsetManager) {
			return IntegrationFlows
					.from(Kafka.messageDriverChannelAdapter(connectionFactory, TEST_TOPIC)
							.autoCommitOffset(false)
							.payloadDecoder(String::new)
							.keyDecoder(b -> Integer.valueOf(new String(b)))
							.configureListenerContainer(c ->
									c.offsetManager(offsetManager)
											.maxFetch(100)))
					.<String, String>transform(String::toUpperCase)
					.channel(c -> c.queue("listeningFromKafkaResults"))
					.get();
		}

		@Bean
		public IntegrationFlow sendToKafkaFlow(String serverAddress) {
			return f -> f.<String>split(p -> FastList.newWithNValues(100, () -> p), null)
					.handle(kafkaMessageHandler(serverAddress));
		}

		@SuppressWarnings("unchecked")
		private KafkaProducerMessageHandlerSpec kafkaMessageHandler(String serverAddress) {
			Encoder<?> intEncoder = new IntEncoder(null);
			return Kafka.outboundChannelAdapter(props -> props.put("queue.buffering.max.ms", "15000"))
					.messageKey(m -> m.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER))
					.addProducer(new ProducerMetadata<>(TEST_TOPIC, Integer.class, String.class,
							new EncoderAdaptingSerializer<>((Encoder<Integer>) intEncoder), new
									StringSerializer()),
							serverAddress);
		}

		@Bean
		public IntegrationFlow pollKafkaFlow(EmbeddedZookeeper zookeeper) {
			return IntegrationFlows
					.from(Kafka.inboundChannelAdapter(new ZookeeperConnect(zookeeper.connectString()))
									.consumerProperties(props ->
											props.put("auto.offset.reset", "smallest")
													.put("auto.commit.interval.ms", "100"))
									.addConsumer("myGroup", metadata -> metadata.consumerTimeout(100)
											.topicStreamMap(m -> m.put(TEST_TOPIC2, 1))
											.maxMessages(10)
											.valueDecoder(String::new)),
							e -> e.poller(p -> p.fixedDelay(100)))
					.<Map<String, Map<Integer, List<String>>>, List<String>>transform(p ->
							p.values()
									.iterator()
									.next()
									.values()
									.iterator()
									.next()
									.stream()
									.map(String::toLowerCase)
									.collect(Collectors.toList()))
					.channel(c -> c.queue("pollKafkaResults"))
					.get();
		}

		@Override
		public void destroy() throws Exception {
			try {
				kafkaServer().shutdown();
			}
			catch (Exception e) {
				// do nothing
			}
			try {
				Utils.rm(kafkaServer().config().logDirs());
			}
			catch (Exception e) {
				// do nothing
			}
			try {
				zookeeper().shutdown();
			}
			catch (Exception e) {
				// do nothing
			}
		}

	}

}
