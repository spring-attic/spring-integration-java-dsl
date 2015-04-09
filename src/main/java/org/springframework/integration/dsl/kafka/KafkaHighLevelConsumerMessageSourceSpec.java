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

	public KafkaHighLevelConsumerMessageSourceSpec consumerProperties(Properties consumerProperties) {
		this.consumerProperties = consumerProperties;
		return _this();
	}

	public KafkaHighLevelConsumerMessageSourceSpec consumerProperties(Consumer<PropertiesBuilder> consumerProperties) {
		Assert.notNull(consumerProperties);
		PropertiesBuilder properties = new PropertiesBuilder();
		consumerProperties.accept(properties);
		return consumerProperties(properties.get());
	}

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

		public ConsumerMetadataSpec consumerTimeout(int consumerTimeout) {
			this.consumerMetadata.setConsumerTimeout("" + consumerTimeout);
			return this;
		}

		public ConsumerMetadataSpec valueDecoder(Decoder valueDecoder) {
			this.consumerMetadata.setValueDecoder(valueDecoder);
			return this;
		}

		public ConsumerMetadataSpec keyDecoder(Decoder keyDecoder) {
			this.consumerMetadata.setKeyDecoder(keyDecoder);
			return this;
		}

		public ConsumerMetadataSpec topicStreamMap(Map<String, Integer> topicStreamMap) {
			this.consumerMetadata.setTopicStreamMap(topicStreamMap);
			return this;
		}

		public ConsumerMetadataSpec topicStreamMap(Consumer<MapBuilder<?, String, Integer>> topicStreamMap) {
			Assert.notNull(topicStreamMap);
			MapBuilder builder = new MapBuilder();
			topicStreamMap.accept(builder);
			return topicStreamMap(builder.get());
		}

		public ConsumerMetadataSpec topicFilter(String pattern, int numberOfStreams, boolean exclude) {
			this.consumerMetadata.setTopicFilterConfiguration(
					new TopicFilterConfiguration(pattern, numberOfStreams, exclude));
			return this;
		}

		public ConsumerMetadataSpec executor(Executor executor) {
			Assert.notNull(executor);
			this.executor = executor;
			return this;
		}

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
