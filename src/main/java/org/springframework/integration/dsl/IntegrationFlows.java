/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.dsl;

import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.channel.MessageChannelSpec;
import org.springframework.integration.dsl.core.ComponentsRegistration;
import org.springframework.integration.dsl.core.MessageProducerSpec;
import org.springframework.integration.dsl.core.MessageSourceSpec;
import org.springframework.integration.dsl.core.MessagingGatewaySpec;
import org.springframework.integration.dsl.support.Consumer;
import org.springframework.integration.dsl.support.FixedSubscriberChannelPrototype;
import org.springframework.integration.dsl.support.Function;
import org.springframework.integration.dsl.support.MessageChannelReference;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.endpoint.MethodInvokingMessageSource;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * The central factory for fluent {@link IntegrationFlowBuilder} API.
 *
 * @author Artem Bilan
 *
 * @see org.springframework.integration.dsl.config.IntegrationFlowBeanPostProcessor
 */
public final class IntegrationFlows {

	/**
	 * Populate the {@link MessageChannel} name to the new {@link IntegrationFlowBuilder} chain.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code inputChannel}.
	 * @param messageChannelName the name of existing {@link MessageChannel} bean.
	 * The new {@link DirectChannel} bean will be created on context startup
	 * if there is no bean with this name.
	 * @return new {@link IntegrationFlowBuilder}.
	 */
	public static IntegrationFlowBuilder from(String messageChannelName) {
		return from(new MessageChannelReference(messageChannelName));
	}

	/**
	 * Populate the {@link MessageChannel} name to the new {@link IntegrationFlowBuilder} chain.
	 * Typically for the {@link org.springframework.integration.channel.FixedSubscriberChannel} together
	 * with {@code fixedSubscriber = true}.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code inputChannel}.
	 * @param messageChannelName the name for {@link DirectChannel} or
	 * {@link org.springframework.integration.channel.FixedSubscriberChannel}
	 * to be created on context startup, not reference.
	 * The {@link MessageChannel} depends on the {@code fixedSubscriber} boolean argument.
	 * @param fixedSubscriber the boolean flag to determine if result {@link MessageChannel} should
	 * be {@link DirectChannel}, if {@code false} or
	 * {@link org.springframework.integration.channel.FixedSubscriberChannel}, if {@code true}.
	 * @return new {@link IntegrationFlowBuilder}.
	 * @see DirectChannel
	 * @see org.springframework.integration.channel.FixedSubscriberChannel
	 */
	public static IntegrationFlowBuilder from(String messageChannelName, boolean fixedSubscriber) {
		return fixedSubscriber
				? from(new FixedSubscriberChannelPrototype(messageChannelName))
				: from(messageChannelName);
	}

	/**
	 * Populate the {@link MessageChannel} object to the
	 * {@link IntegrationFlowBuilder} chain using the fluent API from {@link Channels} factory.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code inputChannel}.
	 * @param channels the {@link ChannelsFunction} to use method chain to configure
	 * {@link MessageChannel} via {@link Channels} factory.
	 * @return new {@link IntegrationFlowBuilder}.
	 * @see Channels
	 */
	public static IntegrationFlowBuilder from(ChannelsFunction channels) {
		Assert.notNull(channels, "'channels' must not be null");
		return from(channels.apply(new Channels()));
	}

	/**
	 * Populate the {@link MessageChannel} object to the
	 * {@link IntegrationFlowBuilder} chain using the fluent API from {@link MessageChannelSpec}.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code inputChannel}.
	 * @param messageChannelSpec the MessageChannelSpec to populate {@link MessageChannel} instance.
	 * @return new {@link IntegrationFlowBuilder}.
	 * @see org.springframework.integration.dsl.channel.MessageChannels
	 */
	public static IntegrationFlowBuilder from(MessageChannelSpec<?, ?> messageChannelSpec) {
		Assert.notNull(messageChannelSpec, "'messageChannelSpec' must not be null");
		return from(messageChannelSpec.get());
	}

