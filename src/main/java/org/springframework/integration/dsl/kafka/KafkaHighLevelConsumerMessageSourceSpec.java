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

package org.springframework.integration.dsl.kafka;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import org.springframework.integration.dsl.core.ComponentsRegistration;
import org.springframework.integration.dsl.core.MessageSourceSpec;
import org.springframework.integration.dsl.support.Consumer;
import org.springframework.integration.dsl.support.MapBuilder;
import org.springframework.integration.dsl.support.PropertiesBuilder;
import org.springframework.integration.kafka.inbound.KafkaHighLevelConsumerMessageSource;
import org.springframework.integration.kafka.support.ConsumerConfigFactoryBean;
import org.springframework.integration.kafka.support.ConsumerConfiguration;
import org.springframework.integration.kafka.support.ConsumerConnectionProvider;
import org.springframework.integration.kafka.support.ConsumerMetadata;
import org.springframework.integration.kafka.support.KafkaConsumerContext;
import org.springframework.integration.kafka.support.MessageLeftOverTracker;
import org.springframework.integration.kafka.support.TopicFilterConfiguration;
import org.springframework.integration.kafka.support.ZookeeperConnect;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import kafka.serializer.Decoder;

/**
 * A {@link MessageSourceSpec} for {@link KafkaHighLevelConsumerMessageSource}.
 *
 * @author Artem Bilan
 * @since 1.1
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class KafkaHighLevelConsumerMessageSourceSpec
		extends MessageSourceSpec<KafkaHighLevelConsumerMessageSourceSpec, KafkaHighLevelConsumerMessageSource<?, ?>>
		implements ComponentsRegistration {

	private final KafkaConsumerContext consumerContext = new KafkaConsumerContext();

	private final KafkaHighLevelConsumerMessageSource kafkaHighLevelConsumerMessageSource =
			new KafkaHighLevelConsumerMessageSource(this.consumerContext);

	private final Map<String, ConsumerConfiguration> consumerConfigurations =
			new HashMap<String, ConsumerConfiguration>();

	private Properties consumerProperties;

	KafkaHighLevelConsumerMessageSourceSpec(ZookeeperConnect zookeeperConnect) {
		this.consumerContext.setZookeeperConnect(zookeeperConnect);
		this.consumerContext.setConsumerConfigurations(this.consumerConfigurations);
	}

	/**
	 * @param consumerProperties the Kafka High Level Consumer properties.
	 * @return the spec.
	 * @see <a href="https://kafka.apache.org/documentation.html#consumerconfigs">Kafka Consumer Configs</a>
	 */
	public KafkaHighLevelConsumerMessageSourceSpec consumerProperties(Properties consumerProperties) {
		this.consumerProperties = consumerProperties;
		return _this();
	}

	/**
	 * @param consumerProperties the {@link PropertiesBuilder} Java 8 Lambda.
	 * @return the spec.
	 * @see <a href="https://kafka.apache.org/documentation.html#consumerconfigs">Kafka Consumer Configs</a>
	 */
	public KafkaHighLevelConsumerMessageSourceSpec consumerProperties(Consumer<PropertiesBuilder> consumerProperties) {
		Assert.notNull(consumerProperties);
		PropertiesBuilder properties = new PropertiesBuilder();
		consumerProperties.accept(properties);
		return consumerProperties(properties.get());
	}

	/**
	 * Add Kafka High Level Consumer to this {@link KafkaHighLevelConsumerMessageSource}
	 * under provided {@code groupId}.
	 * @param groupId the Consumer group id.
	 * @param consumerMetadataSpec the Consumer metadata Java 8 Lambda.
	 * @return the spec.
	 * @see KafkaHighLevelConsumerMessageSourceSpec.ConsumerMetadataSpec
	 */
	public KafkaHighLevelConsumerMessageSourceSpec addConsumer(String groupId,
			Consumer<ConsumerMetadataSpec> consumerMetadataSpec) {
		Assert.hasText(groupId);
		Assert.notNull(consumerMetadataSpec);
		try {
			ConsumerMetadataSpec spec = new ConsumerMetadataSpec(groupId);
			consumerMetadataSpec.accept(spec);
			this.consumerConfigurations.put(groupId, spec.get());
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
		return _this();
	}

	@Override
	public Collection<Object> getComponentsToRegister() {
		return Collections.<Object>singleton(this.consumerContext);
	}

	@Override
	protected KafkaHighLevelConsumerMessageSource<?, ?> doGet() {
		Assert.state(!this.consumerConfigurations.isEmpty(), "At least one 'Consumer' must be specified.");
		return this.kafkaHighLevelConsumerMessageSource;
	}

	/**
	 * A helper class in the Builder pattern style to delegate options to the {@link ConsumerMetadata}
	 * and populate {@link ConsumerConfiguration}.
	 */
	public class ConsumerMetadataSpec {

		private final ConsumerMetadata consumerMetadata = new ConsumerMetadata();

		private final ConsumerConfigFactoryBean consumerConfigFactoryBean;

		private Executor executor;

		private int maxMessages;

		ConsumerMetadataSpec(String groupId) throws Exception {
			this.consumerMetadata.setGroupId(groupId);
			this.consumerConfigFactoryBean = new ConsumerConfigFactoryBean(this.consumerMetadata,
					KafkaHighLevelConsumerMessageSourceSpec.this.consumerContext.getZookeeperConnect(),
					KafkaHighLevelConsumerMessageSourceSpec.this.consumerProperties);
		}

		/**
		 * Specify the Kafka High Level Consumer {@code consumer.timeout.ms} property.
		 * @param consumerTimeout the consumer timeout.
		 * @return the spec.
		 */
		public ConsumerMetadataSpec consumerTimeout(int consumerTimeout) {
			this.consumerMetadata.setConsumerTimeout("" + consumerTimeout);
			return this;
		}

		/**
		 * Specify a {@link Decoder} for Kafka message body.
		 * Can be used as Java 8 Lambda.
		 * @param valueDecoder the value decoder.
		 * @param <T> the expected value type.
		 * @return the spec.
		 */
		public <T> ConsumerMetadataSpec valueDecoder(Decoder<T> valueDecoder) {
			this.consumerMetadata.setValueDecoder(valueDecoder);
			return this;
		}

		/**
		 * Specify a {@link Decoder} for Kafka message key.
		 * Can be used as Java 8 Lambda.
		 * @param keyDecoder the key decoder.
		 * @param <T> the expected key type.
		 * @return the spec.
		 */
		public <T> ConsumerMetadataSpec keyDecoder(Decoder<T> keyDecoder) {
			this.consumerMetadata.setKeyDecoder(keyDecoder);
			return this;
		}

		/**
		 * @param topicStreamMap the of Kafka topics and their number of streams.
		 * @return the spec.
		 */
		public ConsumerMetadataSpec topicStreamMap(Map<String, Integer> topicStreamMap) {
			this.consumerMetadata.setTopicStreamMap(topicStreamMap);
			return this;
		}

		/**
		 * @param topicStreamMap the {@link MapBuilder} Java 8 Lambda for Kafka topics and their number of streams.
		 * @return the spec.
		 */
		public ConsumerMetadataSpec topicStreamMap(Consumer<MapBuilder<?, String, Integer>> topicStreamMap) {
			Assert.notNull(topicStreamMap);
			MapBuilder builder = new MapBuilder();
			topicStreamMap.accept(builder);
			return topicStreamMap(builder.get());
		}
		/**
		 * @param pattern the Kafka topics pattern.
		 * @param numberOfStreams the number of streams.
		 * @param exclude the {@code boolean} flag to include or exclude hte provided pattern.
		 * @return the spec.
		 */
		public ConsumerMetadataSpec topicFilter(String pattern, int numberOfStreams, boolean exclude) {
			this.consumerMetadata.setTopicFilterConfiguration(
					new TopicFilterConfiguration(pattern, numberOfStreams, exclude));
			return this;
		}

		/**
		 * @param executor the Consumer task executor.
		 * @return the spec.
		 */
		public ConsumerMetadataSpec executor(Executor executor) {
			Assert.notNull(executor);
			this.executor = executor;
			return this;
		}

		/**
		 * @param maxMessages the number of messages to consume for one stream during one polling task.
		 * @return the spec.
		 */
		public ConsumerMetadataSpec maxMessages(int maxMessages) {
			this.maxMessages = maxMessages;
			return this;
		}

		ConsumerConfiguration get() throws Exception {
			Assert.state(CollectionUtils.isEmpty(this.consumerMetadata.getTopicStreamMap()) ^
							this.consumerMetadata.getTopicFilterConfiguration() == null,
					"One of 'topicStreamMap' or 'topicFilter' must be specified, but not both.");
			this.consumerMetadata.afterPropertiesSet();
			ConsumerConnectionProvider provider = new ConsumerConnectionProvider(consumerConfigFactoryBean.getObject());
			ConsumerConfiguration configuration = new ConsumerConfiguration(this.consumerMetadata, provider,
					new MessageLeftOverTracker());
			if (this.executor != null) {
				configuration.setExecutor(this.executor);
			}
			configuration.setMaxMessages(this.maxMessages);
			return configuration;
		}

	}

}
