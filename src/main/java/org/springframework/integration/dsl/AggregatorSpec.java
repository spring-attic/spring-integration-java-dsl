/*
 * Copyright 2014-2015 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.aggregator.DefaultAggregatingMessageGroupProcessor;
import org.springframework.integration.aggregator.ExpressionEvaluatingMessageGroupProcessor;
import org.springframework.integration.aggregator.MessageGroupProcessor;
import org.springframework.integration.aggregator.MethodInvokingMessageGroupProcessor;
import org.springframework.integration.store.MessageGroup;
import org.springframework.util.Assert;

/**
 * A {@link CorrelationHandlerSpec} for an {@link AggregatingMessageHandler}.
 * @author Artem Bilan
 */
public class AggregatorSpec extends CorrelationHandlerSpec<AggregatorSpec, AggregatingMessageHandler> {

	AggregatorSpec() {
		super(new InternalAggregatingMessageHandler());
	}

	/**
	 * Configure the handler with {@link org.springframework.integration.aggregator.MethodInvokingCorrelationStrategy}
	 * and {@link org.springframework.integration.aggregator.MethodInvokingReleaseStrategy} using the target
	 * object which should have methods annotated appropriately for each function.
	 * Also set the output processor.
	 * @param target     the target object.
	 * @param methodName The method name for the output processor (or 'null' in which case, the
	 *                   target object must have an {@link org.springframework.integration.annotation.Aggregator}
	 *                   annotation).
	 * @return the handler spec.
	 */
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
		Assert.notNull(outputProcessor);
		((InternalAggregatingMessageHandler) this.target.getT2()).getOutputProcessor().setDelegate(outputProcessor);
		return _this();
	}

	/**
	 * @param expireGroupsUponCompletion the expireGroupsUponCompletion.
	 * @return the aggregator spec.
	 * @see AggregatingMessageHandler#setExpireGroupsUponCompletion(boolean)
	 */
	public AggregatorSpec expireGroupsUponCompletion(boolean expireGroupsUponCompletion) {
		this.target.getT2().setExpireGroupsUponCompletion(expireGroupsUponCompletion);
		return _this();
	}


	private static class InternalAggregatingMessageHandler extends AggregatingMessageHandler {

		public InternalAggregatingMessageHandler() {
			super(new MessageGroupProcessorWrapper());
		}

		@Override
		protected MessageGroupProcessorWrapper getOutputProcessor() {
			return (MessageGroupProcessorWrapper) super.getOutputProcessor();
		}

	}

	private static class MessageGroupProcessorWrapper implements MessageGroupProcessor, BeanFactoryAware {

		private MessageGroupProcessor delegate = new DefaultAggregatingMessageGroupProcessor();

		public void setDelegate(MessageGroupProcessor delegate) {
			this.delegate = delegate;
		}

		@Override
		public Object processMessageGroup(MessageGroup group) {
			return this.delegate.processMessageGroup(group);
		}

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			if (this.delegate instanceof BeanFactoryAware) {
				((BeanFactoryAware) this.delegate).setBeanFactory(beanFactory);
			}
		}

	}

}
