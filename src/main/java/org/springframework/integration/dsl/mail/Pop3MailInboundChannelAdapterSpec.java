/*
 * Copyright 2014-2016 the original author or authors.
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

import org.springframework.integration.mail.Pop3MailReceiver;

/**
 * A {@link MailInboundChannelAdapterSpec} for POP3.
 *
 * @author Gary Russell
 * @author Artem Bilan
 */
public class Pop3MailInboundChannelAdapterSpec
		extends MailInboundChannelAdapterSpec<Pop3MailInboundChannelAdapterSpec, Pop3MailReceiver> {

	Pop3MailInboundChannelAdapterSpec() {
		super(new Pop3MailReceiver());
	}

	Pop3MailInboundChannelAdapterSpec(Pop3MailReceiver receiver) {
		super(receiver, true);
	}

	Pop3MailInboundChannelAdapterSpec(String url) {
		super(new Pop3MailReceiver(url));
	}

	Pop3MailInboundChannelAdapterSpec(String host, String username, String password) {
		super(new Pop3MailReceiver(host, username, password));
	}

	Pop3MailInboundChannelAdapterSpec(String host, int port, String username, String password) {
		super(new Pop3MailReceiver(host, port, username, password));
	}

}
