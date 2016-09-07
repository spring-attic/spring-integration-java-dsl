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

package org.springframework.integration.dsl.jpa;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.integration.dsl.core.ComponentsRegistration;
import org.springframework.integration.dsl.core.MessageHandlerSpec;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.outbound.JpaOutboundGateway;
import org.springframework.integration.jpa.outbound.JpaOutboundGatewayFactoryBean;
import org.springframework.integration.jpa.support.JpaParameter;
import org.springframework.integration.jpa.support.parametersource.ParameterSourceFactory;

/**
 * The base {@link MessageHandlerSpec} for JPA Outbound endpoints.
 *
 * @author Artem Bilan
 * @since 1.2
 */
public abstract class JpaBaseOutboundEndpointSpec<S extends JpaBaseOutboundEndpointSpec<S>>
		extends MessageHandlerSpec<S, JpaOutboundGateway>
		implements ComponentsRegistration {

	protected JpaOutboundGatewayFactoryBean jpaOutboundGatewayFactoryBean = new JpaOutboundGatewayFactoryBean();

	protected final JpaExecutor jpaExecutor;

	private List<JpaParameter> jpaParameters = new LinkedList<JpaParameter>();

	protected JpaBaseOutboundEndpointSpec(JpaExecutor jpaExecutor) {
		this.jpaExecutor = jpaExecutor;
		this.jpaOutboundGatewayFactoryBean.setJpaExecutor(this.jpaExecutor);
	}

	public S entityClass(Class<?> entityClass) {
		this.jpaExecutor.setEntityClass(entityClass);
		return _this();
	}

	public S jpaQuery(String jpaQuery) {
		this.jpaExecutor.setJpaQuery(jpaQuery);
		return _this();
	}

	public S nativeQuery(String nativeQuery) {
		this.jpaExecutor.setNativeQuery(nativeQuery);
		return _this();
	}

	public S namedQuery(String namedQuery) {
		this.jpaExecutor.setNamedQuery(namedQuery);
		return _this();
	}

	public S parameterSourceFactory(ParameterSourceFactory parameterSourceFactory) {
		this.jpaExecutor.setParameterSourceFactory(parameterSourceFactory);
		return _this();
	}

	public S parameter(Object value) {
		return parameter(new JpaParameter(value, null));
	}

	public S parameter(String name, Object value) {
		return parameter(new JpaParameter(name, value, null));
	}

	public S parameterExpression(String expression) {
		return parameter(new JpaParameter(null, expression));
	}

	public S parameterExpression(String name, String expression) {
		return parameter(new JpaParameter(name, null, expression));
	}


	public S parameter(JpaParameter jpaParameter) {
		this.jpaParameters.add(jpaParameter);
		return _this();
	}

	public S usePayloadAsParameterSource(Boolean usePayloadAsParameterSource) {
		this.jpaExecutor.setUsePayloadAsParameterSource(usePayloadAsParameterSource);
		return _this();
	}

	@Override
	public Collection<Object> getComponentsToRegister() {
		return Collections.<Object>singletonList(this.jpaExecutor);
	}

	@Override
	protected JpaOutboundGateway doGet() {
		if (!this.jpaParameters.isEmpty()) {
			this.jpaExecutor.setJpaParameters(this.jpaParameters);
		}

		/*
		We need this artificial BeanFactory to overcome JpaOutboundGatewayFactoryBean initialization.
		The real BeanFactory will be applied later for the target JpaOutboundGateway instance.
		*/
		this.jpaOutboundGatewayFactoryBean.setBeanFactory(new DefaultListableBeanFactory());
		try {
			this.jpaOutboundGatewayFactoryBean.afterPropertiesSet();
			return (JpaOutboundGateway) this.jpaOutboundGatewayFactoryBean.getObject();
		}
		catch (Exception e) {
			throw new BeanCreationException("Cannot create the JpaOutboundGateway", e);
		}
	}

}
