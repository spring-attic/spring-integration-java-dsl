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

import org.springframework.beans.factory.config.AbstractFactoryBean;
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
 * {@link PlatformTransactionManager} is not provided, we look for a bean with name
 * "transactionManager".
 * @author Gary Russell
 * @since 1.2
 *
 */
public class TransactionInterceptorFactoryBean extends AbstractFactoryBean<TransactionInterceptor> {

	private Propagation propagation = Propagation.REQUIRED;

	private Isolation isolation = Isolation.DEFAULT;

	private int timeout = -1;

	private boolean readOnly = false;

	private TransactionAttribute transactionAttribute;

	private PlatformTransactionManager transactionManager;

	public TransactionInterceptorFactoryBean propagation(Propagation propagation) {
		this.propagation = propagation;
		return this;
	}

	public TransactionInterceptorFactoryBean isolation(Isolation isolation) {
		this.isolation = isolation;
		return this;
	}

	public TransactionInterceptorFactoryBean timeout(int timeout) {
		this.timeout = timeout;
		return this;
	}

	public TransactionInterceptorFactoryBean readOnly(boolean readOnly) {
		this.readOnly = readOnly;
		return this;
	}

	public TransactionInterceptorFactoryBean transactionAttribute(TransactionAttribute transactionAttribute) {
		this.transactionAttribute = transactionAttribute;
		return this;
	}

	public TransactionInterceptorFactoryBean transactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
		return this;
	}

	@Override
	protected TransactionInterceptor createInstance() throws Exception {
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
		if (this.transactionManager == null) {
			this.transactionManager = this.getBeanFactory().getBean("transactionManager",
					PlatformTransactionManager.class);
		}
		return new TransactionInterceptor(this.transactionManager, attSource);
	}

	@Override
	public Class<?> getObjectType() {
		return TransactionInterceptor.class;
	}

}
