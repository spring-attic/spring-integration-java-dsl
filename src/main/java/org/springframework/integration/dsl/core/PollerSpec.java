/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.dsl.core;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;

import org.aopalliance.aop.Advice;

import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.transaction.TransactionSynchronizationFactory;
import org.springframework.scheduling.Trigger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.ErrorHandler;

/**
 * An {@link IntegrationComponentSpec} for {@link PollerMetadata}s.
 *
 * @author Artem Bilan
 */
public final class PollerSpec extends IntegrationComponentSpec<PollerSpec, PollerMetadata> {

	private final PollerMetadata pollerMetadata = new PollerMetadata();

	private final List<Advice> adviceChain = new LinkedList<Advice>();

	PollerSpec(Trigger trigger) {
		this.pollerMetadata.setTrigger(trigger);
	}

	/**
	 * Specify the {@link TransactionSynchronizationFactory} to attach a
	 * {@link org.springframework.transaction.support.TransactionSynchronization}
	 * to the transaction around {@code poll} operation.
	 * @param transactionSynchronizationFactory the TransactionSynchronizationFactory to use.
	 * @return the spec.
	 */
	public PollerSpec transactionSynchronizationFactory(
			TransactionSynchronizationFactory transactionSynchronizationFactory) {
		this.pollerMetadata.setTransactionSynchronizationFactory(transactionSynchronizationFactory);
		return this;
	}

	/**
	 * Specify the {@link ErrorHandler} to wrap a {@code taskExecutor}
	 * to the {@link org.springframework.integration.util.ErrorHandlingTaskExecutor}.
	 * @param errorHandler the {@link ErrorHandler} to use.
	 * @return the spec.
	 * @see #taskExecutor
	 */
	public PollerSpec errorHandler(ErrorHandler errorHandler) {
		this.pollerMetadata.setErrorHandler(errorHandler);
		return this;
	}

	/**
	 * @param maxMessagesPerPoll the maxMessagesPerPoll to set.
	 * @return the spec.
	 * @see PollerMetadata#setMaxMessagesPerPoll
	 */
	public PollerSpec maxMessagesPerPoll(long maxMessagesPerPoll) {
		this.pollerMetadata.setMaxMessagesPerPoll(maxMessagesPerPoll);
		return this;
	}

	/**
	 * Specify a timeout in milliseconds to wait for a message in the
	 * {@link org.springframework.messaging.MessageChannel}.
	 * Defaults to {@code 1000}.
	 * @param receiveTimeout the timeout to use.
	 * @return the spec.
	 * @see org.springframework.messaging.PollableChannel#receive(long)
	 */
	public PollerSpec receiveTimeout(long receiveTimeout) {
		this.pollerMetadata.setReceiveTimeout(receiveTimeout);
		return this;
	}

	/**
	 * Specify AOP {@link Advice}s for the {@code pollingTask}.
	 * @param advice the {@link Advice}s to use.
	 * @return the spec.
	 */
	public PollerSpec advice(Advice... advice) {
		this.adviceChain.addAll(Arrays.asList(advice));
		return this;
	}

	/**
	 * Specify a {@link TransactionInterceptor} {@link Advice} with the
	 * provided {@code PlatformTransactionManager} for the {@code pollingTask}.
	 * @param transactionManager the {@link PlatformTransactionManager} to use.
	 * @return the spec.
	 */
	public PollerSpec transactional(PlatformTransactionManager transactionManager) {
		return this.advice(new TransactionInterceptor(transactionManager, new MatchAlwaysTransactionAttributeSource()));
	}

	/**
	 * Specify an {@link Executor} to perform the {@code pollingTask}.
	 * @param taskExecutor the {@link Executor} to use.
	 * @return the spec.
	 */
	public PollerSpec taskExecutor(Executor taskExecutor) {
		this.pollerMetadata.setTaskExecutor(taskExecutor);
		return this;
	}

	public PollerSpec sendTimeout(long sendTimeout) {
		this.pollerMetadata.setSendTimeout(sendTimeout);
		return this;
	}

	@Override
	protected PollerMetadata doGet() {
		this.pollerMetadata.setAdviceChain(this.adviceChain);
		return this.pollerMetadata;
	}

}
