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

package org.springframework.integration.dsl.kafka;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Executor;

import org.springframework.integration.dsl.core.ComponentsRegistration;
import org.springframework.integration.dsl.core.MessageProducerSpec;
import org.springframework.integration.dsl.support.Consumer;
import org.springframework.integration.kafka.core.ConnectionFactory;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter;
import org.springframework.integration.kafka.listener.ErrorHandler;
import org.springframework.integration.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.integration.kafka.listener.OffsetManager;
import org.springframework.util.Assert;

import kafka.serializer.Decoder;

/**
 * A {@link MessageProducerSpec} implementation for the {@link KafkaMessageDrivenChannelAdapter}.
 *
 * @author Artem Bilan
 * @since 1.1
 */
public class KafkaMessageDrivenChannelAdapterSpec<S extends KafkaMessageDrivenChannelAdapterSpec<S>>
		extends MessageProducerSpec<S, KafkaMessageDrivenChannelAdapter> {

	KafkaMessageDrivenChannelAdapterSpec(KafkaMessageListenerContainer messageListenerContainer) {
		super(new KafkaMessageDrivenChannelAdapter(messageListenerContainer));
	}

	/**
	 * Specify a {@link Decoder} for Kafka message key.
	 * Can be used as Java 8 Lambda.
	 * @param keyDecoder the key decoder.
	 * @return the spec.
	 */
	public <T> S keyDecoder(Decoder<T> keyDecoder) {
		this.target.setKeyDecoder(keyDecoder);
		return _this();
	}

	/**
	 * Specify a {@link Decoder} for Kafka message body.
	 * Can be used as Java 8 Lambda.
	 * @param payloadDecoder the value decoder.
	 * @return the spec.
	 */
	public <T> S payloadDecoder(Decoder<T> payloadDecoder) {
		this.target.setPayloadDecoder(payloadDecoder);
		return _this();
	}

	/**
	 * @param autoCommitOffset false to not auto-commit (default true).
	 * @return the spec.
	 * @see KafkaMessageDrivenChannelAdapter#setAutoCommitOffset(boolean)
	 */
	public S autoCommitOffset(boolean autoCommitOffset) {
		this.target.setAutoCommitOffset(autoCommitOffset);
		return _this();
	}

	/**
	 * @param generateMessageId true if a message id should be generated.
	 * @return the spec.
	 * @see KafkaMessageDrivenChannelAdapter#setGenerateMessageId(boolean)
	 */
	public S generateMessageId(boolean generateMessageId) {
		this.target.setGenerateMessageId(generateMessageId);
		return _this();
	}

	/**
	 * @param generateTimestamp true if a timestamp should be generated.
	 * @return the spec.
	 * @see KafkaMessageDrivenChannelAdapter#setGenerateTimestamp(boolean)
	 */
	public S generateTimestamp(boolean generateTimestamp) {
		this.target.setGenerateTimestamp(generateTimestamp);
		return _this();
	}

	/**
	 * @param useMessageBuilderFactory true if the {@code MessageBuilderFactory} returned by
	 * {@link KafkaMessageDrivenChannelAdapter#getMessageBuilderFactory()} should be used.
	 * @return the spec.
	 * @see KafkaMessageDrivenChannelAdapter#setUseMessageBuilderFactory(boolean)
	 */
	public S useMessageBuilderFactory(boolean useMessageBuilderFactory) {
		this.target.setUseMessageBuilderFactory(useMessageBuilderFactory);
		return _this();
	}

	/**
	 * A {@link KafkaMessageListenerContainer} configuration {@link KafkaMessageDrivenChannelAdapterSpec}
	 * extension.
	 */
	public static class KafkaMessageDrivenChannelAdapterListenerContainerSpec extends
			KafkaMessageDrivenChannelAdapterSpec<KafkaMessageDrivenChannelAdapterListenerContainerSpec>
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
	 * {@link KafkaMessageListenerContainer}.
	 */
	public static class KafkaMessageListenerContainerSpec {

		private final KafkaMessageListenerContainer container;

		KafkaMessageListenerContainerSpec(ConnectionFactory connectionFactory, Partition[] partitions) {
			this.container = new KafkaMessageListenerContainer(connectionFactory, partitions);
		}

		KafkaMessageListenerContainerSpec(ConnectionFactory connectionFactory, String[] topics) {
			this.container = new KafkaMessageListenerContainer(connectionFactory, topics);
		}

		/**
		 * Specify an {@link OffsetManager} for the {@link KafkaMessageListenerContainer}.
		 * @param offsetManager the {@link OffsetManager} reference.
		 * @return the spec.
		 * @see OffsetManager
		 */
		public KafkaMessageListenerContainerSpec offsetManager(OffsetManager offsetManager) {
			this.container.setOffsetManager(offsetManager);
			return this;
		}

		/**
		 * Specify an {@link ErrorHandler} for the {@link KafkaMessageListenerContainer}.
		 * @param errorHandler the {@link ErrorHandler}.
		 * @return the spec.
		 * @see ErrorHandler
		 */
		public KafkaMessageListenerContainerSpec errorHandler(ErrorHandler errorHandler) {
			this.container.setErrorHandler(errorHandler);
			return this;
		}

		/**
		 * Specify a concurrency maximum number for the {@link KafkaMessageListenerContainer}.
		 * @param concurrency the concurrency maximum number.
		 * @return the spec.
		 * @see KafkaMessageListenerContainer#setConcurrency(int)
		 */
		public KafkaMessageListenerContainerSpec concurrency(int concurrency) {
			this.container.setConcurrency(concurrency);
			return this;
		}

		/**
		 * Specify a {@code stop} timeout for the {@link KafkaMessageListenerContainer}.
		 * @param stopTimeout timeout in milliseconds.
		 * @return the spec.
		 * @see KafkaMessageListenerContainer#setStopTimeout(int)
		 */
		public KafkaMessageListenerContainerSpec stopTimeout(int stopTimeout) {
			this.container.setStopTimeout(stopTimeout);
			return this;
		}

		/**
		 * Specify an {@link Executor} for fetch tasks for the {@link KafkaMessageListenerContainer}.
		 * @param fetchTaskExecutor the {@link Executor}.
		 * @return the spec.
		 * @see KafkaMessageListenerContainer#setFetchTaskExecutor(Executor)
		 */
		public KafkaMessageListenerContainerSpec fetchTaskExecutor(Executor fetchTaskExecutor) {
			this.container.setFetchTaskExecutor(fetchTaskExecutor);
			return this;
		}

		/**
		 * Specify an {@link Executor} for management tasks for the {@link KafkaMessageListenerContainer}.
		 * @param adminTaskExecutor the {@link Executor}.
		 * @return the spec.
		 * @see KafkaMessageListenerContainer#setAdminTaskExecutor(Executor)
		 */
		public KafkaMessageListenerContainerSpec adminTaskExecutor(Executor adminTaskExecutor) {
			this.container.setAdminTaskExecutor(adminTaskExecutor);
			return this;
		}

		/**
		 * Specify a queue size for the {@link KafkaMessageListenerContainer}.
		 * @param queueSize the queue size.
		 * @return the spec.
		 * @see KafkaMessageListenerContainer#setQueueSize(int)
		 */
		public KafkaMessageListenerContainerSpec queueSize(int queueSize) {
			this.container.setQueueSize(queueSize);
			return this;
		}

		/**
		 * Specify a max fetch for the {@link KafkaMessageListenerContainer}.
		 * @param maxFetch the max fetch.
		 * @return the spec.
		 */
		public KafkaMessageListenerContainerSpec maxFetch(int maxFetch) {
			this.container.setMaxFetch(maxFetch);
			return this;
		}

	}

}
