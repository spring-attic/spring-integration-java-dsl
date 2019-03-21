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

package org.springframework.integration.dsl.jms;

import javax.jms.Destination;

import org.springframework.integration.dsl.core.MessageProducerSpec;
import org.springframework.integration.dsl.support.Consumer;
import org.springframework.integration.jms.ChannelPublishingJmsMessageListener;
import org.springframework.integration.jms.JmsHeaderMapper;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.util.Assert;

/**
 * A {@link MessageProducerSpec} for {@link JmsMessageDrivenChannelAdapter}s.
 *
 * @param <S> the target {@link JmsMessageDrivenChannelAdapterSpec} implementation type.
 *
 * @author Artem Bilan
 */
public class JmsMessageDrivenChannelAdapterSpec<S extends JmsMessageDrivenChannelAdapterSpec<S>>
		extends MessageProducerSpec<S, JmsMessageDrivenChannelAdapter> {

	JmsMessageDrivenChannelAdapterSpec(AbstractMessageListenerContainer listenerContainer) {
		super(new JmsMessageDrivenChannelAdapter(listenerContainer, new ChannelPublishingJmsMessageListener()));
		this.target.getListener().setExpectReply(false);
	}

	/**
	 * @param messageConverter the messageConverter.
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setMessageConverter(MessageConverter)
	 */
	public S jmsMessageConverter(MessageConverter messageConverter) {
		this.target.getListener().setMessageConverter(messageConverter);
		return _this();
	}

	/**
	 * @param headerMapper the headerMapper.
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setHeaderMapper(JmsHeaderMapper)
	 * @deprecated in favor of {@link #headerMapper(JmsHeaderMapper)}
	 */
	@Deprecated
	public S setHeaderMapper(JmsHeaderMapper headerMapper) {
		return headerMapper(headerMapper);
	}

	/**
	 * @param headerMapper the headerMapper.
	 * @return the spec.
	 * @since 1.2
	 * @see ChannelPublishingJmsMessageListener#setHeaderMapper(JmsHeaderMapper)
	 */
	public S headerMapper(JmsHeaderMapper headerMapper) {
		this.target.getListener().setHeaderMapper(headerMapper);
		return _this();
	}

	/**
	 * @param extractRequestPayload the extractRequestPayload.
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setExtractRequestPayload(boolean)
	 */
	public S extractPayload(boolean extractRequestPayload) {
		this.target.getListener().setExtractRequestPayload(extractRequestPayload);
		return _this();
	}

	/**
	 *
	 * @param <S> the target {@link JmsListenerContainerSpec} implementation type.
	 * @param <C> the target {@link AbstractMessageListenerContainer} implementation type.
	 */
	public static class
			JmsMessageDrivenChannelAdapterListenerContainerSpec<S extends JmsListenerContainerSpec<S, C>, C extends AbstractMessageListenerContainer>
			extends JmsMessageDrivenChannelAdapterSpec<JmsMessageDrivenChannelAdapterListenerContainerSpec<S, C>> {

		private final JmsListenerContainerSpec<S, C> spec;

		JmsMessageDrivenChannelAdapterListenerContainerSpec(JmsListenerContainerSpec<S, C> spec) {
			super(spec.get());
			this.spec = spec;
			this.spec.get().setAutoStartup(false);
		}

		/**
		 * @param destination the destination.
		 * @return the spec.
		 * @see JmsListenerContainerSpec#destination(Destination)
		 */
		public JmsMessageDrivenChannelAdapterListenerContainerSpec<S, C> destination(Destination destination) {
			this.spec.destination(destination);
			return _this();
		}

		/**
		 * Specify a destination name to use.
		 * @param destinationName the destinationName.
		 * @return the spec.
		 * @see JmsListenerContainerSpec#destination(String)
		 */
		public JmsMessageDrivenChannelAdapterListenerContainerSpec<S, C> destination(String destinationName) {
			this.spec.destination(destinationName);
			return _this();
		}

		/**
		 * Configure a listener container by invoking the {@link Consumer} callback, with a
		 * {@link JmsListenerContainerSpec} argument.
		 * @param configurer the configurer.
		 * @return the spec.
		 */
		public JmsMessageDrivenChannelAdapterListenerContainerSpec<S, C> configureListenerContainer(
				Consumer<JmsListenerContainerSpec<S, C>> configurer) {
			Assert.notNull(configurer, "'configurer' must not be null");
			configurer.accept(this.spec);
			return _this();
		}

	}

}