	/**
	 * Populate the provided {@link MessageChannel} object to the {@link IntegrationFlowBuilder} chain.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code inputChannel}.
	 * @param messageChannel the {@link MessageChannel} to populate.
	 * @return new {@link IntegrationFlowBuilder}.
	 */
	public static IntegrationFlowBuilder from(MessageChannel messageChannel) {
		return new IntegrationFlowBuilder().channel(messageChannel);
	}

	/**
	 * Populate the {@link MessageSource} object to the {@link IntegrationFlowBuilder} chain
	 * using the fluent API from {@link MessageSources} factory.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code startMessageSource}.
	 * @param sources the {@link MessageSourcesFunction} to use.
	 * @return new {@link IntegrationFlowBuilder}.
	 * @see MessageSources
	 */
	public static IntegrationFlowBuilder from(MessageSourcesFunction sources) {
		return from(sources, (Consumer<SourcePollingChannelAdapterSpec>) null);
	}

	/**
	 * Populate the {@link MessageSource} object to the {@link IntegrationFlowBuilder} chain
	 * using the fluent API from the {@link MessageSources} factory.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code startMessageSource}.
	 * In addition use {@link SourcePollingChannelAdapterSpec} to provide options for the underlying
	 * {@link org.springframework.integration.endpoint.SourcePollingChannelAdapter} endpoint.
	 * @param sources the {@link MessageSourcesFunction} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide more options for the
	 * {@link org.springframework.integration.config.SourcePollingChannelAdapterFactoryBean}.
	 * @return new {@link IntegrationFlowBuilder}.
	 * @see MessageSources
	 * @see SourcePollingChannelAdapterSpec
	 */
	public static IntegrationFlowBuilder from(MessageSourcesFunction sources,
			Consumer<SourcePollingChannelAdapterSpec> endpointConfigurer) {
		Assert.notNull(sources, "'sources' must not be null");
		return from(sources.apply(new MessageSources()), endpointConfigurer);
	}

	/**
	 * Populate the {@link MessageSource} object to the {@link IntegrationFlowBuilder} chain
	 * using the fluent API from the provided {@link MessageSourceSpec}.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code startMessageSource}.
	 * @param messageSourceSpec the {@link MessageSourceSpec} to use.
	 * @return new {@link IntegrationFlowBuilder}.
	 * @see MessageSourceSpec and its implementations.
	 */
	public static IntegrationFlowBuilder from(MessageSourceSpec<?, ? extends MessageSource<?>> messageSourceSpec) {
		return from(messageSourceSpec, (Consumer<SourcePollingChannelAdapterSpec>) null);
	}

	/**
	 * Populate the {@link MessageSource} object to the {@link IntegrationFlowBuilder} chain
	 * using the fluent API from the provided {@link MessageSourceSpec}.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code startMessageSource}.
	 * @param messageSourceSpec the {@link MessageSourceSpec} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide more options for the
	 * {@link org.springframework.integration.config.SourcePollingChannelAdapterFactoryBean}.
	 * @return new {@link IntegrationFlowBuilder}.
	 * @see MessageSourceSpec
	 * @see SourcePollingChannelAdapterSpec
	 */
	public static IntegrationFlowBuilder from(MessageSourceSpec<?, ? extends MessageSource<?>> messageSourceSpec,
			Consumer<SourcePollingChannelAdapterSpec> endpointConfigurer) {
		Assert.notNull(messageSourceSpec, "'messageSourceSpec' must not be null");
		return from(messageSourceSpec.get(), endpointConfigurer, registerComponents(messageSourceSpec));
	}

	/**
	 * Populate the provided {@link MethodInvokingMessageSource} for the method of the provided service.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code startMessageSource}.
	 * @param service the service to use.
	 * @param methodName the method to invoke.
	 * @return new {@link IntegrationFlowBuilder}.
	 * @since 1.1
	 * @see MethodInvokingMessageSource
	 */
	public static IntegrationFlowBuilder from(Object service, String methodName) {
		return from(service, methodName, null);
	}

