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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.reactivestreams.Publisher;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.aggregator.BarrierMessageHandler;
import org.springframework.integration.aggregator.ResequencingMessageHandler;
import org.springframework.integration.channel.ChannelInterceptorAware;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.config.SourcePollingChannelAdapterFactoryBean;
import org.springframework.integration.core.GenericSelector;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.dsl.channel.MessageChannelSpec;
import org.springframework.integration.dsl.channel.WireTapSpec;
import org.springframework.integration.dsl.core.ComponentsRegistration;
import org.springframework.integration.dsl.core.ConsumerEndpointSpec;
import org.springframework.integration.dsl.core.MessageHandlerSpec;
import org.springframework.integration.dsl.core.MessageProcessorSpec;
import org.springframework.integration.dsl.support.BeanNameMessageProcessor;
import org.springframework.integration.dsl.support.Consumer;
import org.springframework.integration.dsl.support.FixedSubscriberChannelPrototype;
import org.springframework.integration.dsl.support.Function;
import org.springframework.integration.dsl.support.FunctionExpression;
import org.springframework.integration.dsl.support.GenericHandler;
import org.springframework.integration.dsl.support.MapBuilder;
import org.springframework.integration.dsl.support.MessageChannelReference;
import org.springframework.integration.dsl.support.tuple.Tuple2;
import org.springframework.integration.expression.ControlBusMethodFilter;
import org.springframework.integration.filter.ExpressionEvaluatingSelector;
import org.springframework.integration.filter.MessageFilter;
import org.springframework.integration.filter.MethodInvokingSelector;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.integration.handler.DelayHandler;
import org.springframework.integration.handler.ExpressionCommandMessageProcessor;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.MessageTriggerAction;
import org.springframework.integration.handler.MethodInvokingMessageProcessor;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.router.AbstractMappingMessageRouter;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.integration.router.ExpressionEvaluatingRouter;
import org.springframework.integration.router.MethodInvokingRouter;
import org.springframework.integration.router.RecipientListRouter;
import org.springframework.integration.scattergather.ScatterGatherHandler;
import org.springframework.integration.splitter.AbstractMessageSplitter;
import org.springframework.integration.splitter.DefaultMessageSplitter;
import org.springframework.integration.splitter.ExpressionEvaluatingSplitter;
import org.springframework.integration.splitter.MethodInvokingSplitter;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.transformer.ClaimCheckInTransformer;
import org.springframework.integration.transformer.ClaimCheckOutTransformer;
import org.springframework.integration.transformer.ContentEnricher;
import org.springframework.integration.transformer.ExpressionEvaluatingTransformer;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.integration.transformer.HeaderFilter;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.integration.transformer.MethodInvokingTransformer;
import org.springframework.integration.transformer.Transformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * The {@code Builder} pattern implementation for the EIP-method chain.
 * Provides a variety of methods to populate Spring Integration components
 * to an {@link IntegrationFlow} for the future registration in the
 * application context.
 *
 * @param <B> the {@link IntegrationFlowDefinition} implementation type.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Gabriele Del Prete
 *
 * @see org.springframework.integration.dsl.config.IntegrationFlowBeanPostProcessor
 */
public abstract class IntegrationFlowDefinition<B extends IntegrationFlowDefinition<B>> {

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	private static final Set<MessageHandler> REFERENCED_REPLY_PRODUCERS = new HashSet<MessageHandler>();

	protected final Set<Object> integrationComponents = new LinkedHashSet<Object>();

	protected MessageChannel currentMessageChannel;

	protected Object currentComponent;

	private StandardIntegrationFlow integrationFlow;

	IntegrationFlowDefinition() {
	}

	B addComponent(Object component) {
		this.integrationComponents.add(component);
		return _this();
	}

	B addComponents(Collection<Object> components) {
		if (components != null) {
			for (Object component : components) {
				this.integrationComponents.add(component);
			}
		}
		return _this();
	}

	B currentComponent(Object component) {
		this.currentComponent = component;
		return _this();
	}

	/**
	 * Populate an {@link org.springframework.integration.channel.FixedSubscriberChannel} instance
	 * at the current {@link IntegrationFlow} chain position.
	 * The 'bean name' will be generated during the bean registration phase.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B fixedSubscriberChannel() {
		return fixedSubscriberChannel(null);
	}

	/**
	 * Populate an {@link org.springframework.integration.channel.FixedSubscriberChannel} instance
	 * at the current {@link IntegrationFlow} chain position.
	 * The provided {@code messageChannelName} is used for the bean registration.
	 * @param messageChannelName the bean name to use.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B fixedSubscriberChannel(String messageChannelName) {
		return channel(new FixedSubscriberChannelPrototype(messageChannelName));
	}

	/**
	 * Populate a {@link MessageChannelReference} instance
	 * at the current {@link IntegrationFlow} chain position.
	 * The provided {@code messageChannelName} is used for the bean registration
	 * ({@link org.springframework.integration.channel.DirectChannel}), if there is no such a bean
	 * in the application context. Otherwise the existing {@link MessageChannel} bean is used
	 * to wire integration endpoints.
	 * @param messageChannelName the bean name to use.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B channel(String messageChannelName) {
		return channel(new MessageChannelReference(messageChannelName));
	}

	/**
	 * Populate a {@link MessageChannel} instance
	 * at the current {@link IntegrationFlow} chain position using the {@link Channels}
	 * factory fluent API.
	 * @param channels the {@link Function} to use.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B channel(Function<Channels, MessageChannelSpec<?, ?>> channels) {
		Assert.notNull(channels, "'channels' must not be null");
		return channel(channels.apply(new Channels()));
	}

	/**
	 * Populate a {@link MessageChannel} instance
	 * at the current {@link IntegrationFlow} chain position using the {@link MessageChannelSpec}
	 * fluent API.
	 * @param messageChannelSpec the {@link MessageChannelSpec} to use.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see org.springframework.integration.dsl.channel.MessageChannels
	 */
	public B channel(MessageChannelSpec<?, ?> messageChannelSpec) {
		Assert.notNull(messageChannelSpec, "'messageChannelSpec' must not be null");
		return channel(messageChannelSpec.get());
	}

	/**
	 * Populate the provided {@link MessageChannel} instance
	 * at the current {@link IntegrationFlow} chain position.
	 * The {@code messageChannel} can be an existing bean, or fresh instance, in which case
	 * the {@link org.springframework.integration.dsl.config.IntegrationFlowBeanPostProcessor}
	 * will populate it as a bean with a generated name.
	 * @param messageChannel the {@link MessageChannel} to populate.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B channel(MessageChannel messageChannel) {
		Assert.notNull(messageChannel, "'messageChannel' must not be null");
		if (this.currentMessageChannel != null) {
			bridge(null);
		}
		this.currentMessageChannel = messageChannel;
		return registerOutputChannelIfCan(this.currentMessageChannel);
	}

	/**
	 * The {@link org.springframework.integration.channel.PublishSubscribeChannel} {@link #channel}
	 * method specific implementation to allow the use of the 'subflow' subscriber capability.
	 * @param publishSubscribeChannelConfigurer the {@link Consumer} to specify
	 * {@link PublishSubscribeSpec} options including 'subflow' definition.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B publishSubscribeChannel(Consumer<PublishSubscribeSpec> publishSubscribeChannelConfigurer) {
		return publishSubscribeChannel(null, publishSubscribeChannelConfigurer);
	}

	/**
	 * The {@link org.springframework.integration.channel.PublishSubscribeChannel} {@link #channel}
	 * method specific implementation to allow the use of the 'subflow' subscriber capability.
	 * Use the provided {@link Executor} for the target subscribers.
	 * @param executor the {@link Executor} to use.
	 * @param publishSubscribeChannelConfigurer the {@link Consumer} to specify
	 * {@link PublishSubscribeSpec} options including 'subflow' definition.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B publishSubscribeChannel(Executor executor,
			Consumer<PublishSubscribeSpec> publishSubscribeChannelConfigurer) {
		Assert.notNull(publishSubscribeChannelConfigurer, "'publishSubscribeChannelConfigurer' must not be null");
		PublishSubscribeSpec spec = new PublishSubscribeSpec(executor);
		publishSubscribeChannelConfigurer.accept(spec);
		return addComponents(spec.getComponentsToRegister()).channel(spec);
	}

	/**
	 * Populate the {@code Wire Tap} EI Pattern specific
	 * {@link org.springframework.messaging.support.ChannelInterceptor} implementation
	 * to the current {@link #currentMessageChannel}.
	 * It is useful when an implicit {@link MessageChannel} is used between endpoints:
	 * <pre class="code">
	 * {@code
	 *  .filter("World"::equals)
	 *  .wireTap(sf -> sf.<String, String>transform(String::toUpperCase))
	 *  .handle(p -> process(p))
	 * }
	 * </pre>
	 * This method can be used after any {@link #channel} for explicit {@link MessageChannel},
	 * but with the caution do not impact existing {@link org.springframework.messaging.support.ChannelInterceptor}s.
	 * @param flow the {@link IntegrationFlow} for wire-tap subflow as an alternative to the {@code wireTapChannel}.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B wireTap(IntegrationFlow flow) {
		return wireTap(flow, null);
	}

	/**
	 * Populate the {@code Wire Tap} EI Pattern specific
	 * {@link org.springframework.messaging.support.ChannelInterceptor} implementation
	 * to the current {@link #currentMessageChannel}.
	 * It is useful when an implicit {@link MessageChannel} is used between endpoints:
	 * <pre class="code">
	 * {@code
	 *  f -> f.wireTap("tapChannel")
	 *    .handle(p -> process(p))
	 * }
	 * </pre>
	 * This method can be used after any {@link #channel} for explicit {@link MessageChannel},
	 * but with the caution do not impact existing {@link org.springframework.messaging.support.ChannelInterceptor}s.
	 * @param wireTapChannel the {@link MessageChannel} bean name to wire-tap.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B wireTap(String wireTapChannel) {
		return wireTap(wireTapChannel, null);
	}

	/**
	 * Populate the {@code Wire Tap} EI Pattern specific
	 * {@link org.springframework.messaging.support.ChannelInterceptor} implementation
	 * to the current {@link #currentMessageChannel}.
	 * It is useful when an implicit {@link MessageChannel} is used between endpoints:
	 * <pre class="code">
	 * {@code
	 *  .transform("payload")
	 *  .wireTap(tapChannel())
	 *  .channel("foo")
	 * }
	 * </pre>
	 * This method can be used after any {@link #channel} for explicit {@link MessageChannel},
	 * but with the caution do not impact existing {@link org.springframework.messaging.support.ChannelInterceptor}s.
	 * @param wireTapChannel the {@link MessageChannel} to wire-tap.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B wireTap(MessageChannel wireTapChannel) {
		return wireTap(wireTapChannel, null);
	}

	/**
	 * Populate the {@code Wire Tap} EI Pattern specific
	 * {@link org.springframework.messaging.support.ChannelInterceptor} implementation
	 * to the current {@link #currentMessageChannel}.
	 * It is useful when an implicit {@link MessageChannel} is used between endpoints:
	 * <pre class="code">
	 * {@code
	 *  .transform("payload")
	 *  .wireTap(sf -> sf.<String, String>transform(String::toUpperCase), wt -> wt.selector("payload == 'foo'"))
	 *  .channel("foo")
	 * }
	 * </pre>
	 * This method can be used after any {@link #channel} for explicit {@link MessageChannel},
	 * but with the caution do not impact existing {@link org.springframework.messaging.support.ChannelInterceptor}s.
	 * @param flow the {@link IntegrationFlow} for wire-tap subflow as an alternative to the {@code wireTapChannel}.
	 * @param wireTapConfigurer the {@link Consumer} to accept options for the {@link WireTap}.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B wireTap(IntegrationFlow flow, Consumer<WireTapSpec> wireTapConfigurer) {
		DirectChannel wireTapChannel = new DirectChannel();
		IntegrationFlowBuilder flowBuilder = IntegrationFlows.from(wireTapChannel);
		flow.configure(flowBuilder);
		addComponent(flowBuilder.get());
		return wireTap(wireTapChannel, wireTapConfigurer);
	}

	/**
	 * Populate the {@code Wire Tap} EI Pattern specific
	 * {@link org.springframework.messaging.support.ChannelInterceptor} implementation
	 * to the current {@link #currentMessageChannel}.
	 * It is useful when an implicit {@link MessageChannel} is used between endpoints:
	 * <pre class="code">
	 * {@code
	 *  .transform("payload")
	 *  .wireTap("tapChannel", wt -> wt.selector(m -> m.getPayload().equals("foo")))
	 *  .channel("foo")
	 * }
	 * </pre>
	 * This method can be used after any {@link #channel} for explicit {@link MessageChannel},
	 * but with the caution do not impact existing {@link org.springframework.messaging.support.ChannelInterceptor}s.
	 * @param wireTapChannel the {@link MessageChannel} bean name to wire-tap.
	 * @param wireTapConfigurer the {@link Consumer} to accept options for the {@link WireTap}.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B wireTap(String wireTapChannel, Consumer<WireTapSpec> wireTapConfigurer) {
		DirectChannel internalWireTapChannel = new DirectChannel();
		addComponent(IntegrationFlows.from(internalWireTapChannel).channel(wireTapChannel).get());
		return wireTap(internalWireTapChannel, wireTapConfigurer);
	}

	/**
	 * Populate the {@code Wire Tap} EI Pattern specific
	 * {@link org.springframework.messaging.support.ChannelInterceptor} implementation
	 * to the current {@link #currentMessageChannel}.
	 * It is useful when an implicit {@link MessageChannel} is used between endpoints:
	 * <pre class="code">
	 * {@code
	 *  .transform("payload")
	 *  .wireTap(tapChannel(), wt -> wt.selector(m -> m.getPayload().equals("foo")))
	 *  .channel("foo")
	 * }
	 * </pre>
	 * This method can be used after any {@link #channel} for explicit {@link MessageChannel},
	 * but with the caution do not impact existing {@link org.springframework.messaging.support.ChannelInterceptor}s.
	 * @param wireTapChannel the {@link MessageChannel} to wire-tap.
	 * @param wireTapConfigurer the {@link Consumer} to accept options for the {@link WireTap}.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B wireTap(MessageChannel wireTapChannel, Consumer<WireTapSpec> wireTapConfigurer) {
		WireTapSpec wireTapSpec = new WireTapSpec(wireTapChannel);
		if (wireTapConfigurer != null) {
			wireTapConfigurer.accept(wireTapSpec);
		}
		addComponent(wireTapChannel);
		return wireTap(wireTapSpec);
	}

	/**
	 * Populate the {@code Wire Tap} EI Pattern specific
	 * {@link org.springframework.messaging.support.ChannelInterceptor} implementation
	 * to the current {@link #currentMessageChannel}.
	 * It is useful when an implicit {@link MessageChannel} is used between endpoints:
	 * <pre class="code">
	 * {@code
	 *  .transform("payload")
	 *  .wireTap(new WireTap(tapChannel().selector(m -> m.getPayload().equals("foo")))
	 *  .channel("foo")
	 * }
	 * </pre>
	 * This method can be used after any {@link #channel} for explicit {@link MessageChannel},
	 * but with the caution do not impact existing {@link org.springframework.messaging.support.ChannelInterceptor}s.
	 * @param wireTapSpec the {@link WireTapSpec} to use.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.2
	 */
	public B wireTap(WireTapSpec wireTapSpec) {
		WireTap interceptor = wireTapSpec.get();
		if (this.currentMessageChannel == null || !(this.currentMessageChannel instanceof ChannelInterceptorAware)) {
			channel(new DirectChannel());
		}
		addComponents(wireTapSpec.getComponentsToRegister());
		((ChannelInterceptorAware) this.currentMessageChannel).addInterceptor(interceptor);
		return _this();
	}

