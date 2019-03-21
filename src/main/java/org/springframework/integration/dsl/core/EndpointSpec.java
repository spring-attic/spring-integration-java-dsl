/*
 * Copyright 2014-2016 the original author or authors.
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

package org.springframework.integration.dsl.core;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.ResolvableType;
import org.springframework.integration.dsl.support.Function;
import org.springframework.integration.dsl.support.tuple.Tuple2;
import org.springframework.integration.dsl.support.tuple.Tuples;
import org.springframework.integration.endpoint.AbstractPollingEndpoint;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;

/**
 * An {@link IntegrationComponentSpec} for endpoints.
 *
 * @param <S> the target {@link ConsumerEndpointSpec} implementation type.
 * @param <F> the target {@link BeanNameAware} implementation type.
 * @param <H> the target {@link MessageHandler} implementation type.
 *
 * @author Artem Bilan
 */
public abstract class EndpointSpec<S extends EndpointSpec<S, F, H>, F extends BeanNameAware, H>
		extends IntegrationComponentSpec<S, Tuple2<F, H>>
		implements ComponentsRegistration {

	protected final Collection<Object> componentToRegister = new ArrayList<Object>();

	protected H handler;

	protected F endpointFactoryBean;

	@SuppressWarnings("unchecked")
	protected EndpointSpec(H handler) {
		try {
			Class<?> fClass = ResolvableType.forClass(this.getClass()).as(EndpointSpec.class).resolveGenerics()[1];
			this.endpointFactoryBean = (F) fClass.newInstance();
			this.handler = handler;
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public S id(String id) {
		this.endpointFactoryBean.setBeanName(id);
		return super.id(id);
	}

	/**
	 * @param pollers the pollers
	 * @return the endpoint spec.
	 * @see AbstractPollingEndpoint
	 * @see PollerFactory
	 */
	public S poller(Function<PollerFactory, PollerSpec> pollers) {
		return poller(pollers.apply(new PollerFactory()));
	}

	/**
	 * @param pollerMetadataSpec the pollerMetadataSpec
	 * @return the endpoint spec.
	 * @see AbstractPollingEndpoint
	 * @see PollerSpec
	 */
	public S poller(PollerSpec pollerMetadataSpec) {
		Collection<Object> componentsToRegister = pollerMetadataSpec.getComponentsToRegister();
		if (componentsToRegister != null) {
			this.componentToRegister.addAll(componentsToRegister);
		}
		return poller(pollerMetadataSpec.get());
	}

	/**
	 * @param pollerMetadata the pollerMetadata
	 * @return the endpoint spec.
	 * @see AbstractPollingEndpoint
	 */
	public abstract S poller(PollerMetadata pollerMetadata);

	/**
	 * @param phase the phase.
	 * @return the endpoint spec.
	 * @see SmartLifecycle
	 */
	public abstract S phase(int phase);

	/**
	 * @param autoStartup the autoStartup.
	 * @return the endpoint spec
	 * @see SmartLifecycle
	 */
	public abstract S autoStartup(boolean autoStartup);

	@Override
	public Collection<Object> getComponentsToRegister() {
		return this.componentToRegister.isEmpty()
				? null
				: this.componentToRegister;
	}

	@Override
	protected Tuple2<F, H> doGet() {
		return Tuples.of(this.endpointFactoryBean, this.handler);
	}

	protected void assertHandler() {
		Assert.state(this.handler != null, "'this.handler' must not be null.");
	}

}
