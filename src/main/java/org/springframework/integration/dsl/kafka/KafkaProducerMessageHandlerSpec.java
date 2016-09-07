/*
 * Copyright 2015-2016 the original author or authors
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

import org.apache.kafka.clients.producer.Producer;

import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.dsl.core.ComponentsRegistration;
import org.springframework.integration.dsl.core.MessageHandlerSpec;
import org.springframework.integration.dsl.support.Function;
import org.springframework.integration.dsl.support.FunctionExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler;
import org.springframework.integration.kafka.support.KafkaProducerContext;
import org.springframework.integration.kafka.support.ProducerConfiguration;
import org.springframework.integration.kafka.support.ProducerFactoryBean;
import org.springframework.integration.kafka.support.ProducerMetadata;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A {@link MessageHandlerSpec} implementation for the {@link KafkaProducerMessageHandler}.
 *
 * @author Artem Bilan
 * @since 1.1
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class KafkaProducerMessageHandlerSpec
		extends MessageHandlerSpec<KafkaProducerMessageHandlerSpec, KafkaProducerMessageHandler>
		implements ComponentsRegistration {

	private final KafkaProducerContext kafkaProducerContext;

	private final Properties producerProperties;

	private final Map<String, ProducerConfiguration<?, ?>> producerConfigurations =
			new HashMap<String, ProducerConfiguration<?, ?>>();

	KafkaProducerMessageHandlerSpec(Properties producerProperties) {
		this.producerProperties = producerProperties;
		this.kafkaProducerContext = new KafkaProducerContext();
		this.kafkaProducerContext.setBeanName(null);
		this.target = new KafkaProducerMessageHandler(kafkaProducerContext);
	}

	/**
	 * Configure the Kafka topic to send messages.
	 * @param topic the Kafka topic name.
	 * @return the spec.
	 * @since 1.1.1
	 */
	public KafkaProducerMessageHandlerSpec topic(String topic) {
		return topicExpression(new LiteralExpression(topic));
	}


	/**
	 * Configure a SpEL expression to determine the Kafka topic at runtime against
	 * request Message as a root object of evaluation context.
	 * @param topicExpression the topic SpEL expression.
	 * @return the spec.
	 */
	public KafkaProducerMessageHandlerSpec topicExpression(String topicExpression) {
		return topicExpression(PARSER.parseExpression(topicExpression));
	}

	/**
	 * Configure an {@link Expression} to determine the Kafka topic at runtime against
	 * request Message as a root object of evaluation context.
	 * @param topicExpression the topic expression.
	 * @return the spec.
	 * @since 1.1.1
	 */
	public KafkaProducerMessageHandlerSpec topicExpression(Expression topicExpression) {
		this.target.setTopicExpression(topicExpression);
		return _this();
	}

	/**
	 * Configure a {@link Function} that will be invoked at run time to determine the topic to
	 * which a message will be sent. Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 * .<Foo>topic(m -> m.getPayload().getTopic())
	 * }
	 * </pre>
	 * @param topicFunction the topic function.
	 * @param <P> the expected payload type.
	 * @return the current {@link KafkaProducerMessageHandlerSpec}.
	 * @see FunctionExpression
	 */
	public <P> KafkaProducerMessageHandlerSpec topic(Function<Message<P>, String> topicFunction) {
		return topicExpression(new FunctionExpression<Message<P>>(topicFunction));
	}

	/**
	 * Configure a SpEL expression to determine the Kafka message key to store at runtime against
	 * request Message as a root object of evaluation context.
	 * @param messageKeyExpression the message key SpEL expression.
	 * @return the spec.
	 */
	public KafkaProducerMessageHandlerSpec messageKeyExpression(String messageKeyExpression) {
		return messageKeyExpression(PARSER.parseExpression(messageKeyExpression));
	}

	/**
	 * Configure the message key to store message in Kafka topic.
	 * @param messageKey the message key to use.
	 * @return the spec.
	 * @since 1.1.1
	 */
	public KafkaProducerMessageHandlerSpec messageKey(String messageKey) {
		return messageKeyExpression(new LiteralExpression(messageKey));
	}

	/**
	 * Configure an {@link Expression} to determine the Kafka message key to store at runtime against
	 * request Message as a root object of evaluation context.
	 * @param messageKeyExpression the message key expression.
	 * @return the spec.
	 * @since 1.1.1
	 */
	public KafkaProducerMessageHandlerSpec messageKeyExpression(Expression messageKeyExpression) {
		target.setMessageKeyExpression(messageKeyExpression);
		return _this();
	}

	/**
	 * Configure a {@link Function} that will be invoked at run time to determine the message key under
	 * which a message will be stored in the topic. Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 * .<Foo>messageKey(m -> m.getPayload().getKey())
	 * }
	 * </pre>
	 * @param messageKeyFunction the message key function.
	 * @param <P> the expected payload type.
	 * @return the current {@link KafkaProducerMessageHandlerSpec}.
	 * @see FunctionExpression
	 */
	public <P> KafkaProducerMessageHandlerSpec messageKey(Function<Message<P>, ?> messageKeyFunction) {
		return messageKeyExpression(new FunctionExpression<Message<P>>(messageKeyFunction));
	}

	/**
	 * Configure a partitionId of Kafka topic.
	 * @param partitionId the partitionId to use.
	 * @return the spec.
	 * @since 1.1.1
	 */
	public KafkaProducerMessageHandlerSpec partitionId(Integer partitionId) {
		return partitionIdExpression(new ValueExpression<Integer>(partitionId));
	}

	/**
	 * Configure a SpEL expression to determine the topic partitionId at runtime against
	 * request Message as a root object of evaluation context.
	 * @param partitionIdExpression the partitionId expression to use.
	 * @return the spec.
	 * @since 1.1.1
	 */
	public KafkaProducerMessageHandlerSpec partitionIdExpression(String partitionIdExpression) {
		return partitionIdExpression(PARSER.parseExpression(partitionIdExpression));
	}

	/**
	 * Configure a {@link Function} that will be invoked at run time to determine the partition id under
	 * which a message will be stored in the topic. Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 * .partitionId(m -> m.getHeaders().get("partitionId", Integer.class))
	 * }
	 * </pre>
	 * @param partitionIdFunction the partitionId function.
	 * @param <P> the expected payload type.
	 * @return the spec.
	 * @since 1.1.1
	 */
	public <P> KafkaProducerMessageHandlerSpec partitionId(Function<Message<P>, Integer> partitionIdFunction) {
		return partitionIdExpression(new FunctionExpression<Message<P>>(partitionIdFunction));
	}

	/**
	 * Configure an {@link Expression} to determine the topic partitionId at runtime against
	 * request Message as a root object of evaluation context.
	 * @param partitionIdExpression the partitionId expression to use.
	 * @return the spec.
	 * @since 1.1.1
	 */
	public KafkaProducerMessageHandlerSpec partitionIdExpression(Expression partitionIdExpression) {
		this.target.setPartitionIdExpression(partitionIdExpression);
		return _this();
	}

	/**
	 * Add Kafka Producer to this {@link KafkaProducerMessageHandler}
	 * for the provided {@code topic} and {@code brokerList}.
	 * @param producerMetadata the {@link ProducerMetadata} - options for Kafka {@link Producer}.
	 * @param brokerList the Kafka brokers ({@code metadata.broker.list})
	 * in the format {@code host1:port1,host2:port2}.
	 * @return the spec.
	 */
	public KafkaProducerMessageHandlerSpec addProducer(ProducerMetadata producerMetadata, String brokerList) {
		Assert.notNull(producerMetadata);
		Assert.hasText(brokerList);
		try {
			ProducerFactoryBean producerFactoryBean =
					new ProducerFactoryBean(producerMetadata, brokerList, this.producerProperties);
			Producer producer = producerFactoryBean.getObject();
			this.producerConfigurations.put(producerMetadata.getTopic(),
					new ProducerConfiguration(producerMetadata, producer));
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
		return _this();
	}

	/**
	 * Add Kafka Producer to this {@link KafkaProducerMessageHandler}
	 * for the provided {@link ProducerConfiguration}.
	 * @param producerConfiguration the {@link ProducerConfiguration} - options for Kafka {@link Producer}.
	 * @return the spec.
	 * @since 1.1.3
	 */
	public KafkaProducerMessageHandlerSpec addProducer(ProducerConfiguration producerConfiguration) {
		Assert.notNull(producerConfiguration);
		this.producerConfigurations.put(producerConfiguration.getProducerMetadata().getTopic(),
					producerConfiguration);
		return _this();
	}

	@Override
	public Collection<Object> getComponentsToRegister() {
		this.kafkaProducerContext.setProducerConfigurations(this.producerConfigurations);
		return Collections.<Object>singleton(this.kafkaProducerContext);
	}

}

