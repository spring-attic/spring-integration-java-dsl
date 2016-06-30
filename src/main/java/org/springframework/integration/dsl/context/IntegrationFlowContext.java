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

package org.springframework.integration.dsl.context;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.context.Lifecycle;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.dsl.support.FixedSubscriberChannelPrototype;
import org.springframework.integration.dsl.support.MessageChannelReference;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * A public API for dynamic (manual) registration of {@link IntegrationFlow},
 * not via standard bean registration phase.
 * <p>
 * The bean of this component is provided via framework automatically.
 * A bean name is based on the decapitalized class name.
 * It must be injected to the target service before use.
 * <p>
 * The typical use-case, and, therefore algorithm, is:
 * <ul>
 * <li> create {@link IntegrationFlow} depending of the business logic
 * <li> register that {@link IntegrationFlow} in this {@link IntegrationFlowContext},
 * with optional {@code id} and {@code autoStartup} flag
 * <li> obtain a {@link MessagingTemplate} for that {@link IntegrationFlow}
 * (if it is started from the {@link MessageChannel}) and send (or send-and-receive)
 * messages to the {@link IntegrationFlow}
 * <li> remove the {@link IntegrationFlow} by its {@code id} from this {@link IntegrationFlowContext}
 * </ul>
 *
 * @author Artem Bilan
 * @since 1.2
 */
public final class IntegrationFlowContext implements BeanFactoryAware {

	private final Map<String, Object> registry = new HashMap<String, Object>();

	private final Map<String, MessageChannel> flowInputChannelCache = new ConcurrentHashMap<String, MessageChannel>();

	private ConfigurableListableBeanFactory beanFactory;

	private IntegrationFlowContext() {
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory,
				"To use Spring Integration Java DSL the 'beanFactory' has to be an instance of " +
						"'ConfigurableListableBeanFactory'. " +
						"Consider using 'GenericApplicationContext' implementation.");
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	/**
	 * Register provided {@link IntegrationFlow} with generated {@code id}
	 * in the application context as a bean together with all its dependant components.
	 * And preform its lifecycle start.
	 * @param integrationFlow the {@link IntegrationFlow} to register
	 * @return a generated id for the provided {@link IntegrationFlow}
	 */
	public String register(IntegrationFlow integrationFlow) {
		return register(integrationFlow, true);
	}

	/**
	 * Register provided {@link IntegrationFlow} with generated {@code id}
	 * in the application context as a bean together with all its dependant components.
	 * And preform its lifecycle start if {@code autoStartup == true}.
	 * @param integrationFlow the {@link IntegrationFlow} to register
	 * @param autoStartup to start or not the {@link IntegrationFlow} automatically after registration
	 * @return a generated id for the provided {@link IntegrationFlow}
	 */
	public String register(IntegrationFlow integrationFlow, boolean autoStartup) {
		String id = generateBeanName(integrationFlow);
		register(id, integrationFlow, autoStartup);
		return id;
	}

	/**
	 * Register provided {@link IntegrationFlow} under provided {@code id}
	 * in the application context as a bean together with all its dependant components.
	 * And preform its lifecycle start.
	 * @param flowId the bean name to register for
	 * @param integrationFlow the {@link IntegrationFlow} to register
	 */
	public void register(String flowId, IntegrationFlow integrationFlow) {
		register(flowId, integrationFlow, true);
	}

	/**
	 * Register provided {@link IntegrationFlow} under provided {@code id}
	 * in the application context as a bean together with all its dependant components.
	 * And preform its lifecycle start.
	 * @param flowId the bean name to register for
	 * @param integrationFlow the {@link IntegrationFlow} to register
	 * @param autoStartup to start or not the {@link IntegrationFlow} automatically after registration
	 */
	public void register(String flowId, IntegrationFlow integrationFlow, boolean autoStartup) {
		Object theFlow = this.beanFactory.initializeBean(integrationFlow, flowId);
		this.beanFactory.registerSingleton(flowId, theFlow);

		if (autoStartup) {
			if (theFlow instanceof Lifecycle) {
				((Lifecycle) theFlow).start();
			}
			else {
				throw new IllegalStateException("For 'autoStartup' mode the 'IntegrationFlow' " +
						"must be an instance of 'Lifecycle'.\n" +
						"Consider to implement it for [" + integrationFlow + "]. " +
						"Or start dependant components on their own.");
			}
		}

		this.registry.put(flowId, theFlow);
	}

