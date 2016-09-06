/*
 * Copyright 2016 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.dsl.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.adapter.FilteringAcknowledgingMessageListenerAdapter;
import org.springframework.kafka.listener.adapter.MessagingMessageListenerAdapter;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.kafka.listener.adapter.RetryingAcknowledgingMessageListenerAdapter;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.converter.MessageConverter;
import org.springframework.messaging.Message;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

/**
 * The copy of {@code org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter}
 * from Spring Integration Kafka 2.0, since we can't upgrade to {@code spring-integration-kafka-2.0}.
 *
 * @author Marius Bogoevici
 * @author Gary Russell
 */
public class Kafka09MessageDrivenChannelAdapter<K, V> extends MessageProducerSupport implements OrderlyShutdownCapable {

	private final AbstractMessageListenerContainer<K, V> messageListenerContainer;

	private final MessagingMessageListenerAdapter<K, V> listener = new IntegrationMessageListener();

	private RecordFilterStrategy<K, V> recordFilterStrategy;

	private boolean ackDiscarded;

	private RetryTemplate retryTemplate;

	private RecoveryCallback<Void> recoveryCallback;

	private boolean filterInRetry;

	public Kafka09MessageDrivenChannelAdapter(AbstractMessageListenerContainer<K, V> messageListenerContainer) {
		Assert.notNull(messageListenerContainer, "messageListenerContainer is required");
		Assert.isNull(messageListenerContainer.getContainerProperties().getMessageListener(),
				"Container must not already have a listener");
		this.messageListenerContainer = messageListenerContainer;
		this.messageListenerContainer.setAutoStartup(false);
		this.messageListenerContainer.getContainerProperties().setMessageListener(this.listener);
	}

	public void setMessageConverter(MessageConverter messageConverter) {
		this.listener.setMessageConverter(messageConverter);
	}

	public void setRecordFilterStrategy(RecordFilterStrategy<K, V> recordFilterStrategy) {
		this.recordFilterStrategy = recordFilterStrategy;
	}

	public void setAckDiscarded(boolean ackDiscarded) {
		this.ackDiscarded = ackDiscarded;
	}

	public void setRetryTemplate(RetryTemplate retryTemplate) {
		this.retryTemplate = retryTemplate;
	}

	public void setRecoveryCallback(RecoveryCallback<Void> recoveryCallback) {
		this.recoveryCallback = recoveryCallback;
	}

	public void setFilterInRetry(boolean filterInRetry) {
		this.filterInRetry = filterInRetry;
	}

	@Override
	protected void onInit() {
		super.onInit();

		AcknowledgingMessageListener<K, V> listener = this.listener;

		boolean filterInRetry = this.filterInRetry && this.retryTemplate != null && this.recordFilterStrategy != null;

		if (filterInRetry) {
			listener = new FilteringAcknowledgingMessageListenerAdapter<K, V>(listener, this.recordFilterStrategy,
					this.ackDiscarded);
			listener = new RetryingAcknowledgingMessageListenerAdapter<K, V>(listener, this.retryTemplate,
					this.recoveryCallback);
		}
		else {
			if (this.retryTemplate != null) {
				listener = new RetryingAcknowledgingMessageListenerAdapter<K, V>(listener, this.retryTemplate,
						this.recoveryCallback);
			}
			if (this.recordFilterStrategy != null) {
				listener = new FilteringAcknowledgingMessageListenerAdapter<K, V>(listener, this.recordFilterStrategy,
						this.ackDiscarded);
			}
		}

		this.messageListenerContainer.getContainerProperties().setMessageListener(listener);
	}

	@Override
	protected void doStart() {
		this.messageListenerContainer.start();
	}

	@Override
	protected void doStop() {
		this.messageListenerContainer.stop();
	}

	@Override
	public String getComponentType() {
		return "kafka:message-driven-channel-adapter";
	}

	@Override
	public int beforeShutdown() {
		this.messageListenerContainer.stop();
		return getPhase();
	}

	@Override
	public int afterShutdown() {
		return getPhase();
	}

	private class IntegrationMessageListener extends MessagingMessageListenerAdapter<K, V> {

		IntegrationMessageListener() {
			super(null);
		}

		@Override
		public void onMessage(ConsumerRecord<K, V> record, Acknowledgment acknowledgment) {
			Message<?> message = toMessagingMessage(record, acknowledgment);
			sendMessage(message);
		}

	}

}
