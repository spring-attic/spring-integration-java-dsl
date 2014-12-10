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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.dsl.support.MessageChannelReference;
import org.springframework.integration.dsl.support.tuple.Tuple2;
import org.springframework.integration.dsl.support.tuple.Tuples;
import org.springframework.integration.filter.ExpressionEvaluatingSelector;
import org.springframework.integration.router.RecipientListRouter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.util.StringUtils;

/**
 * @author Artem Bilan
 */
class DslRecipientListRouter extends RecipientListRouter {

	private final List<Tuple2<?, ?>> recipients = new ArrayList<Tuple2<?, ?>>();

	void add(String channelName, String expression) {
		this.recipients.add(Tuples.of(channelName, expression));
	}

	void add(String channelName, MessageSelector selector) {
		this.recipients.add(Tuples.of(channelName, selector));
	}

	void add(MessageChannel channel, String expression) {
		this.recipients.add(Tuples.of(channel, expression));
	}

	void add(MessageChannel channel, MessageSelector selector) {
		this.recipients.add(Tuples.of(channel, selector));
	}

	@Override
	public void onInit() throws Exception {
		List<Recipient> recipients = new ArrayList<Recipient>(this.recipients.size());

		for (Tuple2<?, ?> recipient : this.recipients) {
			if (recipient.getT1() instanceof String) {
				recipients.add(new DslRecipient(new MessageChannelReference((String) recipient.getT1()),
						populateRecipientSelector(recipient.getT2())));
			}
			else {
				recipients.add(new Recipient((MessageChannel) recipient.getT1(),
						populateRecipientSelector(recipient.getT2())));
			}
		}

		setRecipients(recipients);
		this.recipients.clear();
		super.onInit();
	}

	private MessageSelector populateRecipientSelector(Object recipientSelector) {
		if (recipientSelector instanceof String) {
			String expression = (String) recipientSelector;
			if (StringUtils.hasText(expression)) {
				ExpressionEvaluatingSelector selector = new ExpressionEvaluatingSelector(expression);
				selector.setBeanFactory(getBeanFactory());
				return selector;
			}
		}
		else {
			return (MessageSelector) recipientSelector;
		}
		return null;
	}

	class DslRecipient extends Recipient {

		private volatile MessageChannel channel;

		DslRecipient(MessageChannelReference channel, MessageSelector selector) {
			super(channel, selector);
		}

		@Override
		public MessageChannel getChannel() {
			if (this.channel == null) {
				synchronized (this) {
					if (this.channel == null) {
						this.channel = resolveChannelName((MessageChannelReference) super.getChannel());
					}
				}
			}
			return this.channel;
		}

		private MessageChannel resolveChannelName(MessageChannelReference channelReference) {
			String channelName = channelReference.getName();
			try {
				return DslRecipientListRouter.this.getBeanFactory().getBean(channelName, MessageChannel.class);
			}
			catch (BeansException e) {
				throw new DestinationResolutionException("Failed to look up MessageChannel with name '"
						+ channelName + "' in the BeanFactory.");
			}
		}

	}

}
