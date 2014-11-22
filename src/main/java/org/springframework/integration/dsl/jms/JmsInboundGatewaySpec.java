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

package org.springframework.integration.dsl.jms;

import javax.jms.Destination;

import org.springframework.integration.dsl.core.MessagingGatewaySpec;
import org.springframework.integration.dsl.support.Consumer;
import org.springframework.integration.jms.ChannelPublishingJmsMessageListener;
import org.springframework.integration.jms.JmsHeaderMapper;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.util.Assert;

/**
 * A {@link MessagingGatewaySpec} for a {@link JmsInboundGateway}.
 *
 * @author Artem Bilan
 */
public class JmsInboundGatewaySpec<S extends JmsInboundGatewaySpec<S>>
		extends MessagingGatewaySpec<S, JmsInboundGateway> {

	JmsInboundGatewaySpec(AbstractMessageListenerContainer listenerContainer) {
		super(new JmsInboundGateway(listenerContainer, new ChannelPublishingJmsMessageListener()));
		this.target.getListener().setExpectReply(true);
	}

	/**
	 * @param defaultReplyDestination the defaultReplyDestination
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setDefaultReplyDestination(Destination)
	 */
	public S defaultReplyDestination(Destination defaultReplyDestination) {
		this.target.getListener().setDefaultReplyDestination(defaultReplyDestination);
		return _this();
	}

	/**
	 * @param destinationName the destinationName
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setDefaultReplyQueueName(String)
	 */
	public S defaultReplyQueueName(String destinationName) {
		this.target.getListener().setDefaultReplyQueueName(destinationName);
		return _this();
	}

	/**
	 * @param destinationName the destinationName
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setDefaultReplyTopicName(String)
	 */
	public S defaultReplyTopicName(String destinationName) {
		this.target.getListener().setDefaultReplyTopicName(destinationName);
		return _this();
	}

	/**
	 * @param replyTimeToLive the replyTimeToLive
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setReplyTimeToLive(long)
	 */
	public S replyTimeToLive(long replyTimeToLive) {
		this.target.getListener().setReplyTimeToLive(replyTimeToLive);
		return _this();
	}

	/**
	 * @param replyPriority the replyPriority
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setReplyPriority(int)
	 */
	public S replyPriority(int replyPriority) {
		this.target.getListener().setReplyPriority(replyPriority);
		return _this();
	}

	/**
	 * @param replyDeliveryPersistent the replyDeliveryPersistent
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setReplyDeliveryPersistent(boolean)
	 */
	public S replyDeliveryPersistent(boolean replyDeliveryPersistent) {
		this.target.getListener().setReplyDeliveryPersistent(replyDeliveryPersistent);
		return _this();
	}

	/**
	 * @param correlationKey the correlationKey
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setCorrelationKey(String)
	 */
	public S correlationKey(String correlationKey) {
		this.target.getListener().setCorrelationKey(correlationKey);
		return _this();
	}

	/**
	 * @param explicitQosEnabledForReplies the explicitQosEnabledForReplies.
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setExplicitQosEnabledForReplies(boolean)
	 */
	public S explicitQosEnabledForReplies(boolean explicitQosEnabledForReplies) {
		this.target.getListener().setExplicitQosEnabledForReplies(explicitQosEnabledForReplies);
		return _this();
	}

	/**
	 * @param destinationResolver the destinationResolver.
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setDestinationResolver(DestinationResolver)
	 */
	public S destinationResolver(DestinationResolver destinationResolver) {
		this.target.getListener().setDestinationResolver(destinationResolver);
		return _this();
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
	 */
	public S setHeaderMapper(JmsHeaderMapper headerMapper) {
		this.target.getListener().setHeaderMapper(headerMapper);
		return _this();
	}

	/**
	 * @param extractRequestPayload the extractRequestPayload.
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setExtractRequestPayload(boolean)
	 */
	public S extractRequestPayload(boolean extractRequestPayload) {
		this.target.getListener().setExtractRequestPayload(extractRequestPayload);
		return _this();
	}

	/**
	 * @param extractReplyPayload the extractReplyPayload.
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setExtractReplyPayload(boolean)
	 */
	public S extractReplyPayload(boolean extractReplyPayload) {
		this.target.getListener().setExtractReplyPayload(extractReplyPayload);
		return _this();
	}

	public static class JmsInboundGatewayListenerContainerSpec<C extends AbstractMessageListenerContainer>
			extends JmsInboundGatewaySpec<JmsInboundGatewayListenerContainerSpec<C>> {

		private final JmsListenerContainerSpec<C> spec;

		JmsInboundGatewayListenerContainerSpec(JmsListenerContainerSpec<C> spec) {
			super(spec.get());
			this.spec = spec;
			this.spec.get().setAutoStartup(false);
		}

		/**
		 * @param destination the destination
		 * @return the spec.
		 * @see JmsListenerContainerSpec#destination(Destination)
		 */
		public JmsInboundGatewayListenerContainerSpec<C> destination(Destination destination) {
			spec.destination(destination);
			return _this();
		}

		/**
		 * @param destinationName the destinationName
		 * @return the spec.
		 * @see JmsListenerContainerSpec#destination(String)
		 */
		public JmsInboundGatewayListenerContainerSpec<C> destination(String destinationName) {
			spec.destination(destinationName);
			return _this();
		}

		public JmsInboundGatewayListenerContainerSpec<C> configureListenerContainer(
				Consumer<JmsListenerContainerSpec<C>> configurer) {
			Assert.notNull(configurer);
			configurer.accept(this.spec);
			return _this();
		}

	}

}
