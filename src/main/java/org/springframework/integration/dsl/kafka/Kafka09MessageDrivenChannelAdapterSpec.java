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
import java.util.regex.Pattern;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.OffsetCommitCallback;

import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.integration.dsl.core.ComponentsRegistration;
import org.springframework.integration.dsl.core.MessageProducerSpec;
import org.springframework.integration.dsl.support.Consumer;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ErrorHandler;
import org.springframework.kafka.listener.config.ContainerProperties;
import org.springframework.kafka.support.TopicPartitionInitialOffset;
import org.springframework.util.Assert;

/**
 * A {@link MessageProducerSpec} implementation for the {@link Kafka09MessageDrivenChannelAdapter}.
 *
 * @author Artem Bilan
 *
 * @since 1.2
 */
public class Kafka09MessageDrivenChannelAdapterSpec<K, V, S extends Kafka09MessageDrivenChannelAdapterSpec<K, V, S>>
		extends MessageProducerSpec<S, Kafka09MessageDrivenChannelAdapter<K, V>> {

	Kafka09MessageDrivenChannelAdapterSpec(AbstractMessageListenerContainer<K, V> messageListenerContainer) {
		super(new Kafka09MessageDrivenChannelAdapter<K, V>(messageListenerContainer));
	}

	/**
	 * A {@link ConcurrentMessageListenerContainer} configuration {@link Kafka09MessageDrivenChannelAdapterSpec}
	 * extension.
	 */
	public static class KafkaMessageDrivenChannelAdapterListenerContainerSpec<K, V> extends
			Kafka09MessageDrivenChannelAdapterSpec<K, V, KafkaMessageDrivenChannelAdapterListenerContainerSpec<K, V>>
			implements ComponentsRegistration {

		private KafkaMessageListenerContainerSpec<K, V> spec;

		KafkaMessageDrivenChannelAdapterListenerContainerSpec(KafkaMessageListenerContainerSpec<K, V> spec) {
			super(spec.container);
			this.spec = spec;
		}

		/**
		 * Configure a listener container by invoking the {@link Consumer} callback, with a
		 * {@link KafkaMessageListenerContainerSpec} argument.
		 * @param configurer the configurer Java 8 Lambda.
		 * @return the spec.
		 */
		public KafkaMessageDrivenChannelAdapterListenerContainerSpec<K, V> configureListenerContainer(
				Consumer<KafkaMessageListenerContainerSpec<K, V>> configurer) {
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
	public static class KafkaMessageListenerContainerSpec<K, V> {

		private final ConcurrentMessageListenerContainer<K, V> container;

		private final ContainerProperties containerProperties;

		KafkaMessageListenerContainerSpec(ConsumerFactory<K, V> consumerFactory,
				ContainerProperties containerProperties) {
			this.containerProperties = containerProperties;
			this.container = new ConcurrentMessageListenerContainer<K, V>(consumerFactory, this.containerProperties);
		}

		KafkaMessageListenerContainerSpec(ConsumerFactory<K, V> consumerFactory,
				TopicPartitionInitialOffset... topicPartitions) {
			this(consumerFactory, new ContainerProperties(topicPartitions));
		}

		KafkaMessageListenerContainerSpec(ConsumerFactory<K, V> consumerFactory, String... topics) {
			this(consumerFactory, new ContainerProperties(topics));
		}

		KafkaMessageListenerContainerSpec(ConsumerFactory<K, V> consumerFactory, Pattern topicPattern) {
			this(consumerFactory, new ContainerProperties(topicPattern));
		}

		/**
		 * Specify a concurrency maximum number for the {@link AbstractMessageListenerContainer}.
		 * @param concurrency the concurrency maximum number.
		 * @return the spec.
		 * @see ConcurrentMessageListenerContainer#setConcurrency(int)
		 */
		public KafkaMessageListenerContainerSpec<K, V> concurrency(int concurrency) {
			this.container.setConcurrency(concurrency);
			return this;
		}

		/**
		 * Specify an {@link ErrorHandler} for the {@link AbstractMessageListenerContainer}.
		 * @param errorHandler the {@link ErrorHandler}.
		 * @return the spec.
		 * @see ErrorHandler
		 */
		public KafkaMessageListenerContainerSpec<K, V> errorHandler(ErrorHandler errorHandler) {
			this.containerProperties.setErrorHandler(errorHandler);
			return this;
		}

		/**
		 * Set the ack mode to use when auto ack (in the configuration properties) is false.
		 * <ul>
		 * <li>RECORD: Ack after each record has been passed to the listener.</li>
		 * <li>BATCH: Ack after each batch of records received from the consumer has been
		 * passed to the listener</li>
		 * <li>TIME: Ack after this number of milliseconds; (should be greater than
		 * {@code #setPollTimeout(long) pollTimeout}.</li>
		 * <li>COUNT: Ack after at least this number of records have been received</li>
		 * <li>MANUAL: Listener is responsible for acking - use a
		 * {@link AcknowledgingMessageListener}.
		 * </ul>
		 * @param ackMode the {@link AbstractMessageListenerContainer.AckMode}; default BATCH.
		 * @return the spec.
		 * @see AbstractMessageListenerContainer.AckMode
		 */
		public KafkaMessageListenerContainerSpec<K, V> ackMode(AbstractMessageListenerContainer.AckMode ackMode) {
			this.containerProperties.setAckMode(ackMode);
			return this;
		}

		/**
		 * Set the max time to block in the consumer waiting for records.
		 * @param pollTimeout the timeout in ms; default 1000.
		 * @return the spec.
		 * @see ContainerProperties#setPollTimeout(long)
		 */
		public KafkaMessageListenerContainerSpec<K, V> pollTimeout(long pollTimeout) {
			this.containerProperties.setPollTimeout(pollTimeout);
			return this;
		}

		/**
		 * Set the number of outstanding record count after which offsets should be
		 * committed when {@link AbstractMessageListenerContainer.AckMode#COUNT}
		 * or {@link AbstractMessageListenerContainer.AckMode#COUNT_TIME} is being used.
		 * @param count the count
		 * @return the spec.
		 * @see ContainerProperties#setAckCount(int)
		 */
		public KafkaMessageListenerContainerSpec<K, V> ackCount(int count) {
			this.containerProperties.setAckCount(count);
			return this;
		}

		/**
		 * Set the time (ms) after which outstanding offsets should be committed when
		 * {@link AbstractMessageListenerContainer.AckMode#TIME} or
		 * {@link AbstractMessageListenerContainer.AckMode#COUNT_TIME} is being used.
		 * Should be larger than zero.
		 * @param millis the time
		 * @return the spec.
		 * @see ContainerProperties#setAckTime(long)
		 */
		public KafkaMessageListenerContainerSpec<K, V> ackTime(long millis) {
			this.containerProperties.setAckTime(millis);
			return this;
		}

		/**
		 * Set the executor for threads that poll the consumer.
		 * @param consumerTaskExecutor the executor
		 * @return the spec.
		 * @see ContainerProperties#setConsumerTaskExecutor(AsyncListenableTaskExecutor)
		 */
		public KafkaMessageListenerContainerSpec<K, V> consumerTaskExecutor(
				AsyncListenableTaskExecutor consumerTaskExecutor) {
			this.containerProperties.setConsumerTaskExecutor(consumerTaskExecutor);
			return this;
		}

		/**
		 * Set the executor for threads that invoke the listener.
		 * @param listenerTaskExecutor the executor
		 * @return the spec.
		 * @see ContainerProperties#setListenerTaskExecutor(AsyncListenableTaskExecutor)
		 */
		public KafkaMessageListenerContainerSpec<K, V> listenerTaskExecutor(
				AsyncListenableTaskExecutor listenerTaskExecutor) {
			this.containerProperties.setListenerTaskExecutor(listenerTaskExecutor);
			return this;
		}

		/**
		 * When using Kafka group management and {@link #pauseEnabled(boolean)} is
		 * true, set the delay after which the consumer should be paused. Default 10000.
		 * @param pauseAfter the delay.
		 * @return the spec.
		 * @see ContainerProperties#setPauseAfter(long)
		 */
		public KafkaMessageListenerContainerSpec<K, V> pauseAfter(long pauseAfter) {
			this.containerProperties.setPauseAfter(pauseAfter);
			return this;
		}

		/**
		 * Set to true to avoid rebalancing when this consumer is slow or throws a
		 * qualifying exception - pause the consumer. Default: true.
		 * @param pauseEnabled true to pause.
		 * @return the spec.
		 * @see #pauseAfter(long)
		 * @see ContainerProperties#setPauseEnabled(boolean)
		 */
		public KafkaMessageListenerContainerSpec<K, V> pauseEnabled(boolean pauseEnabled) {
			this.containerProperties.setPauseEnabled(pauseEnabled);
			return this;
		}

		/**
		 * Set the queue depth for handoffs from the consumer thread to the listener
		 * thread. Default 1 (up to 2 in process).
		 * @param queueDepth the queue depth.
		 * @return the spec.
		 * @see ContainerProperties#setQueueDepth(int)
		 */
		public KafkaMessageListenerContainerSpec<K, V> queueDepth(int queueDepth) {
			this.containerProperties.setQueueDepth(queueDepth);
			return this;
		}

		/**
		 * Set the timeout for shutting down the container. This is the maximum amount of
		 * time that the invocation to {@code #stop(Runnable)} will block for, before
		 * returning.
		 * @param shutdownTimeout the shutdown timeout.
		 * @return the spec.
		 * @see ContainerProperties#setShutdownTimeout(long)
		 */
		public KafkaMessageListenerContainerSpec<K, V> shutdownTimeout(long shutdownTimeout) {
			this.containerProperties.setShutdownTimeout(shutdownTimeout);
			return this;
		}

		/**
		 * Set the user defined {@link ConsumerRebalanceListener} implementation.
		 * @param consumerRebalanceListener the {@link ConsumerRebalanceListener} instance
		 * @return the spec.
		 * @see ContainerProperties#setConsumerRebalanceListener(ConsumerRebalanceListener)
		 */
		public KafkaMessageListenerContainerSpec<K, V> consumerRebalanceListener(
				ConsumerRebalanceListener consumerRebalanceListener) {
			this.containerProperties.setConsumerRebalanceListener(consumerRebalanceListener);
			return this;
		}

		/**
		 * Set the commit callback; by default a simple logging callback is used to log
		 * success at DEBUG level and failures at ERROR level.
		 * @param commitCallback the callback.
		 * @return the spec.
		 * @see ContainerProperties#setCommitCallback(OffsetCommitCallback)
		 */
		public KafkaMessageListenerContainerSpec<K, V> commitCallback(OffsetCommitCallback commitCallback) {
			this.containerProperties.setCommitCallback(commitCallback);
			return this;
		}

		/**
		 * Set whether or not to call consumer.commitSync() or commitAsync() when the
		 * container is responsible for commits. Default true. See
		 * https://github.com/spring-projects/spring-kafka/issues/62 At the time of
		 * writing, async commits are not entirely reliable.
		 * @param syncCommits true to use commitSync().
		 * @return the spec.
		 * @see ContainerProperties#setSyncCommits(boolean)
		 */
		public KafkaMessageListenerContainerSpec<K, V> syncCommits(boolean syncCommits) {
			this.containerProperties.setSyncCommits(syncCommits);
			return this;
		}

		/**
		 * Set the idle event interval; when set, an event is emitted if a poll returns
		 * no records and this interval has elapsed since a record was returned.
		 * @param idleEventInterval the interval.
		 * @return the spec.
		 * @see ContainerProperties#setIdleEventInterval(Long)
		 */
		public KafkaMessageListenerContainerSpec<K, V> idleEventInterval(Long idleEventInterval) {
			this.containerProperties.setIdleEventInterval(idleEventInterval);
			return this;
		}

		/**
		 * Set whether the container should ack messages that throw exceptions or not.
		 * @param ackOnError whether the container should acknowledge messages that throw
		 * exceptions.
		 * @return the spec.
		 * @see ContainerProperties#setAckOnError(boolean)
		 */
		public KafkaMessageListenerContainerSpec<K, V> ackOnError(boolean ackOnError) {
			this.containerProperties.setAckOnError(ackOnError);
			return this;
		}

	}

}