	/**
	 * Populate the {@code Control Bus} EI Pattern specific {@link MessageHandler} implementation
	 * at the current {@link IntegrationFlow} chain position.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see ExpressionCommandMessageProcessor
	 */
	public B controlBus() {
		return controlBus(null);
	}

	/**
	 * Populate the {@code Control Bus} EI Pattern specific {@link MessageHandler} implementation
	 * at the current {@link IntegrationFlow} chain position.
	 * @param endpointConfigurer the {@link Consumer} to accept integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see ExpressionCommandMessageProcessor
	 * @see GenericEndpointSpec
	 */
	public B controlBus(Consumer<GenericEndpointSpec<ServiceActivatingHandler>> endpointConfigurer) {
		return this.handle(new ServiceActivatingHandler(new ExpressionCommandMessageProcessor(
				new ControlBusMethodFilter())), endpointConfigurer);
	}

	/**
	 * Populate the {@code Transformer} EI Pattern specific {@link MessageHandler} implementation
	 * for the SpEL {@link Expression}.
	 * @param expression the {@code Transformer} {@link Expression}.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see ExpressionEvaluatingTransformer
	 */
	public B transform(String expression) {
		return transform(expression, (Consumer<GenericEndpointSpec<MessageTransformingHandler>>) null);
	}

	/**
	 * Populate the {@code Transformer} EI Pattern specific {@link MessageHandler} implementation
	 * for the SpEL {@link Expression}.
	 * @param expression the {@code Transformer} {@link Expression}.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 * @see ExpressionEvaluatingTransformer
	 */
	public B transform(String expression, Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) {
		Assert.hasText(expression, "'expression' must not be empty");
		return transform(new ExpressionEvaluatingTransformer(PARSER.parseExpression(expression)),
				endpointConfigurer);
	}

	/**
	 * Populate the {@code MessageTransformingHandler} for the {@link MethodInvokingTransformer}
	 * to invoke the discovered service method at runtime.
	 * @param service the service to use.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 * @see ExpressionEvaluatingTransformer
	 */
	public B transform(Object service) {
		return transform(service, null);
	}

	/**
	 * Populate the {@code MessageTransformingHandler} for the {@link MethodInvokingTransformer}
	 * to invoke the service method at runtime.
	 * @param service the service to use.
	 * @param methodName the method to invoke.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 * @see MethodInvokingTransformer
	 */
	public B transform(Object service, String methodName) {
		return transform(service, methodName, null);
	}

	/**
	 * Populate the {@code MessageTransformingHandler} for the {@link MethodInvokingTransformer}
	 * to invoke the service method at runtime.
	 * @param service the service to use.
	 * @param methodName the method to invoke.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 * @see ExpressionEvaluatingTransformer
	 */
	public B transform(Object service, String methodName,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) {
		MethodInvokingTransformer transformer;
		if (StringUtils.hasText(methodName)) {
			transformer = new MethodInvokingTransformer(service, methodName);
		}
		else {
			transformer = new MethodInvokingTransformer(service);
		}

		return transform(transformer, endpointConfigurer);
	}

	/**
	 * Populate the {@link MessageTransformingHandler} instance for the provided {@link GenericTransformer}.
	 * @param genericTransformer the {@link GenericTransformer} to populate.
	 * @param <S> the source type - 'transform from'.
	 * @param <T> the target type - 'transform to'.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see MethodInvokingTransformer
	 * @see LambdaMessageProcessor
	 */
	public <S, T> B transform(GenericTransformer<S, T> genericTransformer) {
		return this.transform(null, genericTransformer);
	}

	/**
	 * Populate the {@link MessageTransformingHandler} instance for the
	 * {@link org.springframework.integration.handler.MessageProcessor} from provided {@link MessageProcessorSpec}.
	 * <pre class="code">
	 * {@code
	 *  .transform(Scripts.script("classpath:myScript.py").valiable("foo", bar()))
	 * }
	 * </pre>
	 * @param messageProcessorSpec the {@link MessageProcessorSpec} to use.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 * @see MethodInvokingTransformer
	 */
	public B transform(MessageProcessorSpec<?> messageProcessorSpec) {
		return transform(messageProcessorSpec, (Consumer<GenericEndpointSpec<MessageTransformingHandler>>) null);
	}

	/**
	 * Populate the {@link MessageTransformingHandler} instance for the
	 * {@link org.springframework.integration.handler.MessageProcessor} from provided {@link MessageProcessorSpec}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * <pre class="code">
	 * {@code
	 *  .transform(Scripts.script("classpath:myScript.py").valiable("foo", bar()),
	 *           e -> e.autoStartup(false))
	 * }
	 * </pre>
	 * @param messageProcessorSpec the {@link MessageProcessorSpec} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 * @see MethodInvokingTransformer
	 */
	public B transform(MessageProcessorSpec<?> messageProcessorSpec,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) {
		Assert.notNull(messageProcessorSpec, "'messageProcessorSpec' must not be null");
		MessageProcessor<?> processor = messageProcessorSpec.get();
		return addComponent(processor)
				.transform(new MethodInvokingTransformer(processor), endpointConfigurer);
	}

	/**
	 * Populate the {@link MessageTransformingHandler} instance for the provided {@link GenericTransformer}
	 * for the specific {@code payloadType} to convert at runtime.
	 * @param payloadType the {@link Class} for expected payload type.
	 * @param genericTransformer the {@link GenericTransformer} to populate.
	 * @param <P> the payload type - 'transform from'.
	 * @param <T> the target type - 'transform to'.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see MethodInvokingTransformer
	 * @see LambdaMessageProcessor
	 */
	public <P, T> B transform(Class<P> payloadType, GenericTransformer<P, T> genericTransformer) {
		return this.transform(payloadType, genericTransformer, null);
	}

	/**
	 * Populate the {@link MessageTransformingHandler} instance for the provided {@link GenericTransformer}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param genericTransformer the {@link GenericTransformer} to populate.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <S> the source type - 'transform from'.
	 * @param <T> the target type - 'transform to'.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see MethodInvokingTransformer
	 * @see LambdaMessageProcessor
	 * @see GenericEndpointSpec
	 */
	public <S, T> B transform(GenericTransformer<S, T> genericTransformer,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) {
		return this.transform(null, genericTransformer, endpointConfigurer);
	}

	/**
	 * Populate the {@link MessageTransformingHandler} instance for the provided {@link GenericTransformer}
	 * for the specific {@code payloadType} to convert at runtime.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param payloadType the {@link Class} for expected payload type.
	 * @param genericTransformer the {@link GenericTransformer} to populate.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <P> the payload type - 'transform from'.
	 * @param <T> the target type - 'transform to'.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see MethodInvokingTransformer
	 * @see LambdaMessageProcessor
	 * @see GenericEndpointSpec
	 */
	public <P, T> B transform(Class<P> payloadType, GenericTransformer<P, T> genericTransformer,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) {
		Assert.notNull(genericTransformer, "'genericTransformer' must not be null");
		Transformer transformer = genericTransformer instanceof Transformer ? (Transformer) genericTransformer :
				(isLambda(genericTransformer)
						? new MethodInvokingTransformer(new LambdaMessageProcessor(genericTransformer, payloadType))
						: new MethodInvokingTransformer(genericTransformer));
		return addComponent(transformer)
				.handle(new MessageTransformingHandler(transformer), endpointConfigurer);
	}

	/**
	 * Populate a {@link MessageFilter} with {@link MessageSelector} for the provided SpEL expression.
	 * @param expression the SpEL expression.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B filter(String expression) {
		return filter(expression, (Consumer<FilterEndpointSpec>) null);
	}

	/**
	 * Populate a {@link MessageFilter} with {@link MessageSelector} for the provided SpEL expression.
	 * In addition accept options for the integration endpoint using {@link FilterEndpointSpec}:
	 * <pre class="code">
	 * {@code
	 *  .filter("payload.hot"), e -> e.autoStartup(false))
	 * }
	 * </pre>
	 * @param expression the SpEL expression.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.0.2
	 * @see FilterEndpointSpec
	 */
	public B filter(String expression, Consumer<FilterEndpointSpec> endpointConfigurer) {
		Assert.hasText(expression, "'expression' must not be empty");
		return filter(new ExpressionEvaluatingSelector(expression), endpointConfigurer);
	}

	/**
	 * Populate a {@link MessageFilter} with {@link MethodInvokingSelector} for the
	 * discovered method of the provided service.
	 * @param service the service to use.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 * @see MethodInvokingSelector
	 */
	public B filter(Object service) {
		return filter(service, null);
	}

	/**
	 * Populate a {@link MessageFilter} with {@link MethodInvokingSelector} for the
	 * method of the provided service.
	 * @param service the service to use.
	 * @param methodName the method to invoke
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 * @see MethodInvokingSelector
	 */
	public B filter(Object service, String methodName) {
		return filter(service, methodName, null);
	}

	/**
	 * Populate a {@link MessageFilter} with {@link MethodInvokingSelector} for the
	 * method of the provided service.
	 * @param service the service to use.
	 * @param methodName the method to invoke
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 * @see MethodInvokingSelector
	 */
	public B filter(Object service, String methodName, Consumer<FilterEndpointSpec> endpointConfigurer) {
		MethodInvokingSelector selector;
		if (StringUtils.hasText(methodName)) {
			selector = new MethodInvokingSelector(service, methodName);
		}
		else {
			selector = new MethodInvokingSelector(service);
		}
		return filter(selector, endpointConfigurer);
	}

	/**
	 * Populate a {@link MessageFilter} with {@link MethodInvokingSelector}
	 * for the provided {@link GenericSelector}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .filter("World"::equals)
	 * }
	 * </pre>
	 * @param genericSelector the {@link GenericSelector} to use.
	 * @param <P> the source payload type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public <P> B filter(GenericSelector<P> genericSelector) {
		return filter(null, genericSelector);
	}

	/**
	 * Populate a {@link MessageFilter} with {@link MethodInvokingSelector}
	 * for the {@link org.springframework.integration.handler.MessageProcessor} from
	 * the provided {@link MessageProcessorSpec}.
	 * <pre class="code">
	 * {@code
	 *  .filter(Scripts.script(scriptResource).lang("ruby"))
	 * }
	 * </pre>
	 * @param messageProcessorSpec the {@link MessageProcessorSpec} to use.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 */
	public B filter(MessageProcessorSpec<?> messageProcessorSpec) {
		return filter(messageProcessorSpec, (Consumer<FilterEndpointSpec>) null);
	}

	/**
	 * Populate a {@link MessageFilter} with {@link MethodInvokingSelector}
	 * for the {@link org.springframework.integration.handler.MessageProcessor} from
	 * the provided {@link MessageProcessorSpec}.
	 * In addition accept options for the integration endpoint using {@link FilterEndpointSpec}.
	 * <pre class="code">
	 * {@code
	 *  .filter(Scripts.script(scriptResource).lang("ruby"),
	 *        e -> e.autoStartup(false))
	 * }
	 * </pre>
	 * @param messageProcessorSpec the {@link MessageProcessorSpec} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 */
	public B filter(MessageProcessorSpec<?> messageProcessorSpec, Consumer<FilterEndpointSpec> endpointConfigurer) {
		Assert.notNull(messageProcessorSpec, "'messageProcessorSpec' must not be null");
		MessageProcessor<?> processor = messageProcessorSpec.get();
		return addComponent(processor)
				.filter(new MethodInvokingSelector(processor), endpointConfigurer);
	}

