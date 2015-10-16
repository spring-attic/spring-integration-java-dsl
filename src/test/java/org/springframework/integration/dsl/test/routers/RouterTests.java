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

package org.springframework.integration.dsl.test.routers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artem Bilan
 * @since 1.0.3
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class RouterTests {

	@Autowired
	private ListableBeanFactory beanFactory;

	@Autowired
	@Qualifier("routerInput")
	private MessageChannel routerInput;

	@Autowired
	@Qualifier("oddChannel")
	private PollableChannel oddChannel;

	@Autowired
	@Qualifier("evenChannel")
	private PollableChannel evenChannel;


	@Test
	public void testRouter() {
		this.beanFactory.containsBean("routeFlow.subFlow#0.channel#0");

		int[] payloads = new int[] {1, 2, 3, 4, 5, 6};

		for (int payload : payloads) {
			this.routerInput.send(new GenericMessage<>(payload));
		}

		for (int i = 0; i < 3; i++) {
			Message<?> receive = this.oddChannel.receive(2000);
			assertNotNull(receive);
			assertEquals(payloads[i * 2] * 3, receive.getPayload());

			receive = this.evenChannel.receive(2000);
			assertNotNull(receive);
			assertEquals(payloads[i * 2 + 1], receive.getPayload());
		}
	}

	@Autowired
	@Qualifier("routerTwoSubFlows.input")
	private MessageChannel routerTwoSubFlowsInput;

	@Autowired
	@Qualifier("routerTwoSubFlowsOutput")
	private PollableChannel routerTwoSubFlowsOutput;

	@Test
	public void testRouterWithTwoSubflows() {
		this.routerTwoSubFlowsInput.send(new GenericMessage<Object>(Arrays.asList(1, 2, 3, 4, 5, 6)));
		Message<?> receive = this.routerTwoSubFlowsOutput.receive(5000);
		assertNotNull(receive);
		Object payload = receive.getPayload();
		assertThat(payload, instanceOf(List.class));
		@SuppressWarnings("unchecked")
		List<Integer> results = (List<Integer>) payload;

		assertArrayEquals(new Integer[] {3, 4, 9, 8, 15, 12}, results.toArray(new Integer[results.size()]));
	}

	@Autowired
	@Qualifier("routeSubflowToReplyChannelFlow.input")
	private MessageChannel routeSubflowToReplyChannelFlowInput;

	@Test
	public void testRouterSubflowWithReplyChannelHeader() {
		PollableChannel replyChannel = new QueueChannel();
		this.routeSubflowToReplyChannelFlowInput.send(
				MessageBuilder.withPayload("baz")
						.setReplyChannel(replyChannel)
						.build());

		Message<?> receive = replyChannel.receive(10000);
		assertNotNull(receive);
		assertEquals("BAZ", receive.getPayload());
	}


	@Autowired
	@Qualifier("routeSubflowWithoutReplyToMainFlow.input")
	private MessageChannel routeSubflowWithoutReplyToMainFlowInput;

	@Autowired
	@Qualifier("routerSubflowResult")
	private PollableChannel routerSubflowResult;

	@Test
	public void testRouterSubflowWithoutReplyToMainFlow() {
		this.routeSubflowWithoutReplyToMainFlowInput.send(new GenericMessage<>("BOO"));

		Message<?> receive = routerSubflowResult.receive(10000);
		assertNotNull(receive);
		assertEquals("boo", receive.getPayload());
		assertNull(this.defaultOutputChannel.receive(1));
		this.routeSubflowWithoutReplyToMainFlowInput.send(new GenericMessage<>("foo"));
		assertNotNull(this.defaultOutputChannel.receive(10000));
	}

	@Autowired
	@Qualifier("recipientListInput")
	private MessageChannel recipientListInput;

	@Autowired
	@Qualifier("foo-channel")
	private PollableChannel fooChannel;

	@Autowired
	@Qualifier("bar-channel")
	private PollableChannel barChannel;


	@Autowired
	@Qualifier("recipientListSubFlow1Result")
	private PollableChannel recipientListSubFlow1Result;

	@Autowired
	@Qualifier("recipientListSubFlow2Result")
	private PollableChannel recipientListSubFlow2Result;

	@Autowired
	@Qualifier("defaultOutputChannel")
	private PollableChannel defaultOutputChannel;

	@Test
	public void testRecipientListRouter() {

		Message<String> fooMessage = MessageBuilder.withPayload("fooPayload").setHeader("recipient", true).build();
		Message<String> barMessage = MessageBuilder.withPayload("barPayload").setHeader("recipient", true).build();
		Message<String> bazMessage = new GenericMessage<>("baz");
		Message<String> badMessage = new GenericMessage<>("badPayload");

		this.recipientListInput.send(fooMessage);
		Message<?> result1a = this.fooChannel.receive(2000);
		assertNotNull(result1a);
		assertEquals("foo", result1a.getPayload());
		Message<?> result1b = this.barChannel.receive(2000);
		assertNotNull(result1b);
		assertEquals("foo", result1b.getPayload());
		Message<?> result1c = this.recipientListSubFlow1Result.receive(2000);
		assertNotNull(result1c);
		assertEquals("FOO", result1c.getPayload());
		assertNull(this.recipientListSubFlow2Result.receive(0));

		this.recipientListInput.send(barMessage);
		assertNull(this.fooChannel.receive(0));
		assertNull(this.recipientListSubFlow2Result.receive(0));
		Message<?> result2b = this.barChannel.receive(2000);
		assertNotNull(result2b);
		assertEquals("bar", result2b.getPayload());
		Message<?> result2c = this.recipientListSubFlow1Result.receive(2000);
		assertNotNull(result1c);
		assertEquals("BAR", result2c.getPayload());

		this.recipientListInput.send(bazMessage);
		assertNull(this.fooChannel.receive(0));
		assertNull(this.barChannel.receive(0));
		Message<?> result3c = this.recipientListSubFlow1Result.receive(2000);
		assertNotNull(result3c);
		assertEquals("BAZ", result3c.getPayload());
		Message<?> result4c = this.recipientListSubFlow2Result.receive(2000);
		assertNotNull(result4c);
		assertEquals("Hello baz", result4c.getPayload());

		this.recipientListInput.send(badMessage);
		assertNull(this.fooChannel.receive(0));
		assertNull(this.barChannel.receive(0));
		assertNull(this.recipientListSubFlow1Result.receive(0));
		assertNull(this.recipientListSubFlow2Result.receive(0));
		Message<?> resultD = this.defaultOutputChannel.receive(2000);
		assertNotNull(resultD);
		assertEquals("bad", resultD.getPayload());
	}

	@Autowired
	@Qualifier("routerMethodInput")
	private MessageChannel routerMethodInput;

	@Autowired
	@Qualifier("routeMethodInvocationFlow3.input")
	private MessageChannel routerMethod3Input;

	@Autowired
	@Qualifier("routerMultiInput")
	private MessageChannel routerMultiInput;

	@Test
	public void testMethodInvokingRouter() {
		Message<String> fooMessage = new GenericMessage<>("foo");
		Message<String> barMessage = new GenericMessage<>("bar");
		Message<String> badMessage = new GenericMessage<>("bad");

		this.routerMethodInput.send(fooMessage);

		Message<?> result1a = this.fooChannel.receive(2000);
		assertNotNull(result1a);
		assertEquals("foo", result1a.getPayload());
		assertNull(this.barChannel.receive(0));

		this.routerMethodInput.send(barMessage);
		assertNull(this.fooChannel.receive(0));
		Message<?> result2b = this.barChannel.receive(2000);
		assertNotNull(result2b);
		assertEquals("bar", result2b.getPayload());

		try {
			this.routerMethodInput.send(badMessage);
			fail("MessageDeliveryException expected.");
		}
		catch (MessageDeliveryException e) {
			assertThat(e.getMessage(),
					containsString("No channel resolved by router"));
		}

	}

	@Test
	public void testMethodInvokingRouter3() {
		Message<String> fooMessage = new GenericMessage<>("foo");
		Message<String> barMessage = new GenericMessage<>("bar");
		Message<String> badMessage = new GenericMessage<>("bad");

		this.routerMethod3Input.send(fooMessage);

		Message<?> result1a = this.fooChannel.receive(2000);
		assertNotNull(result1a);
		assertEquals("foo", result1a.getPayload());
		assertNull(this.barChannel.receive(0));

		this.routerMethod3Input.send(barMessage);
		assertNull(this.fooChannel.receive(0));
		Message<?> result2b = this.barChannel.receive(2000);
		assertNotNull(result2b);
		assertEquals("bar", result2b.getPayload());

		try {
			this.routerMethod3Input.send(badMessage);
			fail("DestinationResolutionException expected.");
		}
		catch (MessagingException e) {
			assertThat(e.getCause(), instanceOf(DestinationResolutionException.class));
			assertThat(e.getCause().getMessage(),
					containsString("failed to look up MessageChannel with name 'bad-channel'"));
		}
	}

	@Test
	public void testMultiRouter() {

		Message<String> fooMessage = new GenericMessage<>("foo");
		Message<String> barMessage = new GenericMessage<>("bar");
		Message<String> badMessage = new GenericMessage<>("bad");

		this.routerMultiInput.send(fooMessage);
		Message<?> result1a = this.fooChannel.receive(2000);
		assertNotNull(result1a);
		assertEquals("foo", result1a.getPayload());
		Message<?> result1b = this.barChannel.receive(2000);
		assertNotNull(result1b);
		assertEquals("foo", result1b.getPayload());

		this.routerMultiInput.send(barMessage);
		Message<?> result2a = this.fooChannel.receive(2000);
		assertNotNull(result2a);
		assertEquals("bar", result2a.getPayload());
		Message<?> result2b = this.barChannel.receive(2000);
		assertNotNull(result2b);
		assertEquals("bar", result2b.getPayload());

		try {
			this.routerMultiInput.send(badMessage);
			fail("MessageDeliveryException expected.");
		}
		catch (MessageDeliveryException e) {
			assertThat(e.getMessage(),
					containsString("No channel resolved by router"));
		}
	}


	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		public QueueChannel evenChannel() {
			return new QueueChannel();
		}

		@Bean
		public IntegrationFlow routeFlow() {
			return IntegrationFlows.from("routerInput")
					.<Integer, Boolean>route(p -> p % 2 == 0,
							m -> m.channelMapping("true", "evenChannel")
									.subFlowMapping("false", f ->
											f.<Integer>handle((p, h) -> p * 3)))
					.channel(c -> c.queue("oddChannel"))
					.get();
		}

		@Bean
		public IntegrationFlow routeSubflowToReplyChannelFlow() {
			return f -> f
					.<Integer, Boolean>route("true", m -> m
									.subFlowMapping("true", sf -> sf
													.<String>handle((p, h) -> p.toUpperCase())
									)
					);
		}

		@Bean
		public IntegrationFlow routeSubflowWithoutReplyToMainFlow() {
			return f -> f
					.<String, Boolean>route("BOO"::equals, m -> m
							.resolutionRequired(false)
							.subFlowMapping("true", sf -> sf
									.transform(String.class, String::toLowerCase)
									.channel(c -> c.queue("routerSubflowResult"))))
					// Valid: maps to the router's defaultOutputChannel
					.channel("defaultOutputChannel");
		}

		@Bean
		public IntegrationFlow routerTwoSubFlows() {
			return f -> f
					.split()
					.<Integer, Boolean>route(p -> p % 2 == 0, m -> m
							.subFlowMapping("true", sf -> sf.<Integer>handle((p, h) -> p * 2))
							.subFlowMapping("false", sf -> sf.<Integer>handle((p, h) -> p * 3)))
					.aggregate()
					.channel(c -> c.queue("routerTwoSubFlowsOutput"));
		}

		@Bean(name = "foo-channel")
		public MessageChannel fooChannel() {
			return new QueueChannel();
		}

		@Bean(name = "bar-channel")
		public MessageChannel barChannel() {
			return new QueueChannel();
		}

		@Bean
		public MessageChannel defaultOutputChannel() {
			return new QueueChannel();
		}

		@Bean
		public IntegrationFlow recipientListFlow() {
			return IntegrationFlows.from("recipientListInput")
					.<String, String>transform(p -> p.replaceFirst("Payload", ""))
					.routeToRecipients(r -> r
							.recipient("foo-channel", "'foo' == payload")
							.recipient("bar-channel", m ->
									m.getHeaders().containsKey("recipient")
											&& (boolean) m.getHeaders().get("recipient"))
							.recipientFlow("'foo' == payload or 'bar' == payload or 'baz' == payload",
									f -> f.<String, String>transform(String::toUpperCase)
											.channel(c -> c.queue("recipientListSubFlow1Result")))
							.recipientFlow(m -> "baz".equals(m.getPayload()),
									f -> f.transform("Hello "::concat)
											.channel(c -> c.queue("recipientListSubFlow2Result"))))
					.channel("defaultOutputChannel")
					.get();
		}


		@Bean
		public RoutingTestBean routingTestBean() {
			return new RoutingTestBean();
		}

		@Bean
		public IntegrationFlow routeMethodInvocationFlow() {
			return IntegrationFlows.from("routerMethodInput")
					.route("routingTestBean", "routeMessage")
					.get();
		}

		@Bean
		public IntegrationFlow routeMethodInvocationFlow3() {
			return f -> f.route((String p) -> routingTestBean().routePayload(p));
		}

		@Bean
		public IntegrationFlow routeMultiMethodInvocationFlow() {
			return IntegrationFlows.from("routerMultiInput")
					.route(String.class, p -> p.equals("foo") || p.equals("bar") ? new String[] {"foo", "bar"} : null,
							s -> s.suffix("-channel"))
					.get();
		}

	}

	private static class RoutingTestBean {

		public String routePayload(String name) {
			return name + "-channel";
		}

		@Router
		public String routeByHeader(@Header("targetChannel") String name) {
			return name + "-channel";
		}

		public String routeMessage(Message<?> message) {
			if (message.getPayload().equals("foo")) {
				return "foo-channel";
			}
			else if (message.getPayload().equals("bar")) {
				return "bar-channel";
			}
			return null;
		}

	}


}
