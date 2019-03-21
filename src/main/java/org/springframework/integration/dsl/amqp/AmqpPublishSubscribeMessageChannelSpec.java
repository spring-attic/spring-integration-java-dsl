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

package org.springframework.integration.dsl.amqp;

import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.integration.amqp.channel.PublishSubscribeAmqpChannel;
import org.springframework.integration.amqp.config.AmqpChannelFactoryBean;

/**
 * A {@link AmqpMessageChannelSpec} for {@link PublishSubscribeAmqpChannel}s.
 *
 * @author Artem Bilan
 */
public class AmqpPublishSubscribeMessageChannelSpec
		extends AmqpMessageChannelSpec<AmqpPublishSubscribeMessageChannelSpec> {

	AmqpPublishSubscribeMessageChannelSpec(ConnectionFactory connectionFactory) {
		super(connectionFactory);
		this.amqpChannelFactoryBean.setPubSub(true);
	}

	/**
	 * @param exchange the exchange.
	 * @return the spec.
	 * @see AmqpChannelFactoryBean#setExchange(FanoutExchange)
	 */
	public AmqpPublishSubscribeMessageChannelSpec exchange(FanoutExchange exchange) {
		this.amqpChannelFactoryBean.setExchange(exchange);
		return _this();
	}

}
