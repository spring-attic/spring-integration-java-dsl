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

package org.springframework.integration.dsl.mail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.Executor;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.aopalliance.aop.Advice;

import org.springframework.expression.Expression;
import org.springframework.integration.dsl.core.ComponentsRegistration;
import org.springframework.integration.dsl.core.MessageProducerSpec;
import org.springframework.integration.dsl.support.Consumer;
import org.springframework.integration.dsl.support.Function;
import org.springframework.integration.dsl.support.FunctionExpression;
import org.springframework.integration.dsl.support.PropertiesBuilder;
import org.springframework.integration.mail.ImapIdleChannelAdapter;
import org.springframework.integration.mail.ImapMailReceiver;
import org.springframework.integration.mail.SearchTermStrategy;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.transaction.TransactionSynchronizationFactory;
import org.springframework.util.Assert;

/**
 * A {@link MessageProducerSpec} for a {@link ImapIdleChannelAdapter}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 */
public class ImapIdleChannelAdapterSpec
		extends MessageProducerSpec<ImapIdleChannelAdapterSpec, ImapIdleChannelAdapter>
		implements ComponentsRegistration {

	private final ImapMailReceiver receiver;

	protected final boolean externalReceiver;

	private boolean sessionProvided;

	ImapIdleChannelAdapterSpec(ImapMailReceiver receiver) {
		this(receiver, false);
	}

	ImapIdleChannelAdapterSpec(ImapMailReceiver receiver, boolean externalReceiver) {
		super(new ImapIdleChannelAdapter(receiver));
		this.receiver = receiver;
		this.externalReceiver = externalReceiver;
	}

	/**
	 * Configure a SpEL expression to select messages. The root object for the expression
	 * evaluation is a {@link javax.mail.internet.MimeMessage} which should return a boolean
	 * result (true means select the message).
	 * @param selectorExpression the selectorExpression.
	 * @return the spec.
	 */
	public ImapIdleChannelAdapterSpec selectorExpression(String selectorExpression) {
		return selectorExpression(PARSER.parseExpression(selectorExpression));
	}

	/**
	 * Configure an {@link Expression} to select messages. The root object for the expression
	 * evaluation is a {@link javax.mail.internet.MimeMessage} which should return a boolean
	 * result (true means select the message).
	 * @param selectorExpression the selectorExpression.
	 * @return the spec.
	 * @since 1.2
	 */
	public ImapIdleChannelAdapterSpec selectorExpression(Expression selectorExpression) {
		assertReceiver();
		this.receiver.setSelectorExpression(selectorExpression);
		return this;
	}

	private void assertReceiver() {
		Assert.state(!this.externalReceiver, "An external 'receiver' [" + this.receiver + "] can't be modified.");
	}

	/**
	 * Configure a {@link Function} to select messages. The argument for the function
	 * is a {@link javax.mail.internet.MimeMessage}; {@code apply} returns a boolean
	 * result (true means select the message).
	 * @param selectorFunction the selectorFunction.
	 * @return the spec.
	 * @see FunctionExpression
	 */
	public ImapIdleChannelAdapterSpec selector(Function<MimeMessage, Boolean> selectorFunction) {
		return selectorExpression(new FunctionExpression<MimeMessage>(selectorFunction));
	}

	/**
	 * A Java Mail {@link Session} to use.
	 * @param session the session.
	 * @return the spec.
	 * @see ImapMailReceiver#setSession(Session)
	 */
	public ImapIdleChannelAdapterSpec session(Session session) {
		assertReceiver();
		this.receiver.setSession(session);
		this.sessionProvided = true;
		return this;
	}

	/**
	 * @param javaMailProperties the javaMailProperties.
	 * @return the spec.
	 * @see ImapMailReceiver#setJavaMailProperties(Properties)
	 */
	public ImapIdleChannelAdapterSpec javaMailProperties(Properties javaMailProperties) {
		assertReceiver();
		assertSession();
		this.receiver.setJavaMailProperties(javaMailProperties);
		return this;
	}

	private void assertSession() {
		Assert.state(!this.sessionProvided, "Neither 'javaMailProperties' nor 'javaMailAuthenticator' "
				+ "references are allowed when a 'session' reference has been provided.");
	}

	/**
	 * Configure the {@code javaMailProperties} by invoking a {@link Consumer} callback which
	 * is invoked with a {@link PropertiesBuilder}.
	 * @param configurer the configurer.
	 * @return the spec.
	 * @see ImapMailReceiver#setJavaMailProperties(Properties)
	 */
	public ImapIdleChannelAdapterSpec javaMailProperties(Consumer<PropertiesBuilder> configurer) {
		PropertiesBuilder properties = new PropertiesBuilder();
		configurer.accept(properties);
		return javaMailProperties(properties.get());
	}

	/**
	 * @param javaMailAuthenticator the javaMailAuthenticator.
	 * @return the spec.
	 * @see ImapMailReceiver#setJavaMailAuthenticator(Authenticator)
	 */
	public ImapIdleChannelAdapterSpec javaMailAuthenticator(Authenticator javaMailAuthenticator) {
		assertReceiver();
		assertSession();
		this.receiver.setJavaMailAuthenticator(javaMailAuthenticator);
		return this;
	}

	/**
	 * @param maxFetchSize the maxFetchSize.
	 * @return the spec.
	 * @see ImapMailReceiver#setMaxFetchSize(int)
	 */
	public ImapIdleChannelAdapterSpec maxFetchSize(int maxFetchSize) {
		assertReceiver();
		this.receiver.setMaxFetchSize(maxFetchSize);
		return this;
	}

	/**
	 * @param shouldDeleteMessages the shouldDeleteMessages.
	 * @return the spec.
	 * @see ImapMailReceiver#setShouldDeleteMessages(boolean)
	 */
	public ImapIdleChannelAdapterSpec shouldDeleteMessages(boolean shouldDeleteMessages) {
		assertReceiver();
		this.receiver.setShouldDeleteMessages(shouldDeleteMessages);
		return this;
	}

	/**
	 * @param searchTermStrategy the searchTermStrategy.
	 * @return the spec.
	 * @see ImapMailReceiver#setSearchTermStrategy(SearchTermStrategy)
	 */
	public ImapIdleChannelAdapterSpec searchTermStrategy(SearchTermStrategy searchTermStrategy) {
		assertReceiver();
		this.receiver.setSearchTermStrategy(searchTermStrategy);
		return this;
	}

	/**
	 * @param shouldMarkMessagesAsRead the shouldMarkMessagesAsRead.
	 * @return the spec.
	 * @see ImapMailReceiver#setShouldMarkMessagesAsRead(Boolean)
	 */
	public ImapIdleChannelAdapterSpec shouldMarkMessagesAsRead(boolean shouldMarkMessagesAsRead) {
		assertReceiver();
		this.receiver.setShouldMarkMessagesAsRead(shouldMarkMessagesAsRead);
		return this;
	}

	/**
	 * Set the name of the flag to use to flag messages when the server does
	 * not support \Recent but supports user flags;
	 * default {@value ImapMailReceiver#DEFAULT_SI_USER_FLAG}.
	 * @param userFlag the flag.
	 * @return the spec.
	 * @since 1.2
	 * @see ImapMailReceiver#setUserFlag(String)
	 */
	public ImapIdleChannelAdapterSpec userFlag(String userFlag) {
		assertReceiver();
		this.receiver.setUserFlag(userFlag);
		return _this();
	}

	/**
	 * Set the header mapper; if a header mapper is not provided, the message payload is
	 * a {@link MimeMessage}, when provided, the headers are mapped and the payload is
	 * the {@link MimeMessage} content.
	 * @param headerMapper the header mapper.
	 * @return the spec.
	 * @since 1.2
	 * @see ImapMailReceiver#setUserFlag(String)
	 * @see #embeddedPartsAsBytes(boolean)
	 */
	public ImapIdleChannelAdapterSpec headerMapper(HeaderMapper<MimeMessage> headerMapper) {
		assertReceiver();
		this.receiver.setHeaderMapper(headerMapper);
		return _this();
	}

	/**
	 * When a header mapper is provided determine whether an embedded {@link Part} (e.g
	 * {@link Message} or {@link javax.mail.Multipart} content is rendered as a byte[] in the
	 * payload. Otherwise, leave as a {@link Part}. These objects are not suitable for
	 * downstream serialization. Default: true.
	 * <p>This has no effect if there is no header mapper, in that case the payload is the
	 * {@link MimeMessage}.
	 * @param embeddedPartsAsBytes the embeddedPartsAsBytes to set.
	 * @return the spec.
	 * @since 1.2
	 * @see #headerMapper(HeaderMapper)
	 */
	public ImapIdleChannelAdapterSpec embeddedPartsAsBytes(boolean embeddedPartsAsBytes) {
		assertReceiver();
		this.receiver.setEmbeddedPartsAsBytes(embeddedPartsAsBytes);
		return _this();
	}


	/**
	 * Configure a {@link TransactionSynchronizationFactory}. Usually used to synchronize message
	 * deletion with some external transaction manager.
	 * @param transactionSynchronizationFactory the transactionSynchronizationFactory.
	 * @return the spec.
	 */
	public ImapIdleChannelAdapterSpec transactionSynchronizationFactory(
			TransactionSynchronizationFactory transactionSynchronizationFactory) {
		this.target.setTransactionSynchronizationFactory(transactionSynchronizationFactory);
		return this;
	}

	/**
	 * Configure a chain of {@link Advice} objects for message delivery.
	 * @param adviceChain the advice chain.
	 * @return the spec.
	 */
	public ImapIdleChannelAdapterSpec adviceChain(Advice... adviceChain) {
		this.target.setAdviceChain(Arrays.asList(adviceChain));
		return this;
	}

	/**
	 * @param sendingTaskExecutor the sendingTaskExecutor.
	 * @return the spec.
	 * @see ImapIdleChannelAdapter#setSendingTaskExecutor(Executor)
	 */
	public ImapIdleChannelAdapterSpec sendingTaskExecutor(Executor sendingTaskExecutor) {
		this.target.setSendingTaskExecutor(sendingTaskExecutor);
		return this;
	}

	/**
	 * @param shouldReconnectAutomatically the shouldReconnectAutomatically.
	 * @return the spec.
	 * @see ImapIdleChannelAdapter#setShouldReconnectAutomatically(boolean)
	 */
	public ImapIdleChannelAdapterSpec shouldReconnectAutomatically(boolean shouldReconnectAutomatically) {
		this.target.setShouldReconnectAutomatically(shouldReconnectAutomatically);
		return this;
	}

	@Override
	public Collection<Object> getComponentsToRegister() {
		return Collections.<Object>singletonList(this.receiver);
	}

}
