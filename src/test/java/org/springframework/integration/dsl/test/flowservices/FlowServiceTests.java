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

package org.springframework.integration.dsl.test.flowservices;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.Filter;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowAdapter;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.scheduling.TriggerContext;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StringUtils;

/**
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class FlowServiceTests {

	@Autowired
	@Qualifier("flowServiceTests.MyFlow.input")
	private MessageChannel input;

	@Autowired(required = false)
	private MyFlow myFlow;

	@Autowired
	private PollableChannel myFlowAdapterOutput;

	@Test
	public void testFlowService() {
		assertNotNull(this.myFlow);
		QueueChannel replyChannel = new QueueChannel();
		this.input.send(MessageBuilder.withPayload("foo").setReplyChannel(replyChannel).build());
		Message<?> receive = replyChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("FOO", receive.getPayload());
	}

	@Test
	public void testFlowAdapterService() {
		Message<?> receive = this.myFlowAdapterOutput.receive(10000);
		assertNotNull(receive);
		assertEquals("bar:FOO", receive.getPayload());
	}

	@Configuration
	@EnableIntegration
	@ComponentScan
	public static class ContextConfiguration {

	}

	@Component
	public static class MyFlow implements IntegrationFlow {

		@Override
		public void configure(IntegrationFlowDefinition<?> f) {
			f.<String, String>transform(String::toUpperCase);
		}

	}

	@Component
	public static class MyFlowAdapter extends IntegrationFlowAdapter {

		private final AtomicBoolean invoked = new AtomicBoolean();

		public Date nextExecutionTime(TriggerContext triggerContext) {
			return this.invoked.getAndSet(true) ? null : new Date();
		}

		@Override
		protected IntegrationFlowDefinition<?> buildFlow() {
			return from(this, "messageSource", e -> e.poller(p -> p.trigger(this::nextExecutionTime)))
					.split(this)
					.transform(this)
					.aggregate(a -> a.processor(this, null), null)
					.enrichHeaders(Collections.singletonMap("foo", "FOO"))
					.filter(this)
					.handle(this)
					.channel(c -> c.queue("myFlowAdapterOutput"));
		}

		public String messageSource() {
			return "B,A,R";
		}

		@Splitter
		public String[] split(String payload) {
			return StringUtils.commaDelimitedListToStringArray(payload);
		}

		@Transformer
		public String transform(String payload) {
			return payload.toLowerCase();
		}

		@Aggregator
		public String aggregate(List<String> payloads) {
			return payloads.stream().collect(Collectors.joining());
		}

		@Filter
		public boolean filter(@Header Optional<String> foo) {
			return foo.isPresent();
		}

		@ServiceActivator
		public String handle(String payload, @Header String foo) {
			return payload + ":" + foo;
		}

	}

}
