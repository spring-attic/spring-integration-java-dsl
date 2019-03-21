/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.dsl.core;

import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.messaging.MessageChannel;

/**
 * An {@link IntegrationComponentSpec} for
 * {@link org.springframework.integration.core.MessageProducer}s.
 *
 * @author Artem Bilan
 */
public abstract class MessageProducerSpec<S extends MessageProducerSpec<S, P>, P extends MessageProducerSupport>
		extends IntegrationComponentSpec<S, P> {

	public MessageProducerSpec(P producer) {
		this.target = producer;
	}

	/**
	 * {@inheritDoc}
	 * Configure the message producer's bean name.
	 */
	@Override
	public S id(String id) {
		this.target.setBeanName(id);
		return super.id(id);
	}

	/**
	 * @param phase the phase.
	 * @return the spec.
	 * @see org.springframework.context.SmartLifecycle
	 */
	public S phase(int phase) {
		this.target.setPhase(phase);
		return _this();
	}

	/**
	 * @param autoStartup the autoStartup.
	 * @return the spec.
	 * @see org.springframework.context.SmartLifecycle
	 */
	public S autoStartup(boolean autoStartup) {
		this.target.setAutoStartup(autoStartup);
		return _this();
	}

	/**
	 * Specify the {@code outputChannel} for the
	 * {@link org.springframework.integration.core.MessageProducer}
	 * @param outputChannel the outputChannel.
	 * @return the spec.
	 * @see MessageProducerSupport#setOutputChannel(MessageChannel)
	 */
	public S outputChannel(MessageChannel outputChannel) {
		target.setOutputChannel(outputChannel);
		return _this();
	}

	/**
	 * Configure the {@link MessageChannel} to which error messages will be sent.
	 * @param errorChannel the errorChannel.
	 * @return the spec.
	 */
	public S errorChannel(MessageChannel errorChannel) {
		target.setErrorChannel(errorChannel);
		return _this();
	}

	@Override
	protected P doGet() {
		throw new UnsupportedOperationException();
	}

}
