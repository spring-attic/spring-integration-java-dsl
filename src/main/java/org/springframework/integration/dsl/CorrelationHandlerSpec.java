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

import org.springframework.expression.Expression;
import org.springframework.integration.aggregator.AbstractCorrelatingMessageHandler;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.ExpressionEvaluatingCorrelationStrategy;
import org.springframework.integration.aggregator.ExpressionEvaluatingReleaseStrategy;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.config.CorrelationStrategyFactoryBean;
import org.springframework.integration.config.ReleaseStrategyFactoryBean;
import org.springframework.integration.dsl.core.MessageHandlerSpec;
import org.springframework.integration.dsl.support.Function;
import org.springframework.integration.dsl.support.FunctionExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.StringUtils;

/**
 * A {@link MessageHandlerSpec} for an {@link AbstractCorrelatingMessageHandler}.
 *
 * @author Artem Bilan
 */
public abstract class
		CorrelationHandlerSpec<S extends CorrelationHandlerSpec<S, H>, H extends AbstractCorrelatingMessageHandler>
		extends MessageHandlerSpec<S, H> {

	protected MessageGroupStore messageStore;

	protected boolean sendPartialResultOnExpiry;

	private long minimumTimeoutForEmptyGroups;

	private Expression groupTimeoutExpression;

	private TaskScheduler taskScheduler;

	private MessageChannel discardChannel;

	private String discardChannelName;

	private CorrelationStrategy correlationStrategy;

	private ReleaseStrategy releaseStrategy;

	/**
	 * @param messageStore the message group store.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setMessageStore(MessageGroupStore)
	 */
	public S messageStore(MessageGroupStore messageStore) {
		this.messageStore = messageStore;
		return _this();
	}

	/**
	 * @param sendPartialResultOnExpiry the sendPartialResultOnExpiry.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setSendPartialResultOnExpiry(boolean)
	 */
	public S sendPartialResultOnExpiry(boolean sendPartialResultOnExpiry) {
		this.sendPartialResultOnExpiry = sendPartialResultOnExpiry;
		return _this();
	}

	/**
	 * @param minimumTimeoutForEmptyGroups the minimumTimeoutForEmptyGroups
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setMinimumTimeoutForEmptyGroups(long)
	 */
	public S minimumTimeoutForEmptyGroups(long minimumTimeoutForEmptyGroups) {
		this.minimumTimeoutForEmptyGroups = minimumTimeoutForEmptyGroups;
		return _this();
	}

	/**
	 * Configure the handler with a group timeout expression that evaluates to
	 * this constant value.
	 * @param groupTimeout the group timeout in milliseconds.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setGroupTimeoutExpression(Expression)
	 * @see ValueExpression
	 */
	public S groupTimeout(long groupTimeout) {
		this.groupTimeoutExpression = new ValueExpression<Long>(groupTimeout);
		return _this();
	}

	/**
	 * @param groupTimeoutExpression the group timeout expression string.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setGroupTimeoutExpression(Expression)
	 */
	public S groupTimeoutExpression(String groupTimeoutExpression) {
		this.groupTimeoutExpression = PARSER.parseExpression(groupTimeoutExpression);
		return _this();
	}

	/**
	 * Configure the handler with a function that will be invoked to resolve the group timeout,
	 * based on the message group.
	 * Usually used with a JDK8 lambda:
	 * <p>{@code .groupTimeout(g -> g.size() * 2000L)}.
	 * @param groupTimeoutFunction a function invoked to resolve the group timeout in milliseconds.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setGroupTimeoutExpression(Expression)
	 */
	public S groupTimeout(Function<MessageGroup, Long> groupTimeoutFunction) {
		this.groupTimeoutExpression = new FunctionExpression<MessageGroup>(groupTimeoutFunction);
		return _this();
	}

	/**
	 * @param taskScheduler the task scheduler.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setTaskScheduler(TaskScheduler)
	 */
	public S taskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
		return _this();
	}

	/**
	 * @param discardChannel the discard channel.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setDiscardChannel(MessageChannel)
	 */
	public S discardChannel(MessageChannel discardChannel) {
		this.discardChannel = discardChannel;
		return _this();
	}

	/**
	 * @param discardChannelName the discard channel.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setDiscardChannelName(String)
	 */
	public S discardChannel(String discardChannelName) {
		this.discardChannelName = discardChannelName;
		return _this();
	}

	/**
	 * Configure the handler with {@link org.springframework.integration.aggregator.MethodInvokingCorrelationStrategy}
	 * and {@link org.springframework.integration.aggregator.MethodInvokingReleaseStrategy} using the target
	 * object which should have methods annotated appropriately for each function.
	 * @param target the target object,
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setCorrelationStrategy(CorrelationStrategy)
	 * @see AbstractCorrelatingMessageHandler#setReleaseStrategy(ReleaseStrategy)
	 */
	public S processor(Object target) {
		try {
			return correlationStrategy(new CorrelationStrategyFactoryBean(target).getObject())
					.releaseStrategy(new ReleaseStrategyFactoryBean(target).getObject());
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Configure the handler with an {@link ExpressionEvaluatingCorrelationStrategy} for the
	 * given expression.
	 * @param correlationExpression the correlation expression.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setCorrelationStrategy(CorrelationStrategy)
	 */
	public S correlationExpression(String correlationExpression) {
		return correlationStrategy(new ExpressionEvaluatingCorrelationStrategy(correlationExpression));
	}

	/**
	 * Configure the handler with an
	 * {@link org.springframework.integration.aggregator.MethodInvokingCorrelationStrategy}
	 * for the target object and method name.
	 * @param target the target object.
	 * @param methodName the method name.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setCorrelationStrategy(CorrelationStrategy)
	 */
	public S correlationStrategy(Object target, String methodName) {
		try {
			return correlationStrategy(new CorrelationStrategyFactoryBean(target, methodName).getObject());
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * @param correlationStrategy the correlation strategy.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setCorrelationStrategy(CorrelationStrategy)
	 */
	public S correlationStrategy(CorrelationStrategy correlationStrategy) {
		this.correlationStrategy = correlationStrategy;
		return _this();
	}

	/**
	 * Configure the handler with an {@link ExpressionEvaluatingReleaseStrategy} for the
	 * given expression.
	 * @param releaseExpression the correlation expression.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setReleaseStrategy(ReleaseStrategy)
	 */
	public S releaseExpression(String releaseExpression) {
		return releaseStrategy(new ExpressionEvaluatingReleaseStrategy(releaseExpression));
	}

	/**
	 * Configure the handler with an
	 * {@link org.springframework.integration.aggregator.MethodInvokingReleaseStrategy}
	 * for the target object and method name.
	 * @param target the target object.
	 * @param methodName the method name.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setReleaseStrategy(ReleaseStrategy)
	 */
	public S releaseStrategy(Object target, String methodName) {
		try {
			return releaseStrategy(new ReleaseStrategyFactoryBean(target, methodName).getObject());
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * @param releaseStrategy the release strategy.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setReleaseStrategy(ReleaseStrategy)
	 */
	public S releaseStrategy(ReleaseStrategy releaseStrategy) {
		this.releaseStrategy = releaseStrategy;
		return _this();
	}

	protected H configure(H handler) {
		if (this.discardChannel != null) {
			handler.setDiscardChannel(this.discardChannel);
		}
		if (StringUtils.hasText(this.discardChannelName)) {
			handler.setDiscardChannelName(this.discardChannelName);
		}
		if (this.messageStore != null) {
			handler.setMessageStore(this.messageStore);
		}
		handler.setMinimumTimeoutForEmptyGroups(this.minimumTimeoutForEmptyGroups);
		handler.setGroupTimeoutExpression(this.groupTimeoutExpression);
		if (this.taskScheduler != null) {
			handler.setTaskScheduler(this.taskScheduler);
		}
		handler.setSendPartialResultOnExpiry(this.sendPartialResultOnExpiry);
		if (this.correlationStrategy != null) {
			handler.setCorrelationStrategy(this.correlationStrategy);
		}
		if (this.releaseStrategy != null) {
			handler.setReleaseStrategy(this.releaseStrategy);
		}
		return handler;
	}

	CorrelationHandlerSpec() {
	}

}
