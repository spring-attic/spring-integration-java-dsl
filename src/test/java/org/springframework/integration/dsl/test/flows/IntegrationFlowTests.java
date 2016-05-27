/*
 * Copyright 2014-2015 the original author or authors.
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

package org.springframework.integration.dsl.test.flows;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.MessageDispatchingException;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnableMessageHistory;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.channel.DirectChannelSpec;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.dsl.core.Pollers;
import org.springframework.integration.dsl.support.GenericHandler;
import org.springframework.integration.event.core.MessagingEvent;
import org.springframework.integration.event.outbound.ApplicationEventPublishingMessageHandler;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.advice.ExpressionEvaluatingRequestHandlerAdvice;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.MutableMessageBuilder;
import org.springframework.integration.transformer.PayloadDeserializingTransformer;
import org.springframework.integration.transformer.PayloadSerializingTransformer;
import org.springframework.integration.xml.transformer.support.XPathExpressionEvaluatingHeaderValueMessageProcessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artem Bilan
 * @author Tim Ysewyn
 * @author Gary Russell
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class IntegrationFlowTests {

	@Autowired
	private ListableBeanFactory beanFactory;

	@Autowired
	private ControlBusGateway controlBus;

	@Autowired
	@Qualifier("inputChannel")
	private MessageChannel inputChannel;

	@Autowired
	@Qualifier("discardChannel")
	private PollableChannel discardChannel;

	@Autowired
	@Qualifier("foo")
	private SubscribableChannel foo;

	@Autowired
	@Qualifier("successChannel")
	private PollableChannel successChannel;

	@Autowired
	@Qualifier("flow3Input")
	private MessageChannel flow3Input;

	@Autowired
	private AtomicReference<Object> eventHolder;

	@Autowired
	@Qualifier("bridgeFlowInput")
	private PollableChannel bridgeFlowInput;

	@Autowired
	@Qualifier("bridgeFlowOutput")
	private PollableChannel bridgeFlowOutput;

	@Autowired
	@Qualifier("bridgeFlow2Input")
	private MessageChannel bridgeFlow2Input;

	@Autowired
	@Qualifier("bridgeFlow2Output")
	private PollableChannel bridgeFlow2Output;

	@Autowired
	@Qualifier("methodInvokingInput")
	private MessageChannel methodInvokingInput;

	@Autowired
	@Qualifier("delayedAdvice")
	private DelayedAdvice delayedAdvice;

	@Autowired
	@Qualifier("enricherInput")
	private FixedSubscriberChannel enricherInput;

	@Autowired
	@Qualifier("enricherInput2")
	private FixedSubscriberChannel enricherInput2;

	@Autowired
	@Qualifier("enricherInput3")
	private FixedSubscriberChannel enricherInput3;

	@Autowired
	@Qualifier("splitResequenceFlow.input")
	private MessageChannel splitInput;

	@Autowired
	@Qualifier("xpathHeaderEnricherInput")
	private MessageChannel xpathHeaderEnricherInput;

	@Autowired
	@Qualifier("splitAggregateInput")
	private MessageChannel splitAggregateInput;

	@Autowired
	private MessageStore messageStore;

	@Autowired
	@Qualifier("claimCheckInput")
	private MessageChannel claimCheckInput;

	@Autowired
	@Qualifier("lamdasInput")
	private MessageChannel lamdasInput;

	@Autowired
	@Qualifier("gatewayInput")
	private MessageChannel gatewayInput;

	@Autowired
	@Qualifier("gatewayError")
	private PollableChannel gatewayError;

	@Test
	public void testDirectFlow() {
		assertTrue(this.beanFactory.containsBean("filter"));
		assertTrue(this.beanFactory.containsBean("filter.handler"));
		assertTrue(this.beanFactory.containsBean("expressionFilter"));
		assertTrue(this.beanFactory.containsBean("expressionFilter.handler"));
		QueueChannel replyChannel = new QueueChannel();
		Message<String> message = MessageBuilder.withPayload("100").setReplyChannel(replyChannel).build();
		try {
			this.inputChannel.send(message);
			fail("Expected MessageDispatchingException");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(MessageDeliveryException.class));
			assertThat(e.getCause(), instanceOf(MessageDispatchingException.class));
			assertThat(e.getMessage(), containsString("Dispatcher has no subscribers"));
		}
		this.controlBus.send("@payloadSerializingTransformer.start()");

		final AtomicBoolean used = new AtomicBoolean();

		this.foo.subscribe(m -> used.set(true));

		this.inputChannel.send(message);
		Message<?> reply = replyChannel.receive(5000);
		assertNotNull(reply);
		assertEquals(200, reply.getPayload());

		Message<?> successMessage = this.successChannel.receive(5000);
		assertNotNull(successMessage);
		assertEquals(100, successMessage.getPayload());

		assertTrue(used.get());

		this.inputChannel.send(new GenericMessage<Object>(1000));
		Message<?> discarded = this.discardChannel.receive(5000);
		assertNotNull(discarded);
		assertEquals("Discarded: 1000", discarded.getPayload());
	}

	@Test
	public void testHandle() {
		assertNull(this.eventHolder.get());
		this.flow3Input.send(new GenericMessage<>("2"));
		assertNotNull(this.eventHolder.get());
		assertEquals(4, this.eventHolder.get());
	}

	@Test
	public void testBridge() {
		GenericMessage<String> message = new GenericMessage<>("test");
		this.bridgeFlowInput.send(message);
		Message<?> reply = this.bridgeFlowOutput.receive(5000);
		assertNotNull(reply);
		assertEquals("test", reply.getPayload());

		assertTrue(this.beanFactory.containsBean("bridgeFlow2.channel#0"));
		assertThat(this.beanFactory.getBean("bridgeFlow2.channel#0"), instanceOf(FixedSubscriberChannel.class));

		try {
			this.bridgeFlow2Input.send(message);
			fail("Expected MessageDispatchingException");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(MessageDeliveryException.class));
			assertThat(e.getCause(), instanceOf(MessageDispatchingException.class));
			assertThat(e.getMessage(), containsString("Dispatcher has no subscribers"));
		}
		this.controlBus.send("@bridge.start()");
		this.bridgeFlow2Input.send(message);
		reply = this.bridgeFlow2Output.receive(5000);
		assertNotNull(reply);
		assertEquals("test", reply.getPayload());
		assertTrue(this.delayedAdvice.getInvoked());
	}

	@Autowired
	@Qualifier("routerAsNonLastFlow.input")
	private MessageChannel routerAsNonLastFlowChannel;

	@Autowired
	@Qualifier("routerAsNonLastDefaultOutputChannel")
	private PollableChannel routerAsNonLastDefaultOutputChannel;

	@Test
	public void testRouterAsNonLastComponent() {
		this.routerAsNonLastFlowChannel.send(new GenericMessage<>("Hello World"));
		Message<?> receive = this.routerAsNonLastDefaultOutputChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("Hello World", receive.getPayload());
	}

	@Test
	public void testWrongLastMessageChannel() {
		ConfigurableApplicationContext context = null;
		try {
			context = new AnnotationConfigApplicationContext(InvalidLastMessageChannelFlowContext.class);
			fail("BeanCreationException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(BeanCreationException.class));
			assertThat(e.getMessage(), containsString("'.fixedSubscriberChannel()' " +
					"can't be the last EIP-method in the IntegrationFlow definition"));
		}
		finally {
			if (context != null) {
				context.close();
			}
		}
	}

	@Test
	public void testMethodInvokingMessageHandler() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("world")
				.setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel)
				.build();
		this.methodInvokingInput.send(message);
		Message<?> receive = replyChannel.receive(5000);
		assertNotNull(receive);
		assertEquals("Hello World and world", receive.getPayload());
	}

	@Test
	public void testLambdas() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("World")
				.setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel)
				.build();
		this.lamdasInput.send(message);
		Message<?> receive = replyChannel.receive(5000);
		assertNotNull(receive);
		assertEquals("Hello World", receive.getPayload());

		message = MessageBuilder.withPayload("Spring")
				.setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel)
				.build();

		this.lamdasInput.send(message);
		assertNull(replyChannel.receive(10));

	}

	@Test
	public void testContentEnricher() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload(new TestPojo("Bar"))
				.setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel)
				.build();
		this.enricherInput.send(message);
		Message<?> receive = replyChannel.receive(5000);
		assertNotNull(receive);
		assertEquals("Bar Bar", receive.getHeaders().get("foo"));
		Object payload = receive.getPayload();
		assertThat(payload, instanceOf(TestPojo.class));
		TestPojo result = (TestPojo) payload;
		assertEquals("Bar Bar", result.getName());
		assertNotNull(result.getDate());
		assertThat(new Date(), Matchers.greaterThanOrEqualTo(result.getDate()));
	}

	@Test
	public void testContentEnricher2() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload(new TestPojo("Bar"))
				.setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel)
				.build();
		this.enricherInput2.send(message);
		Message<?> receive = replyChannel.receive(5000);
		assertNotNull(receive);
		assertNull(receive.getHeaders().get("foo"));
		Object payload = receive.getPayload();
		assertThat(payload, instanceOf(TestPojo.class));
		TestPojo result = (TestPojo) payload;
		assertEquals("Bar Bar", result.getName());
		assertNotNull(result.getDate());
		assertThat(new Date(), Matchers.greaterThanOrEqualTo(result.getDate()));
	}

	@Test
	public void testContentEnricher3() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload(new TestPojo("Bar"))
				.setHeader(MessageHeaders.REPLY_CHANNEL, replyChannel)
				.build();
		this.enricherInput3.send(message);
		Message<?> receive = replyChannel.receive(5000);
		assertNotNull(receive);
		assertEquals("Bar Bar", receive.getHeaders().get("foo"));
		Object payload = receive.getPayload();
		assertThat(payload, instanceOf(TestPojo.class));
		TestPojo result = (TestPojo) payload;
		assertEquals("Bar", result.getName());
		assertNull(result.getDate());
	}

	@Test
	public void testSplitterResequencer() {
		QueueChannel replyChannel = new QueueChannel();

		this.splitInput.send(MessageBuilder.withPayload("")
				.setReplyChannel(replyChannel)
				.setHeader("foo", "bar")
				.build());

		for (int i = 0; i < 12; i++) {
			Message<?> receive = replyChannel.receive(2000);
			assertNotNull(receive);
			assertFalse(receive.getHeaders().containsKey("foo"));
			assertTrue(receive.getHeaders().containsKey("FOO"));
			assertEquals("BAR", receive.getHeaders().get("FOO"));
			assertEquals(i + 1, receive.getPayload());
		}
	}

	@Test
	public void testSplitterAggregator() {
		List<Character> payload = Arrays.asList('a', 'b', 'c', 'd', 'e');

		QueueChannel replyChannel = new QueueChannel();
		this.splitAggregateInput.send(MessageBuilder.withPayload(payload)
				.setReplyChannel(replyChannel)
				.build());

		Message<?> receive = replyChannel.receive(2000);
		assertNotNull(receive);
		assertThat(receive.getPayload(), instanceOf(List.class));
		@SuppressWarnings("unchecked")
		List<Object> result = (List<Object>) receive.getPayload();
		for (int i = 0; i < payload.size(); i++) {
			assertEquals(payload.get(i), result.get(i));
		}
	}

	@Test
	public void testHeaderEnricher() {
		QueueChannel replyChannel = new QueueChannel();

		Message<String> message =
				MessageBuilder.withPayload("<root><elementOne>1</elementOne><elementTwo>2</elementTwo></root>")
						.setReplyChannel(replyChannel)
						.build();

		try {
			this.xpathHeaderEnricherInput.send(message);
			fail("Expected MessageDispatchingException");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(MessageDeliveryException.class));
			assertThat(e.getCause(), instanceOf(MessageDispatchingException.class));
			assertThat(e.getMessage(), containsString("Dispatcher has no subscribers"));
		}

		this.controlBus.send("@xpathHeaderEnricher.start()");
		this.xpathHeaderEnricherInput.send(message);

		Message<?> result = replyChannel.receive(2000);
		assertNotNull(result);
		MessageHeaders headers = result.getHeaders();
		assertEquals("1", headers.get("one"));
		assertEquals("2", headers.get("two"));
		assertThat(headers.getReplyChannel(), instanceOf(String.class));
	}


	@Autowired
	@Qualifier("recipientListOrderFlow.input")
	private MessageChannel recipientListOrderFlowInput;

	@Autowired
	@Qualifier("recipientListOrderResult")
	private PollableChannel recipientListOrderResult;

	@Test
	@SuppressWarnings("unchecked")
	public void testRecipientListRouterOrder() {
		this.recipientListOrderFlowInput.send(new GenericMessage<>(new AtomicReference<>("")));
		Message<?> receive = this.recipientListOrderResult.receive(10000);
		assertNotNull(receive);

		AtomicReference<String> result = (AtomicReference<String>) receive.getPayload();
		assertEquals("Hello World", result.get());

		receive = this.recipientListOrderResult.receive(10000);
		assertNotNull(receive);
		result = (AtomicReference<String>) receive.getPayload();
		assertEquals("Hello World", result.get());
	}

	@Test
	public void testClaimCheck() {
		QueueChannel replyChannel = new QueueChannel();

		Message<String> message = MutableMessageBuilder.withPayload("foo").setReplyChannel(replyChannel).build();

		this.claimCheckInput.send(message);

		Message<?> receive = replyChannel.receive(2000);
		assertNotNull(receive);
		assertSame(message, receive);

		assertEquals(1, this.messageStore.getMessageCount());
		assertSame(message, this.messageStore.getMessage(message.getHeaders().getId()));
	}

	@Test
	public void testGatewayFlow() throws Exception {
		PollableChannel replyChannel = new QueueChannel();
		Message<String> message = MessageBuilder.withPayload("foo").setReplyChannel(replyChannel).build();

		this.gatewayInput.send(message);

		Message<?> receive = replyChannel.receive(2000);
		assertNotNull(receive);
		assertEquals("From Gateway SubFlow: FOO", receive.getPayload());
		assertNull(this.gatewayError.receive(1));

		message = MessageBuilder.withPayload("bar").setReplyChannel(replyChannel).build();

		this.gatewayInput.send(message);

		receive = replyChannel.receive(1);
		assertNull(receive);

		receive = this.gatewayError.receive(2000);
		assertNotNull(receive);
		assertThat(receive, instanceOf(ErrorMessage.class));
		assertThat(receive.getPayload(), instanceOf(MessageRejectedException.class));
		assertThat(((Exception) receive.getPayload()).getMessage(), containsString("' rejected Message"));
	}

	@Autowired
	private SubscribableChannel tappedChannel1;

	@Autowired
	@Qualifier("wireTapFlow2.input")
	private SubscribableChannel tappedChannel2;

	@Autowired
	@Qualifier("wireTapFlow3.input")
	private SubscribableChannel tappedChannel3;

	@Autowired
	private SubscribableChannel tappedChannel4;

	@Autowired
	@Qualifier("tapChannel")
	private QueueChannel tapChannel;

	@Autowired
	@Qualifier("wireTapFlow5.input")
	private SubscribableChannel tappedChannel5;

	@Autowired
	private PollableChannel wireTapSubflowResult;

	@Test
	public void testWireTap() {
		this.tappedChannel1.send(new GenericMessage<>("foo"));
		this.tappedChannel1.send(new GenericMessage<>("bar"));
		Message<?> out = this.tapChannel.receive(10000);
		assertNotNull(out);
		assertEquals("foo", out.getPayload());
		assertNull(this.tapChannel.receive(0));

		this.tappedChannel2.send(new GenericMessage<>("foo"));
		this.tappedChannel2.send(new GenericMessage<>("bar"));
		out = this.tapChannel.receive(10000);
		assertNotNull(out);
		assertEquals("foo", out.getPayload());
		assertNull(this.tapChannel.receive(0));

		this.tappedChannel3.send(new GenericMessage<>("foo"));
		this.tappedChannel3.send(new GenericMessage<>("bar"));
		out = this.tapChannel.receive(10000);
		assertNotNull(out);
		assertEquals("foo", out.getPayload());
		assertNull(this.tapChannel.receive(0));

		this.tappedChannel4.send(new GenericMessage<>("foo"));
		this.tappedChannel4.send(new GenericMessage<>("bar"));
		out = this.tapChannel.receive(10000);
		assertNotNull(out);
		assertEquals("foo", out.getPayload());
		out = this.tapChannel.receive(10000);
		assertNotNull(out);
		assertEquals("bar", out.getPayload());

		this.tappedChannel5.send(new GenericMessage<>("foo"));
		out = this.wireTapSubflowResult.receive(10000);
		assertNotNull(out);
		assertEquals("FOO", out.getPayload());
	}

	@Autowired
	@Qualifier("subscribersFlow.input")
	private MessageChannel subscribersFlowInput;

	@Autowired
	@Qualifier("subscriber1Results")
	private PollableChannel subscriber1Results;

	@Autowired
	@Qualifier("subscriber2Results")
	private PollableChannel subscriber2Results;

	@Autowired
	@Qualifier("subscriber3Results")
	private PollableChannel subscriber3Results;

	@Test
	public void testSubscribersSubFlows() {
		this.subscribersFlowInput.send(new GenericMessage<>(2));

		Message<?> receive1 = this.subscriber1Results.receive(5000);
		assertNotNull(receive1);
		assertEquals(1, receive1.getPayload());

		Message<?> receive2 = this.subscriber2Results.receive(5000);
		assertNotNull(receive2);
		assertEquals(4, receive2.getPayload());
		Message<?> receive3 = this.subscriber3Results.receive(5000);
		assertNotNull(receive3);
		assertEquals(6, receive3.getPayload());
	}


	@Autowired
	@Qualifier("publishSubscribeFlow.input")
	private MessageChannel subscriberAggregateFlowInput;

	@Autowired
	private PollableChannel subscriberAggregateResult;

	@Test
	public void testSubscriberAggregateFlow() {
		this.subscriberAggregateFlowInput.send(new GenericMessage<>("test"));

		Message<?> receive1 = this.subscriberAggregateResult.receive(10000);
		assertNotNull(receive1);
		assertEquals("Hello World!", receive1.getPayload());
	}


	@MessagingGateway(defaultRequestChannel = "controlBus")
	private interface ControlBusGateway {

		void send(String command);
	}

	@Configuration
	@EnableIntegration
	@IntegrationComponentScan
	@EnableMessageHistory({ "recipientListOrder*", "recipient1*", "recipient2*" })
	public static class ContextConfiguration {

		@Bean
		public IntegrationFlow controlBusFlow() {
			return IntegrationFlows.from("controlBus").controlBus().get();
		}

		@Bean(name = PollerMetadata.DEFAULT_POLLER)
		public PollerMetadata poller() {
			return Pollers.fixedRate(500).get();
		}

		@Bean(name = IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME)
		public TaskScheduler taskScheduler() {
			ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
			threadPoolTaskScheduler.setPoolSize(100);
			return threadPoolTaskScheduler;
		}

		@Bean
		public MessageChannel inputChannel() {
			return MessageChannels.direct().get();
		}

		@Bean
		public MessageChannel foo() {
			return MessageChannels.publishSubscribe().get();
		}

		@Bean
		public IntegrationFlow routerAsNonLastFlow() {
			return f -> f.<String, String>route(p -> p, r -> r.resolutionRequired(false))
					.channel(MessageChannels.queue("routerAsNonLastDefaultOutputChannel"));
		}

	}

	@Configuration
	@ComponentScan
	public static class ContextConfiguration2 {

		@Autowired
		@Qualifier("inputChannel")
		private MessageChannel inputChannel;

		@Autowired
		@Qualifier("successChannel")
		private PollableChannel successChannel;


		@Bean
		public Advice expressionAdvice() {
			ExpressionEvaluatingRequestHandlerAdvice advice = new ExpressionEvaluatingRequestHandlerAdvice();
			advice.setOnSuccessExpression("payload");
			advice.setSuccessChannel(this.successChannel);
			return advice;
		}

		@Bean
		public IntegrationFlow flow2() {
			return IntegrationFlows.from(this.inputChannel)
					.filter(p -> p instanceof String, e -> e
							.id("filter")
							.discardFlow(df -> df
									.transform(String.class, "Discarded: "::concat)
									.channel(c -> c.queue("discardChannel"))))
					.channel("foo")
					.fixedSubscriberChannel()
					.<String, Integer>transform(Integer::parseInt)
					.transform(new PayloadSerializingTransformer(),
							c -> c.autoStartup(false).id("payloadSerializingTransformer"))
					.channel(MessageChannels.queue(new SimpleMessageStore(), "fooQueue"))
					.transform(new PayloadDeserializingTransformer())
					.filter("true", e -> e.id("expressionFilter"))
					.channel(publishSubscribeChannel())
					.transform((Integer p) -> p * 2, c -> c.advice(this.expressionAdvice()))
					.get();
		}

		@Bean
		public MessageChannel publishSubscribeChannel() {
			return MessageChannels.publishSubscribe().get();
		}

		@Bean
		public IntegrationFlow subscribersFlow() {
			return flow -> flow
					.publishSubscribeChannel(Executors.newCachedThreadPool(), s -> s
							.subscribe(f -> f
									.<Integer>handle((p, h) -> p / 2)
									.channel(c -> c.queue("subscriber1Results")))
							.subscribe(f -> f
									.<Integer>handle((p, h) -> p * 2)
									.channel(c -> c.queue("subscriber2Results"))))
					.<Integer>handle((p, h) -> p * 3)
					.channel(c -> c.queue("subscriber3Results"));
		}

		@Bean
		public IntegrationFlow publishSubscribeFlow() {
			return flow -> flow
					.publishSubscribeChannel(s -> s
							.applySequence(true)
							.subscribe(f -> f
									.handle((p, h) -> "Hello")
									.channel("publishSubscribeAggregateFlow.input"))
							.subscribe(f -> f
									.handle((p, h) -> "World!")
									.channel("publishSubscribeAggregateFlow.input"))
					);
		}

		@Bean
		public IntegrationFlow publishSubscribeAggregateFlow() {
			return flow -> flow
					.aggregate(a -> a.outputProcessor(g -> g.getMessages()
							.stream()
							.<String>map(m -> (String) m.getPayload())
							.collect(Collectors.joining(" "))))
					.channel(c -> c.queue("subscriberAggregateResult"));
		}


		@Bean
		public IntegrationFlow wireTapFlow1() {
			return IntegrationFlows.from("tappedChannel1")
					.wireTap("tapChannel", wt -> wt.selector(m -> m.getPayload().equals("foo")))
					.channel("nullChannel")
					.get();
		}

		@Bean
		public IntegrationFlow wireTapFlow2() {
			return f -> f
					.wireTap("tapChannel", wt -> wt.selector(m -> m.getPayload().equals("foo")))
					.channel("nullChannel");
		}

		@Bean
		public IntegrationFlow wireTapFlow3() {
			return f -> f
					.transform("payload")
					.wireTap("tapChannel", wt -> wt.selector("payload == 'foo'"))
					.channel("nullChannel");
		}

		@Bean
		public IntegrationFlow wireTapFlow4() {
			return IntegrationFlows.from("tappedChannel4")
					.wireTap(tapChannel())
					.channel("nullChannel")
					.get();
		}

		@Bean
		public IntegrationFlow wireTapFlow5() {
			return f -> f
					.wireTap(sf -> sf
							.<String, String>transform(String::toUpperCase)
							.channel(c -> c.queue("wireTapSubflowResult")))
					.channel("nullChannel");
		}

		@Bean
		public QueueChannel tapChannel() {
			return new QueueChannel();
		}

	}

	@MessageEndpoint
	public static class AnnotationTestService {

		@ServiceActivator(inputChannel = "publishSubscribeChannel")
		public void handle(Object payload) {
			assertEquals(100, payload);
		}
	}

	@Configuration
	public static class ContextConfiguration3 {

		@Autowired
		@Qualifier("delayedAdvice")
		private MethodInterceptor delayedAdvice;

		@Bean
		public QueueChannel successChannel() {
			return MessageChannels.queue().get();
		}

		@Bean
		public AtomicReference<Object> eventHolder() {
			return new AtomicReference<>();
		}

		@Bean
		public ApplicationListener<MessagingEvent> eventListener() {
			return new ApplicationListener<MessagingEvent>() {

				@Override
				public void onApplicationEvent(MessagingEvent event) {
					eventHolder().set(event.getMessage().getPayload());
				}

			};
		}

		@Bean
		public IntegrationFlow flow3() {
			return IntegrationFlows.from("flow3Input")
					.handle(Integer.class, new GenericHandler<Integer>() {

						public void setFoo(String foo) {
						}

						public void setFoo(Integer foo) {
						}

						@Override
						public Object handle(Integer p, Map<String, Object> h) {
							return p * 2;
						}

					})
					.handle(new ApplicationEventPublishingMessageHandler())
					.get();
		}

		@Bean
		public IntegrationFlow bridgeFlow() {
			return IntegrationFlows.from(MessageChannels.queue("bridgeFlowInput"))
					.channel(MessageChannels.queue("bridgeFlowOutput"))
					.get();
		}

		@Bean
		public IntegrationFlow bridgeFlow2() {
			return IntegrationFlows.from("bridgeFlow2Input")
					.bridge(c -> c.autoStartup(false).id("bridge"))
					.fixedSubscriberChannel()
					.delay("delayer", "200", c -> c.advice(this.delayedAdvice).messageStore(this.messageStore()))
					.channel(c -> c.queue("bridgeFlow2Output"))
					.get();
		}

		@Bean
		public SimpleMessageStore messageStore() {
			return new SimpleMessageStore();
		}

		@Bean
		public IntegrationFlow claimCheckFlow() {
			return IntegrationFlows.from("claimCheckInput")
					.claimCheckIn(this.messageStore())
					.claimCheckOut(this.messageStore())
					.get();
		}

		@Bean
		public IntegrationFlow recipientListOrderFlow() {
			return f -> f
					.routeToRecipients(r -> r
							.recipient("recipient2.input")
							.recipient("recipient1.input"));
		}

		@Bean
		public IntegrationFlow recipient1() {
			return f -> f
					.<AtomicReference<String>>handle((p, h) -> {
						p.set(p.get() + "World");
						return p;
					})
					.channel("recipientListOrderResult");
		}

		@Bean
		public IntegrationFlow recipient2() {
			return f -> f
					.<AtomicReference<String>>handle((p, h) -> {
						p.set(p.get() + "Hello ");
						return p;
					})
					.channel("recipientListOrderResult");
		}

		@Bean
		public PollableChannel recipientListOrderResult() {
			return new QueueChannel();
		}

	}

	@Component("delayedAdvice")
	public static class DelayedAdvice implements MethodInterceptor {

		private final AtomicBoolean invoked = new AtomicBoolean();

		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {
			this.invoked.set(true);
			return invocation.proceed();
		}

		public Boolean getInvoked() {
			return invoked.get();
		}

	}

	@Configuration
	public static class ContextConfiguration4 {

		@Autowired
		@Qualifier("integrationFlowTests.GreetingService")
		private MessageHandler greetingService;

		@Bean
		public IntegrationFlow methodInvokingFlow() {
			return IntegrationFlows.from("methodInvokingInput")
					.handle(this.greetingService)
					.get();
		}

		@Bean
		public IntegrationFlow lamdasFlow() {
			return IntegrationFlows.from("lamdasInput")
					.filter("World"::equals)
					.transform("Hello "::concat)
					.get();
		}

		@Bean
		@DependsOn("enrichFlow")
		public IntegrationFlow enricherFlow() {
			return IntegrationFlows.from("enricherInput", true)
					.enrich(e -> e.requestChannel("enrichChannel")
							.requestPayloadExpression("payload")
							.shouldClonePayload(false)
							.propertyExpression("name", "payload['name']")
							.propertyFunction("date", m -> new Date())
							.headerExpression("foo", "payload['name']")
					)
					.get();
		}

		@Bean
		@DependsOn("enrichFlow")
		public IntegrationFlow enricherFlow2() {
			return IntegrationFlows.from("enricherInput2", true)
					.enrich(e -> e.requestChannel("enrichChannel")
							.requestPayloadExpression("payload")
							.shouldClonePayload(false)
							.propertyExpression("name", "payload['name']")
							.propertyExpression("date", "new java.util.Date()")
					)
					.get();
		}

		@Bean
		@DependsOn("enrichFlow")
		public IntegrationFlow enricherFlow3() {
			return IntegrationFlows.from("enricherInput3", true)
					.enrich(e -> e.requestChannel("enrichChannel")
							.requestPayload(Message::getPayload)
							.shouldClonePayload(false)
							.<Map<String, String>>headerFunction("foo", m -> m.getPayload().get("name")))
					.get();
		}

		@Bean
		public IntegrationFlow enrichFlow() {
			return IntegrationFlows.from("enrichChannel")
					.<TestPojo, Map<?, ?>>transform(p -> Collections.singletonMap("name", p.getName() + " Bar"))
					.get();
		}

		@Bean
		public Executor taskExecutor() {
			return Executors.newCachedThreadPool();
		}

		@Bean
		public TestSplitterPojo testSplitterData() {
			List<String> first = new ArrayList<>();
			first.add("1,2,3");
			first.add("4,5,6");

			List<String> second = new ArrayList<>();
			second.add("7,8,9");
			second.add("10,11,12");

			return new TestSplitterPojo(first, second);
		}

		@Bean
		public IntegrationFlow splitResequenceFlow() {
			return f -> f.enrichHeaders(s -> s.header("FOO", "BAR"))
					.split("testSplitterData", "buildList", c -> c.applySequence(false))
					.channel(c -> c.executor(this.taskExecutor()))
					.split(Message.class, target -> target.getPayload(), c -> c.applySequence(false))
					.channel(MessageChannels.executor(this.taskExecutor()))
					.split(s -> s.applySequence(false).get().getT2().setDelimiters(","))
					.channel(c -> c.executor(this.taskExecutor()))
					.<String, Integer>transform(Integer::parseInt)
					.enrichHeaders(h ->
							h.headerFunction(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, Message::getPayload))
					.resequence(r -> r.releasePartialSequences(true).correlationExpression("'foo'"))
					.headerFilter("foo", false);
		}

		@Bean
		public IntegrationFlow splitAggregateFlow() {
			return IntegrationFlows.from("splitAggregateInput", true)
					.split()
					.channel(MessageChannels.executor(taskExecutor()))
					.resequence()
					.aggregate()
					.get();
		}

		@Bean
		public IntegrationFlow xpathHeaderEnricherFlow() {
			return IntegrationFlows.from("xpathHeaderEnricherInput")
					.enrichHeaders(
							s -> s.header("one",
									new XPathExpressionEvaluatingHeaderValueMessageProcessor("/root/elementOne"))
									.header("two",
											new XPathExpressionEvaluatingHeaderValueMessageProcessor("/root/elementTwo"))
									.headerChannelsToString(),
							c -> c.autoStartup(false).id("xpathHeaderEnricher")
					)
					.get();
		}

		@Bean
		public IntegrationFlow gatewayFlow() {
			return IntegrationFlows.from("gatewayInput")
					.gateway("gatewayRequest", g -> g.errorChannel("gatewayError").replyTimeout(10L))
					.gateway(f -> f.transform("From Gateway SubFlow: "::concat))
					.get();
		}

		@Bean
		public IntegrationFlow gatewayRequestFlow() {
			return IntegrationFlows.from("gatewayRequest")
					.filter("foo"::equals, f -> f.throwExceptionOnRejection(true))
					.<String, String>transform(String::toUpperCase)
					.get();
		}

		@Bean
		public MessageChannel gatewayError() {
			return MessageChannels.queue().get();
		}

	}

	@Service
	public static class GreetingService extends AbstractReplyProducingMessageHandler {

		@Autowired
		private WorldService worldService;

		@Override
		protected Object handleRequestMessage(Message<?> requestMessage) {
			return "Hello " + this.worldService.world() + " and " + requestMessage.getPayload();
		}
	}

	@Service
	public static class WorldService {

		public String world() {
			return "World";
		}
	}


	private static class InvalidLastMessageChannelFlowContext {

		@Bean
		public IntegrationFlow wrongLastComponent() {
			return IntegrationFlows.from(MessageChannels.direct())
					.fixedSubscriberChannel()
					.get();
		}

	}

	private static class TestPojo {

		private String name;

		private Date date;

		private TestPojo(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Date getDate() {
			return date;
		}

		public void setDate(Date date) {
			this.date = date;
		}

	}

	private static class TestSplitterPojo {

		final List<String> first;

		final List<String> second;

		private TestSplitterPojo(List<String> first, List<String> second) {
			this.first = first;
			this.second = second;
		}

		public List<String> getFirst() {
			return first;
		}

		public List<String> getSecond() {
			return second;
		}

		public List<List<String>> buildList() {
			return Arrays.asList(this.first, this.second);
		}

	}

}

