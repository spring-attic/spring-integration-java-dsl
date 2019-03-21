/*
 * Copyright 2014-2017 the original author or authors.
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

package org.springframework.integration.dsl.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.MethodInvokingMessageProcessor;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * An "artificial" {@link MessageProcessor} for lazy-load of target bean by its name.
 * For internal use only.
 *
 * @param <T> the expected {@link #processMessage} result type.
 *
 * @author Artem Bilan
 */
public class BeanNameMessageProcessor<T> implements MessageProcessor<T>, BeanFactoryAware {

	private final String beanName;

	private final String methodName;

	private MessageProcessor<T> delegate;

	private BeanFactory beanFactory;

	public BeanNameMessageProcessor(String object, String methodName) {
		this.beanName = object;
		this.methodName = methodName;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.notNull(beanFactory, "'beanFactory' must not be null");
		this.beanFactory = beanFactory;
	}

	@Override
	public T processMessage(Message<?> message) {
		if (this.delegate == null) {
			Object target = this.beanFactory.getBean(this.beanName);
			this.delegate = new MethodInvokingMessageProcessor<T>(target, this.methodName);
		}
		return this.delegate.processMessage(message);
	}

}
