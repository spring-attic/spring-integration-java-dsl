/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.dsl;

import org.springframework.core.Ordered;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.aggregator.BarrierMessageHandler;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.DefaultAggregatingMessageGroupProcessor;
import org.springframework.integration.aggregator.HeaderAttributeCorrelationStrategy;
import org.springframework.integration.aggregator.MessageGroupProcessor;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.dsl.core.ConsumerEndpointSpec;
import org.springframework.integration.dsl.core.MessageHandlerSpec;
import org.springframework.integration.dsl.support.tuple.Tuple2;
import org.springframework.integration.dsl.support.tuple.Tuples;
import org.springframework.util.Assert;

/**
 * A {@link MessageHandlerSpec} for the {@link BarrierMessageHandler}.
 *
 * @author Artem Bilan
 * @since 1.2
 */
public class BarrierSpec extends ConsumerEndpointSpec<BarrierSpec, BarrierMessageHandler> {

	private final long timeout;

	private MessageGroupProcessor outputProcessor = new DefaultAggregatingMessageGroupProcessor();

	private CorrelationStrategy correlationStrategy =
			new HeaderAttributeCorrelationStrategy(IntegrationMessageHeaderAccessor.CORRELATION_ID);

	private boolean requiresReply;

	private long sendTimeout = -1;

	private int order = Ordered.LOWEST_PRECEDENCE;

	private boolean async;

	BarrierSpec(long timeout) {
		super(null);
		this.timeout = timeout;
	}

	public BarrierSpec outputProcessor(MessageGroupProcessor outputProcessor) {
		Assert.notNull(outputProcessor, "'outputProcessor' must not be null.");
		this.outputProcessor = outputProcessor;
		return this;
	}

	public BarrierSpec correlationStrategy(CorrelationStrategy correlationStrategy) {
		Assert.notNull(correlationStrategy, "'correlationStrategy' must not be null.");
		this.correlationStrategy = correlationStrategy;
		return this;
	}

	@Override
	public BarrierSpec requiresReply(boolean requiresReply) {
		this.requiresReply = requiresReply;
		return this;
	}

	@Override
	public BarrierSpec sendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
		return this;
	}

	@Override
	public BarrierSpec order(int order) {
		this.order = order;
		return this;
	}

	@Override
	public BarrierSpec async(boolean async) {
		this.async = async;
		return this;
	}

	@Override
	public Tuple2<ConsumerEndpointFactoryBean, BarrierMessageHandler> doGet() {
		BarrierMessageHandler barrierMessageHandler =
				new BarrierMessageHandler(this.timeout, this.outputProcessor, this.correlationStrategy);
		barrierMessageHandler.setAdviceChain(this.adviceChain);
		barrierMessageHandler.setRequiresReply(this.requiresReply);
		barrierMessageHandler.setSendTimeout(this.sendTimeout);
		barrierMessageHandler.setAsync(this.async);
		barrierMessageHandler.setOrder(this.order);
		this.endpointFactoryBean.setHandler(barrierMessageHandler);
		return Tuples.of(this.endpointFactoryBean, barrierMessageHandler);
	}

}
