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

package org.springframework.integration.dsl;

import javax.jms.ConnectionFactory;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.expression.Expression;
import org.springframework.integration.dsl.amqp.Amqp;
import org.springframework.integration.dsl.amqp.AmqpBaseInboundGatewaySpec;
import org.springframework.integration.dsl.amqp.AmqpInboundGatewaySpec;
import org.springframework.integration.dsl.http.Http;
import org.springframework.integration.dsl.http.HttpControllerEndpointSpec;
import org.springframework.integration.dsl.http.HttpRequestHandlerEndpointSpec;
import org.springframework.integration.dsl.jms.Jms;
import org.springframework.integration.dsl.jms.JmsDefaultListenerContainerSpec;
import org.springframework.integration.dsl.jms.JmsInboundGatewaySpec;
import org.springframework.integration.dsl.jms.JmsListenerContainerSpec;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

/**
 * @author Artem Bilan
 */
public class MessagingGateways {

	public AmqpInboundGatewaySpec amqp(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory,
			String... queueNames) {
		return Amqp.inboundGateway(connectionFactory, queueNames);
	}

	public AmqpInboundGatewaySpec amqp(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory,
			Queue... queues) {
		return Amqp.inboundGateway(connectionFactory, queues);
	}

	public AmqpBaseInboundGatewaySpec<?> amqp(SimpleMessageListenerContainer listenerContainer) {
		return Amqp.inboundGateway(listenerContainer);
	}

	public AmqpInboundGatewaySpec amqp(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory,
	                                   AmqpTemplate amqpTemplate, String... queueNames) {
		return Amqp.inboundGateway(connectionFactory, amqpTemplate, queueNames);
	}

	public AmqpInboundGatewaySpec amqp(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory,
	                                   AmqpTemplate amqpTemplate, Queue... queues) {
		return Amqp.inboundGateway(connectionFactory, amqpTemplate, queues);
	}

	public AmqpBaseInboundGatewaySpec<?> amqp(SimpleMessageListenerContainer listenerContainer,
	                                          AmqpTemplate amqpTemplate) {
		return Amqp.inboundGateway(listenerContainer, amqpTemplate);
	}

	public JmsInboundGatewaySpec.JmsInboundGatewayListenerContainerSpec<JmsDefaultListenerContainerSpec, DefaultMessageListenerContainer> jms(
			javax.jms.ConnectionFactory connectionFactory) {
		return Jms.inboundGateway(connectionFactory);
	}

	public <S extends JmsListenerContainerSpec<S, C>, C extends AbstractMessageListenerContainer>
	JmsInboundGatewaySpec.JmsInboundGatewayListenerContainerSpec<S, C> jms(ConnectionFactory connectionFactory,
			Class<C> containerClass) {
		return Jms.inboundGateway(connectionFactory, containerClass);
	}

	public JmsInboundGatewaySpec<? extends JmsInboundGatewaySpec<?>> jms(
			AbstractMessageListenerContainer listenerContainer) {
		return Jms.inboundGateway(listenerContainer);
	}

	public HttpControllerEndpointSpec http(String viewName, String... path) {
		return Http.inboundControllerGateway(viewName, path);
	}

	public HttpControllerEndpointSpec http(Expression viewExpression, String... path) {
		return Http.inboundControllerGateway(viewExpression, path);
	}

	public HttpRequestHandlerEndpointSpec httpGateway(String... path) {
		return Http.inboundGateway(path);
	}

	MessagingGateways() {
	}

}
