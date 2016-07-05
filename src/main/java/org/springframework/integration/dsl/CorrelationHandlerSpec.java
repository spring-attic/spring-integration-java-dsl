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

package org.springframework.integration.dsl;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.aopalliance.aop.Advice;

import org.springframework.expression.Expression;
import org.springframework.integration.aggregator.AbstractCorrelatingMessageHandler;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.ExpressionEvaluatingCorrelationStrategy;
import org.springframework.integration.aggregator.ExpressionEvaluatingReleaseStrategy;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.config.CorrelationStrategyFactoryBean;
import org.springframework.integration.config.ReleaseStrategyFactoryBean;
import org.springframework.integration.dsl.core.ConsumerEndpointSpec;
import org.springframework.integration.dsl.core.MessageHandlerSpec;
import org.springframework.integration.dsl.support.Function;
import org.springframework.integration.dsl.support.FunctionExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

/**
 * A {@link MessageHandlerSpec} for an {@link AbstractCorrelatingMessageHandler}.
 *
 * @author Artem Bilan
 */
public abstract class
		CorrelationHandlerSpec<S extends CorrelationHandlerSpec<S, H>, H extends AbstractCorrelatingMessageHandler>
		extends ConsumerEndpointSpec<S, H> {

	private final List<Advice> forceReleaseAdviceChain = new LinkedList<Advice>();

	protected CorrelationHandlerSpec(H messageHandler) {
		super(messageHandler);
		messageHandler.setForceReleaseAdviceChain(this.forceReleaseAdviceChain);
	}

	/**
	 * @param messageStore the message group store.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setMessageStore(MessageGroupStore)
	 */
	public S messageStore(MessageGroupStore messageStore) {
		Assert.notNull(messageStore);
		this.target.getT2().setMessageStore(messageStore);
		return _this();
	}

	/**
	 * @param sendPartialResultOnExpiry the sendPartialResultOnExpiry.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setSendPartialResultOnExpiry(boolean)
	 */
	public S sendPartialResultOnExpiry(boolean sendPartialResultOnExpiry) {
		this.target.getT2().setSendPartialResultOnExpiry(sendPartialResultOnExpiry);
		return _this();
	}

	/**
	 * @param minimumTimeoutForEmptyGroups the minimumTimeoutForEmptyGroups
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setMinimumTimeoutForEmptyGroups(long)
	 */
	public S minimumTimeoutForEmptyGroups(long minimumTimeoutForEmptyGroups) {
		this.target.getT2().setMinimumTimeoutForEmptyGroups(minimumTimeoutForEmptyGroups);
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
		this.target.getT2().setGroupTimeoutExpression(new ValueExpression<Long>(groupTimeout));
		return _this();
	}

	/**
	 * @param groupTimeoutExpression the group timeout expression string.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setGroupTimeoutExpression(Expression)
	 */
	public S groupTimeoutExpression(String groupTimeoutExpression) {
		Assert.hasText(groupTimeoutExpression);
		this.target.getT2().setGroupTimeoutExpression(PARSER.parseExpression(groupTimeoutExpression));
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
		this.target.getT2().setGroupTimeoutExpression(new FunctionExpression<MessageGroup>(groupTimeoutFunction));
		return _this();
	}

	/**
	 * @param taskScheduler the task scheduler.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setTaskScheduler(TaskScheduler)
	 */
	public S taskScheduler(TaskScheduler taskScheduler) {
		Assert.notNull(taskScheduler);
		this.target.getT2().setTaskScheduler(taskScheduler);
		return _this();
	}

	/**
	 * @param discardChannel the discard channel.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setDiscardChannel(MessageChannel)
	 */
	public S discardChannel(MessageChannel discardChannel) {
		Assert.notNull(discardChannel);
		this.target.getT2().setDiscardChannel(discardChannel);
		return _this();
	}

	/**
	 * @param discardChannelName the discard channel.
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setDiscardChannelName(String)
	 */
	public S discardChannel(String discardChannelName) {
		Assert.hasText(discardChannelName);
		this.target.getT2().setDiscardChannelName(discardChannelName);
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
			CorrelationStrategyFactoryBean correlationStrategyFactoryBean = new CorrelationStrategyFactoryBean();
			correlationStrategyFactoryBean.setTarget(target);
			correlationStrategyFactoryBean.afterPropertiesSet();
			ReleaseStrategyFactoryBean releaseStrategyFactoryBean = new ReleaseStrategyFactoryBean();
			releaseStrategyFactoryBean.setTarget(target);
			releaseStrategyFactoryBean.afterPropertiesSet();
			return correlationStrategy(correlationStrategyFactoryBean.getObject())
					.releaseStrategy(releaseStrategyFactoryBean.getObject());
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
			CorrelationStrategyFactoryBean correlationStrategyFactoryBean = new CorrelationStrategyFactoryBean();
			correlationStrategyFactoryBean.setTarget(target);
			correlationStrategyFactoryBean.setMethodName(methodName);
			correlationStrategyFactoryBean.afterPropertiesSet();
			return correlationStrategy(correlationStrategyFactoryBean.getObject());
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
		this.target.getT2().setCorrelationStrategy(correlationStrategy);
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
			ReleaseStrategyFactoryBean releaseStrategyFactoryBean = new ReleaseStrategyFactoryBean();
			releaseStrategyFactoryBean.setTarget(target);
			releaseStrategyFactoryBean.setMethodName(methodName);
			releaseStrategyFactoryBean.afterPropertiesSet();
			return releaseStrategy(releaseStrategyFactoryBean.getObject());
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
		this.target.getT2().setReleaseStrategy(releaseStrategy);
		return _this();
	}

	/**
	 * Expire (completely remove) a group if it is completed due to timeout.
	 * Default {@code true} for aggregator and {@code false} for resequencer.
	 * @param expireGroupsUponTimeout the expireGroupsUponTimeout to set
	 * @return the handler spec.
	 * @see AbstractCorrelatingMessageHandler#setExpireGroupsUponTimeout
	 * @since 1.1
	 */
	public S expireGroupsUponTimeout(boolean expireGroupsUponTimeout) {
		this.target.getT2().setExpireGroupsUponTimeout(expireGroupsUponTimeout);
		return _this();
	}

	/**
	 * Configure a list of {@link Advice} objects to be applied to the
	 * {@code forceComplete()} operation.
	 * @param advice the advice chain.
	 * @return the endpoint spec.
	 * @since 1.1
	 */
	public S forceReleaseAdvice(Advice... advice) {
		this.forceReleaseAdviceChain.addAll(Arrays.asList(advice));
		return _this();
	}

	/**
	 * Used to obtain a {@code Lock} based on the {@code groupId} for concurrent operations
	 * on the {@code MessageGroup}.
	 * By default, an internal {@code DefaultLockRegistry} is used.
	 * Use of a distributed {@link LockRegistry}, such as the {@code RedisLockRegistry},
	 * ensures only one instance of the aggregator will operate on a group concurrently.
	 * @param lockRegistry the {@link LockRegistry} to use.
	 * @return the endpoint spec.
	 * @since 1.1
	 */
	public S lockRegistry(LockRegistry lockRegistry) {
		Assert.notNull(lockRegistry);
		this.target.getT2().setLockRegistry(lockRegistry);
		return _this();
	}

}
