/*
 * Copyright 2014-2015 the original author or authors.
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

package org.springframework.integration.dsl.jms;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

/**
 * Factory class for JMS components.
 * @author Artem Bilan
 * @author Gary Russell
 */
public abstract class Jms {

	/**
	 * The factory to produce a {@link JmsPollableMessageChannelSpec}
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @param <S>               the {@link JmsPollableMessageChannelSpec} inheritor type
	 * @return the {@link JmsPollableMessageChannelSpec} instance
	 */
	public static <S extends JmsPollableMessageChannelSpec<S>> JmsPollableMessageChannelSpec<S> pollableChannel(
			ConnectionFactory connectionFactory) {
		return pollableChannel(null, connectionFactory);
	}

	/**
	 * The factory to produce a {@link JmsPollableMessageChannelSpec}
	 * @param id                the bean name for the target {@code PollableChannel} component
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @param <S>               the {@link JmsPollableMessageChannelSpec} inheritor type
	 * @return the {@link JmsPollableMessageChannelSpec} instance
	 */
	public static <S extends JmsPollableMessageChannelSpec<S>> JmsPollableMessageChannelSpec<S> pollableChannel(
			String id, ConnectionFactory connectionFactory) {
		return new JmsPollableMessageChannelSpec<S>(connectionFactory).id(id);
	}

	/**
	 * The factory to produce a {@link JmsMessageChannelSpec}
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @param <S>               the {@link JmsMessageChannelSpec} inheritor type
	 * @return the {@link JmsMessageChannelSpec} instance
	 */
	public static <S extends JmsMessageChannelSpec<S>> JmsMessageChannelSpec<S> channel(
			ConnectionFactory connectionFactory) {
		return channel(null, connectionFactory);
	}

	/**
	 * The factory to produce a {@link JmsMessageChannelSpec}
	 * @param id                the bean name for the target {@code MessageChannel} component
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @param <S>               the {@link JmsMessageChannelSpec} inheritor type
	 * @return the {@link JmsMessageChannelSpec} instance
	 */
	public static <S extends JmsMessageChannelSpec<S>> JmsMessageChannelSpec<S> channel(String id,
	                                                                                    ConnectionFactory connectionFactory) {
		return new JmsMessageChannelSpec<S>(connectionFactory).id(id);
	}

	/**
	 * The factory to produce a {@link JmsPublishSubscribeMessageChannelSpec}
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @return the {@link JmsPublishSubscribeMessageChannelSpec} instance
	 */
	public static JmsPublishSubscribeMessageChannelSpec publishSubscribeChannel(ConnectionFactory connectionFactory) {
		return publishSubscribeChannel(null, connectionFactory);
	}

	/**
	 * The factory to produce a {@link JmsPublishSubscribeMessageChannelSpec}
	 * @param id                the bean name for the target {@code MessageChannel} component
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @return the {@link JmsPublishSubscribeMessageChannelSpec} instance
	 */
	public static JmsPublishSubscribeMessageChannelSpec publishSubscribeChannel(String id,
	                                                                            ConnectionFactory connectionFactory) {
		return new JmsPublishSubscribeMessageChannelSpec(connectionFactory).id(id);
	}

	/**
	 * The factory to produce a {@link JmsOutboundChannelAdapterSpec}
	 * @param jmsTemplate the JmsTemplate to build on
	 * @param <S>         the {@link JmsOutboundChannelAdapterSpec} inheritor type
	 * @return the {@link JmsOutboundChannelAdapterSpec} instance
	 */
	public static <S extends JmsOutboundChannelAdapterSpec<S>> JmsOutboundChannelAdapterSpec<S> outboundAdapter(
			JmsTemplate jmsTemplate) {
		return new JmsOutboundChannelAdapterSpec<S>(jmsTemplate);
	}

	/**
	 * The factory to produce a {@link JmsOutboundChannelAdapterSpec}
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @return the {@link JmsOutboundChannelAdapterSpec} instance
	 */
	public static JmsOutboundChannelAdapterSpec.JmsOutboundChannelSpecTemplateAware outboundAdapter(
			ConnectionFactory connectionFactory) {
		return new JmsOutboundChannelAdapterSpec.JmsOutboundChannelSpecTemplateAware(connectionFactory);
	}

	/**
	 * The factory to produce a {@link JmsInboundChannelAdapterSpec}
	 * @param jmsTemplate the JmsTemplate to build on
	 * @param <S>         the {@link JmsInboundChannelAdapterSpec} inheritor type
	 * @return the {@link JmsInboundChannelAdapterSpec} instance
	 */
	public static <S extends JmsInboundChannelAdapterSpec<S>> JmsInboundChannelAdapterSpec<S> inboundAdapter(
			JmsTemplate jmsTemplate) {
		return new JmsInboundChannelAdapterSpec<S>(jmsTemplate);
	}

	/**
	 * The factory to produce a {@link JmsInboundChannelAdapterSpec}
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @return the {@link JmsInboundChannelAdapterSpec} instance
	 */
	public static JmsInboundChannelAdapterSpec.JmsInboundChannelSpecTemplateAware inboundAdapter(
			ConnectionFactory connectionFactory) {
		return new JmsInboundChannelAdapterSpec.JmsInboundChannelSpecTemplateAware(connectionFactory);
	}

