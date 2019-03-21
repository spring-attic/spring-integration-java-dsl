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
package org.springframework.integration.dsl.mail;

import org.springframework.integration.mail.ImapMailReceiver;
import org.springframework.integration.mail.SearchTermStrategy;

/**
 * A {@link MailInboundChannelAdapterSpec} for IMAP.
 *
 * @author Gary Russell
 */
public class ImapMailInboundChannelAdapterSpec
		extends MailInboundChannelAdapterSpec<ImapMailInboundChannelAdapterSpec, ImapMailReceiver> {

	ImapMailInboundChannelAdapterSpec() {
		this.receiver = new ImapMailReceiver();
	}

	ImapMailInboundChannelAdapterSpec(String url) {
		this.receiver = new ImapMailReceiver(url);
	}

	/**
	 * @param searchTermStrategy the searchTermStrategy.
	 * @return the spec.
	 * @see ImapMailReceiver#setSearchTermStrategy(SearchTermStrategy)
	 */
	public ImapMailInboundChannelAdapterSpec searchTermStrategy(SearchTermStrategy searchTermStrategy) {
		this.receiver.setSearchTermStrategy(searchTermStrategy);
		return this;
	}

	/**
	 * @param shouldMarkMessagesAsRead the shouldMarkMessagesAsRead.
	 * @return the spec.
	 * @see ImapMailReceiver#setShouldMarkMessagesAsRead(Boolean)
	 */
	public ImapMailInboundChannelAdapterSpec shouldMarkMessagesAsRead(boolean shouldMarkMessagesAsRead) {
		this.receiver.setShouldMarkMessagesAsRead(shouldMarkMessagesAsRead);
		return this;
	}

}
