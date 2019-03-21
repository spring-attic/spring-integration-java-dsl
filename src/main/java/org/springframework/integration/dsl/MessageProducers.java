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

package org.springframework.integration.dsl;

import java.io.File;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.expression.Expression;
import org.springframework.integration.dsl.amqp.Amqp;
import org.springframework.integration.dsl.amqp.AmqpBaseInboundChannelAdapterSpec;
import org.springframework.integration.dsl.amqp.AmqpInboundChannelAdapterSpec;
import org.springframework.integration.dsl.file.Files;
import org.springframework.integration.dsl.file.TailAdapterSpec;
import org.springframework.integration.dsl.http.Http;
import org.springframework.integration.dsl.http.HttpControllerEndpointSpec;
import org.springframework.integration.dsl.http.HttpRequestHandlerEndpointSpec;
import org.springframework.integration.dsl.jms.Jms;
import org.springframework.integration.dsl.jms.JmsListenerContainerSpec;
import org.springframework.integration.dsl.jms.JmsMessageDrivenChannelAdapterSpec;
import org.springframework.integration.dsl.mail.ImapIdleChannelAdapterSpec;
import org.springframework.integration.dsl.mail.Mail;
import org.springframework.jms.listener.AbstractMessageListenerContainer;

/**
 * @author Artem Bilan
 */
public class MessageProducers {

	public AmqpInboundChannelAdapterSpec amqp(
			org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory, String... queueNames) {
		return Amqp.inboundAdapter(connectionFactory, queueNames);
	}

	public AmqpInboundChannelAdapterSpec amqp(
			org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory, Queue... queues) {
		return Amqp.inboundAdapter(connectionFactory, queues);
	}

	/**
	 * @deprecated {@code since 1.1.1} in favor of {@link #amqp(SimpleMessageListenerContainer)}
	 * as factory method with an inconvenient name.
	 * @param listenerContainer the {@link SimpleMessageListenerContainer} to use.
	 * @return the {@link AmqpBaseInboundChannelAdapterSpec} instance.
	 */
	@Deprecated
	public AmqpBaseInboundChannelAdapterSpec<?> inboundAdapter(SimpleMessageListenerContainer listenerContainer) {
		return amqp(listenerContainer);
	}

	public AmqpBaseInboundChannelAdapterSpec<?> amqp(SimpleMessageListenerContainer listenerContainer) {
		return Amqp.inboundAdapter(listenerContainer);
	}

	public TailAdapterSpec tail(File file) {
		return Files.tailAdapter(file);
	}

	public ImapIdleChannelAdapterSpec imap(String url) {
		return Mail.imapIdleAdapter(url);
	}

	public JmsMessageDrivenChannelAdapterSpec<? extends JmsMessageDrivenChannelAdapterSpec<?>> jms(
			AbstractMessageListenerContainer listenerContainer) {
		return Jms.messageDrivenChannelAdapter(listenerContainer);
	}

	public JmsMessageDrivenChannelAdapterSpec<? extends JmsMessageDrivenChannelAdapterSpec<?>> jms(
			javax.jms.ConnectionFactory connectionFactory) {
		return Jms.messageDrivenChannelAdapter(connectionFactory);
	}

	public <S extends JmsListenerContainerSpec<S, C>, C extends AbstractMessageListenerContainer>
	JmsMessageDrivenChannelAdapterSpec<? extends JmsMessageDrivenChannelAdapterSpec<?>> jms(
			javax.jms.ConnectionFactory connectionFactory,
			Class<C> containerClass) {
		return Jms.<S, C>messageDrivenChannelAdapter(connectionFactory, containerClass);
	}

	public HttpControllerEndpointSpec http(String viewName, String... path) {
		return Http.inboundControllerAdapter(viewName, path);
	}

	public HttpControllerEndpointSpec http(Expression viewExpression, String... path) {
		return Http.inboundControllerAdapter(viewExpression, path);
	}

	public HttpRequestHandlerEndpointSpec httpChannelAdapter(String... path) {
		return Http.inboundChannelAdapter(path);
	}

	MessageProducers() {
	}

}
