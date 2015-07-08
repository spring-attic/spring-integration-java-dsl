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

import static org.mockito.Mockito.mock;

import java.util.Date;

import org.reactivestreams.Publisher;
import org.testng.SkipException;

import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
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
public class PollablePublisherIntegrationFlowVerification extends AbstractPublisherIntegrationFlowVerification {

	@Override
	public void optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingOneByOne() throws Throwable {
		throw new SkipException("The Spring Integration Publisher supports " +
				"'TheSameElementsInTheSameSequenceToAllOfItsSubscribers' only in case of 'PublishSubscribeChannel' " +
				"and unbounded (Long.MAX_VALUE) request(n).");
	}

	@Override
	public void optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfront() throws Throwable {
		optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingOneByOne();
	}

	@Override
	public void optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingManyUpfrontAndCompleteAsExpected() throws Throwable {
		optional_spec111_multicast_mustProduceTheSameElementsInTheSameSequenceToAllOfItsSubscribersWhenRequestingOneByOne();
	}

	@Override
	protected Class<?> getConfigClass() {
		return PollablePublisherConfiguration.class;
	}

	@Configuration
	@EnableIntegration
	public static class PollablePublisherConfiguration extends PublisherConfiguration {

		@Bean
		public MessageChannel reactiveChannel() {
			if (this.failedPublisher) {
				return mock(MessageChannel.class);
			}
			else {
				QueueChannel queueChannel = new QueueChannel();
				if (this.completionSignalRequired) {
					queueChannel.addInterceptor(new ChannelInterceptorAdapter() {

						private long count;

						@Override
						public void afterReceiveCompletion(Message<?> message, MessageChannel channel, Exception ex) {
							super.afterReceiveCompletion(message, channel, ex);
							if (count++ == elements) {
								beanFactory.getBean("publisher", Lifecycle.class).stop();
							}
						}

					});
				}
				return queueChannel;
			}
		}

		@Bean
		public Publisher<Message<String>> publisher() {
			return IntegrationFlows
					.from(() -> new GenericMessage<>(Math.random()),
							e -> e.poller(p ->
									p.trigger(ctx -> this.invoked.getAndSet(true) ? null : new Date())
											.maxMessagesPerPoll(this.elements)))
					.channel(reactiveChannel())
					.toReactivePublisher();
		}

	}

}
