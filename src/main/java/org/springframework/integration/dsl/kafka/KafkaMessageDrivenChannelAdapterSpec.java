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
 * @author Artem Bilan
 * @since 1.1
 */
public class KafkaMessageDrivenChannelAdapterSpec<S extends KafkaMessageDrivenChannelAdapterSpec<S>>
		extends MessageProducerSpec<S, KafkaMessageDrivenChannelAdapter> {

	KafkaMessageDrivenChannelAdapterSpec(KafkaMessageListenerContainer messageListenerContainer) {
		super(new KafkaMessageDrivenChannelAdapter(messageListenerContainer));
	}

	public S keyDecoder(Decoder<?> keyDecoder) {
		this.target.setKeyDecoder(keyDecoder);
		return _this();
	}

	public S payloadDecoder(Decoder<?> payloadDecoder) {
		this.target.setPayloadDecoder(payloadDecoder);
		return _this();
	}

	public S autoCommitOffset(boolean autoCommitOffset) {
		this.target.setAutoCommitOffset(autoCommitOffset);
		return _this();
	}

	public S generateMessageId(boolean generateMessageId) {
		this.target.setGenerateMessageId(generateMessageId);
		return _this();
	}

	public S generateTimestamp(boolean generateTimestamp) {
		this.target.setGenerateTimestamp(generateTimestamp);
		return _this();
	}

	public S useMessageBuilderFactory(boolean useMessageBuilderFactory) {
		this.target.setUseMessageBuilderFactory(useMessageBuilderFactory);
		return _this();
	}

	public static class KafkaMessageDrivenChannelAdapterListenerContainerSpec extends
			KafkaMessageDrivenChannelAdapterSpec<KafkaMessageDrivenChannelAdapterListenerContainerSpec>
			implements ComponentsRegistration {

		private KafkaMessageListenerContainerSpec spec;

		public KafkaMessageDrivenChannelAdapterListenerContainerSpec(KafkaMessageListenerContainerSpec spec) {
			super(spec.container);
			this.spec = spec;
		}

		/**
		 * Configure a listener container by invoking the {@link Consumer} callback, with a
		 * {@link KafkaMessageListenerContainerSpec} argument.
		 * @param configurer the configurer.
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

	public static class KafkaMessageListenerContainerSpec {

		private final KafkaMessageListenerContainer container;

		KafkaMessageListenerContainerSpec(ConnectionFactory connectionFactory, Partition[] partitions) {
			this.container = new KafkaMessageListenerContainer(connectionFactory, partitions);
		}

		KafkaMessageListenerContainerSpec(ConnectionFactory connectionFactory, String[] topics) {
			this.container = new KafkaMessageListenerContainer(connectionFactory, topics);
		}

		public KafkaMessageListenerContainerSpec offsetManager(OffsetManager offsetManager) {
			this.container.setOffsetManager(offsetManager);
			return this;
		}

		public KafkaMessageListenerContainerSpec errorHandler(ErrorHandler errorHandler) {
			this.container.setErrorHandler(errorHandler);
			return this;
		}

		public KafkaMessageListenerContainerSpec concurrency(int concurrency) {
			this.container.setConcurrency(concurrency);
			return this;
		}

		public KafkaMessageListenerContainerSpec stopTimeout(int stopTimeout) {
			this.container.setStopTimeout(stopTimeout);
			return this;
		}

		public KafkaMessageListenerContainerSpec fetchTaskExecutor(Executor fetchTaskExecutor) {
			this.container.setFetchTaskExecutor(fetchTaskExecutor);
			return this;
		}

		public KafkaMessageListenerContainerSpec adminTaskExecutor(Executor adminTaskExecutor) {
			this.container.setAdminTaskExecutor(adminTaskExecutor);
			return this;
		}

		public KafkaMessageListenerContainerSpec queueSize(int queueSize) {
			this.container.setQueueSize(queueSize);
			return this;
		}

		public KafkaMessageListenerContainerSpec maxFetch(int maxFetch) {
			this.container.setMaxFetch(maxFetch);
			return this;
		}

	}

}
