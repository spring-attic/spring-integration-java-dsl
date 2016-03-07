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

package org.springframework.integration.dsl.kafka;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

import org.apache.kafka.common.TopicPartition;

import org.springframework.integration.dsl.core.ComponentsRegistration;
import org.springframework.integration.dsl.core.MessageProducerSpec;
import org.springframework.integration.dsl.support.Consumer;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ErrorHandler;
import org.springframework.util.Assert;

/**
 * A {@link MessageProducerSpec} implementation for the {@link Kafka09MessageDrivenChannelAdapter}.
 *
 * @author Artem Bilan
 *
 * @since 1.2
 */
public class Kafka09MessageDrivenChannelAdapterSpec<S extends Kafka09MessageDrivenChannelAdapterSpec<S>>
		extends MessageProducerSpec<S, Kafka09MessageDrivenChannelAdapter> {

	<K, V> Kafka09MessageDrivenChannelAdapterSpec(AbstractMessageListenerContainer<K, V> messageListenerContainer) {
		super(new Kafka09MessageDrivenChannelAdapter<K, V>(messageListenerContainer));
	}

	/**
	 * @param generateMessageId true if a message id should be generated.
	 * @return the spec.
	 * @see Kafka09MessageDrivenChannelAdapter#setGenerateMessageId(boolean)
	 */
	public S generateMessageId(boolean generateMessageId) {
		this.target.setGenerateMessageId(generateMessageId);
		return _this();
	}

	/**
	 * @param generateTimestamp true if a timestamp should be generated.
	 * @return the spec.
	 * @see Kafka09MessageDrivenChannelAdapter#setGenerateTimestamp(boolean)
	 */
	public S generateTimestamp(boolean generateTimestamp) {
		this.target.setGenerateTimestamp(generateTimestamp);
		return _this();
	}

	/**
	 * @param useMessageBuilderFactory true if the {@code MessageBuilderFactory} returned by
	 * {@link Kafka09MessageDrivenChannelAdapter#getMessageBuilderFactory()} should be used.
	 * @return the spec.
	 * @see Kafka09MessageDrivenChannelAdapter#setUseMessageBuilderFactory(boolean)
	 */
	public S useMessageBuilderFactory(boolean useMessageBuilderFactory) {
		this.target.setUseMessageBuilderFactory(useMessageBuilderFactory);
		return _this();
	}

	/**
	 * A {@link ConcurrentMessageListenerContainer} configuration {@link Kafka09MessageDrivenChannelAdapterSpec}
	 * extension.
	 */
	public static class KafkaMessageDrivenChannelAdapterListenerContainerSpec extends
			Kafka09MessageDrivenChannelAdapterSpec<KafkaMessageDrivenChannelAdapterListenerContainerSpec>
			implements ComponentsRegistration {

		private KafkaMessageListenerContainerSpec spec;

		KafkaMessageDrivenChannelAdapterListenerContainerSpec(KafkaMessageListenerContainerSpec spec) {
			super(spec.container);
			this.spec = spec;
		}

		/**
		 * Configure a listener container by invoking the {@link Consumer} callback, with a
		 * {@link KafkaMessageListenerContainerSpec} argument.
		 * @param configurer the configurer Java 8 Lambda.
		 * @return the spec.
		 */
		public KafkaMessageDrivenChannelAdapterListenerContainerSpec configureListenerContainer(
				Consumer<KafkaMessageListenerContainerSpec> configurer) {
			Assert.notNull(configurer);
			configurer.accept(this.spec);
			return _this();
		}

		@Override
		public Collection<Object> getComponentsToRegister() {
			return Collections.<Object>singleton(this.spec.container);
		}

	}

	/**
	 * A helper class in the Builder pattern style to delegate options to the
	 * {@link ConcurrentMessageListenerContainer}.
	 */
	public static class KafkaMessageListenerContainerSpec {

		private final ConcurrentMessageListenerContainer<?, ?> container;

		<K, V> KafkaMessageListenerContainerSpec(ConsumerFactory<K, V> consumerFactory,
				TopicPartition... topicPartitions) {
			this.container = new ConcurrentMessageListenerContainer<K, V>(consumerFactory, topicPartitions);
		}

		<K, V> KafkaMessageListenerContainerSpec(ConsumerFactory<K, V> consumerFactory, String... topics) {
			this.container = new ConcurrentMessageListenerContainer<K, V>(consumerFactory, topics);
		}

		<K, V> KafkaMessageListenerContainerSpec(ConsumerFactory<K, V> consumerFactory, Pattern topicPattern) {
			this.container = new ConcurrentMessageListenerContainer<K, V>(consumerFactory, topicPattern);
		}

		/**
		 * Specify an {@link ErrorHandler} for the {@link AbstractMessageListenerContainer}.
		 * @param errorHandler the {@link ErrorHandler}.
		 * @return the spec.
		 * @see ErrorHandler
		 */
		public KafkaMessageListenerContainerSpec errorHandler(ErrorHandler errorHandler) {
			this.container.setErrorHandler(errorHandler);
			return this;
		}

		/**
		 * Specify a concurrency maximum number for the {@link AbstractMessageListenerContainer}.
		 * @param concurrency the concurrency maximum number.
		 * @return the spec.
		 * @see ConcurrentMessageListenerContainer#setConcurrency(int)
		 */
		public KafkaMessageListenerContainerSpec concurrency(int concurrency) {
			this.container.setConcurrency(concurrency);
			return this;
		}

		public KafkaMessageListenerContainerSpec ackMode(AbstractMessageListenerContainer.AckMode ackMode) {
			this.container.setAckMode(ackMode);
			return this;
		}

		public KafkaMessageListenerContainerSpec pollTimeout(long pollTimeout) {
			this.container.setPollTimeout(pollTimeout);
			return this;
		}

		public KafkaMessageListenerContainerSpec ackCount(int count) {
			this.container.setAckCount(count);
			return this;
		}

		public KafkaMessageListenerContainerSpec ackTime(long millis) {
			this.container.setAckTime(millis);
			return this;
		}

		public KafkaMessageListenerContainerSpec taskExecutor(Executor taskExecutor) {
			this.container.setTaskExecutor(taskExecutor);
			return this;
		}

		public KafkaMessageListenerContainerSpec recentOffset(long recentOffset) {
			this.container.setRecentOffset(recentOffset);
			return this;
		}

	}

}
