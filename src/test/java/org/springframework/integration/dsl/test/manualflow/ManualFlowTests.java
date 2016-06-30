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

import static junit.framework.TestCase.assertFalse;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowAdapter;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Artem Bilan
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class ManualFlowTests {

	@Autowired
	private IntegrationFlowContext integrationFlowContext;

	@Autowired
	private BeanFactory beanFactory;

	@Test
	public void testManualFlowRegistration() {
		IntegrationFlow myFlow = f -> f
				.<String, String>transform(String::toUpperCase)
				.transform("Hello, "::concat);

		String flowId = this.integrationFlowContext.register(myFlow);

		MessagingTemplate messagingTemplate = this.integrationFlowContext.messagingTemplateFor(flowId);
		messagingTemplate.setReceiveTimeout(10000);

		assertEquals("Hello, FOO", messagingTemplate.convertSendAndReceive("foo", String.class));

		assertEquals("Hello, BAR", messagingTemplate.convertSendAndReceive("bar", String.class));

		try {
			messagingTemplate.receive();
			fail("UnsupportedOperationException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(UnsupportedOperationException.class));
			assertThat(e.getMessage(), containsString("The 'receive()/receiveAndConvert()' isn't supported"));
		}

		this.integrationFlowContext.remove(flowId);

		assertFalse(this.beanFactory.containsBean(flowId));
		assertFalse(this.beanFactory.containsBean(flowId + ".input"));
	}

	@Test
	public void testWrongLifecycle() {
		IntegrationFlowAdapter testFlow = new IntegrationFlowAdapter() {

			@Override
			protected IntegrationFlowDefinition<?> buildFlow() {
				return from("foo")
						.bridge(null);
			}

		};

		// This is fine because we are not going to start it automatically.
		assertNotNull(this.integrationFlowContext.register(testFlow, false));

		try {
			this.integrationFlowContext.register(testFlow);
			fail("IllegalStateException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(IllegalStateException.class));
			assertThat(e.getMessage(), containsString("Consider to implement it for [" + testFlow + "]."));
		}

		try {
			this.integrationFlowContext.remove("foo");
			fail("IllegalStateException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(IllegalStateException.class));
			assertThat(e.getMessage(), containsString("But [" + "foo" + "] ins't one of them."));
		}
	}


	@Configuration
	@EnableIntegration
	public static class RootConfiguration {

	}

}
