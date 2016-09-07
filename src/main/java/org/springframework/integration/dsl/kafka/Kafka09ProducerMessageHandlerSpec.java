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

import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.dsl.core.ComponentsRegistration;
import org.springframework.integration.dsl.core.MessageHandlerSpec;
import org.springframework.integration.dsl.support.Function;
import org.springframework.integration.dsl.support.FunctionExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.messaging.Message;

/**
 * A {@link MessageHandlerSpec} implementation for the {@link KafkaProducerMessageHandler}.
 *
 * @author Artem Bilan
 * @since 1.2
 */
public class Kafka09ProducerMessageHandlerSpec<K, V>
		extends MessageHandlerSpec<Kafka09ProducerMessageHandlerSpec<K, V>, Kafka09ProducerMessageHandler<K, V>> {

	protected final KafkaTemplate<K, V> kafkaTemplate;

	Kafka09ProducerMessageHandlerSpec(KafkaTemplate<K, V> kafkaTemplate) {
		this.target = new Kafka09ProducerMessageHandler<K, V>(kafkaTemplate);
		this.kafkaTemplate = kafkaTemplate;
	}

	/**
	 * Configure the Kafka topic to send messages.
	 * @param topic the Kafka topic name.
	 * @return the spec.
	 */
	public Kafka09ProducerMessageHandlerSpec<K, V> topic(String topic) {
		return topicExpression(new LiteralExpression(topic));
	}


	/**
	 * Configure a SpEL expression to determine the Kafka topic at runtime against
	 * request Message as a root object of evaluation context.
	 * @param topicExpression the topic SpEL expression.
	 * @return the spec.
	 */
	public Kafka09ProducerMessageHandlerSpec<K, V> topicExpression(String topicExpression) {
		return topicExpression(PARSER.parseExpression(topicExpression));
	}

	/**
	 * Configure an {@link Expression} to determine the Kafka topic at runtime against
	 * request Message as a root object of evaluation context.
	 * @param topicExpression the topic expression.
	 * @return the spec.
	 */
	public Kafka09ProducerMessageHandlerSpec<K, V> topicExpression(Expression topicExpression) {
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
	 * @return the current {@link Kafka09ProducerMessageHandlerSpec}.
	 * @see FunctionExpression
	 */
	public <P> Kafka09ProducerMessageHandlerSpec<K, V> topic(Function<Message<P>, String> topicFunction) {
		return topicExpression(new FunctionExpression<Message<P>>(topicFunction));
	}

	/**
	 * Configure a SpEL expression to determine the Kafka message key to store at runtime against
	 * request Message as a root object of evaluation context.
	 * @param messageKeyExpression the message key SpEL expression.
	 * @return the spec.
	 */
	public Kafka09ProducerMessageHandlerSpec<K, V> messageKeyExpression(String messageKeyExpression) {
		return messageKeyExpression(PARSER.parseExpression(messageKeyExpression));
	}

	/**
	 * Configure the message key to store message in Kafka topic.
	 * @param messageKey the message key to use.
	 * @return the spec.
	 */
	public Kafka09ProducerMessageHandlerSpec<K, V> messageKey(String messageKey) {
		return messageKeyExpression(new LiteralExpression(messageKey));
	}

	/**
	 * Configure an {@link Expression} to determine the Kafka message key to store at runtime against
	 * request Message as a root object of evaluation context.
	 * @param messageKeyExpression the message key expression.
	 * @return the spec.
	 */
	public Kafka09ProducerMessageHandlerSpec<K, V> messageKeyExpression(Expression messageKeyExpression) {
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
	 * @return the current {@link Kafka09ProducerMessageHandlerSpec}.
	 * @see FunctionExpression
	 */
	public <P> Kafka09ProducerMessageHandlerSpec<K, V> messageKey(Function<Message<P>, ?> messageKeyFunction) {
		return messageKeyExpression(new FunctionExpression<Message<P>>(messageKeyFunction));
	}

	/**
	 * Configure a partitionId of Kafka topic.
	 * @param partitionId the partitionId to use.
	 * @return the spec.
	 */
	public Kafka09ProducerMessageHandlerSpec<K, V> partitionId(Integer partitionId) {
		return partitionIdExpression(new ValueExpression<Integer>(partitionId));
	}

	/**
	 * Configure a SpEL expression to determine the topic partitionId at runtime against
	 * request Message as a root object of evaluation context.
	 * @param partitionIdExpression the partitionId expression to use.
	 * @return the spec.
	 */
	public Kafka09ProducerMessageHandlerSpec<K, V> partitionIdExpression(String partitionIdExpression) {
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
	 */
	public <P> Kafka09ProducerMessageHandlerSpec<K, V> partitionId(Function<Message<P>, Integer> partitionIdFunction) {
		return partitionIdExpression(new FunctionExpression<Message<P>>(partitionIdFunction));
	}

	/**
	 * Configure an {@link Expression} to determine the topic partitionId at runtime against
	 * request Message as a root object of evaluation context.
	 * @param partitionIdExpression the partitionId expression to use.
	 * @return the spec.
	 */
	public Kafka09ProducerMessageHandlerSpec<K, V> partitionIdExpression(Expression partitionIdExpression) {
		this.target.setPartitionIdExpression(partitionIdExpression);
		return _this();
	}


	public static class KafkaProducerMessageHandlerTemplateSpec<K, V> extends Kafka09ProducerMessageHandlerSpec<K, V>
			implements ComponentsRegistration {

		KafkaProducerMessageHandlerTemplateSpec(ProducerFactory<K, V> producerFactory) {
			super(new KafkaTemplate<K, V>(producerFactory));
		}

		public KafkaProducerMessageHandlerTemplateSpec<K, V> defaultTopic(String defaultTopic) {
			this.kafkaTemplate.setDefaultTopic(defaultTopic);
			return this;
		}

		public KafkaProducerMessageHandlerTemplateSpec<K, V> producerListener(ProducerListener<K, V> producerListener) {
			this.kafkaTemplate.setProducerListener(producerListener);
			return this;
		}

		@Override
		public Collection<Object> getComponentsToRegister() {
			return Collections.<Object>singleton(this.kafkaTemplate);
		}

	}

}