	/**
	 * Populate the provided {@link MethodInvokingMessageSource} for the method of the provided service.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code startMessageSource}.
	 * @param service the service to use.
	 * @param methodName the method to invoke.
	 * @param endpointConfigurer the {@link Consumer} to provide more options for the
	 * {@link org.springframework.integration.config.SourcePollingChannelAdapterFactoryBean}.
	 * @return new {@link IntegrationFlowBuilder}.
	 * @since 1.1
	 * @see MethodInvokingMessageSource
	 */
	public static IntegrationFlowBuilder from(Object service, String methodName,
			Consumer<SourcePollingChannelAdapterSpec> endpointConfigurer) {
		Assert.notNull(service, "'service' must not be null");
		Assert.hasText(methodName, "'methodName' must not be empty");
		MethodInvokingMessageSource messageSource = new MethodInvokingMessageSource();
		messageSource.setObject(service);
		messageSource.setMethodName(methodName);
		return from(messageSource, endpointConfigurer);
	}

	/**
	 * Populate the provided {@link MessageSource} object to the {@link IntegrationFlowBuilder} chain.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code startMessageSource}.
	 * @param messageSource the {@link MessageSource} to populate.
	 * @return new {@link IntegrationFlowBuilder}.
	 * @see MessageSource
	 */
	public static IntegrationFlowBuilder from(MessageSource<?> messageSource) {
		return from(messageSource, (Consumer<SourcePollingChannelAdapterSpec>) null);
	}

	/**
	 * Populate the provided {@link MessageSource} object to the {@link IntegrationFlowBuilder} chain.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code startMessageSource}.
	 * In addition use {@link SourcePollingChannelAdapterSpec} to provide options for the underlying
	 * {@link org.springframework.integration.endpoint.SourcePollingChannelAdapter} endpoint.
	 * @param messageSource the {@link MessageSource} to populate.
	 * @param endpointConfigurer the {@link Consumer} to provide more options for the
	 * {@link org.springframework.integration.config.SourcePollingChannelAdapterFactoryBean}.
	 * @return new {@link IntegrationFlowBuilder}.
	 * @see MessageSource
	 * @see SourcePollingChannelAdapterSpec
	 */
	public static IntegrationFlowBuilder from(MessageSource<?> messageSource,
			Consumer<SourcePollingChannelAdapterSpec> endpointConfigurer) {
		return from(messageSource, endpointConfigurer, null);
	}

	private static IntegrationFlowBuilder from(MessageSource<?> messageSource,
			Consumer<SourcePollingChannelAdapterSpec> endpointConfigurer,
			IntegrationFlowBuilder integrationFlowBuilder) {
		SourcePollingChannelAdapterSpec spec = new SourcePollingChannelAdapterSpec(messageSource);
		if (endpointConfigurer != null) {
			endpointConfigurer.accept(spec);
		}
		if (integrationFlowBuilder == null) {
			integrationFlowBuilder = new IntegrationFlowBuilder();
		}
		return integrationFlowBuilder.addComponent(spec)
				.currentComponent(spec);
	}

	/**
	 * Populate the {@link MessageProducerSupport} object to the {@link IntegrationFlowBuilder} chain
	 * using the fluent API from the {@link MessageProducers} factory.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code startMessageProducer}.
	 * @param producers the {@link MessageProducersFunction} to use.
	 * @return new {@link IntegrationFlowBuilder}.
	 * @see MessageProducers
	 */
	public static IntegrationFlowBuilder from(MessageProducersFunction producers) {
		return from(producers.apply(new MessageProducers()));
	}

	/**
	 * Populate the {@link MessageProducerSupport} object to the {@link IntegrationFlowBuilder} chain
	 * using the fluent API from the {@link MessageProducerSpec}.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code startMessageProducer}.
	 * @param messageProducerSpec the {@link MessageProducerSpec} to use.
	 * @return new {@link IntegrationFlowBuilder}.
	 * @see MessageProducerSpec
	 */
	public static IntegrationFlowBuilder from(MessageProducerSpec<?, ?> messageProducerSpec) {
		return from(messageProducerSpec.get(), registerComponents(messageProducerSpec));
	}

	/**
	 * Populate the provided {@link MessageProducerSupport} object to the {@link IntegrationFlowBuilder} chain.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code startMessageProducer}.
	 * @param messageProducer the {@link MessageProducerSupport} to populate.
	 * @return new {@link IntegrationFlowBuilder}.
	 */
	public static IntegrationFlowBuilder from(MessageProducerSupport messageProducer) {
		return from(messageProducer, (IntegrationFlowBuilder) null);
	}

