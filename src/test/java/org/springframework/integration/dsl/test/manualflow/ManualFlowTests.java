/*
 * Copyright 2016 the original author or authors
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

package org.springframework.integration.dsl.test.manualflow;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

import org.junit.Test;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Artem Bilan
 */
public class ManualFlowTests {

	@Test
	public void testManualFlowRegistration() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(RootConfiguration.class);

		ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();

		IntegrationFlow flow = f -> f
				.<String, String>transform(String::toUpperCase)
				.channel(c -> c.queue("results"));

		beanFactory.registerSingleton("testFlow", flow);
		beanFactory.initializeBean(flow, "testFlow");

		ctx.start();

		MessageChannel input = ctx.getBean("testFlow.input", MessageChannel.class);
		PollableChannel output = ctx.getBean("results", PollableChannel.class);

		input.send(new GenericMessage<>("foo"));
		Message<?> receive = output.receive(10000);
		assertNotNull(receive);
		assertEquals("FOO", receive.getPayload());

		ctx.close();
	}

	@Configuration
	@EnableIntegration
	public static class RootConfiguration {

	}

}
