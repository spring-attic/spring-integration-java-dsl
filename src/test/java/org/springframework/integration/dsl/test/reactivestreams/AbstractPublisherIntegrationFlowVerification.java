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

package org.springframework.integration.dsl.test.reactivestreams;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.mockito.stubbing.Answer;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Artem Bilan
 * @since 1.1
 */
public abstract class AbstractPublisherIntegrationFlowVerification extends PublisherVerification<Message<?>> {

	private static final String ELEMENTS = "publisher.elements";

	private static final String FAILED_PUBLISHER = "failed.publisher";

	private static final String REQUEST_COMPLETE = "request.complete";

	private static final String TEST_METHOD = "test.method";

	protected final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

	protected volatile AnnotationConfigApplicationContext applicationContext;

	protected volatile TestEnvironment originalEnvironment;

	protected boolean completionSignalRequired;

	protected String testName;

	public AbstractPublisherIntegrationFlowVerification() {
		super(new TestEnvironment(2000), 1000);
		this.originalEnvironment = TestUtils.getPropertyValue(this, "env", TestEnvironment.class);
	}

	@BeforeMethod
	public void setUp(Method method) throws Exception {
		this.setUp();
		this.testName = method.getName();
		DirectFieldAccessor dfa = new DirectFieldAccessor(this);
		dfa.setPropertyValue("env", this.originalEnvironment);
		this.completionSignalRequired = false;

		if (!testName.equals("required_spec313_cancelMustMakeThePublisherEventuallyDropAllReferencesToTheSubscriber")) {
			if (testName.equals("stochastic_spec103_mustSignalOnMethodsSequentially")) {
				this.completionSignalRequired = true;
			}
			else {
				TestEnvironment env = new TestEnvironment(2000) {

					@Override
					public <T> ManualSubscriber<T> newManualSubscriber(Publisher<T> pub, long timeoutMillis)
							throws InterruptedException {
						ManualSubscriber<T> subscriber = spy(super.newManualSubscriber(pub, timeoutMillis));
						Answer<Object> onCompleteAnswer = invocation -> {
							applicationContext.getBean("publisher", Lifecycle.class).stop();
							return invocation.callRealMethod();
						};
						doAnswer(onCompleteAnswer).when(subscriber).expectCompletion(anyLong(), anyString());
						doAnswer(onCompleteAnswer).when(subscriber).requestEndOfStream(anyLong(), anyString());

						doAnswer(invocation -> {
							if (applicationContext.containsBean("input")) {
								MessageChannel input = applicationContext.getBean("input", MessageChannel.class);
								final Long n = (Long) invocation.getArguments()[0];
								scheduledExecutor.schedule(() -> {
											for (int i = 0; i < n; i++) {
												input.send(new GenericMessage<>(Math.random()));
											}
										},
										100, TimeUnit.MILLISECONDS);

							}
							return invocation.callRealMethod();
						}).when(subscriber).request(anyLong());

						return subscriber;
					}

				};
				dfa.setPropertyValue("env", env);
			}
		}
	}

	@AfterMethod
	public void teardown() {
		if (this.applicationContext != null) {
			this.applicationContext.close();
		}
	}

	@Override
	public Publisher<Message<?>> createPublisher(long elements) {
		MockEnvironment environment = new MockEnvironment()
				.withProperty(ELEMENTS, "" + elements)
				.withProperty(TEST_METHOD, this.testName)
				.withProperty(REQUEST_COMPLETE, "" + this.completionSignalRequired);
		return doCreatePublisher(environment);
	}

	@Override
	public Publisher<Message<?>> createFailedPublisher() {
		MockEnvironment environment = new MockEnvironment()
				.withProperty(FAILED_PUBLISHER, "true");
		return doCreatePublisher(environment);
	}

	@SuppressWarnings("unchecked")
	private Publisher<Message<?>> doCreatePublisher(ConfigurableEnvironment environment) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(getConfigClass());
		applicationContext.setEnvironment(environment);
		applicationContext.refresh();
		this.applicationContext = applicationContext;
		return this.applicationContext.getBean(Publisher.class);
	}


	protected abstract Class<?> getConfigClass();


	public static class PublisherConfiguration {

		protected final AtomicBoolean invoked = new AtomicBoolean();

		@Value("${" + ELEMENTS + ":" + Long.MAX_VALUE + "}")
		protected long elements;

		@Value("${" + FAILED_PUBLISHER + ":false}")
		protected boolean failedPublisher;

		@Value("${" + REQUEST_COMPLETE + ":false}")
		protected boolean completionSignalRequired;

		@Value("${" + TEST_METHOD + ":}")
		protected String testMethod;

		@Autowired
		protected ConfigurableBeanFactory beanFactory;

		@Bean
		public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

	}

}
