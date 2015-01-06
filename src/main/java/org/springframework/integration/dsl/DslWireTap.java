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
package org.springframework.integration.dsl;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;
import org.springframework.integration.channel.ChannelInterceptorAware;
import org.springframework.integration.channel.interceptor.VetoCapableInterceptor;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.dsl.support.MessageChannelReference;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.util.Assert;

/**
 * @author Gary Russell
 * @since 1.0.2
 *
 */
class DslWireTap implements ChannelInterceptor, BeanFactoryAware, VetoCapableInterceptor, InitializingBean,
		Lifecycle {

	private WireTap wireTap;

	private final MessageChannel channel;

	private final MessageSelector selector;

	private BeanFactory beanFactory;

	/**
	 * Create a new wire tap with <em>no</em> {@link MessageSelector}.
	 *
	 * @param channel the MessageChannel to which intercepted messages will be sent
	 */
	public DslWireTap(MessageChannel channel) {
		this(channel, null);
	}

	/**
	 * Create a new wire tap with the provided {@link MessageSelector}.
	 *
	 * @param channel the channel to which intercepted messages will be sent
	 * @param selector the selector that must accept a message for it to be
	 * sent to the intercepting channel
	 */
	public DslWireTap(MessageChannel channel, MessageSelector selector) {
		Assert.notNull(channel, "channel must not be null");
		this.channel = channel;
		this.selector = selector;
	}

	public void setTimeout(long timeout) {
		this.wireTap.setTimeout(timeout);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void afterPropertiesSet() {
		synchronized (this) {
			if (this.wireTap == null) {
				MessageChannel channel = this.channel;
				if (channel instanceof MessageChannelReference) {
					Assert.notNull(this.beanFactory, "a bean factory is required");
					String channelName = ((MessageChannelReference) channel).getName();
					try {
						channel = this.beanFactory.getBean(channelName, MessageChannel.class);
					}
					catch (BeansException e) {
						throw new DestinationResolutionException("Failed to look up MessageChannel with name '"
								+ channelName + "' in the BeanFactory.");
					}
				}
				this.wireTap = new WireTap(channel, this.selector);
			}
		}
	}

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		return this.wireTap.preSend(message, channel);
	}

	@Override
	public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
		this.wireTap.postSend(message, channel, sent);
	}

	@Override
	public boolean preReceive(MessageChannel channel) {
		return this.wireTap.preReceive(channel);
	}

	@Override
	public Message<?> postReceive(Message<?> message, MessageChannel channel) {
		return this.wireTap.postReceive(message, channel);
	}

	@Override
	public boolean isRunning() {
		return this.wireTap.isRunning();
	}

	@Override
	public void start() {
		if (this.wireTap == null) {
			afterPropertiesSet();
		}
		this.wireTap.start();
	}

	@Override
	public void stop() {
		this.wireTap.stop();
	}

	@Override
	public boolean shouldIntercept(String beanName, ChannelInterceptorAware channel) {
		return this.wireTap.shouldIntercept(beanName, channel);
	}

	@Override
	public String toString() {
		return this.wireTap.toString();
	}


}