	/**
	 * Destroy an {@link IntegrationFlow} bean (as well as all its dependant beans)
	 * for provided {@code flowId} and clean up all the local cache for it.
	 * @param flowId the bean name to destroy from
	 */
	public synchronized void remove(String flowId) {
		if (this.registry.containsKey(flowId)) {
			((DefaultSingletonBeanRegistry) this.beanFactory).destroySingleton(flowId);
			this.registry.remove(flowId);
			this.flowInputChannelCache.remove(flowId);
		}
		else {
			throw new IllegalStateException("Only manually registered IntegrationFlows can be removed. " +
					"But [" + flowId + "] ins't one of them.");
		}
	}

	/**
	 * Obtain a {@link MessagingTemplate} with its default destination set to the input channel
	 * of the {@link IntegrationFlow} for provided {@code flowId}.
	 * <p> Any {@link IntegrationFlow} bean (not only manually registered) can be used for this method.
	 * <p> If {@link IntegrationFlow} doesn't start with the {@link MessageChannel}, the
	 * {@link IllegalStateException} is thrown.
	 * @param flowId the bean name to obtain the input channel from
	 * @return the {@link MessagingTemplate} instance
	 */
	public MessagingTemplate messagingTemplateFor(String flowId) {
		MessageChannel channel = this.flowInputChannelCache.get(flowId);
		if (channel == null) {
			Object o = this.beanFactory.getBean(flowId);
			if (o instanceof StandardIntegrationFlow) {
				StandardIntegrationFlow integrationFlow = (StandardIntegrationFlow) o;
				Object next = integrationFlow.getIntegrationComponents().iterator().next();
				if (next instanceof MessageChannel) {
					channel = (MessageChannel) next;
					if (channel instanceof MessageChannelReference) {
						channel = this.beanFactory.getBean(((MessageChannelReference) channel).getName(),
								MessageChannel.class);
					}
					else if (channel instanceof FixedSubscriberChannelPrototype) {
						channel = this.beanFactory.getBean(((FixedSubscriberChannelPrototype) channel).getName(),
								MessageChannel.class);
					}
					this.flowInputChannelCache.put(flowId, channel);
				}
				else {
					throw new IllegalStateException("The 'IntegrationFlow' [" + integrationFlow + "] " +
							"doesn't start with 'MessageChannel' for direct message sending.");
				}
			}
			else {
				throw new IllegalStateException("Only 'StandardIntegrationFlow' instances " +
						"(e.g. extracted from 'IntegrationFlow' Lambdas) can be used for direct 'send' operation. " +
						"But [" + o + "] ins't one of them.\n" +
						"Consider 'BeanFactory.getBean()' usage for sending messages " +
						"to the required 'MessageChannel'.");
			}
		}

		MessagingTemplate messagingTemplate = new MessagingTemplate(channel) {

			@Override
			public Message<?> receive() {
				return receiveAndConvert(Message.class);
			}

			@Override
			public <T> T receiveAndConvert(Class<T> targetClass) {
				throw new UnsupportedOperationException("The 'receive()/receiveAndConvert()' isn't supported on " +
						"the 'IntegrationFlow' input channel.");
			}

		};
		messagingTemplate.setBeanFactory(this.beanFactory);
		return messagingTemplate;
	}

	private String generateBeanName(Object instance) {
		if (instance instanceof NamedComponent && ((NamedComponent) instance).getComponentName() != null) {
			return ((NamedComponent) instance).getComponentName();
		}
		String generatedBeanName = instance.getClass().getName();
		String id = instance.getClass().getName();
		int counter = -1;
		while (counter == -1 || this.beanFactory.containsBean(id)) {
			counter++;
			id = generatedBeanName + BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR + counter;
		}
		return id;
	}

}
