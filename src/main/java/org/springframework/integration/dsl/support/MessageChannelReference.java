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

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * An "artificial" {@link MessageChannel} implementation which will be unwrapped to the
 * {@link MessageChannel} bean on the bean registration phase.
 * For internal use only.
 *
 * @author Artem Bilan
 * @see org.springframework.integration.dsl.config.IntegrationFlowBeanPostProcessor
 */
public class MessageChannelReference implements MessageChannel {

	private final String name;

	public MessageChannelReference(String name) {
		Assert.hasText(name, "'name' must not be empty");
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public boolean send(Message<?> message) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean send(Message<?> message, long timeout) {
		throw new UnsupportedOperationException();
	}

}
