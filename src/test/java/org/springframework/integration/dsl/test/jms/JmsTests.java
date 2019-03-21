/*
 * Copyright 2014-2017 the original author or authors.
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

package org.springframework.integration.dsl.test.jms;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.channel.ChannelInterceptorAware;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageSources;
import org.springframework.integration.dsl.MessagingGateways;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.dsl.core.Pollers;
import org.springframework.integration.dsl.jms.Jms;
import org.springframework.integration.endpoint.MethodInvokingMessageSource;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @author Nasko Vasilev
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class JmsTests {

	@Autowired
	private ListableBeanFactory beanFactory;

	@Autowired
	private ControlBusGateway controlBus;

	@Autowired
	@Qualifier("flow1QueueChannel")
	private PollableChannel outputChannel;

	@Autowired
	@Qualifier("jmsOutboundFlow.input")
	private MessageChannel jmsOutboundInboundChannel;

	@Autowired
	@Qualifier("jmsOutboundInboundReplyChannel")
	private PollableChannel jmsOutboundInboundReplyChannel;

	@Autowired
	@Qualifier("jmsOutboundGatewayFlow.input")
	private MessageChannel jmsOutboundGatewayChannel;

	@Autowired
	private TestChannelInterceptor testChannelInterceptor;

	@Autowired
	private ConnectionFactory jmsConnectionFactory;

	@Autowired
	private PollableChannel jmsPubSubBridgeChannel;

	@Autowired
	@Qualifier("jmsOutboundGateway.handler")
	private MessageHandler jmsOutboundGatewayHandler;

	@Autowired
	private AtomicBoolean jmsMessageDrivenChannelCalled;

	@Autowired
	private AtomicBoolean jmsInboundGatewayChannelCalled;

	@Test
	public void testPollingFlow() {
		this.controlBus.send("@'jmsTests.ContextConfiguration.integerMessageSource.inboundChannelAdapter'.start()");
		assertThat(this.beanFactory.getBean("integerChannel"), instanceOf(FixedSubscriberChannel.class));
		for (int i = 0; i < 5; i++) {
			Message<?> message = this.outputChannel.receive(20000);
			assertNotNull(message);
			assertEquals("" + i, message.getPayload());
		}
		this.controlBus.send("@'jmsTests.ContextConfiguration.integerMessageSource.inboundChannelAdapter'.stop()");

		assertTrue(((ChannelInterceptorAware) this.outputChannel).getChannelInterceptors()
				.contains(this.testChannelInterceptor));
		assertThat(this.testChannelInterceptor.invoked.get(), Matchers.greaterThanOrEqualTo(5));

	}

	@Test
	public void testJmsOutboundInboundFlow() {
		this.jmsOutboundInboundChannel.send(MessageBuilder.withPayload("hello THROUGH the JMS")
				.setHeader(SimpMessageHeaderAccessor.DESTINATION_HEADER, "jmsInbound")
				.build());

		Message<?> receive = this.jmsOutboundInboundReplyChannel.receive(10000);

		assertNotNull(receive);
		assertEquals("HELLO THROUGH THE JMS", receive.getPayload());

		this.jmsOutboundInboundChannel.send(MessageBuilder.withPayload("hello THROUGH the JMS")
				.setHeader(SimpMessageHeaderAccessor.DESTINATION_HEADER, "jmsMessageDriver")
				.build());

		receive = this.jmsOutboundInboundReplyChannel.receive(10000);

		assertNotNull(receive);
		assertEquals("hello through the jms", receive.getPayload());

		assertTrue(this.jmsMessageDrivenChannelCalled.get());

		this.jmsOutboundInboundChannel.send(MessageBuilder.withPayload("    foo    ")
				.setHeader(SimpMessageHeaderAccessor.DESTINATION_HEADER, "containerSpecDestination")
				.build());

		receive = this.jmsOutboundInboundReplyChannel.receive(10000);

		assertNotNull(receive);
		assertEquals("foo", receive.getPayload());
	}

	@Test
	public void testJmsPipelineFlow() {
		assertEquals(new Long(10000),
				TestUtils.getPropertyValue(this.jmsOutboundGatewayHandler, "idleReplyContainerTimeout", Long.class));
		PollableChannel replyChannel = new QueueChannel();
		Message<String> message = MessageBuilder.withPayload("hello through the jms pipeline")
				.setReplyChannel(replyChannel)
				.setHeader("destination", "jmsPipelineTest")
				.build();
		this.jmsOutboundGatewayChannel.send(message);

		Message<?> receive = replyChannel.receive(5000);

		assertNotNull(receive);
		assertEquals("HELLO THROUGH THE JMS PIPELINE", receive.getPayload());

		assertTrue(this.jmsInboundGatewayChannelCalled.get());
	}

	@Test
	public void testPubSubFlow() {
		JmsTemplate template = new JmsTemplate(this.jmsConnectionFactory);
		template.setPubSubDomain(true);
		template.setDefaultDestinationName("pubsub");
		template.convertAndSend("foo");
		Message<?> received = this.jmsPubSubBridgeChannel.receive(5000);
		assertNotNull(received);
		assertEquals("foo", received.getPayload());
	}

	@Autowired
	private CountDownLatch redeliveryLatch;

	@Test
	public void testJmsRedeliveryFlow() throws InterruptedException {
		this.jmsOutboundInboundChannel.send(MessageBuilder.withPayload("foo")
				.setHeader(SimpMessageHeaderAccessor.DESTINATION_HEADER, "jmsMessageDriverRedelivery")
				.build());

		assertTrue(this.redeliveryLatch.await(10, TimeUnit.SECONDS));
	}

	@MessagingGateway(defaultRequestChannel = "controlBus.input")
	private interface ControlBusGateway {

		void send(String command);

	}

	@Configuration
	@ImportAutoConfiguration({ ActiveMQAutoConfiguration.class, JmxAutoConfiguration.class, IntegrationAutoConfiguration.class })
	@IntegrationComponentScan
	@ComponentScan
	public static class ContextConfiguration {

		@Autowired
		private ConnectionFactory jmsConnectionFactory;

		@PostConstruct
		public void init() {
			((ActiveMQConnectionFactory) this.jmsConnectionFactory).setTrustAllPackages(true);
		}

		@Bean(name = PollerMetadata.DEFAULT_POLLER)
		public PollerMetadata poller() {
			return Pollers.fixedRate(500).get();
		}

		@Bean
		public IntegrationFlow controlBus() {
			return IntegrationFlowDefinition::controlBus;
		}

		@Bean
		@InboundChannelAdapter(value = "flow1.input", autoStartup = "false", poller = @Poller(fixedRate = "100"))
		public MessageSource<?> integerMessageSource() {
			MethodInvokingMessageSource source = new MethodInvokingMessageSource();
			source.setObject(new AtomicInteger());
			source.setMethodName("getAndIncrement");
			return source;
		}

		@Bean
		public IntegrationFlow flow1() {
			return f -> f
					.fixedSubscriberChannel("integerChannel")
					.transform("payload.toString()")
					.channel(Jms.pollableChannel("flow1QueueChannel", this.jmsConnectionFactory)
							.destination("flow1QueueChannel"));
		}

		@Bean
		public IntegrationFlow jmsOutboundFlow() {
			return f -> f.handleWithAdapter(h -> h.jms(this.jmsConnectionFactory)
					.destinationExpression("headers." + SimpMessageHeaderAccessor.DESTINATION_HEADER));
		}

		@Bean
		public MessageChannel jmsOutboundInboundReplyChannel() {
			return MessageChannels.queue().get();
		}

		@Bean
		public IntegrationFlow jmsInboundFlow() {
			return IntegrationFlows
					.from((MessageSources s) -> s.jms(this.jmsConnectionFactory).destination("jmsInbound"))
					.<String, String>transform(String::toUpperCase)
					.channel(this.jmsOutboundInboundReplyChannel())
					.get();
		}

		@Bean
		public IntegrationFlow pubSubFlow() {
			return IntegrationFlows
					.from(Jms.publishSubscribeChannel(this.jmsConnectionFactory)
							.destination("pubsub"))
					.channel(c -> c.queue("jmsPubSubBridgeChannel"))
					.get();
		}

		@Bean
		public IntegrationFlow jmsMessageDrivenFlow() {
			return IntegrationFlows
					.from(Jms.messageDrivenChannelAdapter(this.jmsConnectionFactory)
							.outputChannel(jmsMessageDriverInputChannel())
							.destination("jmsMessageDriver"))
					.<String, String>transform(String::toLowerCase)
					.channel(jmsOutboundInboundReplyChannel())
					.get();
		}

		@Bean
		public AtomicBoolean jmsMessageDrivenChannelCalled() {
			return new AtomicBoolean();
		}

		@Bean
		public MessageChannel jmsMessageDriverInputChannel() {
			DirectChannel directChannel = new DirectChannel();
			directChannel.addInterceptor(new ChannelInterceptorAdapter() {

				@Override
				public Message<?> preSend(Message<?> message, MessageChannel channel) {
					jmsMessageDrivenChannelCalled().set(true);
					return super.preSend(message, channel);
				}

			});
			return directChannel;
		}

		@Bean
		public IntegrationFlow jmsMessageDrivenFlowWithContainer() {
			return IntegrationFlows
					.from(Jms.messageDrivenChannelAdapter(
							Jms.container(this.jmsConnectionFactory, "containerSpecDestination")
									.pubSubDomain(false)
									.taskExecutor(Executors.newCachedThreadPool())
									.get()))
					.transform(String::trim)
					.channel(jmsOutboundInboundReplyChannel())
					.get();
		}

		@Bean
		public IntegrationFlow jmsOutboundGatewayFlow() {
			return f -> f.handleWithAdapter(a ->
							a.jmsGateway(this.jmsConnectionFactory)
									.replyContainer(c -> c.idleReplyContainerTimeout(10))
									.requestDestination("jmsPipelineTest"),
					e -> e.id("jmsOutboundGateway"));
		}

		@Bean
		public IntegrationFlow jmsInboundGatewayFlow() {
			return IntegrationFlows.from((MessagingGateways g) ->
					g.jms(this.jmsConnectionFactory)
							.requestChannel(jmsInboundGatewayInputChannel())
							.destination("jmsPipelineTest"))
					.<String, String>transform(String::toUpperCase)
					.get();
		}

		@Bean
		public AtomicBoolean jmsInboundGatewayChannelCalled() {
			return new AtomicBoolean();
		}

		@Bean
		public MessageChannel jmsInboundGatewayInputChannel() {
			DirectChannel directChannel = new DirectChannel();
			directChannel.addInterceptor(new ChannelInterceptorAdapter() {

				@Override
				public Message<?> preSend(Message<?> message, MessageChannel channel) {
					jmsInboundGatewayChannelCalled().set(true);
					return super.preSend(message, channel);
				}

			});
			return directChannel;
		}

		@Bean
		public IntegrationFlow jmsMessageDrivenRedeliveryFlow() {
			return IntegrationFlows
					.from(Jms.messageDrivenChannelAdapter(this.jmsConnectionFactory)
							.errorChannel(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME)
							.configureListenerContainer(c -> c.clientId("foo"))
							.destination("jmsMessageDriverRedelivery"))
					.<String, String>transform(p -> {
						throw new RuntimeException("intentional");
					})
					.get();
		}

		@Bean
		public CountDownLatch redeliveryLatch() {
			return new CountDownLatch(3);
		}

		@Bean
		public IntegrationFlow errorHandlingFlow() {
			return IntegrationFlows.from(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME)
					.handle(m -> {
						MessagingException exception = (MessagingException) m.getPayload();
						redeliveryLatch().countDown();
						throw exception;
					})
					.get();
		}

	}

	@Component
	@GlobalChannelInterceptor(patterns = "flow1QueueChannel")
	public static class TestChannelInterceptor extends ChannelInterceptorAdapter {

		private final AtomicInteger invoked = new AtomicInteger();

		@Override
		public Message<?> preSend(Message<?> message, MessageChannel channel) {
			this.invoked.incrementAndGet();
			return message;
		}

	}

}
