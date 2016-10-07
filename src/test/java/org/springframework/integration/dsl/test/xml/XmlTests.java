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

package org.springframework.integration.dsl.test.xml;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.apache.commons.logging.Log;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.stubbing.answers.DoesNothing;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.MessageDispatchingException;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.dsl.channel.QueueChannelSpec;
import org.springframework.integration.dsl.support.FunctionExpression;
import org.springframework.integration.dsl.support.Transformers;
import org.springframework.integration.dsl.support.tuple.Tuples;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.router.AbstractMappingMessageRouter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.xml.router.XPathRouter;
import org.springframework.integration.xml.selector.StringValueTestXPathMessageSelector;
import org.springframework.integration.xml.transformer.support.XPathExpressionEvaluatingHeaderValueMessageProcessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class XmlTests {

	@Autowired
	private MessageChannel inputChannel;

	@Autowired
	@Qualifier("xsltFlow.input")
	private MessageChannel xsltFlowInput;

	@Autowired
	private PollableChannel wrongMessagesChannel;

	@Autowired
	private PollableChannel splittingChannel;

	@Autowired
	private PollableChannel receivedChannel;

	@Autowired
	private FixedSubscriberChannel loggingChannel;

	@Autowired
	private PollableChannel wrongMessagesWireTapChannel;

	@Autowired
	@Qualifier("xpathHeaderEnricherInput")
	private MessageChannel xpathHeaderEnricherInput;

	@Autowired
	@Qualifier("controlBusFlow.input")
	private MessageChannel controlBusFlowInput;

	@Test
	public void testXpathFlow() {
		assertNotNull(this.loggingChannel);
		MessageHandler handler = TestUtils.getPropertyValue(this.loggingChannel, "handler", MessageHandler.class);
		assertThat(handler, instanceOf(LoggingHandler.class));
		assertEquals(LoggingHandler.Level.ERROR,
				TestUtils.getPropertyValue(handler, "level", LoggingHandler.Level.class));

		Log messageLogger = TestUtils.getPropertyValue(handler, "messageLogger", Log.class);
		assertEquals("test.category", TestUtils.getPropertyValue(messageLogger, "name"));

		messageLogger = spy(messageLogger);

		ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
		willAnswer(new DoesNothing()).given(messageLogger).error(argumentCaptor.capture());

		for (String log : argumentCaptor.getAllValues()) {
			assertNotNull(UUID.fromString(log));
		}

		new DirectFieldAccessor(handler).setPropertyValue("messageLogger", messageLogger);

		this.inputChannel.send(new GenericMessage<>("<foo/>"));
		assertNotNull(this.wrongMessagesChannel.receive(10000));

		this.inputChannel.send(new GenericMessage<>("<foo xmlns=\"my:namespace\"/>"));
		assertNotNull(this.wrongMessagesChannel.receive(10000));

		this.inputChannel.send(new GenericMessage<>("<Tags xmlns=\"my:namespace\"/>"));
		assertNotNull(this.splittingChannel.receive(10000));

		this.inputChannel.send(new GenericMessage<>("<Tag xmlns=\"my:namespace\"/>"));
		assertNotNull(this.receivedChannel.receive(10000));

		verify(messageLogger, times(3)).error(anyString());

		assertNotNull(this.wrongMessagesWireTapChannel.receive(10000));
		assertNotNull(this.wrongMessagesWireTapChannel.receive(10000));
	}

	@Test
	public void testXsltFlow() {
		String doc = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><order><orderItem>test</orderItem></order>";
		this.xsltFlowInput.send(MessageBuilder.withPayload(doc)
				.setHeader("testParam", "testParamValue")
				.setHeader("testParam2", "FOO")
				.build());
		Message<?> resultMessage = this.receivedChannel.receive(10000);
		assertEquals("Wrong payload type", String.class, resultMessage.getPayload().getClass());
		String payload = (String) resultMessage.getPayload();
		assertThat(payload, containsString("testParamValue"));
		assertThat(payload, containsString("FOO"));
		assertThat(payload, containsString("hello"));
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

		this.controlBusFlowInput.send(new GenericMessage<>("@xpathHeaderEnricher.start()"));
		this.xpathHeaderEnricherInput.send(message);

		Message<?> result = replyChannel.receive(2000);
		assertNotNull(result);
		MessageHeaders headers = result.getHeaders();
		assertEquals("1", headers.get("one"));
		assertEquals("2", headers.get("two"));
		assertThat(headers.getReplyChannel(), instanceOf(String.class));
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Value("org/springframework/integration/dsl/test/xml/transformer.xslt")
		private Resource xslt;

		@Bean
		public QueueChannelSpec wrongMessagesChannel() {
			return MessageChannels
					.queue()
					.wireTap("wrongMessagesWireTapChannel");
		}

		@Bean
		public QueueChannelSpec wrongMessagesWireTapChannel() {
			return MessageChannels.queue();
		}

		@Bean
		public IntegrationFlow xpathFlow(MessageChannel wrongMessagesChannel) {
			return IntegrationFlows.from("inputChannel")
					.filter(new StringValueTestXPathMessageSelector("namespace-uri(/*)", "my:namespace"),
							e -> e.discardChannel(wrongMessagesChannel))
					.log(LoggingHandler.Level.ERROR, "test.category", m -> m.getHeaders().getId())
					.route(xpathRouter(wrongMessagesChannel))
					.get();
		}

		@Bean
		public PollableChannel splittingChannel() {
			return new QueueChannel();
		}

		@Bean
		public PollableChannel receivedChannel() {
			return new QueueChannel();
		}

		@Bean
		public AbstractMappingMessageRouter xpathRouter(MessageChannel wrongMessagesChannel) {
			XPathRouter router = new XPathRouter("local-name(/*)");
			router.setEvaluateAsString(true);
			router.setResolutionRequired(false);
			router.setDefaultOutputChannel(wrongMessagesChannel);
			router.setChannelMapping("Tags", "splittingChannel");
			router.setChannelMapping("Tag", "receivedChannel");
			return router;
		}

		@Bean
		public IntegrationFlow xsltFlow() {
			return f -> f
					.transform(Transformers.xslt(this.xslt,
							Tuples.of("testParam", new FunctionExpression<Message<?>>(m -> m.getHeaders().get("testParam"))),
							Tuples.of("testParam2", new FunctionExpression<Message<?>>(m -> m.getHeaders().get("testParam2"))),
							Tuples.of("unresolved", new FunctionExpression<Message<?>>(m -> m.getHeaders().get("foo"))),
							Tuples.of("testParam3", new LiteralExpression("hello"))
					))
					.channel(receivedChannel());
		}

		@Bean
		public IntegrationFlow controlBusFlow() {
			return IntegrationFlowDefinition::controlBus;
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

	}

}
