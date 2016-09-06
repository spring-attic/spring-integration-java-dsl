/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.dsl.transaction;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * Provides a fluent API to build a transaction interceptor. See
 * {@link TransactionAttribute} for property meanings; if a {@link TransactionAttribute}
 * is provided, the individual properties are ignored. If a
 * {@link PlatformTransactionManager} is not provided, a single instance of
 * {@link PlatformTransactionManager} will be discovered at runtime; if you have more
 * than one transaction manager, you must inject the one you want to use here.
 * @author Gary Russell
 * @since 1.2
 *
 */
public class TransactionInterceptorBuilder {

	private Propagation propagation = Propagation.REQUIRED;

	private Isolation isolation = Isolation.DEFAULT;

	private int timeout = -1;

	private boolean readOnly = false;

	private TransactionAttribute transactionAttribute;

	private PlatformTransactionManager transactionManager;

	public TransactionInterceptorBuilder propagation(Propagation propagation) {
		this.propagation = propagation;
		return this;
	}

	public TransactionInterceptorBuilder isolation(Isolation isolation) {
		this.isolation = isolation;
		return this;
	}

	public TransactionInterceptorBuilder timeout(int timeout) {
		this.timeout = timeout;
		return this;
	}

	public TransactionInterceptorBuilder readOnly(boolean readOnly) {
		this.readOnly = readOnly;
		return this;
	}

	public TransactionInterceptorBuilder transactionAttribute(TransactionAttribute transactionAttribute) {
		this.transactionAttribute = transactionAttribute;
		return this;
	}

	public TransactionInterceptorBuilder transactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
		return this;
	}

	public TransactionInterceptor build() {
		MatchAlwaysTransactionAttributeSource attSource = new MatchAlwaysTransactionAttributeSource();
		if (this.transactionAttribute == null) {
			DefaultTransactionAttribute transactionAttribute = new DefaultTransactionAttribute();
			transactionAttribute.setPropagationBehavior(this.propagation.value());
			transactionAttribute.setIsolationLevel(this.isolation.value());
			transactionAttribute.setTimeout(this.timeout);
			transactionAttribute.setReadOnly(this.readOnly);
			attSource.setTransactionAttribute(transactionAttribute);
		}
		else {
			attSource.setTransactionAttribute(this.transactionAttribute);
		}
		return new TransactionInterceptor(this.transactionManager, attSource);
	}

}
