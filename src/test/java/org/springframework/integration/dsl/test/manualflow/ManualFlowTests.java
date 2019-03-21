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

package org.springframework.integration.dsl.test.manualflow;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.dsl.Channels;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowAdapter;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.dsl.context.IntegrationFlowRegistration;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.annotation.RequestScope;

/**
 * @author Artem Bilan
 * @since 1.2
 */
@ContextConfiguration(classes = ManualFlowTests.RootConfiguration.class)
@RunWith(SpringRunner.class)
@DirtiesContext
public class ManualFlowTests {

	@Autowired
	private IntegrationFlowContext integrationFlowContext;

	@Autowired
	private BeanFactory beanFactory;

	@Test
	public void testManualFlowRegistration() throws InterruptedException {
		IntegrationFlow myFlow = f -> f
				.<String, String>transform(String::toUpperCase)
				.channel(Channels::queue)
				.transform("Hello, "::concat, e -> e
						.poller(p -> p
								.fixedDelay(10)
								.maxMessagesPerPoll(1)
								.receiveTimeout(10)))
				.handle(new BeanFactoryHandler());

		BeanFactoryHandler additionalBean = new BeanFactoryHandler();
		IntegrationFlowRegistration flowRegistration =
				this.integrationFlowContext.registration(myFlow)
						.addBean(additionalBean)
						.register();

		BeanFactoryHandler bean =
				this.beanFactory.getBean(flowRegistration.getId() + BeanFactoryHandler.class.getName() + "#0",
						BeanFactoryHandler.class);
		assertSame(additionalBean, bean);
		assertSame(this.beanFactory, bean.beanFactory);

		MessagingTemplate messagingTemplate = flowRegistration.getMessagingTemplate();
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

		flowRegistration.destroy();

		assertFalse(this.beanFactory.containsBean(flowRegistration.getId()));
		assertFalse(this.beanFactory.containsBean(flowRegistration.getId() + ".input"));
		assertFalse(this.beanFactory.containsBean(flowRegistration.getId() + BeanFactoryHandler.class.getName() + "#0"));

		ThreadPoolTaskScheduler taskScheduler = this.beanFactory.getBean(ThreadPoolTaskScheduler.class);
		Thread.sleep(100);
		assertEquals(0, taskScheduler.getActiveCount());
	}

	@Test
	public void testWrongLifecycle() {

		class MyIntegrationFlow implements IntegrationFlow {

			@Override
			public void configure(IntegrationFlowDefinition<?> flow) {
				flow.bridge(null);
			}

		}

		IntegrationFlow testFlow = new MyIntegrationFlow();

		// This is fine because we are not going to start it automatically.
		assertNotNull(this.integrationFlowContext.registration(testFlow)
				.autoStartup(false)
				.register());

		try {
			this.integrationFlowContext.registration(testFlow).register();
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

	@Test
	public void testDynamicSubFlow() {
		PollableChannel resultChannel = new QueueChannel();

		this.integrationFlowContext.registration(flow ->
				flow.publishSubscribeChannel(p -> p
						.minSubscribers(1)
						.subscribe(f -> f.channel(resultChannel))
				))
				.id("dynamicFlow")
				.register();

		this.integrationFlowContext.messagingTemplateFor("dynamicFlow").send(new GenericMessage<>("test"));

		Message<?> receive = resultChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("test", receive.getPayload());
	}

	@Test
	public void testDynamicAdapterFlow() {
		this.integrationFlowContext.registration(new MyFlowAdapter()).register();
		PollableChannel resultChannel = this.beanFactory.getBean("flowAdapterOutput", PollableChannel.class);

		Message<?> receive = resultChannel.receive(1000);
		assertNotNull(receive);
		assertEquals("flowAdapterMessage", receive.getPayload());
	}


	@Test
	public void testWrongIntegrationFlowScope() {
		try {
			new AnnotationConfigApplicationContext(InvalidIntegrationFlowScopeConfiguration.class).close();
			fail("BeanCreationNotAllowedException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(BeanCreationNotAllowedException.class));
			assertThat(e.getMessage(), containsString("IntegrationFlows can not be scoped beans."));
		}
	}

	@Configuration
	@EnableIntegration
	public static class RootConfiguration {

		@Bean
		@RequestScope
		public Date foo() {
			return new Date();
		}

	}

	private static class MyFlowAdapter extends IntegrationFlowAdapter {

		private final AtomicReference<Date> nextExecutionTime = new AtomicReference<>(new Date());

		@Override
		protected IntegrationFlowDefinition<?> buildFlow() {
			return from(() -> new GenericMessage<>("flowAdapterMessage"),
					e -> e.poller(p -> p
							.trigger(ctx -> this.nextExecutionTime.getAndSet(null))))
					.channel(c -> c.queue("flowAdapterOutput"));

		}

	}

	@Configuration
	@EnableIntegration
	public static class InvalidIntegrationFlowScopeConfiguration {

		@Bean
		@RequestScope
		public IntegrationFlow wrongScopeFlow() {
			return flow -> flow.bridge(null);
		}

	}

	private final class BeanFactoryHandler extends AbstractReplyProducingMessageHandler {

		@Autowired
		private BeanFactory beanFactory;

		@Override
		protected Object handleRequestMessage(Message<?> requestMessage) {
			Objects.requireNonNull(this.beanFactory);
			return requestMessage;
		}

		@Override
		protected void doInit() {
			this.beanFactory.getClass(); // ensure wiring before afterPropertiesSet()
		}

	}

}
