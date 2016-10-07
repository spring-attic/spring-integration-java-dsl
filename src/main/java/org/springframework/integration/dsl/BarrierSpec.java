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

package org.springframework.integration.dsl;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.aggregator.BarrierMessageHandler;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.DefaultAggregatingMessageGroupProcessor;
import org.springframework.integration.aggregator.HeaderAttributeCorrelationStrategy;
import org.springframework.integration.aggregator.MessageGroupProcessor;
import org.springframework.integration.dsl.core.MessageHandlerSpec;
import org.springframework.util.Assert;

/**
 * A {@link MessageHandlerSpec} for the {@link BarrierMessageHandler}.
 *
 * @author Artem Bilan
 * @since 1.2
 */
public class BarrierSpec extends MessageHandlerSpec<BarrierSpec, BarrierMessageHandler> {

	private final long timeout;

	private MessageGroupProcessor outputProcessor = new DefaultAggregatingMessageGroupProcessor();

	private CorrelationStrategy correlationStrategy =
			new HeaderAttributeCorrelationStrategy(IntegrationMessageHeaderAccessor.CORRELATION_ID);

	BarrierSpec(long timeout) {
		this.timeout = timeout;
	}

	public BarrierSpec outputProcessor(MessageGroupProcessor outputProcessor) {
		Assert.notNull(outputProcessor);
		this.outputProcessor = outputProcessor;
		return this;
	}

	public BarrierSpec correlationStrategy(CorrelationStrategy correlationStrategy) {
		Assert.notNull(correlationStrategy);
		this.correlationStrategy = correlationStrategy;
		return this;
	}

	@Override
	protected BarrierMessageHandler doGet() {
		return new BarrierMessageHandler(this.timeout, this.outputProcessor, this.correlationStrategy);
	}

}
