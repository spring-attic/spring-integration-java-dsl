/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.integration.dsl.mail;

import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.springframework.integration.dsl.core.ComponentsRegistration;
import org.springframework.integration.dsl.core.MessageSourceSpec;
import org.springframework.integration.dsl.support.Consumer;
import org.springframework.integration.dsl.support.Function;
import org.springframework.integration.dsl.support.FunctionExpression;
import org.springframework.integration.dsl.support.PropertiesBuilder;
import org.springframework.integration.mail.AbstractMailReceiver;
import org.springframework.integration.mail.MailReceivingMessageSource;

/**
 * A {@link MessageSourceSpec} for a {@link MailReceivingMessageSource}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 */
public abstract class MailInboundChannelAdapterSpec<S extends MailInboundChannelAdapterSpec<S, R>,
		R extends AbstractMailReceiver>
		extends MessageSourceSpec<S, MailReceivingMessageSource>
		implements ComponentsRegistration {

	protected volatile R receiver;

	/**
	 * Configure a SpEL expression to select messages. The root object for the expression
	 * evaluation is a {@link javax.mail.internet.MimeMessage} which should return a boolean
	 * result (true means select the message).
	 * @param selectorExpression the selectorExpression.
	 * @return the spec.
	 */
	public S selectorExpression(String selectorExpression) {
		this.receiver.setSelectorExpression(PARSER.parseExpression(selectorExpression));
		return _this();
	}

	/**
	 * Configure a {@link Function} to select messages. The argument for the function
	 * is a {@link javax.mail.internet.MimeMessage}; {@code apply} returns a boolean
	 * result (true means select the message).
	 * @param selectorFunction the selectorFunction.
	 * @return the spec.
	 */
	public S selector(Function<MimeMessage, Boolean> selectorFunction) {
		this.receiver.setSelectorExpression(new FunctionExpression<MimeMessage>(selectorFunction));
		return _this();
	}

	/**
	 * @param session the session.
	 * @return the spec.
	 * @see AbstractMailReceiver#setSession(Session)
	 */
	public S session(Session session) {
		this.receiver.setSession(session);
		return _this();
	}

	/**
	 * @param javaMailProperties the javaMailProperties.
	 * @return the spec.
	 * @see AbstractMailReceiver#setJavaMailProperties(Properties)
	 */
	public S javaMailProperties(Properties javaMailProperties) {
		this.receiver.setJavaMailProperties(javaMailProperties);
		return _this();
	}

	/**
	 * Configure the javaMailProperties by invoking a {@link Consumer} callback which
	 * is invoked with a {@link PropertiesBuilder}.
	 * @param configurer the configurer.
	 * @return the spec.
	 * @see AbstractMailReceiver#setJavaMailProperties(Properties)
	 */
	public S javaMailProperties(Consumer<PropertiesBuilder> configurer) {
		PropertiesBuilder properties = new PropertiesBuilder();
		configurer.accept(properties);
		return javaMailProperties(properties.get());
	}

	/**
	 * @param javaMailAuthenticator the javaMailAuthenticator.
	 * @return the spec.
	 * @see AbstractMailReceiver#setJavaMailAuthenticator(Authenticator)
	 */
	public S javaMailAuthenticator(Authenticator javaMailAuthenticator) {
		this.receiver.setJavaMailAuthenticator(javaMailAuthenticator);
		return _this();
	}

	/**
	 * @param maxFetchSize the maxFetchSize.
	 * @return the spec.
	 * @see AbstractMailReceiver#setMaxFetchSize(int)
	 */
	public S maxFetchSize(int maxFetchSize) {
		this.receiver.setMaxFetchSize(maxFetchSize);
		return _this();
	}

	/**
	 * @param shouldDeleteMessages the shouldDeleteMessages.
	 * @return the spec.
	 * @see AbstractMailReceiver#setShouldDeleteMessages(boolean)
	 */
	public S shouldDeleteMessages(boolean shouldDeleteMessages) {
		this.receiver.setShouldDeleteMessages(shouldDeleteMessages);
		return _this();
	}

	@Override
	public Collection<Object> getComponentsToRegister() {
		return Collections.<Object>singletonList(this.receiver);
	}

	@Override
	public MailReceivingMessageSource doGet() {
		return new MailReceivingMessageSource(this.receiver);
	}

}
