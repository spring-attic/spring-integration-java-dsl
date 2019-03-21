/*
 * Copyright 2014-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.dsl.test.amqp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.channel.AbstractAmqpChannel;
import org.springframework.integration.amqp.inbound.AmqpInboundGateway;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageProducers;
import org.springframework.integration.dsl.amqp.Amqp;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artem Bilan
 * @author Gary Russell
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
@Ignore("Requires RabbitMQ resources. Covered with an appropriate Rule in the version 5.0")
public class AmqpTests {

	@Autowired
	private ConnectionFactory rabbitConnectionFactory;

	@Autowired
	private AmqpTemplate amqpTemplate;

	@Autowired
	@Qualifier("queue")
	private Queue amqpQueue;

	@Autowired
	private AmqpInboundGateway amqpInboundGateway;

	@Test
	public void testAmqpInboundGatewayFlow() throws Exception {
		Object result = this.amqpTemplate.convertSendAndReceive(this.amqpQueue.getName(), "world");
		assertEquals("HELLO WORLD", result);

		this.amqpInboundGateway.stop();
		//INTEXT-209
		this.amqpInboundGateway.start();

		this.amqpTemplate.convertAndSend(this.amqpQueue.getName(), "world");
		((RabbitTemplate) this.amqpTemplate).setReceiveTimeout(10000);
		result = this.amqpTemplate.receiveAndConvert("defaultReplyTo");
		assertEquals("HELLO WORLD", result);
		assertSame(this.amqpTemplate, TestUtils.getPropertyValue(this.amqpInboundGateway, "amqpTemplate"));
	}

	@Autowired
	@Qualifier("amqpOutboundInput")
	private MessageChannel amqpOutboundInput;

	@Autowired
	@Qualifier("amqpReplyChannel.channel")
	private PollableChannel amqpReplyChannel;

	@Autowired
	@Qualifier("fooQueue")
	private Queue fooQueue;

	@Test
	public void testAmqpOutboundFlow() throws Exception {
		this.amqpOutboundInput.send(MessageBuilder.withPayload("hello through the amqp")
				.setHeader("routingKey", this.fooQueue.getName())
				.build());
		Message<?> receive = null;
		int i = 0;
		do {
			receive = this.amqpReplyChannel.receive();
			if (receive != null) {
				break;
			}
			Thread.sleep(100);
			i++;
		}
		while (i < 10);

		assertNotNull(receive);
		assertEquals("HELLO THROUGH THE AMQP", receive.getPayload());
	}

	@Test
	public void testTemplateChannelTransacted() {
		IntegrationFlowBuilder flow = IntegrationFlows.from(Amqp.channel("testTemplateChannelTransacted",
				this.rabbitConnectionFactory)
				.autoStartup(false)
				.templateChannelTransacted(true));
		assertTrue(TestUtils.getPropertyValue(flow, "currentMessageChannel.amqpTemplate.transactional",
				Boolean.class));
	}

	@Autowired
	@Qualifier("amqpAsyncOutboundFlow.input")
	private MessageChannel amqpAsyncOutboundFlowInput;

	@Test
	public void testAmqpAsyncOutboundGatewayFlow() throws Exception {
		QueueChannel replyChannel = new QueueChannel();
		this.amqpAsyncOutboundFlowInput.send(MessageBuilder.withPayload("async gateway")
				.setReplyChannel(replyChannel)
				.build());

		Message<?> receive = replyChannel.receive(10000);
		assertNotNull(receive);
		assertEquals("HELLO ASYNC GATEWAY", receive.getPayload());
	}

	@Autowired
	private AbstractAmqpChannel unitChannel;

	@Autowired
	private AmqpHeaderMapper mapperIn;

	@Autowired
	private AmqpHeaderMapper mapperOut;

	@Test
	public void unitTestChannel() {
		assertEquals(MessageDeliveryMode.NON_PERSISTENT,
				TestUtils.getPropertyValue(this.unitChannel, "defaultDeliveryMode"));
		assertSame(this.mapperIn, TestUtils.getPropertyValue(this.unitChannel, "inboundHeaderMapper"));
		assertSame(this.mapperOut, TestUtils.getPropertyValue(this.unitChannel, "outboundHeaderMapper"));
		assertTrue(TestUtils.getPropertyValue(this.unitChannel, "extractPayload", Boolean.class));
	}

	@Configuration
	@EnableIntegration
	@ImportAutoConfiguration(RabbitAutoConfiguration.class)
	public static class ContextConfiguration {

		@Bean
		public Queue queue() {
			return new AnonymousQueue();
		}

		@Bean
		public Queue defaultReplyTo() {
			return new Queue("defaultReplyTo");
		}

		@Bean
		public IntegrationFlow amqpFlow(ConnectionFactory rabbitConnectionFactory, AmqpTemplate amqpTemplate) {
			return IntegrationFlows
					.from(Amqp.inboundGateway(rabbitConnectionFactory, amqpTemplate, queue())
							.id("amqpInboundGateway")
							.defaultReplyTo(defaultReplyTo().getName()))
					.transform("hello "::concat)
					.transform(String.class, String::toUpperCase)
					.get();
		}

		@Bean
		public IntegrationFlow amqpOutboundFlow(ConnectionFactory rabbitConnectionFactory, AmqpTemplate amqpTemplate) {
			return IntegrationFlows.from(Amqp.channel("amqpOutboundInput", rabbitConnectionFactory))
					.handle(Amqp.outboundAdapter(amqpTemplate).routingKeyExpression("headers.routingKey"))
					.get();
		}

		@Bean
		public Queue fooQueue() {
			return new Queue(UUID.randomUUID().toString());
		}

		@Bean
		public Queue amqpReplyChannel() {
			return new Queue("amqpReplyChannel");
		}

		@Bean
		public IntegrationFlow amqpInboundFlow(ConnectionFactory rabbitConnectionFactory) {
			return IntegrationFlows.from((MessageProducers p) -> p.amqp(rabbitConnectionFactory, fooQueue()))
					.transform(String.class, String::toUpperCase)
					.channel(Amqp.pollableChannel(rabbitConnectionFactory)
							.queueName("amqpReplyChannel")
							.channelTransacted(true))
					.get();
		}

		@Bean
		public Queue asyncReplies() {
			return new Queue("asyncReplies");
		}

		@Bean
		public AsyncRabbitTemplate asyncRabbitTemplate(ConnectionFactory rabbitConnectionFactory) {
			return new AsyncRabbitTemplate(rabbitConnectionFactory, "", "", "asyncReplies");
		}

		@Bean
		public IntegrationFlow amqpAsyncOutboundFlow(AsyncRabbitTemplate asyncRabbitTemplate) {
			return f -> f
					.handle(Amqp.asyncOutboundGateway(asyncRabbitTemplate)
							.routingKeyFunction(m -> queue().getName()));
		}

		@Bean
		public AbstractAmqpChannel unitChannel(ConnectionFactory rabbitConnectionFactory) {
			return Amqp.pollableChannel(rabbitConnectionFactory)
					.queueName(fooQueue().getName())
					.channelTransacted(true)
					.extractPayload(true)
					.inboundHeaderMapper(mapperIn())
					.outboundHeaderMapper(mapperOut())
					.defaultDeliveryMode(MessageDeliveryMode.NON_PERSISTENT)
					.get();
		}

		@Bean
		public AmqpHeaderMapper mapperIn() {
			return DefaultAmqpHeaderMapper.inboundMapper();
		}

		@Bean
		public AmqpHeaderMapper mapperOut() {
			return DefaultAmqpHeaderMapper.outboundMapper();
		}

	}

}
