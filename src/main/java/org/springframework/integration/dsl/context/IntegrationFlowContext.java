/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.dsl.context;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.support.context.NamedComponent;
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
 * <p>
 * For convenience an associated {@link IntegrationFlowRegistration} is returned after registration.
 * It can be used for access to the target {@link IntegrationFlow} or for manipulation with its lifecycle.
 *
 * @author Artem Bilan
 *
 * @since 1.2
 *
 * @see IntegrationFlowRegistration
 */
public final class IntegrationFlowContext implements BeanFactoryAware {

	private final Map<String, IntegrationFlowRegistration> registry =
			new HashMap<String, IntegrationFlowRegistration>();

	private ConfigurableListableBeanFactory beanFactory;

	private AutowiredAnnotationBeanPostProcessor autowiredAnnotationBeanPostProcessor;

	private IntegrationFlowContext() {
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory,
				"To use Spring Integration Java DSL the 'beanFactory' has to be an instance of " +
						"'ConfigurableListableBeanFactory'. " +
						"Consider using 'GenericApplicationContext' implementation.");
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		this.autowiredAnnotationBeanPostProcessor = new AutowiredAnnotationBeanPostProcessor();
		this.autowiredAnnotationBeanPostProcessor.setBeanFactory(this.beanFactory);
	}

	/**
	 * Associate provided {@link IntegrationFlow} with an {@link IntegrationFlowRegistrationBuilder}
	 * for additional options and farther registration in the application context.
	 * @param integrationFlow the {@link IntegrationFlow} to register
	 * @return the IntegrationFlowRegistrationBuilder associated with the provided {@link IntegrationFlow}
	 */
	public IntegrationFlowRegistrationBuilder registration(IntegrationFlow integrationFlow) {
		return new IntegrationFlowRegistrationBuilder(integrationFlow);
	}

	private void register(IntegrationFlowRegistrationBuilder builder) {
		IntegrationFlow integrationFlow = builder.integrationFlowRegistration.getIntegrationFlow();
		String flowId = builder.integrationFlowRegistration.getId();
		if (flowId == null) {
			flowId = generateBeanName(integrationFlow, null);
			builder.id(flowId);
		}
		IntegrationFlow theFlow = (IntegrationFlow) registerBean(integrationFlow, flowId, null);
		builder.integrationFlowRegistration.setIntegrationFlow(theFlow);

		for (Map.Entry<Object, String> entry : builder.additionalBeans.entrySet()) {
			registerBean(entry.getKey(), entry.getValue(), flowId);
		}

		if (builder.autoStartup) {
			builder.integrationFlowRegistration.start();
		}
		this.registry.put(flowId, builder.integrationFlowRegistration);
	}

	private Object registerBean(Object bean, String beanName, String parentName) {
		if (beanName == null) {
			beanName = generateBeanName(bean, parentName);
		}

		this.autowiredAnnotationBeanPostProcessor.processInjection(bean);
		bean = this.beanFactory.initializeBean(bean, beanName);
		this.beanFactory.registerSingleton(beanName, bean);
		if (parentName != null) {
			this.beanFactory.registerDependentBean(parentName, beanName);
		}
		if (bean instanceof DisposableBean) {
			((DefaultSingletonBeanRegistry) this.beanFactory)
					.registerDisposableBean(beanName, (DisposableBean) bean);
		}
		return bean;
	}

	/**
	 * Obtain an {@link IntegrationFlowRegistration} for the {@link IntegrationFlow}
	 * associated with the provided {@code flowId}.
	 * @param flowId the bean name to obtain
	 * @return the IntegrationFlowRegistration for provided {@code id} or {@code null}
	 */
	public IntegrationFlowRegistration getRegistrationById(String flowId) {
		return this.registry.get(flowId);
	}

	/**
	 * Destroy an {@link IntegrationFlow} bean (as well as all its dependant beans)
	 * for provided {@code flowId} and clean up all the local cache for it.
	 * @param flowId the bean name to destroy from
	 */
	public synchronized void remove(String flowId) {
		if (this.registry.containsKey(flowId)) {
			IntegrationFlowRegistration flowRegistration = this.registry.remove(flowId);
			flowRegistration.stop();
			((DefaultSingletonBeanRegistry) this.beanFactory).destroySingleton(flowId);
		}
		else {
			throw new IllegalStateException("Only manually registered IntegrationFlows can be removed. "
					+ "But [" + flowId + "] ins't one of them.");
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
		return this.registry.get(flowId).getMessagingTemplate();
	}

	private String generateBeanName(Object instance, String parentName) {
		if (instance instanceof NamedComponent && ((NamedComponent) instance).getComponentName() != null) {
			return ((NamedComponent) instance).getComponentName();
		}
		String generatedBeanName = (parentName != null ? parentName : "") + instance.getClass().getName();
		String id = generatedBeanName;
		int counter = -1;
		while (counter == -1 || this.beanFactory.containsBean(id)) {
			counter++;
			id = generatedBeanName + BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR + counter;
		}
		return id;
	}

	/**
	 * A Builder pattern implementation for the options to register {@link IntegrationFlow}
	 * in the application context.
	 */
	public final class IntegrationFlowRegistrationBuilder {

		private Map<Object, String> additionalBeans = new HashMap<Object, String>();

		private final IntegrationFlowRegistration integrationFlowRegistration;

		private boolean autoStartup = true;

		private IntegrationFlowRegistrationBuilder(IntegrationFlow integrationFlow) {
			this.integrationFlowRegistration = new IntegrationFlowRegistration(integrationFlow);
			this.integrationFlowRegistration.setBeanFactory(IntegrationFlowContext.this.beanFactory);
			this.integrationFlowRegistration.setIntegrationFlowContext(IntegrationFlowContext.this);
		}

		public IntegrationFlowRegistrationBuilder id(String id) {
			this.integrationFlowRegistration.setId(id);
			return this;
		}

		public IntegrationFlowRegistrationBuilder autoStartup(boolean autoStartup) {
			this.autoStartup = autoStartup;
			return this;
		}

		public IntegrationFlowRegistrationBuilder addBean(Object bean) {
			return addBean(null, bean);
		}

		public IntegrationFlowRegistrationBuilder addBean(String name, Object bean) {
			this.additionalBeans.put(bean, name);
			return this;
		}

		public IntegrationFlowRegistration register() {
			IntegrationFlowContext.this.register(this);
			return this.integrationFlowRegistration;
		}

	}

}
