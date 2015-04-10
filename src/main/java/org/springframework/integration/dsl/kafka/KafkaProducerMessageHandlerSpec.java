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

import org.springframework.integration.dsl.core.ComponentsRegistration;
import org.springframework.integration.dsl.core.MessageHandlerSpec;
import org.springframework.integration.dsl.support.Consumer;
import org.springframework.integration.dsl.support.Function;
import org.springframework.integration.dsl.support.FunctionExpression;
import org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler;
import org.springframework.integration.kafka.support.KafkaProducerContext;
import org.springframework.integration.kafka.support.ProducerConfiguration;
import org.springframework.integration.kafka.support.ProducerFactoryBean;
import org.springframework.integration.kafka.support.ProducerMetadata;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

import kafka.javaapi.producer.Producer;
import kafka.producer.Partitioner;
import kafka.serializer.Encoder;

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

	private final Map<String, ProducerConfiguration> producerConfigurations =
			new HashMap<String, ProducerConfiguration>();

	KafkaProducerMessageHandlerSpec(Properties producerProperties) {
		this.producerProperties = producerProperties;
		this.kafkaProducerContext = new KafkaProducerContext();
		this.target = new KafkaProducerMessageHandler(kafkaProducerContext);
	}

	/**
	 * Configure s SpEL expression to determine the Kafka topic at runtime against
	 * request Message as a root object of evaluation context.
	 * @param topicExpression the topic SpEL expression.
	 * @return the spec.
	 */
	public KafkaProducerMessageHandlerSpec topicExpression(String topicExpression) {
		this.target.setTopicExpression(PARSER.parseExpression(topicExpression));
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
		this.target.setTopicExpression(new FunctionExpression<Message<P>>(topicFunction));
		return _this();
	}

	/**
	 * Configure s SpEL expression to determine the Kafka message key to store at runtime against
	 * request Message as a root object of evaluation context.
	 * @param messageKeyExpression the message key SpEL expression.
	 * @return the spec.
	 */
	public KafkaProducerMessageHandlerSpec messageKeyExpression(String messageKeyExpression) {
		target.setMessageKeyExpression(PARSER.parseExpression(messageKeyExpression));
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
		this.target.setMessageKeyExpression(new FunctionExpression<Message<P>>(messageKeyFunction));
		return _this();
	}

	public KafkaProducerMessageHandlerSpec addProducer(String topic, String brokerList,
			Consumer<ProducerMetadataSpec> producerMetadataSpecConsumer) {
		Assert.hasText(topic);
		Assert.hasText(brokerList);
		Assert.notNull(producerMetadataSpecConsumer);
		ProducerMetadataSpec spec = new ProducerMetadataSpec(new ProducerMetadata(topic));
		producerMetadataSpecConsumer.accept(spec);
		try {
			ProducerMetadata producerMetadata = spec.producerMetadata;
			producerMetadata.afterPropertiesSet();
			ProducerFactoryBean producerFactoryBean =
					new ProducerFactoryBean(producerMetadata, brokerList, this.producerProperties);
			Producer producer = producerFactoryBean.getObject();
			this.producerConfigurations.put(topic, new ProducerConfiguration(producerMetadata, producer));
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
		return _this();
	}

	@Override
	public Collection<Object> getComponentsToRegister() {
		this.kafkaProducerContext.setProducerConfigurations(this.producerConfigurations);
		return Collections.<Object>singleton(this.kafkaProducerContext);
	}

	@Override
	protected KafkaProducerMessageHandler doGet() {
		throw new UnsupportedOperationException();
	}

	/**
	 * A helper class in the Builder pattern style to delegate options to the
	 * {@link ProducerMetadata}.
	 */
	public static class ProducerMetadataSpec {

		private final ProducerMetadata producerMetadata;

		ProducerMetadataSpec(ProducerMetadata producerMetadata) {
			this.producerMetadata = producerMetadata;
		}

		/**
		 * Specify an {@link Encoder} for Kafka message body.
		 * Can be used as Java 8 Lambda.
		 * @param valueEncoder the value encoder.
		 * @return the spec.
		 */
		public <T> ProducerMetadataSpec valueEncoder(Encoder<T> valueEncoder) {
			this.producerMetadata.setValueEncoder(valueEncoder);
			return this;
		}

		/**
		 * Specify an {@link Encoder} for Kafka message key.
		 * Can be used as Java 8 Lambda.
		 * @param keyEncoder the key encoder.
		 * @return the spec.
		 */
		public <T> ProducerMetadataSpec keyEncoder(Encoder<T> keyEncoder) {
			this.producerMetadata.setKeyEncoder(keyEncoder);
			return this;
		}

		/**
		 * Specify a {@link Class} for the message key.
		 * @param keyClassType the type for key to encode.
		 * @return the spec.
		 */
		public ProducerMetadataSpec keyClassType(Class<?> keyClassType) {
			this.producerMetadata.setKeyClassType(keyClassType);
			return this;
		}

		/**
		 * Specify a {@link Class} for the message body.
		 * @param valueClassType the type for message body to encode.
		 * @return the spec.
		 */
		public ProducerMetadataSpec valueClassType(Class<?> valueClassType) {
			this.producerMetadata.setValueClassType(valueClassType);
			return this;
		}

		/**
		 * Specify a compression codec constant.
		 * Valid values are:
		 * <ul>
		 * 	<li>none
		 * 	<li>gzip
		 * 	<li>snappy
		 * </ul>
		 * @param compressionCodec the compression codec constant.
		 * @return the spec.
		 */
		public ProducerMetadataSpec compressionCodec(String compressionCodec) {
			this.producerMetadata.setCompressionCodec(compressionCodec);
			return this;
		}

		/**
		 * Specify a {@link Partitioner} reference.
		 * Can be used as Java 8 Lambda.
		 * @param partitioner the partitioner.
		 * @return spec.
		 * @see Partitioner
		 */
		public ProducerMetadataSpec partitioner(Partitioner partitioner) {
			this.producerMetadata.setPartitioner(partitioner);
			return this;
		}

		/**
		 * Specify a {@code sync/async} ({@code producer.type}) Producer behaviour.
		 * @param async the {@code boolean} flag to indicate the Producer behaviour. Defaults to {@code false}.
		 * @return the spec.
		 */
		public ProducerMetadataSpec async(boolean async) {
			this.producerMetadata.setAsync(async);
			return this;
		}

		/**
		 * Specify a number of message property ({@code batch.num.messages}) for {@code async} Producer behaviour.
		 * @param batchNumMessages the number of message to batch.
		 * @return the spec.
		 */
		public ProducerMetadataSpec batchNumMessages(int batchNumMessages) {
			this.producerMetadata.setBatchNumMessages("" + batchNumMessages);
			return this;
		}

	}

}

