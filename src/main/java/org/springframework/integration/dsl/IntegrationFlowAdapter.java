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

package org.springframework.integration.dsl;

import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.channel.MessageChannelSpec;
import org.springframework.integration.dsl.core.MessageProducerSpec;
import org.springframework.integration.dsl.core.MessageSourceSpec;
import org.springframework.integration.dsl.core.MessagingGatewaySpec;
import org.springframework.integration.dsl.support.Consumer;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * The base {@code Adapter} class for the {@link IntegrationFlow} abstraction.
 * Requires the implementation for the {@link #buildFlow()} method to produce
 * {@link IntegrationFlowDefinition} using one of {@link #from} support methods.
 * <p>
 * Typically is used for target service implementation:
 * <pre class="code">
 *  &#64;Component
 *  public class MyFlowAdapter extends IntegrationFlowAdapter {
 *
 *     &#64;Autowired
 *     private ConnectionFactory rabbitConnectionFactory;
 *
 *     &#64;Override
 *     protected IntegrationFlowDefinition&lt;?&gt; buildFlow() {
 *          return from(Amqp.inboundAdapter(this.rabbitConnectionFactory, "myQueue"))
 *                   .&lt;String, String&gt;transform(String::toLowerCase)
 *                   .channel(c -&gt; c.queue("myFlowAdapterOutput"));
 *     }
 *
 * }
 * </pre>
 *
 * @author Artem Bilan
 *
 * @since 1.1
 */
public abstract class IntegrationFlowAdapter implements IntegrationFlow {

	@Override
	public final void configure(IntegrationFlowDefinition<?> flow) {
		IntegrationFlowDefinition<?> targetFlow = buildFlow();
		Assert.state(targetFlow != null, "the 'buildFlow()' must not return null");
		flow.integrationComponents.clear();
		flow.integrationComponents.addAll(targetFlow.integrationComponents);
	}

	protected IntegrationFlowDefinition<?> from(String messageChannelName) {
		return IntegrationFlows.from(messageChannelName);
	}

	protected IntegrationFlowDefinition<?> from(MessageChannel messageChannel) {
		return IntegrationFlows.from(messageChannel);
	}

	protected IntegrationFlowDefinition<?> from(String messageChannelName, boolean fixedSubscriber) {
		return IntegrationFlows.from(messageChannelName, fixedSubscriber);
	}

	protected IntegrationFlowDefinition<?> from(MessageSourceSpec<?, ? extends MessageSource<?>> messageSourceSpec,
			Consumer<SourcePollingChannelAdapterSpec> endpointConfigurer) {
		return IntegrationFlows.from(messageSourceSpec, endpointConfigurer);
	}

	protected IntegrationFlowDefinition<?> from(MessageSource<?> messageSource,
			Consumer<SourcePollingChannelAdapterSpec> endpointConfigurer) {
		return IntegrationFlows.from(messageSource, endpointConfigurer);
	}

	protected IntegrationFlowDefinition<?> from(IntegrationFlows.MessageSourcesFunction sources,
			Consumer<SourcePollingChannelAdapterSpec> endpointConfigurer) {
		return IntegrationFlows.from(sources, endpointConfigurer);
	}

	protected IntegrationFlowDefinition<?> from(IntegrationFlows.ChannelsFunction channels) {
		return IntegrationFlows.from(channels);
	}

	protected IntegrationFlowDefinition<?> from(MessageProducerSupport messageProducer) {
		return IntegrationFlows.from(messageProducer);
	}

	protected IntegrationFlowDefinition<?> from(MessageSource<?> messageSource) {
		return IntegrationFlows.from(messageSource);
	}

	protected IntegrationFlowDefinition<?> from(IntegrationFlows.MessageSourcesFunction sources) {
		return IntegrationFlows.from(sources);
	}

	protected IntegrationFlowDefinition<?> from(MessagingGatewaySupport inboundGateway) {
		return IntegrationFlows.from(inboundGateway);
	}

	protected IntegrationFlowDefinition<?> from(MessageChannelSpec<?, ?> messageChannelSpec) {
		return IntegrationFlows.from(messageChannelSpec);
	}

	protected IntegrationFlowDefinition<?> from(IntegrationFlows.MessageProducersFunction producers) {
		return IntegrationFlows.from(producers);
	}

	protected IntegrationFlowDefinition<?> from(MessageProducerSpec<?, ?> messageProducerSpec) {
		return IntegrationFlows.from(messageProducerSpec);
	}

	protected IntegrationFlowDefinition<?> from(MessageSourceSpec<?, ? extends MessageSource<?>> messageSourceSpec) {
		return IntegrationFlows.from(messageSourceSpec);
	}

	protected IntegrationFlowDefinition<?> from(MessagingGatewaySpec<?, ?> inboundGatewaySpec) {
		return IntegrationFlows.from(inboundGatewaySpec);
	}

	protected IntegrationFlowDefinition<?> from(IntegrationFlows.MessagingGatewaysFunction gateways) {
		return IntegrationFlows.from(gateways);
	}
	

	public static IntegrationFlowBuilder from(Object service, String methodName) {
		return IntegrationFlows.from(service, methodName);
	}

	public static IntegrationFlowBuilder from(Object service, String methodName, 
			Consumer<SourcePollingChannelAdapterSpec> endpointConfigurer) {
		return IntegrationFlows.from(service, methodName, endpointConfigurer);
	}

	protected abstract IntegrationFlowDefinition<?> buildFlow();

}
