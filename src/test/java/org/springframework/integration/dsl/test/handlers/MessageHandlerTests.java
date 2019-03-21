/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.dsl.test.handlers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

/**
 * @author Artem Bilan
 * @since 1.2
 */
public class MessageHandlerTests {

	@Test
	public void testWrongReuseOfAbstractMessageProducingHandler() {
		try {
			new AnnotationConfigApplicationContext(InvalidReuseOfAbstractMessageProducingHandlerContext.class).close();
			fail("BeanCreationException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(BeanCreationException.class));
			assertThat(e.getMessage(),
					containsString("An AbstractMessageProducingHandler may only be referenced once"));
		}
	}

	@Configuration
	@EnableIntegration
	public static class InvalidReuseOfAbstractMessageProducingHandlerContext {

		@Bean
		public IntegrationFlow wrongReuseOfAbstractMessageProducingHandler() {
			return IntegrationFlows.from("foo")
					.handle(testHandler())
					.handle(testHandler())
					.log()
					.get();
		}

		@Bean
		public MessageHandler testHandler() {
			return new AbstractMessageProducingHandler() {

				@Override
				protected void handleMessageInternal(Message<?> message) throws Exception {

				}

			};
		}

	}

}
