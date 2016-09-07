/*
 * Copyright 2014-2016 the original author or authors.
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

package org.springframework.integration.dsl.core;

import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.mapping.InboundMessageMapper;
import org.springframework.integration.mapping.OutboundMessageMapper;
import org.springframework.messaging.MessageChannel;

/**
 * An {@link IntegrationComponentSpec} for {@link MessagingGatewaySupport}s.
 *
 * @author Artem Bilan
 */
public abstract class MessagingGatewaySpec<S extends MessagingGatewaySpec<S, G>, G extends MessagingGatewaySupport>
		extends IntegrationComponentSpec<S, G> {

	public MessagingGatewaySpec(G gateway) {
		this.target = gateway;
	}

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
	 * @param replyChannel the replyChannel.
	 * @return the spec.
	 * @see MessagingGatewaySupport#setReplyChannel(MessageChannel)
	 */
	public S replyChannel(MessageChannel replyChannel) {
		this.target.setReplyChannel(replyChannel);
		return _this();
	}

	/**
	 * @param replyChannelName the name of replyChannel.
	 * @return the spec.
	 * @see MessagingGatewaySupport#setReplyChannelName(String)
	 * @since 1.1.1
	 */
	public S replyChannel(String replyChannelName) {
		this.target.setReplyChannelName(replyChannelName);
		return _this();
	}

	/**
	 * @param requestChannel the requestChannel.
	 * @return the spec.
	 * @see MessagingGatewaySupport#setRequestChannel(MessageChannel)
	 */
	public S requestChannel(MessageChannel requestChannel) {
		target.setRequestChannel(requestChannel);
		return _this();
	}

	/**
	 * @param requestChannelName the name of requestChannel.
	 * @return the spec.
	 * @see MessagingGatewaySupport#setRequestChannelName(String)
	 * @since 1.1.1
	 */
	public S requestChannel(String requestChannelName) {
		target.setRequestChannelName(requestChannelName);
		return _this();
	}

	/**
	 * @param errorChannel the errorChannel.
	 * @return the spec.
	 * @see MessagingGatewaySupport#setErrorChannel(MessageChannel)
	 */
	public S errorChannel(MessageChannel errorChannel) {
		target.setErrorChannel(errorChannel);
		return _this();
	}

	/**
	 * @param errorChannelName the name of errorChannel.
	 * @return the spec.
	 * @see MessagingGatewaySupport#setErrorChannelName(String)
	 * @since 1.1.1
	 */
	public S errorChannel(String errorChannelName) {
		target.setErrorChannelName(errorChannelName);
		return _this();
	}

	/**
	 * @param requestTimeout the requestTimeout.
	 * @return the spec.
	 * @see MessagingGatewaySupport#setRequestTimeout(long)
	 */
	public S requestTimeout(long requestTimeout) {
		target.setRequestTimeout(requestTimeout);
		return _this();
	}

	/**
	 * @param replyTimeout the replyTimeout.
	 * @return the spec.
	 * @see MessagingGatewaySupport#setReplyTimeout(long)
	 */
	public S replyTimeout(long replyTimeout) {
		target.setReplyTimeout(replyTimeout);
		return _this();
	}

	/**
	 * @param requestMapper the requestMapper.
	 * @return the spec.
	 * @see MessagingGatewaySupport#setRequestMapper(InboundMessageMapper)
	 */
	public S requestMapper(InboundMessageMapper<?> requestMapper) {
		target.setRequestMapper(requestMapper);
		return _this();
	}

	/**
	 * @param replyMapper the replyMapper.
	 * @return the spec.
	 * @see MessagingGatewaySupport#setReplyMapper(OutboundMessageMapper)
	 */
	public S replyMapper(OutboundMessageMapper<?> replyMapper) {
		target.setReplyMapper(replyMapper);
		return _this();
	}

}
