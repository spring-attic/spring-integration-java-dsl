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

package org.springframework.integration.dsl.mail;

import org.springframework.integration.mail.ImapMailReceiver;
import org.springframework.integration.mail.Pop3MailReceiver;

/**
 * @author Gary Russell
 * @author Artem Bilan
 */
public class Mail {

	public static MailSendingMessageHandlerSpec outboundAdapter(String host) {
		return new MailSendingMessageHandlerSpec(host);
	}

	public static Pop3MailInboundChannelAdapterSpec pop3InboundAdapter() {
		return new Pop3MailInboundChannelAdapterSpec();
	}

	/**
	 * A {@link Pop3MailInboundChannelAdapterSpec} factory based on the provided {@link Pop3MailReceiver}.
	 * @param pop3MailReceiver the {@link Pop3MailReceiver} to use.
	 * @return the {@link Pop3MailInboundChannelAdapterSpec} instance.
	 * @since 1.2
	 */
	public static Pop3MailInboundChannelAdapterSpec pop3InboundAdapter(Pop3MailReceiver pop3MailReceiver) {
		return new Pop3MailInboundChannelAdapterSpec(pop3MailReceiver);
	}

	public static Pop3MailInboundChannelAdapterSpec pop3InboundAdapter(String url) {
		return new Pop3MailInboundChannelAdapterSpec(url);
	}

	public static Pop3MailInboundChannelAdapterSpec pop3InboundAdapter(String host, String username, String password) {
		return pop3InboundAdapter(host, -1, username, password);
	}

	public static Pop3MailInboundChannelAdapterSpec pop3InboundAdapter(String host, int port, String username,
			String password) {
		return new Pop3MailInboundChannelAdapterSpec(host, port, username, password);
	}

	public static ImapMailInboundChannelAdapterSpec imapInboundAdapter() {
		return new ImapMailInboundChannelAdapterSpec();
	}

	/**
	 * An {@link ImapMailInboundChannelAdapterSpec} factory based on the provided {@link ImapMailReceiver}.
	 * @param imapMailReceiver the {@link ImapMailReceiver} to use.
	 * @return the {@link ImapMailInboundChannelAdapterSpec} instance.
	 * @since 1.2
	 */
	public static ImapMailInboundChannelAdapterSpec imapInboundAdapter(ImapMailReceiver imapMailReceiver) {
		return new ImapMailInboundChannelAdapterSpec(imapMailReceiver);
	}

	public static ImapMailInboundChannelAdapterSpec imapInboundAdapter(String url) {
		return new ImapMailInboundChannelAdapterSpec(url);
	}

	public static ImapIdleChannelAdapterSpec imapIdleAdapter() {
		return new ImapIdleChannelAdapterSpec(new ImapMailReceiver());
	}

	public static ImapIdleChannelAdapterSpec imapIdleAdapter(String url) {
		return new ImapIdleChannelAdapterSpec(new ImapMailReceiver(url));
	}

	/**
	 * An {@link ImapIdleChannelAdapterSpec} factory based on the provided {@link ImapMailReceiver}.
	 * @param imapMailReceiver the {@link ImapMailReceiver} to use.
	 * @return the {@link ImapIdleChannelAdapterSpec} instance.
	 * @since 1.2
	 */
	public static ImapIdleChannelAdapterSpec imapIdleAdapter(ImapMailReceiver imapMailReceiver) {
		return new ImapIdleChannelAdapterSpec(imapMailReceiver, true);
	}

	public static MailHeadersBuilder headers() {
		return new MailHeadersBuilder();
	}

}
