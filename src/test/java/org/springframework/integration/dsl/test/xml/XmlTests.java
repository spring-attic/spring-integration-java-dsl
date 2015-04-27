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

package org.springframework.integration.dsl.test.xml;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.support.FunctionExpression;
import org.springframework.integration.dsl.support.Transformers;
import org.springframework.integration.dsl.support.tuple.Tuples;
import org.springframework.integration.router.AbstractMappingMessageRouter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.xml.router.XPathRouter;
import org.springframework.integration.xml.selector.StringValueTestXPathMessageSelector;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
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

	@Test
	public void testXpathFlow() {
		this.inputChannel.send(new GenericMessage<>("<foo/>"));
		assertNotNull(this.wrongMessagesChannel.receive(10000));

		this.inputChannel.send(new GenericMessage<>("<foo xmlns=\"my:namespace\"/>"));
		assertNotNull(this.wrongMessagesChannel.receive(10000));

		this.inputChannel.send(new GenericMessage<>("<Tags xmlns=\"my:namespace\"/>"));
		assertNotNull(this.splittingChannel.receive(10000));

		this.inputChannel.send(new GenericMessage<>("<Tag xmlns=\"my:namespace\"/>"));
		assertNotNull(this.receivedChannel.receive(10000));
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


	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Value("org/springframework/integration/dsl/test/xml/transformer.xslt")
		private Resource xslt;

		@Bean
		public PollableChannel wrongMessagesChannel() {
			return new QueueChannel();
		}

		@Bean
		public IntegrationFlow xpathFlow() {
			return IntegrationFlows.from("inputChannel")
					.filter(new StringValueTestXPathMessageSelector("namespace-uri(/*)", "my:namespace"),
							e -> e.discardChannel(wrongMessagesChannel()))
					.route(xpathRouter())
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
		public AbstractMappingMessageRouter xpathRouter() {
			XPathRouter router = new XPathRouter("local-name(/*)");
			router.setEvaluateAsString(true);
			router.setResolutionRequired(false);
			router.setDefaultOutputChannel(wrongMessagesChannel());
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

	}

}
