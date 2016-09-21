/*
 * Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.dsl.kafka;

import java.util.Properties;

import org.springframework.integration.dsl.support.Consumer;
import org.springframework.integration.dsl.support.PropertiesBuilder;
import org.springframework.integration.kafka.core.ConnectionFactory;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.integration.kafka.support.ZookeeperConnect;
import org.springframework.util.Assert;

/**
 * Factory class for Kafka components.
 *
 * @author Artem Bilan
 * @author Nasko Vasilev
 * @since 1.1
 */
public final class Kafka {

	/**
	 * Create an initial {@link KafkaHighLevelConsumerMessageSourceSpec}.
	 * @param zookeeperConnect the zookeeperConnect.
	 * @return the KafkaHighLevelConsumerMessageSourceSpec.
	 * @deprecated since Spring Integration Kafka 1.3
	 */
	@Deprecated
	public static KafkaHighLevelConsumerMessageSourceSpec inboundChannelAdapter(ZookeeperConnect zookeeperConnect) {
		return new KafkaHighLevelConsumerMessageSourceSpec(zookeeperConnect);
	}

	/**
	 * Create an initial {@link KafkaProducerMessageHandlerSpec}.
	 * @return the KafkaProducerMessageHandlerSpec.
	 */
	public static KafkaProducerMessageHandlerSpec outboundChannelAdapter() {
		return outboundChannelAdapter((Properties) null);
	}

	/**
	 * Create an initial {@link KafkaProducerMessageHandlerSpec} with Kafka Producer properties.
	 * @param producerProperties the {@link PropertiesBuilder} Java 8 Lambda.
	 * @return the KafkaProducerMessageHandlerSpec.
	 * @see <a href="https://kafka.apache.org/documentation.html#producerconfigs">Kafka Producer Configs</a>
	 */
	public static KafkaProducerMessageHandlerSpec outboundChannelAdapter(
			Consumer<PropertiesBuilder> producerProperties) {
		Assert.notNull(producerProperties);
		PropertiesBuilder properties = new PropertiesBuilder();
		producerProperties.accept(properties);
		return outboundChannelAdapter(properties.get());
	}

	/**
	 * Create an initial {@link KafkaProducerMessageHandlerSpec} with Kafka Producer properties.
	 * @param producerProperties the producerProperties.
	 * @return the KafkaProducerMessageHandlerSpec.
	 * @see <a href="https://kafka.apache.org/documentation.html#producerconfigs">Kafka Producer Configs</a>
	 */
	public static KafkaProducerMessageHandlerSpec outboundChannelAdapter(Properties producerProperties) {
		return new KafkaProducerMessageHandlerSpec(producerProperties);
	}

	/**
	 * Create an initial {@link KafkaMessageDrivenChannelAdapterSpec}.
	 * @param messageListenerContainer the {@link KafkaMessageListenerContainer}.
	 * @return the KafkaMessageDrivenChannelAdapterSpec.
	 * @deprecated - use {@link #messageDrivenChannelAdapter(KafkaMessageListenerContainer)}.
	 */
	@SuppressWarnings("rawtypes")
	@Deprecated
	public static KafkaMessageDrivenChannelAdapterSpec messageDriverChannelAdapter(
			KafkaMessageListenerContainer messageListenerContainer) {
		return messageDrivenChannelAdapter(messageListenerContainer);
	}

	/**
	 * Create an initial {@link KafkaMessageDrivenChannelAdapterSpec}.
	 * @param messageListenerContainer the {@link KafkaMessageListenerContainer}.
	 * @return the KafkaMessageDrivenChannelAdapterSpec.
	 * @since 1.2
	 */
	@SuppressWarnings("rawtypes")
	public static KafkaMessageDrivenChannelAdapterSpec messageDrivenChannelAdapter(
			KafkaMessageListenerContainer messageListenerContainer) {
		return new KafkaMessageDrivenChannelAdapterSpec(messageListenerContainer);
	}

	/**
	 * Create an initial
	 * {@link KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec}.
	 * @param connectionFactory the {@link ConnectionFactory}.
	 * @param partitions the {@link Partition} vararg.
	 * @return the KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec.
	 * @deprecated - use {@link #messageDrivenChannelAdapter(ConnectionFactory, Partition...)}.
	 */
	@Deprecated
	public static KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec
	messageDriverChannelAdapter(ConnectionFactory connectionFactory, Partition... partitions) {
		return messageDrivenChannelAdapter(connectionFactory, partitions);
	}

	/**
	 * Create an initial
	 * {@link KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec}.
	 * @param connectionFactory the {@link ConnectionFactory}.
	 * @param partitions the {@link Partition} vararg.
	 * @return the KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec.
	 * @since 1.2
	 */
	public static KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec
	messageDrivenChannelAdapter(ConnectionFactory connectionFactory, Partition... partitions) {
		return messageDrivenChannelAdapter(
				new KafkaMessageDrivenChannelAdapterSpec.KafkaMessageListenerContainerSpec(connectionFactory,
						partitions));
	}

	/**
	 * Create an initial
	 * {@link KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec}.
	 * @param connectionFactory the {@link ConnectionFactory}.
	 * @param topics the Kafka topic name vararg.
	 * @return the KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec.
	 * @deprecated - use {@link #messageDrivenChannelAdapter(ConnectionFactory, String...)}.
	 */
	@Deprecated
	public static KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec
	messageDriverChannelAdapter(ConnectionFactory connectionFactory, String... topics) {
		return messageDrivenChannelAdapter(connectionFactory, topics);
	}

	/**
	 * Create an initial
	 * {@link KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec}.
	 * @param connectionFactory the {@link ConnectionFactory}.
	 * @param topics the Kafka topic name vararg.
	 * @return the KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec.
	 * @since 1.2
	 */
	public static KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec
	messageDrivenChannelAdapter(ConnectionFactory connectionFactory, String... topics) {
		return messageDrivenChannelAdapter(
				new KafkaMessageDrivenChannelAdapterSpec.KafkaMessageListenerContainerSpec(connectionFactory, topics));
	}

	private static KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec
	messageDrivenChannelAdapter(KafkaMessageDrivenChannelAdapterSpec.KafkaMessageListenerContainerSpec spec) {
		return new KafkaMessageDrivenChannelAdapterSpec.KafkaMessageDrivenChannelAdapterListenerContainerSpec(spec);
	}

	private Kafka() {
	}

}
