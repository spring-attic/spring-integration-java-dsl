/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.dsl.amqp;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;

/**
 * Factory class for AMQP components.
 *
 * @author Artem Bilan
 */
public abstract class Amqp {

	/**
	 * Create an initial {@link AmqpInboundGatewaySpec}.
	 * @param connectionFactory the connectionFactory.
	 * @param queueNames the queueNames.
	 * @return the AmqpInboundGatewaySpec.
	 */
	public static AmqpInboundGatewaySpec inboundGateway(ConnectionFactory connectionFactory, String... queueNames) {
		SimpleMessageListenerContainer listenerContainer = new SimpleMessageListenerContainer(connectionFactory);
		listenerContainer.setQueueNames(queueNames);
		return (AmqpInboundGatewaySpec) inboundGateway(listenerContainer);
	}

	/**
	 * Create an initial {@link AmqpInboundGatewaySpec}.
	 * @param connectionFactory the connectionFactory.
	 * @param queues the queues.
	 * @return the AmqpInboundGatewaySpec.
	 */
	public static AmqpInboundGatewaySpec inboundGateway(ConnectionFactory connectionFactory, Queue... queues) {
		SimpleMessageListenerContainer listenerContainer = new SimpleMessageListenerContainer(connectionFactory);
		listenerContainer.setQueues(queues);
		return (AmqpInboundGatewaySpec) inboundGateway(listenerContainer);
	}

	/**
	 * Create an initial {@link AmqpBaseInboundGatewaySpec}
	 * with provided {@link SimpleMessageListenerContainer}.
	 * Note: only endpoint options are available from spec.
	 * The {@code listenerContainer} options should be specified
	 * on the provided {@link SimpleMessageListenerContainer}.
	 * @param listenerContainer the listenerContainer
	 * @return the AmqpBaseInboundGatewaySpec.
	 */
	public static  AmqpBaseInboundGatewaySpec<?> inboundGateway(SimpleMessageListenerContainer listenerContainer) {
		return new AmqpInboundGatewaySpec(listenerContainer);
	}

	/**
	 * Create an initial AmqpInboundChannelAdapterSpec.
	 * @param connectionFactory the connectionFactory.
	 * @param queueNames the queueNames.
	 * @return the AmqpInboundChannelAdapterSpec.
	 */
	public static AmqpInboundChannelAdapterSpec inboundAdapter(ConnectionFactory connectionFactory,
			String... queueNames) {
		SimpleMessageListenerContainer listenerContainer = new SimpleMessageListenerContainer(connectionFactory);
		listenerContainer.setQueueNames(queueNames);
		return (AmqpInboundChannelAdapterSpec) inboundAdapter(listenerContainer);
	}

	/**
	 * Create an initial AmqpInboundChannelAdapterSpec.
	 * @param connectionFactory the connectionFactory.
	 * @param queues the queues.
	 * @return the AmqpInboundChannelAdapterSpec.
	 */
	public static AmqpInboundChannelAdapterSpec inboundAdapter(ConnectionFactory connectionFactory, Queue... queues) {
		SimpleMessageListenerContainer listenerContainer = new SimpleMessageListenerContainer(connectionFactory);
		listenerContainer.setQueues(queues);
		return (AmqpInboundChannelAdapterSpec) inboundAdapter(listenerContainer);
	}

	/**
	 * Create an initial AmqpInboundChannelAdapterSpec.
	 * @param listenerContainer the listenerContainer
	 * @return the AmqpInboundChannelAdapterSpec.
	 */
	public static AmqpBaseInboundChannelAdapterSpec<?> inboundAdapter(
			SimpleMessageListenerContainer listenerContainer) {
		return new AmqpInboundChannelAdapterSpec(listenerContainer);
	}

	/**
	 * Create an initial AmqpOutboundEndpointSpec (adapter).
	 * @param amqpTemplate the amqpTemplate.
	 * @return the AmqpOutboundEndpointSpec.
	 */
	public static AmqpOutboundEndpointSpec outboundAdapter(AmqpTemplate amqpTemplate) {
		return new AmqpOutboundEndpointSpec(amqpTemplate, false);
	}

	/**
	 * Create an initial AmqpOutboundEndpointSpec (gateway).
	 * @param amqpTemplate the amqpTemplate.
	 * @return the AmqpOutboundEndpointSpec.
	 */
	public static AmqpOutboundEndpointSpec outboundGateway(AmqpTemplate amqpTemplate) {
		return new AmqpOutboundEndpointSpec(amqpTemplate, true);
	}

	/**
	 * Create an initial AmqpPollableMessageChannelSpec.
	 * @param connectionFactory the connectionFactory.
	 * @param <S> the spec type.
	 * @return the AmqpPollableMessageChannelSpec.
	 */
	public static <S extends AmqpPollableMessageChannelSpec<S>> AmqpPollableMessageChannelSpec<S> pollableChannel(
			ConnectionFactory connectionFactory) {
		return pollableChannel(null, connectionFactory);
	}

	/**
	 * Create an initial AmqpPollableMessageChannelSpec.
	 * @param id the id.
	 * @param connectionFactory the connectionFactory.
	 * @param <S> the spec type.
	 * @return the AmqpPollableMessageChannelSpec.
	 */
	public static <S extends AmqpPollableMessageChannelSpec<S>> AmqpPollableMessageChannelSpec<S> pollableChannel(
			String id, ConnectionFactory connectionFactory) {
		return new AmqpPollableMessageChannelSpec<S>(connectionFactory).id(id);
	}

	/**
	 * Create an initial AmqpMessageChannelSpec.
	 * @param connectionFactory the connectionFactory.
	 * @param <S> the spec type.
	 * @return the AmqpMessageChannelSpec.
	 */
	public static <S extends AmqpMessageChannelSpec<S>> AmqpMessageChannelSpec<S> channel(
			ConnectionFactory connectionFactory) {
		return channel(null, connectionFactory);
	}

	/**
	 * Create an initial AmqpMessageChannelSpec.
	 * @param id the id.
	 * @param connectionFactory the connectionFactory.
	 * @param <S> the spec type.
	 * @return the AmqpMessageChannelSpec.
	 */
	public static <S extends AmqpMessageChannelSpec<S>> AmqpMessageChannelSpec<S> channel(String id,
			ConnectionFactory connectionFactory) {
		return new AmqpMessageChannelSpec<S>(connectionFactory).id(id);
	}

	/**
	 * Create an initial AmqpPublishSubscribeMessageChannelSpec.
	 * @param connectionFactory the connectionFactory.
	 * @return the AmqpPublishSubscribeMessageChannelSpec.
	 */
	public static AmqpPublishSubscribeMessageChannelSpec publishSubscribeChannel(ConnectionFactory connectionFactory) {
		return publishSubscribeChannel(null, connectionFactory);
	}

	/**
	 * Create an initial AmqpPublishSubscribeMessageChannelSpec.
	 * @param id the id.
	 * @param connectionFactory the connectionFactory.
	 * @return the AmqpPublishSubscribeMessageChannelSpec.
	 */
	public static AmqpPublishSubscribeMessageChannelSpec publishSubscribeChannel(String id,
			ConnectionFactory connectionFactory) {
		return new AmqpPublishSubscribeMessageChannelSpec(connectionFactory).id(id);
	}

}
