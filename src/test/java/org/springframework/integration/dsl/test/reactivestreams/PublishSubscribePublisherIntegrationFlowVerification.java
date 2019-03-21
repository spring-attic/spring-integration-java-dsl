/*
 * Copyright 2015 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.dsl.test.reactivestreams;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.testng.annotations.BeforeMethod;

import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Artem Bilan
 * @since 1.1
 */
@org.testng.annotations.Test
public class PublishSubscribePublisherIntegrationFlowVerification extends AbstractPublisherIntegrationFlowVerification {


	@BeforeMethod
	@Override
	public void setUp(Method method) throws Exception {
		super.setUp(method);
		if (this.testName.equals("required_spec313_cancelMustMakeThePublisherEventuallyDropAllReferencesToTheSubscriber") ||
				this.testName.equals("required_spec303_mustNotAllowUnboundedRecursion")) {
			if (this.testName.equals("required_spec303_mustNotAllowUnboundedRecursion")) {
				this.completionSignalRequired = true;
			}
			this.scheduledExecutor.schedule(() -> {
						MessageChannel input = applicationContext.getBean("input", MessageChannel.class);
						for (int i = 0; i < 10; i++) {
							input.send(new GenericMessage<>("foo"));
						}
					},
					500, TimeUnit.MILLISECONDS);
		}
	}

	@Override
	protected Class<?> getConfigClass() {
		return PublishSubscribePublisherConfiguration.class;
	}

	@Configuration
	@EnableIntegration
	public static class PublishSubscribePublisherConfiguration extends PublisherConfiguration {

		@Bean
		public MessageChannel reactiveChannel() {
			if (this.failedPublisher) {
				return mock(MessageChannel.class);
			}
			else {
				PublishSubscribeChannel channel = new PublishSubscribeChannel();


				channel.addInterceptor(new ChannelInterceptorAdapter() {

					private final AtomicLong count = new AtomicLong();

					@Override
					public Message<?> preSend(Message<?> message, MessageChannel channel) {
						if (this.count.get() < elements) {
							return super.preSend(message, channel);
						}
						else {
							return null;
						}
					}

					@Override
					public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent,
					                                Exception ex) {
						super.afterSendCompletion(message, channel, sent, ex);
						if (this.count.incrementAndGet() == elements) {
							if (completionSignalRequired) {
								beanFactory.getBean("publisher", Lifecycle.class).stop();
							}
						}
					}

				});

				return channel;
			}
		}

		@Bean
		@SuppressWarnings("unchecked")
		public Publisher<Message<String>> publisher() {
			Publisher<Message<String>> publisher = getPublisher();
			if (this.testMethod.equals("stochastic_spec103_mustSignalOnMethodsSequentially")) {
				publisher = spy(publisher);
				doAnswer(invocation -> {
					Subscriber<Object> subscriber = (Subscriber<Object>) invocation.getArguments()[0];
					invocation.getArguments()[0] = new WrappedSubscriber(subscriber);
					return invocation.callRealMethod();
				}).when(publisher).subscribe(any(Subscriber.class));
			}
			return publisher;
		}

		private Publisher<Message<String>> getPublisher() {
			return IntegrationFlows
					.from("input")
					.channel(reactiveChannel())
					.toReactivePublisher();
		}

		private class WrappedSubscriber implements Subscriber<Object> {

			private final Subscriber<Object> delegate;

			private WrappedSubscriber(Subscriber<Object> delegate) {
				this.delegate = delegate;
			}

			@Override
			public void onSubscribe(Subscription s) {
				this.delegate.onSubscribe(new WrappedSubscription(s));
			}

			@Override
			public void onNext(Object o) {
				this.delegate.onNext(o);
			}

			@Override
			public void onError(Throwable t) {
				this.delegate.onError(t);
			}

			@Override
			public void onComplete() {
				this.delegate.onComplete();
			}

		}

		private class WrappedSubscription implements Subscription {

			private final Subscription delegate;

			private final MessageChannel input = beanFactory.getBean("input", MessageChannel.class);

			private WrappedSubscription(Subscription delegate) {
				this.delegate = delegate;
			}

			@Override
			public void request(long n) {
				delegate.request(n);
				for (int i = 0; i < n; i++) {
					this.input.send(new GenericMessage<>(Math.random()));
				}
			}

			@Override
			public void cancel() {
				delegate.cancel();
			}

		}

	}

}
