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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class FlowServiceTests {

	@Autowired
	@Qualifier("flowServiceTests.MyFlow.input")
	private MessageChannel input;

	@Autowired(required = false)
	private MyFlow myFlow;

	@Test
	public void testFlowService() {
		assertNotNull(this.myFlow);
		QueueChannel replyChannel = new QueueChannel();
		this.input.send(MessageBuilder.withPayload("foo").setReplyChannel(replyChannel).build());
		Message<?> receive = replyChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("FOO", receive.getPayload());
	}

	@Autowired
	@Qualifier("testGateway.input")
	private MessageChannel testGatewayInput;

	@Test
	public void testGatewayExplicitReplyChannel() {
		QueueChannel replyChannel = new QueueChannel();
		this.testGatewayInput.send(MessageBuilder.withPayload("foo").setReplyChannel(replyChannel).build());
		Message<?> message = replyChannel.receive(10000);
		assertNotNull(message);
		assertEquals("FOO", message.getPayload());
	}

	@Configuration
	@EnableIntegration
	@ComponentScan
	public static class ContextConfiguration {

		@Bean
		public IntegrationFlow testGateway() {
			return f -> f.gateway("processChannel", g -> g.replyChannel("replyChannel"));
		}


		@Bean
		public MessageChannel replyChannel() {
			return new DirectChannel();
		}


		@Bean
		public IntegrationFlow subFlow() {
			return IntegrationFlows
					.from("processChannel")
					.<String, String>transform(String::toUpperCase)
					.channel("replyChannel")
					.get();
		}

	}

	@Component
	public static class MyFlow implements IntegrationFlow {

		@Override
		public void accept(IntegrationFlowDefinition<?> f) {
			f.<String, String>transform(String::toUpperCase);
		}

	}

}