	private static IntegrationFlowBuilder from(MessageProducerSupport messageProducer,
			IntegrationFlowBuilder integrationFlowBuilder) {
		MessageChannel outputChannel = messageProducer.getOutputChannel();
		if (outputChannel == null) {
			outputChannel = new DirectChannel();
			messageProducer.setOutputChannel(outputChannel);
		}
		if (integrationFlowBuilder == null) {
			integrationFlowBuilder = from(outputChannel);
		}
		else {
			integrationFlowBuilder.channel(outputChannel);
		}
		return integrationFlowBuilder.addComponent(messageProducer);
	}

	/**
	 * Populate the {@link MessagingGatewaySupport} object to the {@link IntegrationFlowBuilder} chain
	 * using the fluent API from the {@link MessagingGateways} factory.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code startMessagingGateway}.
	 * @param gateways the {@link MessagingGatewaysFunction} to use.
	 * @return new {@link IntegrationFlowBuilder}.
	 * @see MessagingGateways
	 */
	public static IntegrationFlowBuilder from(MessagingGatewaysFunction gateways) {
		return from(gateways.apply(new MessagingGateways()));
	}

	/**
	 * Populate the {@link MessagingGatewaySupport} object to the {@link IntegrationFlowBuilder} chain
	 * using the fluent API from the {@link MessagingGatewaySpec}.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code startMessagingGateway}.
	 * @param inboundGatewaySpec the {@link MessagingGatewaysFunction} to use.
	 * @return new {@link IntegrationFlowBuilder}.
	 */
	public static IntegrationFlowBuilder from(MessagingGatewaySpec<?, ?> inboundGatewaySpec) {
		return from(inboundGatewaySpec.get(), registerComponents(inboundGatewaySpec));
	}

	/**
	 * Populate the provided {@link MessagingGatewaySupport} object to the {@link IntegrationFlowBuilder} chain.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code startMessageProducer}.
	 * @param inboundGateway the {@link MessagingGatewaySupport} to populate.
	 * @return new {@link IntegrationFlowBuilder}.
	 */
	public static IntegrationFlowBuilder from(MessagingGatewaySupport inboundGateway) {
		return from(inboundGateway, (IntegrationFlowBuilder) null);
	}

	private static IntegrationFlowBuilder from(MessagingGatewaySupport inboundGateway,
			IntegrationFlowBuilder integrationFlowBuilder) {
		MessageChannel outputChannel = inboundGateway.getRequestChannel();
		if (outputChannel == null) {
			outputChannel = new DirectChannel();
			inboundGateway.setRequestChannel(outputChannel);
		}
		if (integrationFlowBuilder == null) {
			integrationFlowBuilder = from(outputChannel);
		}
		else {
			integrationFlowBuilder.channel(outputChannel);
		}
		return integrationFlowBuilder.addComponent(inboundGateway);
	}

	private static IntegrationFlowBuilder registerComponents(Object spec) {
		if (spec instanceof ComponentsRegistration) {
			return new IntegrationFlowBuilder()
					.addComponents(((ComponentsRegistration) spec).getComponentsToRegister());
		}
		return null;
	}

	private IntegrationFlows() {
	}

	/**
	 * The {@link Channels}-specific {@link Function}.
	 */
	public interface ChannelsFunction extends Function<Channels, MessageChannelSpec<?, ?>> {

	}

	/**
	 * The {@link MessageSources}-specific {@link Function}.
	 */
	public interface MessageSourcesFunction extends Function<MessageSources, MessageSourceSpec<?, ?>> {

	}

	/**
	 * The {@link MessageProducers}-specific {@link Function}.
	 */
	public interface MessageProducersFunction extends Function<MessageProducers, MessageProducerSpec<?, ?>> {

	}

	/**
	 * The {@link MessagingGateways}-specific {@link Function}.
	 */
	public interface MessagingGatewaysFunction extends Function<MessagingGateways, MessagingGatewaySpec<?, ?>> {

	}

}