	/**
	 * Populate a {@link MessageFilter} with {@link MethodInvokingSelector}
	 * for the provided {@link GenericSelector}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .filter(Date.class, p -> p.after(new Date()))
	 * }
	 * </pre>
	 * @param payloadType the {@link Class} for desired {@code payload} type.
	 * @param genericSelector the {@link GenericSelector} to use.
	 * @param <P> the source payload type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see LambdaMessageProcessor
	 */
	public <P> B filter(Class<P> payloadType, GenericSelector<P> genericSelector) {
		return this.filter(payloadType, genericSelector, null);
	}

	/**
	 * Populate a {@link MessageFilter} with {@link MethodInvokingSelector}
	 * for the provided {@link GenericSelector}.
	 * In addition accept options for the integration endpoint using {@link FilterEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .filter("World"::equals, e -> e.autoStartup(false))
	 * }
	 * </pre>
	 * @param genericSelector the {@link GenericSelector} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <P> the source payload type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see FilterEndpointSpec
	 */
	public <P> B filter(GenericSelector<P> genericSelector, Consumer<FilterEndpointSpec> endpointConfigurer) {
		return filter(null, genericSelector, endpointConfigurer);
	}

	/**
	 * Populate a {@link MessageFilter} with {@link MethodInvokingSelector}
	 * for the provided {@link GenericSelector}.
	 * In addition accept options for the integration endpoint using {@link FilterEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .filter(Date.class, p -> p.after(new Date()), e -> e.autoStartup(false))
	 * }
	 * </pre>
	 * @param payloadType the {@link Class} for desired {@code payload} type.
	 * @param genericSelector the {@link GenericSelector} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <P> the source payload type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see LambdaMessageProcessor
	 * @see FilterEndpointSpec
	 */
	public <P> B filter(Class<P> payloadType, GenericSelector<P> genericSelector,
			Consumer<FilterEndpointSpec> endpointConfigurer) {
		Assert.notNull(genericSelector, "'genericSelector' must not be null");
		MessageSelector selector = genericSelector instanceof MessageSelector ? (MessageSelector) genericSelector :
				(isLambda(genericSelector)
						? new MethodInvokingSelector(new LambdaMessageProcessor(genericSelector, payloadType))
						: new MethodInvokingSelector(genericSelector));
		return this.register(new FilterEndpointSpec(new MessageFilter(selector)), endpointConfigurer);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the selected protocol specific
	 * {@link MessageHandler} implementation from {@link Adapters} factory.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .handleWithAdapter(a ->
	 *         a.jmsGateway(this.jmsConnectionFactory)
	 *          .replyContainer()
	 *          .requestDestination("fooQueue"))
	 * }
	 * </pre>
	 * @param adapters the {@link Function} to select and configure protocol specific
	 * {@link MessageHandler}.
	 * @param <H> the target {@link MessageHandler} type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see MessageHandlerSpec
	 */
	public <H extends MessageHandler> B handleWithAdapter(Function<Adapters, MessageHandlerSpec<?, H>> adapters) {
		return handleWithAdapter(adapters, null);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the selected protocol specific
	 * {@link MessageHandler} implementation from {@link Adapters} factory.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .handleWithAdapter(a ->
	 *         a.jmsGateway(this.jmsConnectionFactory)
	 *          .replyContainer()
	 *          .requestDestination("fooQueue"),
	 *       e -> e.autoStartup(false))
	 * }
	 * </pre>
	 * @param adapters the {@link Function} to select and configure protocol specific
	 * {@link MessageHandler}.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <H> the target {@link MessageHandler} type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see MessageHandlerSpec
	 * @see GenericEndpointSpec
	 */
	public <H extends MessageHandler> B handleWithAdapter(Function<Adapters, MessageHandlerSpec<?, H>> adapters,
			Consumer<GenericEndpointSpec<H>> endpointConfigurer) {
		return handle(adapters.apply(new Adapters()), endpointConfigurer);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the selected protocol specific
	 * {@link MessageHandler} implementation from {@code Namespace Factory}:
	 * <pre class="code">
	 * {@code
	 *  .handle(Amqp.outboundAdapter(this.amqpTemplate).routingKeyExpression("headers.routingKey"))
	 * }
	 * </pre>
	 * @param messageHandlerSpec the {@link MessageHandlerSpec} to configure protocol specific
	 * {@link MessageHandler}.
	 * @param <H> the target {@link MessageHandler} type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public <H extends MessageHandler> B handle(MessageHandlerSpec<?, H> messageHandlerSpec) {
		return handle(messageHandlerSpec, (Consumer<GenericEndpointSpec<H>>) null);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the provided
	 * {@link MessageHandler} implementation.
	 * Can be used as Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .handle(m -> System.out.println(m.getPayload())
	 * }
	 * </pre>
	 * @param messageHandler the {@link MessageHandler} to use.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B handle(MessageHandler messageHandler) {
		return handle(messageHandler, (Consumer<GenericEndpointSpec<MessageHandler>>) null);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the
	 * {@link org.springframework.integration.handler.MethodInvokingMessageProcessor}
	 * to invoke the {@code method} for provided {@code bean} at runtime.
	 * @param beanName the bean name to use.
	 * @param methodName the method to invoke.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B handle(String beanName, String methodName) {
		return this.handle(beanName, methodName, null);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the
	 * {@link org.springframework.integration.handler.MethodInvokingMessageProcessor}
	 * to invoke the {@code method} for provided {@code bean} at runtime.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param beanName the bean name to use.
	 * @param methodName the method to invoke.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B handle(String beanName, String methodName,
			Consumer<GenericEndpointSpec<ServiceActivatingHandler>> endpointConfigurer) {
		return handle(new ServiceActivatingHandler(new BeanNameMessageProcessor<Object>(beanName, methodName)),
				endpointConfigurer);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the
	 * {@link MethodInvokingMessageProcessor}
	 * to invoke the discovered {@code method} for provided {@code service} at runtime.
	 * @param service the service object to use.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 */
	public B handle(Object service) {
		return handle(service, null);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the
	 * {@link MethodInvokingMessageProcessor}
	 * to invoke the {@code method} for provided {@code bean} at runtime.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param service the service object to use.
	 * @param methodName the method to invoke.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 */
	public B handle(Object service, String methodName) {
		return handle(service, methodName, null);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the
	 * {@link MethodInvokingMessageProcessor}
	 * to invoke the {@code method} for provided {@code bean} at runtime.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param service the service object to use.
	 * @param methodName the method to invoke.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 */
	public B handle(Object service, String methodName,
			Consumer<GenericEndpointSpec<ServiceActivatingHandler>> endpointConfigurer) {
		ServiceActivatingHandler handler;
		if (StringUtils.hasText(methodName)) {
			handler = new ServiceActivatingHandler(service, methodName);
		}
		else {
			handler = new ServiceActivatingHandler(service);
		}
		return handle(handler, endpointConfigurer);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the
	 * {@link org.springframework.integration.handler.MethodInvokingMessageProcessor}
	 * to invoke the provided {@link GenericHandler} at runtime.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .<Integer>handle((p, h) -> p / 2)
	 * }
	 * </pre>
	 * @param handler the handler to invoke.
	 * @param <P> the payload type to expect.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see LambdaMessageProcessor
	 */
	public <P> B handle(GenericHandler<P> handler) {
		return handle(null, handler);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the
	 * {@link org.springframework.integration.handler.MethodInvokingMessageProcessor}
	 * to invoke the provided {@link GenericHandler} at runtime.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .<Integer>handle((p, h) -> p / 2, e -> e.autoStartup(false))
	 * }
	 * </pre>
	 * @param handler the handler to invoke.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <P> the payload type to expect.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see LambdaMessageProcessor
	 * @see GenericEndpointSpec
	 */
	public <P> B handle(GenericHandler<P> handler,
			Consumer<GenericEndpointSpec<ServiceActivatingHandler>> endpointConfigurer) {
		return this.handle(null, handler, endpointConfigurer);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the
	 * {@link org.springframework.integration.handler.MethodInvokingMessageProcessor}
	 * to invoke the provided {@link GenericHandler} at runtime.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .handle(Integer.class, (p, h) -> p / 2)
	 * }
	 * </pre>
	 * @param payloadType the expected payload type.
	 * The accepted payload can be converted to this one at runtime
	 * @param handler the handler to invoke.
	 * @param <P> the payload type to expect.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see LambdaMessageProcessor
	 */
	public <P> B handle(Class<P> payloadType, GenericHandler<P> handler) {
		return this.handle(payloadType, handler, null);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the
	 * {@link org.springframework.integration.handler.MethodInvokingMessageProcessor}
	 * to invoke the provided {@link GenericHandler} at runtime.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .handle(Integer.class, (p, h) -> p / 2, e -> e.autoStartup(false))
	 * }
	 * </pre>
	 * @param payloadType the expected payload type.
	 * The accepted payload can be converted to this one at runtime
	 * @param handler the handler to invoke.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <P> the payload type to expect.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see LambdaMessageProcessor
	 */
	public <P> B handle(Class<P> payloadType, GenericHandler<P> handler,
			Consumer<GenericEndpointSpec<ServiceActivatingHandler>> endpointConfigurer) {
		ServiceActivatingHandler serviceActivatingHandler = null;
		if (isLambda(handler)) {
			serviceActivatingHandler = new ServiceActivatingHandler(new LambdaMessageProcessor(handler, payloadType));
		}
		else {
			serviceActivatingHandler = new ServiceActivatingHandler(handler, "handle");
		}
		return this.handle(serviceActivatingHandler, endpointConfigurer);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the
	 * {@link org.springframework.integration.handler.MessageProcessor} from the provided
	 *  {@link MessageProcessorSpec}.
	 * <pre class="code">
	 * {@code
	 *  .handle(Scripts.script("classpath:myScript.ruby"))
	 * }
	 * </pre>
	 * @param messageProcessorSpec the {@link MessageProcessorSpec} to use.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 */
	public B handle(MessageProcessorSpec<?> messageProcessorSpec) {
		return handle(messageProcessorSpec, (Consumer<GenericEndpointSpec<ServiceActivatingHandler>>) null);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the
	 * {@link org.springframework.integration.handler.MessageProcessor} from the provided
	 *  {@link MessageProcessorSpec}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * <pre class="code">
	 * {@code
	 *  .handle(Scripts.script("classpath:myScript.ruby"), e -> e.autoStartup(false))
	 * }
	 * </pre>
	 * @param messageProcessorSpec the {@link MessageProcessorSpec} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 */
	public B handle(MessageProcessorSpec<?> messageProcessorSpec,
			Consumer<GenericEndpointSpec<ServiceActivatingHandler>> endpointConfigurer) {
		Assert.notNull(messageProcessorSpec, "'messageProcessorSpec' must not be null");
		MessageProcessor<?> processor = messageProcessorSpec.get();
		return addComponent(processor)
				.handle(new ServiceActivatingHandler(processor), endpointConfigurer);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the selected protocol specific
	 * {@link MessageHandler} implementation from {@code Namespace Factory}:
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .handle(Amqp.outboundAdapter(this.amqpTemplate).routingKeyExpression("headers.routingKey"),
	 *       e -> e.autoStartup(false))
	 * }
	 * </pre>
	 * @param messageHandlerSpec the {@link MessageHandlerSpec} to configure protocol specific
	 * {@link MessageHandler}.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <H> the {@link MessageHandler} type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public <H extends MessageHandler> B handle(MessageHandlerSpec<?, H> messageHandlerSpec,
			Consumer<GenericEndpointSpec<H>> endpointConfigurer) {
		Assert.notNull(messageHandlerSpec, "'messageHandlerSpec' must not be null");
		if (messageHandlerSpec instanceof ComponentsRegistration) {
			addComponents(((ComponentsRegistration) messageHandlerSpec).getComponentsToRegister());
		}
		return handle(messageHandlerSpec.get(), endpointConfigurer);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the provided
	 * {@link MessageHandler} implementation.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Can be used as Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .handle(m -> System.out.println(m.getPayload(), e -> e.autoStartup(false))
	 * }
	 * </pre>
	 * @param messageHandler the {@link MessageHandler} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <H> the {@link MessageHandler} type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public <H extends MessageHandler> B handle(H messageHandler, Consumer<GenericEndpointSpec<H>> endpointConfigurer) {
		Assert.notNull(messageHandler, "'messageHandler' must not be null");
		return this.register(new GenericEndpointSpec<H>(messageHandler), endpointConfigurer);
	}

	/**
	 * Populate a {@link BridgeHandler} to the current integration flow position.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .bridge(s -> s.poller(Pollers.fixedDelay(100))
	 *                   .autoStartup(false)
	 *                   .id("priorityChannelBridge"))
	 * }
	 * </pre>
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see GenericEndpointSpec
	 */
	public B bridge(Consumer<GenericEndpointSpec<BridgeHandler>> endpointConfigurer) {
		return this.register(new GenericEndpointSpec<BridgeHandler>(new BridgeHandler()), endpointConfigurer);
	}

	/**
	 * Populate a {@link DelayHandler} to the current integration flow position
	 * with default options.
	 * @param groupId the {@code groupId} for delayed messages in the
	 * {@link org.springframework.integration.store.MessageGroupStore}.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B delay(String groupId) {
		return this.delay(groupId, (String) null);
	}

	/**
	 * Populate a {@link DelayHandler} to the current integration flow position
	 * with provided options.
	 * @param groupId the {@code groupId} for delayed messages in the
	 * {@link org.springframework.integration.store.MessageGroupStore}.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see DelayerEndpointSpec
	 */
	public B delay(String groupId, Consumer<DelayerEndpointSpec> endpointConfigurer) {
		return this.delay(groupId, (String) null, endpointConfigurer);
	}

	/**
	 * Populate a {@link DelayHandler} to the current integration flow position
	 * with provided {@code delayExpression}.
	 * @param groupId the {@code groupId} for delayed messages in the
	 * {@link org.springframework.integration.store.MessageGroupStore}.
	 * @param expression the delay SpEL expression.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B delay(String groupId, String expression) {
		return this.delay(groupId, expression, null);
	}

	/**
	 * Populate a {@link DelayHandler} to the current integration flow position
	 * with provided {@code delayFunction}.
	 * @param groupId the {@code groupId} for delayed messages in the
	 * {@link org.springframework.integration.store.MessageGroupStore}.
	 * @param function the delay {@link Function}.
	 * @param <P> the expected payload type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see FunctionExpression
	 */
	public <P> B delay(String groupId, Function<Message<P>, Object> function) {
		return this.delay(groupId, function, null);
	}

	/**
	 * Populate a {@link DelayHandler} to the current integration flow position
	 * with provided {@code delayFunction}.
	 * Typically used with a Java 8 Lambda expression:
	 * * <pre class="code">
	 * {@code
	 *  .<Foo>delay("delayer", m -> m.getPayload().getDate(),
	 *            c -> c.advice(this.delayedAdvice).messageStore(this.messageStore()))
	 * }
	 * </pre>
	 * @param groupId the {@code groupId} for delayed messages in the
	 * {@link org.springframework.integration.store.MessageGroupStore}.
	 * @param function the delay {@link Function}.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <P> the expected payload type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see FunctionExpression
	 * @see DelayerEndpointSpec
	 */
	public <P> B delay(String groupId, Function<Message<P>, Object> function,
			Consumer<DelayerEndpointSpec> endpointConfigurer) {
		return this.delay(groupId, new FunctionExpression<Message<P>>(function), endpointConfigurer);
	}

	/**
	 * Populate a {@link DelayHandler} to the current integration flow position
	 * with provided {@code delayExpression}.
	 * @param groupId the {@code groupId} for delayed messages in the
	 * {@link org.springframework.integration.store.MessageGroupStore}.
	 * @param expression the delay SpEL expression.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see DelayerEndpointSpec
	 */
	public B delay(String groupId, String expression, Consumer<DelayerEndpointSpec> endpointConfigurer) {
		return delay(groupId,
				StringUtils.hasText(expression) ? PARSER.parseExpression(expression) : null,
				endpointConfigurer);
	}

	private B delay(String groupId, Expression expression, Consumer<DelayerEndpointSpec> endpointConfigurer) {
		DelayHandler delayHandler = new DelayHandler(groupId);
		if (expression != null) {
			delayHandler.setDelayExpression(expression);
		}
		return this.register(new DelayerEndpointSpec(delayHandler), endpointConfigurer);
	}

	/**
	 * Populate a {@link ContentEnricher} to the current integration flow position
	 * with provided options.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .enrich(e -> e.requestChannel("enrichChannel")
	 *                  .requestPayload(Message::getPayload)
	 *                  .shouldClonePayload(false)
	 *                  .<Map<String, String>>headerFunction("foo", m -> m.getPayload().get("name")))
	 * }
	 * </pre>
	 * @param enricherConfigurer the {@link Consumer} to provide {@link ContentEnricher} options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see EnricherSpec
	 */
	public B enrich(Consumer<EnricherSpec> enricherConfigurer) {
		return this.enrich(enricherConfigurer, null);
	}

	/**
	 * Populate a {@link ContentEnricher} to the current integration flow position
	 * with provided options.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .enrich(e -> e.requestChannel("enrichChannel")
	 *                  .requestPayload(Message::getPayload)
	 *                  .shouldClonePayload(false)
	 *                  .<Map<String, String>>headerFunction("foo", m -> m.getPayload().get("name")),
	 *           e -> e.autoStartup(false))
	 * }
	 * </pre>
	 * @param enricherConfigurer the {@link Consumer} to provide {@link ContentEnricher} options.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see EnricherSpec
	 * @see GenericEndpointSpec
	 */
	public B enrich(Consumer<EnricherSpec> enricherConfigurer,
			Consumer<GenericEndpointSpec<ContentEnricher>> endpointConfigurer) {
		Assert.notNull(enricherConfigurer, "'enricherConfigurer' must not be null");
		EnricherSpec enricherSpec = new EnricherSpec();
		enricherConfigurer.accept(enricherSpec);
		return this.handle(enricherSpec.get(), endpointConfigurer);
	}

	/**
	 * Populate a {@link MessageTransformingHandler} for
	 * a {@link org.springframework.integration.transformer.HeaderEnricher}
	 * using header values from provided {@link MapBuilder}.
	 * Can be used together with {@code Namespace Factory}:
	 * <pre class="code">
	 * {@code
	 *  .enrichHeaders(Mail.headers()
	 *                    .subjectFunction(m -> "foo")
	 *                    .from("foo@bar")
	 *                    .toFunction(m -> new String[] {"bar@baz"}))
	 * }
	 * </pre>
	 * @param headers the {@link MapBuilder} to use.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B enrichHeaders(MapBuilder<?, String, Object> headers) {
		return enrichHeaders(headers, null);
	}

	/**
	 * Populate a {@link MessageTransformingHandler} for
	 * a {@link org.springframework.integration.transformer.HeaderEnricher}
	 * using header values from provided {@link MapBuilder}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Can be used together with {@code Namespace Factory}:
	 * <pre class="code">
	 * {@code
	 *  .enrichHeaders(Mail.headers()
	 *                    .subjectFunction(m -> "foo")
	 *                    .from("foo@bar")
	 *                    .toFunction(m -> new String[] {"bar@baz"}),
	 *                 e -> e.autoStartup(false))
	 * }
	 * </pre>
	 * @param headers the {@link MapBuilder} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see GenericEndpointSpec
	 */
	public B enrichHeaders(MapBuilder<?, String, Object> headers,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) {
		return enrichHeaders(headers.get(), endpointConfigurer);
	}

	/**
	 * Accept a {@link Map} of values to be used for the
	 * {@link org.springframework.messaging.Message} header enrichment.
	 * {@code values} can apply an {@link org.springframework.expression.Expression}
	 * to be evaluated against a request {@link org.springframework.messaging.Message}.
	 * @param headers the Map of headers to enrich.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B enrichHeaders(Map<String, Object> headers) {
		return enrichHeaders(headers, null);
	}

	/**
	 * Accept a {@link Map} of values to be used for the
	 * {@link org.springframework.messaging.Message} header enrichment.
	 * {@code values} can apply an {@link org.springframework.expression.Expression}
	 * to be evaluated against a request {@link org.springframework.messaging.Message}.
	 * @param headers the Map of headers to enrich.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see GenericEndpointSpec
	 */
	public B enrichHeaders(final Map<String, Object> headers,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) {
		return enrichHeaders(new Consumer<HeaderEnricherSpec>() {

			@Override
			public void accept(HeaderEnricherSpec spec) {
				spec.headers(headers);
			}

		}, endpointConfigurer);
	}

	/**
	 * Populate a {@link MessageTransformingHandler} for
	 * a {@link org.springframework.integration.transformer.HeaderEnricher}
	 * as the result of provided {@link Consumer}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .enrichHeaders(h -> h.header(FileHeaders.FILENAME, "foo.sitest")
	 *                       .header("directory", new File(tmpDir, "fileWritingFlow")))
	 * }
	 * </pre>
	 * @param headerEnricherConfigurer the {@link Consumer} to use.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see HeaderEnricherSpec
	 */
	public B enrichHeaders(Consumer<HeaderEnricherSpec> headerEnricherConfigurer) {
		return this.enrichHeaders(headerEnricherConfigurer, null);
	}

	/**
	 * Populate a {@link MessageTransformingHandler} for
	 * a {@link org.springframework.integration.transformer.HeaderEnricher}
	 * as the result of provided {@link Consumer}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .enrichHeaders(
	 *s -> s.header("one", new XPathExpressionEvaluatingHeaderValueMessageProcessor("/root/elementOne"))
	 *            .header("two", new XPathExpressionEvaluatingHeaderValueMessageProcessor("/root/elementTwo"))
	 *            .headerChannelsToString(),
	 *            c -> c.autoStartup(false).id("xpathHeaderEnricher"))
	 * }
	 * </pre>
	 * @param headerEnricherConfigurer the {@link Consumer} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see HeaderEnricherSpec
	 * @see GenericEndpointSpec
	 */
	public B enrichHeaders(Consumer<HeaderEnricherSpec> headerEnricherConfigurer,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) {
		Assert.notNull(headerEnricherConfigurer, "'headerEnricherConfigurer' must not be null");
		HeaderEnricherSpec headerEnricherSpec = new HeaderEnricherSpec();
		headerEnricherConfigurer.accept(headerEnricherSpec);
		return transform(headerEnricherSpec.get(), endpointConfigurer);
	}

	/**
	 * Populate the {@link DefaultMessageSplitter} with default options
	 * to the current integration flow position.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B split() {
		return this.split((Consumer<SplitterEndpointSpec<DefaultMessageSplitter>>) null);
	}

	/**
	 * Populate the {@link DefaultMessageSplitter} with provided options
	 * to the current integration flow position.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .split(s -> s.applySequence(false).get().getT2().setDelimiters(","))
	 * }
	 * </pre>
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options
	 * and for {@link DefaultMessageSplitter}.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see SplitterEndpointSpec
	 */
	public B split(Consumer<SplitterEndpointSpec<DefaultMessageSplitter>> endpointConfigurer) {
		return this.split(new DefaultMessageSplitter(), endpointConfigurer);
	}

	/**
	 * Populate the {@link ExpressionEvaluatingSplitter} with provided
	 * SpEL expression.
	 * @param expression the splitter SpEL expression.
	 * and for {@link ExpressionEvaluatingSplitter}.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 * @see SplitterEndpointSpec
	 */
	public B split(String expression) {
		return split(expression, (Consumer<SplitterEndpointSpec<ExpressionEvaluatingSplitter>>) null);
	}

	/**
	 * Populate the {@link ExpressionEvaluatingSplitter} with provided
	 * SpEL expression.
	 * @param expression the splitter SpEL expression.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options
	 * and for {@link ExpressionEvaluatingSplitter}.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see SplitterEndpointSpec
	 */
	public B split(String expression, Consumer<SplitterEndpointSpec<ExpressionEvaluatingSplitter>> endpointConfigurer) {
		Assert.hasText(expression, "'expression' must not be empty");
		return split(new ExpressionEvaluatingSplitter(PARSER.parseExpression(expression)), endpointConfigurer);
	}

	/**
	 * Populate the {@link MethodInvokingSplitter} to evaluate the discovered
	 * {@code method} of the {@code service} at runtime.
	 * @param service the service to use.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 * @see MethodInvokingSplitter
	 */
	public B split(Object service) {
		return split(service, null);
	}

	/**
	 * Populate the {@link MethodInvokingSplitter} to evaluate the provided
	 * {@code method} of the {@code service} at runtime.
	 * @param service the service to use.
	 * @param methodName the method to invoke.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 * @see MethodInvokingSplitter
	 */
	public B split(Object service, String methodName) {
		return split(service, methodName, null);
	}

	/**
	 * Populate the {@link MethodInvokingSplitter} to evaluate the provided
	 * {@code method} of the {@code bean} at runtime.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param service the service to use.
	 * @param methodName the method to invoke.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options
	 * and for {@link MethodInvokingSplitter}.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 * @see SplitterEndpointSpec
	 * @see MethodInvokingSplitter
	 */
	public B split(Object service, String methodName,
			Consumer<SplitterEndpointSpec<MethodInvokingSplitter>> endpointConfigurer) {
		MethodInvokingSplitter splitter;
		if (StringUtils.hasText(methodName)) {
			splitter = new MethodInvokingSplitter(service, methodName);
		}
		else {
			splitter = new MethodInvokingSplitter(service);
		}
		return split(splitter, endpointConfigurer);
	}

	/**
	 * Populate the {@link MethodInvokingSplitter} to evaluate the provided
	 * {@code method} of the {@code bean} at runtime.
	 * @param beanName the bean name to use.
	 * @param methodName the method to invoke at runtime.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B split(String beanName, String methodName) {
		return this.split(beanName, methodName, null);
	}

	/**
	 * Populate the {@link MethodInvokingSplitter} to evaluate the provided
	 * {@code method} of the {@code bean} at runtime.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param beanName the bean name to use.
	 * @param methodName the method to invoke at runtime.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options
	 * and for {@link MethodInvokingSplitter}.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see SplitterEndpointSpec
	 */
	public B split(String beanName, String methodName,
			Consumer<SplitterEndpointSpec<MethodInvokingSplitter>> endpointConfigurer) {
		return split(new MethodInvokingSplitter(new BeanNameMessageProcessor<Object>(beanName, methodName)),
				endpointConfigurer);
	}

	/**
	 * Populate the {@link MethodInvokingSplitter} to evaluate the
	 * {@link org.springframework.integration.handler.MessageProcessor} at runtime
	 * from provided {@link MessageProcessorSpec}.
	 * <pre class="code">
	 * {@code
	 *  .split(Scripts.script("classpath:myScript.ruby"))
	 * }
	 * </pre>
	 * @param messageProcessorSpec the splitter {@link MessageProcessorSpec}.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 * @see SplitterEndpointSpec
	 * @see org.springframework.integration.dsl.scripting.ScriptSpec
	 */
	public B split(MessageProcessorSpec<?> messageProcessorSpec) {
		return split(messageProcessorSpec, (Consumer<SplitterEndpointSpec<MethodInvokingSplitter>>) null);
	}

	/**
	 * Populate the {@link MethodInvokingSplitter} to evaluate the
	 * {@link org.springframework.integration.handler.MessageProcessor} at runtime
	 * from provided {@link MessageProcessorSpec}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * <pre class="code">
	 * {@code
	 *  .split(Scripts.script(myScriptResource).lang("groovy").refreshCheckDelay(1000),
	 *  			, e -> e.applySequence(false))
	 * }
	 * </pre>
	 * @param messageProcessorSpec the splitter {@link MessageProcessorSpec}.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options
	 * and for {@link MethodInvokingSplitter}.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 * @see SplitterEndpointSpec
	 * @see org.springframework.integration.dsl.scripting.ScriptSpec
	 */
	public B split(MessageProcessorSpec<?> messageProcessorSpec,
			Consumer<SplitterEndpointSpec<MethodInvokingSplitter>> endpointConfigurer) {
		Assert.notNull(messageProcessorSpec, "'messageProcessorSpec' must not be null");
		MessageProcessor<?> processor = messageProcessorSpec.get();
		return addComponent(processor)
				.split(new MethodInvokingSplitter(processor), endpointConfigurer);
	}

	/**
	 * Populate the {@link MethodInvokingSplitter} to evaluate the provided
	 * {@link Function} at runtime.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .split(String.class, p ->
	 *        jdbcTemplate.execute("SELECT * from FOO",
	 *            (PreparedStatement ps) ->
	 *                 new ResultSetIterator<Foo>(ps.executeQuery(),
	 *                     (rs, rowNum) ->
	 *                           new Foo(rs.getInt(1), rs.getString(2)))))
	 * }
	 * </pre>
	 * @param payloadType the expected payload type. Used at runtime to convert received payload type to.
	 * @param splitter the splitter {@link Function}.
	 * @param <P> the payload type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see LambdaMessageProcessor
	 */
	public <P> B split(Class<P> payloadType, Function<P, ?> splitter) {
		return split(payloadType, splitter, null);
	}

	/**
	 * Populate the {@link MethodInvokingSplitter} to evaluate the provided
	 * {@link Function} at runtime.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .<String>split(p ->
	 *        jdbcTemplate.execute("SELECT * from FOO",
	 *            (PreparedStatement ps) ->
	 *                 new ResultSetIterator<Foo>(ps.executeQuery(),
	 *                     (rs, rowNum) ->
	 *                           new Foo(rs.getInt(1), rs.getString(2))))
	 *       , e -> e.applySequence(false))
	 * }
	 * </pre>
	 * @param splitter the splitter {@link Function}.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <P> the payload type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see LambdaMessageProcessor
	 * @see SplitterEndpointSpec
	 */
	public <P> B split(Function<P, ?> splitter,
			Consumer<SplitterEndpointSpec<MethodInvokingSplitter>> endpointConfigurer) {
		return split(null, splitter, endpointConfigurer);
	}

	/**
	 * Populate the {@link MethodInvokingSplitter} to evaluate the provided
	 * {@link Function} at runtime.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .split(String.class, p ->
	 *        jdbcTemplate.execute("SELECT * from FOO",
	 *            (PreparedStatement ps) ->
	 *                 new ResultSetIterator<Foo>(ps.executeQuery(),
	 *                     (rs, rowNum) ->
	 *                           new Foo(rs.getInt(1), rs.getString(2))))
	 *       , e -> e.applySequence(false))
	 * }
	 * </pre>
	 * @param payloadType the expected payload type. Used at runtime to convert received payload type to.
	 * @param splitter the splitter {@link Function}.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <P> the payload type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see LambdaMessageProcessor
	 * @see SplitterEndpointSpec
	 */
	public <P> B split(Class<P> payloadType, Function<P, ?> splitter,
			Consumer<SplitterEndpointSpec<MethodInvokingSplitter>> endpointConfigurer) {
		MethodInvokingSplitter split = isLambda(splitter)
				? new MethodInvokingSplitter(new LambdaMessageProcessor(splitter, payloadType))
				: new MethodInvokingSplitter(splitter);
		return this.split(split, endpointConfigurer);
	}

	/**
	 * Populate the provided {@link AbstractMessageSplitter} to the current integration
	 * flow position.
	 * @param splitterMessageHandlerSpec the {@link MessageHandlerSpec} to populate.
	 * @param <S> the {@link AbstractMessageSplitter}
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 * @see SplitterEndpointSpec
	 */
	public <S extends AbstractMessageSplitter> B split(MessageHandlerSpec<?, S> splitterMessageHandlerSpec) {
		return split(splitterMessageHandlerSpec, (Consumer<SplitterEndpointSpec<S>>) null);
	}

	/**
	 * Populate the provided {@link AbstractMessageSplitter} to the current integration
	 * flow position.
	 * @param splitterMessageHandlerSpec the {@link MessageHandlerSpec} to populate.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <S> the {@link AbstractMessageSplitter}
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 * @see SplitterEndpointSpec
	 */
	public <S extends AbstractMessageSplitter> B split(MessageHandlerSpec<?, S> splitterMessageHandlerSpec,
			Consumer<SplitterEndpointSpec<S>> endpointConfigurer) {
		Assert.notNull(splitterMessageHandlerSpec, "'splitterMessageHandlerSpec' must not be null");
		return split(splitterMessageHandlerSpec.get(), endpointConfigurer);
	}

	/**
	 * Populate the provided {@link AbstractMessageSplitter} to the current integration
	 * flow position.
	 * @param splitter the {@link AbstractMessageSplitter} to populate.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 * @see SplitterEndpointSpec
	 */
	public B split(AbstractMessageSplitter splitter) {
		return split(splitter, (Consumer<SplitterEndpointSpec<AbstractMessageSplitter>>) null);
	}

	/**
	 * Populate the provided {@link AbstractMessageSplitter} to the current integration
	 * flow position.
	 * @param splitter the {@link AbstractMessageSplitter} to populate.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <S> the {@link AbstractMessageSplitter}
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see SplitterEndpointSpec
	 */
	public <S extends AbstractMessageSplitter> B split(S splitter,
			Consumer<SplitterEndpointSpec<S>> endpointConfigurer) {
		Assert.notNull(splitter, "'splitter' must not be null");
		return this.register(new SplitterEndpointSpec<S>(splitter), endpointConfigurer);
	}

	/**
	 * Provide the {@link HeaderFilter} to the current {@link StandardIntegrationFlow}.
	 * @param headersToRemove the array of headers (or patterns)
	 * to remove from {@link org.springframework.messaging.MessageHeaders}.
	 * @return this {@link IntegrationFlowDefinition}.
	 */
	public B headerFilter(String... headersToRemove) {
		return this.headerFilter(new HeaderFilter(headersToRemove), null);
	}

	/**
	 * Provide the {@link HeaderFilter} to the current {@link StandardIntegrationFlow}.
	 * @param headersToRemove the comma separated headers (or patterns) to remove from
	 * {@link org.springframework.messaging.MessageHeaders}.
	 * @param patternMatch the {@code boolean} flag to indicate if {@code headersToRemove}
	 * should be interpreted as patterns or direct header names.
	 * @return this {@link IntegrationFlowDefinition}.
	 */
	public B headerFilter(String headersToRemove, boolean patternMatch) {
		HeaderFilter headerFilter = new HeaderFilter(StringUtils.delimitedListToStringArray(headersToRemove, ",", " "));
		headerFilter.setPatternMatch(patternMatch);
		return this.headerFilter(headerFilter, null);
	}

	/**
	 * Populate the provided {@link MessageTransformingHandler} for the provided
	 * {@link HeaderFilter}.
	 * @param headerFilter the {@link HeaderFilter} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see GenericEndpointSpec
	 */
	public B headerFilter(HeaderFilter headerFilter,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) {
		return this.transform(headerFilter, endpointConfigurer);
	}

	/**
	 * Populate the {@link MessageTransformingHandler} for the {@link ClaimCheckInTransformer}
	 * with provided {@link MessageStore}.
	 * @param messageStore the {@link MessageStore} to use.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B claimCheckIn(MessageStore messageStore) {
		return this.claimCheckIn(messageStore, null);
	}

	/**
	 * Populate the {@link MessageTransformingHandler} for the {@link ClaimCheckInTransformer}
	 * with provided {@link MessageStore}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param messageStore the {@link MessageStore} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see GenericEndpointSpec
	 */
	public B claimCheckIn(MessageStore messageStore,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) {
		return this.transform(new ClaimCheckInTransformer(messageStore), endpointConfigurer);
	}

	/**
	 * Populate the {@link MessageTransformingHandler} for the {@link ClaimCheckOutTransformer}
	 * with provided {@link MessageStore}.
	 * The {@code removeMessage} option of {@link ClaimCheckOutTransformer} is to {@code false}.
	 * @param messageStore the {@link MessageStore} to use.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B claimCheckOut(MessageStore messageStore) {
		return this.claimCheckOut(messageStore, false);
	}

	/**
	 * Populate the {@link MessageTransformingHandler} for the {@link ClaimCheckOutTransformer}
	 * with provided {@link MessageStore} and {@code removeMessage} flag.
	 * @param messageStore the {@link MessageStore} to use.
	 * @param removeMessage the removeMessage boolean flag.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see ClaimCheckOutTransformer#setRemoveMessage(boolean)
	 */
	public B claimCheckOut(MessageStore messageStore, boolean removeMessage) {
		return this.claimCheckOut(messageStore, removeMessage, null);
	}

	/**
	 * Populate the {@link MessageTransformingHandler} for the {@link ClaimCheckOutTransformer}
	 * with provided {@link MessageStore} and {@code removeMessage} flag.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param messageStore the {@link MessageStore} to use.
	 * @param removeMessage the removeMessage boolean flag.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see GenericEndpointSpec
	 * @see ClaimCheckOutTransformer#setRemoveMessage(boolean)
	 */
	public B claimCheckOut(MessageStore messageStore, boolean removeMessage,
			Consumer<GenericEndpointSpec<MessageTransformingHandler>> endpointConfigurer) {
		ClaimCheckOutTransformer claimCheckOutTransformer = new ClaimCheckOutTransformer(messageStore);
		claimCheckOutTransformer.setRemoveMessage(removeMessage);
		return this.transform(claimCheckOutTransformer, endpointConfigurer);
	}

	/**
	 * Populate the {@link ResequencingMessageHandler} with default options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B resequence() {
		return resequence(null);
	}

	/**
	 * Populate the {@link ResequencingMessageHandler} with provided options from {@link ResequencerSpec}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .resequence(r -> r.releasePartialSequences(true).correlationExpression("'foo'"),
	 *             e -> e.phase(100))
	 * }
	 * </pre>
	 * @param resequencerConfigurer the {@link Consumer} to provide {@link ResequencingMessageHandler} options.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see GenericEndpointSpec
	 * @deprecated since 1.1 in favor of {@link #resequence(Consumer)}
	 */
	@Deprecated
	public B resequence(Consumer<ResequencerSpec> resequencerConfigurer,
			Consumer<GenericEndpointSpec<ResequencingMessageHandler>> endpointConfigurer) {
		Assert.notNull(resequencerConfigurer, "'resequencerConfigurer' must not be null");
		ResequencerSpec spec = new ResequencerSpec();
		resequencerConfigurer.accept(spec);
		return handle(spec.get().getT2(), endpointConfigurer);
	}

	/**
	 * Populate the {@link ResequencingMessageHandler} with provided options from {@link ResequencerSpec}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .resequence(r -> r.releasePartialSequences(true)
	 *                    .correlationExpression("'foo'")
	 *                    .phase(100))
	 * }
	 * </pre>
	 * @param resequencer the {@link Consumer} to provide {@link ResequencingMessageHandler} options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 * @see ResequencerSpec
	 */
	public B resequence(Consumer<ResequencerSpec> resequencer) {
		return register(new ResequencerSpec(), resequencer);
	}

	/**
	 * Populate the {@link AggregatingMessageHandler} with default options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B aggregate() {
		return aggregate(null);
	}

	/**
	 * Populate the {@link AggregatingMessageHandler} with provided options from {@link AggregatorSpec}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .aggregate(a -> a.correlationExpression("1").releaseStrategy(g -> g.size() == 25),
	 *            e -> e.applySequence(false))
	 * }
	 * </pre>
	 * @param aggregatorConfigurer the {@link Consumer} to provide {@link AggregatingMessageHandler} options.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see GenericEndpointSpec
	 * @deprecated since 1.1 in favor of {@link #aggregate(Consumer)}
	 */
	@Deprecated
	public B aggregate(Consumer<AggregatorSpec> aggregatorConfigurer,
			Consumer<GenericEndpointSpec<AggregatingMessageHandler>> endpointConfigurer) {
		Assert.notNull(aggregatorConfigurer, "'aggregatorConfigurer' must not be null");
		AggregatorSpec spec = new AggregatorSpec();
		aggregatorConfigurer.accept(spec);
		return this.handle(spec.get().getT2(), endpointConfigurer);
	}

	/**
	 * Populate the {@link AggregatingMessageHandler} with provided options from {@link AggregatorSpec}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .aggregate(a -> a.correlationExpression("1")
	 *                   .releaseStrategy(g -> g.size() == 25)
	 *                   .phase(100))
	 * }
	 * </pre>
	 * @param aggregator the {@link Consumer} to provide {@link AggregatingMessageHandler} options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 * @see AggregatorSpec
	 */
	public B aggregate(Consumer<AggregatorSpec> aggregator) {
		return register(new AggregatorSpec(), aggregator);
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for provided bean and its method
	 * with default options.
	 * @param beanName the bean to use.
	 * @param method the method to invoke at runtime.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B route(String beanName, String method) {
		return this.route(beanName, method, null);
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for provided bean and its method
	 * with provided options from {@link RouterSpec}.
	 * @param beanName the bean to use.
	 * @param method the method to invoke at runtime.
	 * @param routerConfigurer the {@link Consumer} to provide {@link MethodInvokingRouter} options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B route(String beanName, String method, Consumer<RouterSpec<Object, MethodInvokingRouter>> routerConfigurer) {
		return route(beanName, method, routerConfigurer, null);
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for provided bean and its method
	 * with provided options from {@link RouterSpec}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param beanName the bean to use.
	 * @param method the method to invoke at runtime.
	 * @param routerConfigurer the {@link Consumer} to provide {@link MethodInvokingRouter} options.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B route(String beanName, String method, Consumer<RouterSpec<Object, MethodInvokingRouter>> routerConfigurer,
			Consumer<GenericEndpointSpec<MethodInvokingRouter>> endpointConfigurer) {
		return this.route(new MethodInvokingRouter(new BeanNameMessageProcessor<Object>(beanName, method)),
				routerConfigurer, endpointConfigurer);
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for the discovered method
	 * of the provided service and its method with default options.
	 * @param service the bean to use.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 * @see MethodInvokingRouter
	 */
	public B route(Object service) {
		return route(service, null);
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for the method
	 * of the provided service and its method with default options.
	 * @param service the service to use.
	 * @param methodName the method to invoke.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 * @see MethodInvokingRouter
	 */
	public B route(Object service, String methodName) {
		return route(service, methodName, null);
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for the method
	 * of the provided service and its method with provided options from {@link RouterSpec}.
	 * @param service the service to use.
	 * @param methodName the method to invoke.
	 * @param routerConfigurer the {@link Consumer} to provide {@link MethodInvokingRouter} options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 * @see MethodInvokingRouter
	 */
	public B route(Object service, String methodName,
			Consumer<RouterSpec<Object, MethodInvokingRouter>> routerConfigurer) {
		return route(service, methodName, routerConfigurer, null);
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for the method
	 * of the provided service and its method with provided options from {@link RouterSpec}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param service the service to use.
	 * @param methodName the method to invoke.
	 * @param routerConfigurer the {@link Consumer} to provide {@link MethodInvokingRouter} options.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 * @see MethodInvokingRouter
	 */
	public B route(Object service, String methodName,
			Consumer<RouterSpec<Object, MethodInvokingRouter>> routerConfigurer,
			Consumer<GenericEndpointSpec<MethodInvokingRouter>> endpointConfigurer) {
		MethodInvokingRouter router;
		if (StringUtils.hasText(methodName)) {
			router = new MethodInvokingRouter(service, methodName);
		}
		else {
			router = new MethodInvokingRouter(service);
		}
		return route(router, routerConfigurer, endpointConfigurer);
	}


	/**
	 * Populate the {@link ExpressionEvaluatingRouter} for provided SpEL expression
	 * with default options.
	 * @param expression the expression to use.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B route(String expression) {
		return this.route(expression, (Consumer<RouterSpec<Object, ExpressionEvaluatingRouter>>) null);
	}

	/**
	 * Populate the {@link ExpressionEvaluatingRouter} for provided SpEL expression
	 * with provided options from {@link RouterSpec}.
	 * @param expression the expression to use.
	 * @param routerConfigurer the {@link Consumer} to provide {@link ExpressionEvaluatingRouter} options.
	 * @param <T> the target result type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public <T> B route(String expression, Consumer<RouterSpec<T, ExpressionEvaluatingRouter>> routerConfigurer) {
		return route(expression, routerConfigurer, null);
	}

	/**
	 * Populate the {@link ExpressionEvaluatingRouter} for provided bean and its method
	 * with provided options from {@link RouterSpec}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param expression the expression to use.
	 * @param routerConfigurer the {@link Consumer} to provide {@link ExpressionEvaluatingRouter} options.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <T> the target result type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public <T> B route(String expression, Consumer<RouterSpec<T, ExpressionEvaluatingRouter>> routerConfigurer,
			Consumer<GenericEndpointSpec<ExpressionEvaluatingRouter>> endpointConfigurer) {
		return this.route(new ExpressionEvaluatingRouter(PARSER.parseExpression(expression)), routerConfigurer,
				endpointConfigurer);
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for provided {@link Function}
	 * with default options.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .route(p -> p.equals("foo") || p.equals("bar") ? new String[] {"foo", "bar"} : null)
	 * }
	 * </pre>
	 * @param router the {@link Function} to use.
	 * @param <S> the source payload type.
	 * @param <T> the target result type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public <S, T> B route(Function<S, T> router) {
		return this.route(null, router);
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for provided {@link Function}
	 * with provided options from {@link RouterSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .<Integer, Boolean>route(p -> p % 2 == 0,
	 *                 m -> m.channelMapping("true", "evenChannel")
	 *                       .subFlowMapping("false", f ->
	 *                                   f.<Integer>handle((p, h) -> p * 3)))
	 * }
	 * </pre>
	 * @param router the {@link Function} to use.
	 * @param routerConfigurer the {@link Consumer} to provide {@link MethodInvokingRouter} options.
	 * @param <S> the source payload type.
	 * @param <T> the target result type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public <S, T> B route(Function<S, T> router, Consumer<RouterSpec<T, MethodInvokingRouter>> routerConfigurer) {
		return this.route(null, router, routerConfigurer);
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for provided {@link Function}
	 * and payload type with default options.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .route(Integer.class, p -> p % 2 == 0)
	 * }
	 * </pre>
	 * @param payloadType the expected payload type.
	 * @param router  the {@link Function} to use.
	 * @param <S> the source payload type.
	 * @param <T> the target result type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see LambdaMessageProcessor
	 */
	public <S, T> B route(Class<S> payloadType, Function<S, T> router) {
		return this.route(payloadType, router, null, null);
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for provided {@link Function}
	 * and payload type and options from {@link RouterSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .route(Integer.class, p -> p % 2 == 0,
	 *                 m -> m.channelMapping("true", "evenChannel")
	 *                       .subFlowMapping("false", f ->
	 *                                   f.<Integer>handle((p, h) -> p * 3)))
	 * }
	 * </pre>
	 * @param payloadType the expected payload type.
	 * @param router  the {@link Function} to use.
	 * @param routerConfigurer the {@link Consumer} to provide {@link MethodInvokingRouter} options.
	 * @param <S> the source payload type.
	 * @param <T> the target result type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see LambdaMessageProcessor
	 */
	public <S, T> B route(Class<S> payloadType, Function<S, T> router,
			Consumer<RouterSpec<T, MethodInvokingRouter>> routerConfigurer) {
		return this.route(payloadType, router, routerConfigurer, null);
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for provided {@link Function}
	 * with provided options from {@link RouterSpec}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .<Integer, Boolean>route(p -> p % 2 == 0,
	 *                 m -> m.channelMapping("true", "evenChannel")
	 *                       .subFlowMapping("false", f ->
	 *                                   f.<Integer>handle((p, h) -> p * 3)),
	 *            e -> e.applySequence(false))
	 * }
	 * </pre>
	 * @param router the {@link Function} to use.
	 * @param routerConfigurer the {@link Consumer} to provide {@link MethodInvokingRouter} options.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <S> the source payload type.
	 * @param <T> the target result type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public <S, T> B route(Function<S, T> router, Consumer<RouterSpec<T, MethodInvokingRouter>> routerConfigurer,
			Consumer<GenericEndpointSpec<MethodInvokingRouter>> endpointConfigurer) {
		return route(null, router, routerConfigurer, endpointConfigurer);
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for provided {@link Function}
	 * and payload type and options from {@link RouterSpec}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .route(Integer.class, p -> p % 2 == 0,
	 *					m -> m.channelMapping("true", "evenChannel")
	 *                       .subFlowMapping("false", f ->
	 *                                   f.<Integer>handle((p, h) -> p * 3)),
	 * 		           e -> e.applySequence(false))
	 * }
	 * </pre>
	 * @param payloadType the expected payload type.
	 * @param router the {@link Function} to use.
	 * @param routerConfigurer the {@link Consumer} to provide {@link MethodInvokingRouter} options.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <P> the source payload type.
	 * @param <T> the target result type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @see LambdaMessageProcessor
	 */
	public <P, T> B route(Class<P> payloadType, Function<P, T> router,
			Consumer<RouterSpec<T, MethodInvokingRouter>> routerConfigurer,
			Consumer<GenericEndpointSpec<MethodInvokingRouter>> endpointConfigurer) {
		MethodInvokingRouter methodInvokingRouter = isLambda(router)
				? new MethodInvokingRouter(new LambdaMessageProcessor(router, payloadType))
				: new MethodInvokingRouter(router);
		return route(methodInvokingRouter, routerConfigurer, endpointConfigurer);
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for the {@link org.springframework.integration.handler.MessageProcessor}
	 * from the provided {@link MessageProcessorSpec} with default options.
	 * <pre class="code">
	 * {@code
	 *  .route(Scripts.script(myScriptResource).lang("groovy").refreshCheckDelay(1000))
	 * }
	 * </pre>
	 * @param messageProcessorSpec the {@link MessageProcessorSpec} to use.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 */
	public B route(MessageProcessorSpec<?> messageProcessorSpec) {
		return route(messageProcessorSpec, (Consumer<RouterSpec<Object, MethodInvokingRouter>>) null);
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for the {@link org.springframework.integration.handler.MessageProcessor}
	 * from the provided {@link MessageProcessorSpec} with default options.
	 * <pre class="code">
	 * {@code
	 *  .route(Scripts.script(myScriptResource).lang("groovy").refreshCheckDelay(1000),
	 *                 m -> m.channelMapping("true", "evenChannel")
	 *                       .subFlowMapping("false", f ->
	 *                                   f.<Integer>handle((p, h) -> p * 3)))
	 * }
	 * </pre>
	 * @param messageProcessorSpec the {@link MessageProcessorSpec} to use.
	 * @param routerConfigurer the {@link Consumer} to provide {@link MethodInvokingRouter} options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 */
	public B route(MessageProcessorSpec<?> messageProcessorSpec,
			Consumer<RouterSpec<Object, MethodInvokingRouter>> routerConfigurer) {
		return route(messageProcessorSpec, routerConfigurer, null);
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for the {@link org.springframework.integration.handler.MessageProcessor}
	 * from the provided {@link MessageProcessorSpec} with default options.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * <pre class="code">
	 * {@code
	 *  .route(Scripts.script(myScriptResource).lang("groovy").refreshCheckDelay(1000),
	 *                 m -> m.channelMapping("true", "evenChannel")
	 *                       .subFlowMapping("false", f ->
	 *                                   f.<Integer>handle((p, h) -> p * 3)),
	 *                 e -> e.applySequence(false))
	 * }
	 * </pre>
	 * @param messageProcessorSpec the {@link MessageProcessorSpec} to use.
	 * @param routerConfigurer the {@link Consumer} to provide {@link MethodInvokingRouter} options.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.1
	 */
	public B route(MessageProcessorSpec<?> messageProcessorSpec,
			Consumer<RouterSpec<Object, MethodInvokingRouter>> routerConfigurer,
			Consumer<GenericEndpointSpec<MethodInvokingRouter>> endpointConfigurer) {
		Assert.notNull(messageProcessorSpec, "'messageProcessorSpec' must not be null");
		MessageProcessor<?> processor = messageProcessorSpec.get();
		return addComponent(processor)
				.route(new MethodInvokingRouter(processor), routerConfigurer, endpointConfigurer);
	}

	/**
	 * Populate the provided {@link AbstractMappingMessageRouter} implementation
	 * with options from {@link RouterSpec} and endpoint options from {@link GenericEndpointSpec}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param router the {@link AbstractMappingMessageRouter} to populate.
	 * @param routerConfigurer the {@link Consumer} to provide {@link MethodInvokingRouter} options.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <K> the {@code channelKey mapping} type.
	 * @param <R> the {@link AbstractMappingMessageRouter} type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public <K, R extends AbstractMappingMessageRouter> B route(R router, Consumer<RouterSpec<K, R>> routerConfigurer,
			Consumer<GenericEndpointSpec<R>> endpointConfigurer) {

		RouterSpec<K, R> routerSpec = new RouterSpec<K, R>(router);
		if (routerConfigurer != null) {
			routerConfigurer.accept(routerSpec);
		}

		return route(router, routerSpec, endpointConfigurer);
	}

	private <R extends AbstractMessageRouter, S extends AbstractRouterSpec<S, R>> B route(R router,
			S routerSpec, Consumer<GenericEndpointSpec<R>> endpointConfigurer) {

		route(router, endpointConfigurer);

		final BridgeHandler bridgeHandler = new BridgeHandler();
		boolean registerSubflowBridge = false;
		Collection<Object> componentsToRegister = routerSpec.getComponentsToRegister();
		if (!CollectionUtils.isEmpty(componentsToRegister)) {
			for (Object component : componentsToRegister) {
				if (component instanceof IntegrationFlowDefinition) {
					IntegrationFlowDefinition<?> flowBuilder = (IntegrationFlowDefinition<?>) component;
					if (flowBuilder.isOutputChannelRequired()) {
						registerSubflowBridge = true;
						flowBuilder.channel(new FixedSubscriberChannel(bridgeHandler));
					}
					addComponent(flowBuilder.get());
				}
				else {
					addComponent(component);
				}
			}
		}
		if (routerSpec.isDefaultToParentFlow()) {
			routerSpec.defaultOutputChannel(new FixedSubscriberChannel(bridgeHandler));
			registerSubflowBridge = true;
		}

		if (registerSubflowBridge) {
			this.currentComponent = null;
			handle(bridgeHandler);
		}
		return _this();
	}

	/**
	 * Populate the {@link RecipientListRouter} options from {@link RecipientListRouterSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .routeToRecipients(r -> r
	 *.recipient("bar-channel", m ->
	 *            m.getHeaders().containsKey("recipient") && (boolean) m.getHeaders().get("recipient"))
	 *      .recipientFlow("'foo' == payload or 'bar' == payload or 'baz' == payload",
	 *                         f -> f.transform(String.class, p -> p.toUpperCase())
	 *                               .channel(c -> c.queue("recipientListSubFlow1Result"))))
	 * }
	 * </pre>
	 * @param routerConfigurer the {@link Consumer} to provide {@link RecipientListRouter} options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B routeToRecipients(Consumer<RecipientListRouterSpec> routerConfigurer) {
		return routeToRecipients(routerConfigurer, null);
	}

	/**
	 * Populate the {@link RecipientListRouter} options from {@link RecipientListRouterSpec}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .routeToRecipients(r -> r
	 *.recipient("bar-channel", m ->
	 *            m.getHeaders().containsKey("recipient") && (boolean) m.getHeaders().get("recipient"))
	 *      .recipientFlow("'foo' == payload or 'bar' == payload or 'baz' == payload",
	 *                         f -> f.transform(String.class, p -> p.toUpperCase())
	 *                               .channel(c -> c.queue("recipientListSubFlow1Result"))),
	 *      e -> e.applySequence(false))
	 * }
	 * </pre>
	 * @param routerConfigurer the {@link Consumer} to provide {@link RecipientListRouter} options.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B routeToRecipients(Consumer<RecipientListRouterSpec> routerConfigurer,
			Consumer<GenericEndpointSpec<RecipientListRouter>> endpointConfigurer) {

		RecipientListRouterSpec spec = new RecipientListRouterSpec();
		if (routerConfigurer != null) {
			routerConfigurer.accept(spec);
		}

		return route(spec.get(), spec, endpointConfigurer);
	}

	/**
	 * Populate the provided {@link AbstractMessageRouter} implementation to the
	 * current integration flow position.
	 * @param router the {@link AbstractMessageRouter} to populate.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B route(AbstractMessageRouter router) {
		return route(router, (Consumer<GenericEndpointSpec<AbstractMessageRouter>>) null);
	}

	/**
	 * Populate the provided {@link AbstractMessageRouter} implementation to the
	 * current integration flow position.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param router the {@link AbstractMessageRouter} to populate.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <R> the {@link AbstractMessageRouter} type.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public <R extends AbstractMessageRouter> B route(R router, Consumer<GenericEndpointSpec<R>> endpointConfigurer) {
		return handle(router, endpointConfigurer);
	}

	/**
	 * Populate the "artificial" {@link GatewayMessageHandler} for the provided
	 * {@code requestChannel} to send a request with default options.
	 * Uses {@link org.springframework.integration.gateway.RequestReplyExchanger} Proxy
	 * on the background.
	 * @param requestChannel the {@link MessageChannel} bean name.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B gateway(String requestChannel) {
		return gateway(requestChannel, null);
	}

	/**
	 * Populate the "artificial" {@link GatewayMessageHandler} for the provided
	 * {@code requestChannel} to send a request with options from {@link GatewayEndpointSpec}.
	 * Uses {@link org.springframework.integration.gateway.RequestReplyExchanger} Proxy
	 * on the background.
	 * @param requestChannel the {@link MessageChannel} bean name.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B gateway(String requestChannel, Consumer<GatewayEndpointSpec> endpointConfigurer) {
		return register(new GatewayEndpointSpec(requestChannel), endpointConfigurer);
	}

	/**
	 * Populate the "artificial" {@link GatewayMessageHandler} for the provided
	 * {@code requestChannel} to send a request with default options.
	 * Uses {@link org.springframework.integration.gateway.RequestReplyExchanger} Proxy
	 * on the background.
	 * @param requestChannel the {@link MessageChannel} to use.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B gateway(MessageChannel requestChannel) {
		return gateway(requestChannel, null);
	}

	/**
	 * Populate the "artificial" {@link GatewayMessageHandler} for the provided
	 * {@code requestChannel} to send a request with options from {@link GatewayEndpointSpec}.
	 * Uses {@link org.springframework.integration.gateway.RequestReplyExchanger} Proxy
	 * on the background.
	 * @param requestChannel the {@link MessageChannel} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B gateway(MessageChannel requestChannel, Consumer<GatewayEndpointSpec> endpointConfigurer) {
		return register(new GatewayEndpointSpec(requestChannel), endpointConfigurer);
	}

	/**
	 * Populate the "artificial" {@link GatewayMessageHandler} for the provided
	 * {@code subflow}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .gateway(f -> f.transform("From Gateway SubFlow: "::concat))
	 * }
	 * </pre>
	 * @param flow the {@link IntegrationFlow} to to send a request message and wait for reply.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B gateway(IntegrationFlow flow) {
		return gateway(flow, null);
	}

	/**
	 * Populate the "artificial" {@link GatewayMessageHandler} for the provided
	 * {@code subflow} with options from {@link GatewayEndpointSpec}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .gateway(f -> f.transform("From Gateway SubFlow: "::concat), e -> e.replyTimeout(100L))
	 * }
	 * </pre>
	 * @param flow the {@link IntegrationFlow} to to send a request message and wait for reply.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 */
	public B gateway(IntegrationFlow flow, Consumer<GatewayEndpointSpec> endpointConfigurer) {
		Assert.notNull(flow, "'flow' must not be null");
		final DirectChannel requestChannel = new DirectChannel();
		IntegrationFlowBuilder flowBuilder = IntegrationFlows.from(requestChannel);
		flow.configure(flowBuilder);
		addComponent(flowBuilder.get());
		return gateway(requestChannel, endpointConfigurer);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the {@code INFO}
	 * logging level and {@code org.springframework.integration.handler.LoggingHandler}
	 * as a default logging category.
	 * <p> The full request {@link Message} will be logged.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.2
	 */
	public B log() {
		return log(LoggingHandler.Level.INFO);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for provided {@link LoggingHandler.Level}
	 * logging level and {@code org.springframework.integration.handler.LoggingHandler}
	 * as a default logging category.
	 * <p> The full request {@link Message} will be logged.
	 * @param level the {@link LoggingHandler.Level}.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.2
	 */
	public B log(LoggingHandler.Level level) {
		return log(level, (String) null);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the provided logging category
	 * and {@code INFO} logging level.
	 * <p> The full request {@link Message} will be logged.
	 * @param category the logging category to use.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.2
	 */
	public B log(String category) {
		return log(LoggingHandler.Level.INFO, category);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the provided
	 * {@link LoggingHandler.Level} logging level and logging category.
	 * <p> The full request {@link Message} will be logged.
	 * @param level the {@link LoggingHandler.Level}.
	 * @param category the logging category to use.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.2
	 */
	public B log(LoggingHandler.Level level, String category) {
		return log(level, category, (Expression) null);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the provided
	 * {@link LoggingHandler.Level} logging level, logging category
	 * and SpEL expression for the log message.
	 * @param level the {@link LoggingHandler.Level}.
	 * @param category the logging category.
	 * @param logExpression the SpEL expression to evaluate logger message at runtime
	 * against the request {@link Message}.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.2
	 */
	public B log(LoggingHandler.Level level, String category, String logExpression) {
		Assert.hasText(logExpression, "'logExpression' must not be empty");
		return log(level, category, PARSER.parseExpression(logExpression));
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the {@code INFO} logging level,
	 * the {@code org.springframework.integration.handler.LoggingHandler}
	 * as a default logging category and {@link Function} for the log message.
	 * @param logFunction the function to evaluate logger message at runtime
	 * @param <P> the expected payload type.
	 * against the request {@link Message}.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.2
	 */
	public <P> B log(Function<Message<P>, Object> logFunction) {
		Assert.notNull(logFunction, "'logFunction' must not be null");
		return log(new FunctionExpression<Message<P>>(logFunction));
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the {@code INFO} logging level,
	 * the {@code org.springframework.integration.handler.LoggingHandler}
	 * as a default logging category and SpEL expression to evaluate
	 * logger message at runtime against the request {@link Message}.
	 * @param logExpression the {@link Expression} to evaluate logger message at runtime
	 * against the request {@link Message}.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.2
	 */
	public B log(Expression logExpression) {
		return log(LoggingHandler.Level.INFO, logExpression);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the provided
	 * {@link LoggingHandler.Level} logging level,
	 * the {@code org.springframework.integration.handler.LoggingHandler}
	 * as a default logging category and SpEL expression to evaluate
	 * logger message at runtime against the request {@link Message}.
	 * @param level the {@link LoggingHandler.Level}.
	 * @param logExpression the {@link Expression} to evaluate logger message at runtime
	 * against the request {@link Message}.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.2
	 */
	public B log(LoggingHandler.Level level, Expression logExpression) {
		return log(level, null, logExpression);
	}


	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the {@code INFO}
	 * {@link LoggingHandler.Level} logging level,
	 * the provided logging category and SpEL expression to evaluate
	 * logger message at runtime against the request {@link Message}.
	 * @param category the logging category.
	 * @param logExpression the {@link Expression} to evaluate logger message at runtime
	 * against the request {@link Message}.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.2
	 */
	public B log(String category, Expression logExpression) {
		return log(LoggingHandler.Level.INFO, category, logExpression);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the provided
	 * {@link LoggingHandler.Level} logging level,
	 * the {@code org.springframework.integration.handler.LoggingHandler}
	 * as a default logging category and {@link Function} for the log message.
	 * @param level the {@link LoggingHandler.Level}.
	 * @param function the function to evaluate logger message at runtime
	 * @param <P> the expected payload type.
	 * against the request {@link Message}.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.2
	 */
	public <P> B log(LoggingHandler.Level level, Function<Message<P>, Object> function) {
		return log(level, null, function);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the provided
	 * {@link LoggingHandler.Level} logging level,
	 * the provided logging category and {@link Function} for the log message.
	 * @param category the logging category.
	 * @param function the function to evaluate logger message at runtime
	 * @param <P> the expected payload type.
	 * against the request {@link Message}.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.2
	 */
	public <P> B log(String category, Function<Message<P>, Object> function) {
		return log(LoggingHandler.Level.INFO, category, function);
	}

	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the provided
	 * {@link LoggingHandler.Level} logging level, logging category
	 * and {@link Function} for the log message.
	 * @param level the {@link LoggingHandler.Level}.
	 * @param category the logging category.
	 * @param function the function to evaluate logger message at runtime
	 * @param <P> the expected payload type.
	 * against the request {@link Message}.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.2
	 */
	public <P> B log(LoggingHandler.Level level, String category, Function<Message<P>, Object> function) {
		Assert.notNull(function, "'function' must not be null");
		return log(level, category, new FunctionExpression<Message<P>>(function));
	}


	/**
	 * Populate a {@link WireTap} for the {@link #currentMessageChannel}
	 * with the {@link LoggingHandler} subscriber for the provided
	 * {@link LoggingHandler.Level} logging level, logging category
	 * and SpEL expression for the log message.
	 * @param level the {@link LoggingHandler.Level}.
	 * @param category the logging category.
	 * @param logExpression the {@link Expression} to evaluate logger message at runtime
	 * against the request {@link Message}.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.2
	 */
	public B log(LoggingHandler.Level level, String category, Expression logExpression) {
		LoggingHandler loggingHandler = new LoggingHandler(level);
		if (StringUtils.hasText(category)) {
			loggingHandler.setLoggerName(category);
		}

		if (logExpression != null) {
			loggingHandler.setLogExpression(logExpression);
		}
		else {
			loggingHandler.setShouldLogFullMessage(true);
		}

		addComponent(loggingHandler);
		MessageChannel loggerChannel = new FixedSubscriberChannel(loggingHandler);
		return wireTap(loggerChannel);
	}

	/**
	 * Populate a {@link ScatterGatherHandler} to the current integration flow position
	 * based on the provided {@link MessageChannel} for scattering function
	 * and default {@link AggregatorSpec} for gathering function.
	 * @param scatterChannel the {@link MessageChannel} for scatting requests.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.2
	 */
	public B scatterGather(MessageChannel scatterChannel) {
		return scatterGather(scatterChannel, null);
	}

	/**
	 * Populate a {@link ScatterGatherHandler} to the current integration flow position
	 * based on the provided {@link MessageChannel} for scattering function
	 * and {@link AggregatorSpec} for gathering function.
	 * @param scatterChannel the {@link MessageChannel} for scatting requests.
	 * @param gatherer the {@link Consumer} for {@link AggregatorSpec} to configure gatherer.
	 * Can be {@code null}.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.2
	 */
	public B scatterGather(MessageChannel scatterChannel, Consumer<AggregatorSpec> gatherer) {
		return scatterGather(scatterChannel, gatherer, null);
	}

	/**
	 * Populate a {@link ScatterGatherHandler} to the current integration flow position
	 * based on the provided {@link MessageChannel} for scattering function
	 * and {@link AggregatorSpec} for gathering function.
	 * @param scatterChannel the {@link MessageChannel} for scatting requests.
	 * @param gatherer the {@link Consumer} for {@link AggregatorSpec} to configure gatherer.
	 * Can be {@code null}.
	 * @param scatterGather the {@link Consumer} for {@link ScatterGatherSpec} to configure
	 * {@link ScatterGatherHandler} and its endpoint. Can be {@code null}.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.2
	 */
	public B scatterGather(MessageChannel scatterChannel, Consumer<AggregatorSpec> gatherer,
			Consumer<ScatterGatherSpec> scatterGather) {
		AggregatorSpec aggregatorSpec = new AggregatorSpec();
		if (gatherer != null) {
			gatherer.accept(aggregatorSpec);
		}

		AggregatingMessageHandler aggregatingMessageHandler = aggregatorSpec.get().getT2();
		addComponent(aggregatingMessageHandler);
		ScatterGatherHandler messageHandler = new ScatterGatherHandler(scatterChannel, aggregatingMessageHandler);
		return register(new ScatterGatherSpec(messageHandler), scatterGather);
	}

	/**
	 * Populate a {@link ScatterGatherHandler} to the current integration flow position
	 * based on the provided {@link RecipientListRouterSpec} for scattering function
	 * and default {@link AggregatorSpec} for gathering function.
	 * @param scatterer the {@link Consumer} for {@link RecipientListRouterSpec} to configure scatterer.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.2
	 */
	public B scatterGather(Consumer<RecipientListRouterSpec> scatterer) {
		return scatterGather(scatterer, null);
	}

	/**
	 * Populate a {@link ScatterGatherHandler} to the current integration flow position
	 * based on the provided {@link RecipientListRouterSpec} for scattering function
	 * and {@link AggregatorSpec} for gathering function.
	 * @param scatterer the {@link Consumer} for {@link RecipientListRouterSpec} to configure scatterer.
	 * Can be {@code null}.
	 * @param gatherer the {@link Consumer} for {@link AggregatorSpec} to configure gatherer.
	 * Can be {@code null}.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.2
	 */
	public B scatterGather(Consumer<RecipientListRouterSpec> scatterer, Consumer<AggregatorSpec> gatherer) {
		return scatterGather(scatterer, gatherer, null);
	}

	/**
	 * Populate a {@link ScatterGatherHandler} to the current integration flow position
	 * based on the provided {@link RecipientListRouterSpec} for scattering function
	 * and {@link AggregatorSpec} for gathering function.
	 * @param scatterer the {@link Consumer} for {@link RecipientListRouterSpec} to configure scatterer.
	 * @param gatherer the {@link Consumer} for {@link AggregatorSpec} to configure gatherer.
	 * @param scatterGather the {@link Consumer} for {@link ScatterGatherSpec} to configure
	 * {@link ScatterGatherHandler} and its endpoint. Can be {@code null}.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.2
	 */
	public B scatterGather(Consumer<RecipientListRouterSpec> scatterer, Consumer<AggregatorSpec> gatherer,
			Consumer<ScatterGatherSpec> scatterGather) {
		Assert.notNull(scatterer, "'scatterer' must not be null");
		RecipientListRouterSpec recipientListRouterSpec = new RecipientListRouterSpec();
		scatterer.accept(recipientListRouterSpec);
		AggregatorSpec aggregatorSpec = new AggregatorSpec();
		if (gatherer != null) {
			gatherer.accept(aggregatorSpec);
		}

		RecipientListRouter recipientListRouter = recipientListRouterSpec.get();
		addComponent(recipientListRouter);
		addComponents(recipientListRouterSpec.getComponentsToRegister());
		AggregatingMessageHandler aggregatingMessageHandler = aggregatorSpec.get().getT2();
		addComponent(aggregatingMessageHandler);
		ScatterGatherHandler messageHandler = new ScatterGatherHandler(recipientListRouter, aggregatingMessageHandler);
		return register(new ScatterGatherSpec(messageHandler), scatterGather);
	}

	/**
	 * Populate a {@link BarrierMessageHandler} instance for provided timeout.
	 * @param timeout the timeout in milliseconds.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.2
	 */
	public B barrier(long timeout) {
		return barrier(timeout, null);
	}

	/**
	 * Populate a {@link BarrierMessageHandler} instance for provided timeout
	 * and options from {@link BarrierSpec} and endpoint options from {@link GenericEndpointSpec}.
	 * @param timeout the timeout in milliseconds.
	 * @param barrierConfigurer the {@link Consumer} to provide {@link BarrierMessageHandler} options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.2
	 */
	public B barrier(long timeout, Consumer<BarrierSpec> barrierConfigurer) {
		return register(new BarrierSpec(timeout), barrierConfigurer);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} instance to perform {@link MessageTriggerAction}.
	 * @param triggerActionId the {@link MessageTriggerAction} bean id.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.2
	 */
	public B trigger(String triggerActionId) {
		return trigger(triggerActionId, null);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} instance to perform {@link MessageTriggerAction}
	 * and endpoint options from {@link GenericEndpointSpec}.
	 * @param triggerActionId the {@link MessageTriggerAction} bean id.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.2
	 */
	public B trigger(String triggerActionId,
			Consumer<GenericEndpointSpec<ServiceActivatingHandler>> endpointConfigurer) {
		MessageProcessor<Void> trigger = new BeanNameMessageProcessor<Void>(triggerActionId, "trigger");
		return handle(new ServiceActivatingHandler(trigger), endpointConfigurer);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} instance to perform {@link MessageTriggerAction}.
	 * @param triggerAction the {@link MessageTriggerAction}.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.2
	 */
	public B trigger(MessageTriggerAction triggerAction) {
		return trigger(triggerAction, null);
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} instance to perform {@link MessageTriggerAction}
	 * and endpoint options from {@link GenericEndpointSpec}.
	 * @param triggerAction the {@link MessageTriggerAction}.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @return the current {@link IntegrationFlowDefinition}.
	 * @since 1.2
	 */
	public B trigger(MessageTriggerAction triggerAction,
			Consumer<GenericEndpointSpec<ServiceActivatingHandler>> endpointConfigurer) {
		return handle(new ServiceActivatingHandler(triggerAction, "trigger"), endpointConfigurer);
	}


	/**
	 * Represent an Integration Flow as a Reactive Streams {@link Publisher} bean.
	 * @param <T> the {@code payload} type
	 * @return the Reactive Streams {@link Publisher}
	 */
	public <T> Publisher<Message<T>> toReactivePublisher() {
		return toReactivePublisher(Executors.newSingleThreadExecutor());
	}

	/**
	 * Represent an Integration Flow as a Reactive Streams {@link Publisher} bean.
	 * @param executor the managed {@link Executor} to be used for the background task to
	 *                 poll messages from the {@link PollableChannel}.
	 *                 Defaults to {@link Executors#newSingleThreadExecutor()}.
	 * @param <T> the {@code payload} type
	 * @return the Reactive Streams {@link Publisher}
	 */
	public <T> Publisher<Message<T>> toReactivePublisher(Executor executor) {
		Assert.notNull(executor, "'executor' must not be null");
		MessageChannel channelForPublisher = this.currentMessageChannel;
		if (channelForPublisher == null) {
			PublishSubscribeChannel publishSubscribeChannel = new PublishSubscribeChannel();
			publishSubscribeChannel.setMinSubscribers(1);
			channelForPublisher = publishSubscribeChannel;
			channel(channelForPublisher);
		}
		get();
		return new PublisherIntegrationFlow<T>(this.integrationComponents, channelForPublisher, executor);
	}

	private <S extends ConsumerEndpointSpec<S, ? extends MessageHandler>> B register(S endpointSpec,
			Consumer<S> endpointConfigurer) {
		if (endpointConfigurer != null) {
			endpointConfigurer.accept(endpointSpec);
		}

		addComponents(endpointSpec.getComponentsToRegister());

		MessageChannel inputChannel = this.currentMessageChannel;
		this.currentMessageChannel = null;
		if (inputChannel == null) {
			inputChannel = new DirectChannel();
			this.registerOutputChannelIfCan(inputChannel);
		}

		Tuple2<ConsumerEndpointFactoryBean, ? extends MessageHandler> factoryBeanTuple2 = endpointSpec.get();
		if (inputChannel instanceof MessageChannelReference) {
			factoryBeanTuple2.getT1().setInputChannelName(((MessageChannelReference) inputChannel).getName());
		}
		else {
			if (inputChannel instanceof FixedSubscriberChannelPrototype) {
				String beanName = ((FixedSubscriberChannelPrototype) inputChannel).getName();
				inputChannel = new FixedSubscriberChannel(factoryBeanTuple2.getT2());
				if (beanName != null) {
					((FixedSubscriberChannel) inputChannel).setBeanName(beanName);
				}
				registerOutputChannelIfCan(inputChannel);
			}
			factoryBeanTuple2.getT1().setInputChannel(inputChannel);
		}

		return addComponent(endpointSpec).currentComponent(factoryBeanTuple2.getT2());
	}

	private B registerOutputChannelIfCan(MessageChannel outputChannel) {
		if (!(outputChannel instanceof FixedSubscriberChannelPrototype)) {
			this.integrationComponents.add(outputChannel);
			if (this.currentComponent != null) {
				String channelName = null;
				if (outputChannel instanceof MessageChannelReference) {
					channelName = ((MessageChannelReference) outputChannel).getName();
				}

				Object currentComponent = this.currentComponent;

				if (AopUtils.isAopProxy(currentComponent)) {
					currentComponent = extractProxyTarget(currentComponent);
				}

				if (currentComponent instanceof AbstractMessageProducingHandler) {
					AbstractMessageProducingHandler messageProducer =
							(AbstractMessageProducingHandler) currentComponent;
					checkReuse(messageProducer);
					if (channelName != null) {
						messageProducer.setOutputChannelName(channelName);
					}
					else {
						messageProducer.setOutputChannel(outputChannel);
					}
				}
				else if (currentComponent instanceof SourcePollingChannelAdapterSpec) {
					SourcePollingChannelAdapterFactoryBean pollingChannelAdapterFactoryBean =
							((SourcePollingChannelAdapterSpec) currentComponent).get().getT1();
					if (channelName != null) {
						pollingChannelAdapterFactoryBean.setOutputChannelName(channelName);
					}
					else {
						pollingChannelAdapterFactoryBean.setOutputChannel(outputChannel);
					}
				}
				else {
					throw new BeanCreationException("The 'currentComponent' (" + currentComponent +
							") is a one-way 'MessageHandler' and it isn't appropriate to configure 'outputChannel'. " +
							"This is the end of the integration flow.");
				}
				this.currentComponent = null;
			}
		}
		return _this();
	}

	private boolean isOutputChannelRequired() {
		if (this.currentComponent != null) {
			Object currentComponent = this.currentComponent;

			if (AopUtils.isAopProxy(currentComponent)) {
				currentComponent = extractProxyTarget(currentComponent);
			}

			return currentComponent instanceof AbstractMessageProducingHandler
					|| currentComponent instanceof SourcePollingChannelAdapterSpec;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	protected final B _this() {
		return (B) this;
	}

	protected StandardIntegrationFlow get() {
		if (this.integrationFlow == null) {
			if (this.currentMessageChannel instanceof FixedSubscriberChannelPrototype) {
				throw new BeanCreationException("The 'currentMessageChannel' (" + this.currentMessageChannel
						+ ") is a prototype for FixedSubscriberChannel which can't be created without MessageHandler "
						+ "constructor argument. That means that '.fixedSubscriberChannel()' can't be the last "
						+ "EIP-method in the IntegrationFlow definition.");
			}

			if (this.integrationComponents.size() == 1) {
				if (this.currentComponent != null) {
					if (this.currentComponent instanceof SourcePollingChannelAdapterSpec) {
						throw new BeanCreationException("The 'SourcePollingChannelAdapter' (" + this.currentComponent
								+ ") " + "must be configured with at least one 'MessageChannel' or 'MessageHandler'.");
					}
				}
				else if (this.currentMessageChannel != null) {
					throw new BeanCreationException("The 'IntegrationFlow' can't consist of only one 'MessageChannel'. "
							+ "Add at lest '.bridge()' EIP-method before the end of flow.");
				}
			}
			this.integrationFlow = new StandardIntegrationFlow(this.integrationComponents);
		}
		return this.integrationFlow;
	}

	private static boolean isLambda(Object o) {
		Class<?> aClass = o.getClass();
		return aClass.isSynthetic() && !aClass.isAnonymousClass() && !aClass.isLocalClass();
	}

	private static Object extractProxyTarget(Object target) {
		if (!(target instanceof Advised)) {
			return target;
		}
		Advised advised = (Advised) target;
		if (advised.getTargetSource() == null) {
			return null;
		}
		try {
			return extractProxyTarget(advised.getTargetSource().getTarget());
		}
		catch (Exception e) {
			throw new BeanCreationException("Could not extract target", e);
		}
	}

	private void checkReuse(AbstractMessageProducingHandler replyHandler) {
		Assert.isTrue(!REFERENCED_REPLY_PRODUCERS.contains(replyHandler),
				"An AbstractMessageProducingHandler may only be referenced once ("
						+ replyHandler.getComponentName()
						+ ") - use @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE) on @Bean definition.");
		REFERENCED_REPLY_PRODUCERS.add(replyHandler);
	}

}
