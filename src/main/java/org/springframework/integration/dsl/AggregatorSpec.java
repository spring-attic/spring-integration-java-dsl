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

import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.aggregator.DefaultAggregatingMessageGroupProcessor;
import org.springframework.integration.aggregator.ExpressionEvaluatingMessageGroupProcessor;
import org.springframework.integration.aggregator.MessageGroupProcessor;
import org.springframework.integration.aggregator.MethodInvokingMessageGroupProcessor;

/**
 * A {@link CorrelationHandlerSpec} for an {@link AggregatingMessageHandler}.
 *
 * @author Artem Bilan
 */
public class AggregatorSpec extends CorrelationHandlerSpec<AggregatorSpec, AggregatingMessageHandler> {

	private MessageGroupProcessor outputProcessor = new DefaultAggregatingMessageGroupProcessor();

	private boolean expireGroupsUponCompletion;

	AggregatorSpec() {
	}

	public AggregatorSpec processor(Object target, String methodName) {
		super.processor(target);
		return this.outputProcessor(methodName != null
				? new MethodInvokingMessageGroupProcessor(target, methodName)
				: new MethodInvokingMessageGroupProcessor(target));
	}

	/**
	 * An expression to determine the output message from the released group. Defaults to a message
	 * with a payload that is a collection of payloads from the input messages.
	 * @param expression the expression.
	 * @return the aggregator spec.
	 */
	public AggregatorSpec outputExpression(String expression) {
		return this.outputProcessor(new ExpressionEvaluatingMessageGroupProcessor(expression));
	}

	/**
	 * A processor to determine the output message from the released group. Defaults to a message
	 * with a payload that is a collection of payloads from the input messages.
	 * @param outputProcessor the processor.
	 * @return the aggregator spec.
	 */
	public AggregatorSpec outputProcessor(MessageGroupProcessor outputProcessor) {
		this.outputProcessor = outputProcessor;
		return _this();
	}

	/**
	 * @param expireGroupsUponCompletion the expireGroupsUponCompletion.
	 * @return the aggregator spec.
	 * @see AggregatingMessageHandler#setExpireGroupsUponCompletion(boolean)
	 */
	public AggregatorSpec expireGroupsUponCompletion(boolean expireGroupsUponCompletion) {
		this.expireGroupsUponCompletion = expireGroupsUponCompletion;
		return _this();
	}

	@Override
	protected AggregatingMessageHandler doGet() {
		AggregatingMessageHandler handler = new AggregatingMessageHandler(this.outputProcessor);
		handler.setExpireGroupsUponCompletion(this.expireGroupsUponCompletion);
		return this.configure(handler);
	}

}