	/**
	 * The factory to produce a {@link JmsOutboundGatewaySpec}
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @return the {@link JmsOutboundGatewaySpec} instance
	 */
	public static JmsOutboundGatewaySpec outboundGateway(ConnectionFactory connectionFactory) {
		return new JmsOutboundGatewaySpec(connectionFactory);
	}

	/**
	 * The factory to produce a {@link JmsOutboundGatewaySpec}
	 * @param listenerContainer the JMS {@link AbstractMessageListenerContainer} to build on
	 * @param <S> the {@link JmsInboundGatewaySpec} inheritor type
	 * @return the {@link JmsOutboundGatewaySpec} instance
	 */
	public static <S extends JmsInboundGatewaySpec<S>> JmsInboundGatewaySpec<S> inboundGateway(
			AbstractMessageListenerContainer listenerContainer) {
		return new JmsInboundGatewaySpec<S>(listenerContainer);
	}

	/**
	 * The factory to produce a {@link JmsOutboundGatewaySpec}
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @return the {@link JmsOutboundGatewaySpec} instance
	 */
	public static JmsInboundGatewaySpec.JmsInboundGatewayListenerContainerSpec<DefaultMessageListenerContainer>
	inboundGateway(ConnectionFactory connectionFactory) {
		return inboundGateway(connectionFactory, DefaultMessageListenerContainer.class);
	}

	/**
	 * The factory to produce a {@link JmsOutboundGatewaySpec}
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @param containerClass    the {@link AbstractMessageListenerContainer} implementation class
	 *                          to instantiate listener container
	 * @param <C>               the {@link AbstractMessageListenerContainer} inheritor type
	 * @return the {@link JmsOutboundGatewaySpec} instance
	 */
	public static <C extends AbstractMessageListenerContainer>
	JmsInboundGatewaySpec.JmsInboundGatewayListenerContainerSpec<C> inboundGateway(ConnectionFactory connectionFactory,
	                                                                               Class<C> containerClass) {
		try {
			JmsListenerContainerSpec<C> spec = new JmsListenerContainerSpec<C>(containerClass)
					.connectionFactory(connectionFactory);
			return new JmsInboundGatewaySpec.JmsInboundGatewayListenerContainerSpec<C>(spec);
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * The factory to produce a {@link JmsMessageDrivenChannelAdapterSpec}
	 * @param listenerContainer the {@link AbstractMessageListenerContainer} to build on
	 * @param <S>               the {@link JmsMessageDrivenChannelAdapterSpec} inheritor type
	 * @return the {@link JmsMessageDrivenChannelAdapterSpec} instance
	 */
	public static <S extends JmsMessageDrivenChannelAdapterSpec<S>>
	JmsMessageDrivenChannelAdapterSpec<S> messageDriverChannelAdapter(
			AbstractMessageListenerContainer listenerContainer) {
		return new JmsMessageDrivenChannelAdapterSpec<S>(listenerContainer);
	}

	/**
	 * The factory to produce a {@link JmsMessageDrivenChannelAdapterSpec}
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @return the {@link JmsMessageDrivenChannelAdapterSpec} instance
	 */
	public static JmsMessageDrivenChannelAdapterSpec.JmsMessageDrivenChannelAdapterListenerContainerSpec<DefaultMessageListenerContainer>
	messageDriverChannelAdapter(ConnectionFactory connectionFactory) {
		return messageDriverChannelAdapter(connectionFactory, DefaultMessageListenerContainer.class);
	}

	/**
	 * The factory to produce a {@link JmsMessageDrivenChannelAdapterSpec}
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @param containerClass    the {@link AbstractMessageListenerContainer} implementation class
	 *                          to instantiate listener container
	 * @param <C>               the {@link AbstractMessageListenerContainer} inheritor type
	 * @return the {@link JmsMessageDrivenChannelAdapterSpec} instance
	 */
	public static <C extends AbstractMessageListenerContainer>
	JmsMessageDrivenChannelAdapterSpec.JmsMessageDrivenChannelAdapterListenerContainerSpec<C>
	messageDriverChannelAdapter(ConnectionFactory connectionFactory, Class<C> containerClass) {
		try {
			JmsListenerContainerSpec<C> spec = new JmsListenerContainerSpec<C>(containerClass)
					.connectionFactory(connectionFactory);
			return new JmsMessageDrivenChannelAdapterSpec.JmsMessageDrivenChannelAdapterListenerContainerSpec<C>(spec);
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * The factory to produce a {@link JmsListenerContainerSpec}
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @param destination       the {@link Destination} to listen to
	 * @return the {@link JmsListenerContainerSpec} instance
	 * @since 1.1
	 */
	public static JmsListenerContainerSpec<DefaultMessageListenerContainer> container(
			ConnectionFactory connectionFactory, Destination destination) {
		try {
			return new JmsListenerContainerSpec<DefaultMessageListenerContainer>(DefaultMessageListenerContainer.class)
					.connectionFactory(connectionFactory)
					.destination(destination);
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * The factory to produce a {@link JmsListenerContainerSpec}
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @param destinationName   the destination name to listen to
	 * @return the {@link JmsListenerContainerSpec} instance
	 * @since 1.1
	 */
	public static JmsListenerContainerSpec<DefaultMessageListenerContainer> container(
			ConnectionFactory connectionFactory, String destinationName) {
		try {
			return new JmsListenerContainerSpec<DefaultMessageListenerContainer>(DefaultMessageListenerContainer.class)
					.connectionFactory(connectionFactory)
					.destination(destinationName);
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

}
