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
package org.springframework.integration.dsl.channel;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.dsl.core.ComponentsRegistration;
import org.springframework.integration.dsl.core.IntegrationComponentSpec;
import org.springframework.integration.dsl.support.MessageChannelReference;
import org.springframework.integration.filter.ExpressionEvaluatingSelector;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * @author Gary Russell
 * @since 1.0.2
 *
 */
public class WireTapSpec extends IntegrationComponentSpec<WireTapSpec, DslWireTap> implements ComponentsRegistration {

	private MessageSelector selector;

	private MessageChannel tapChannel;

	private Long timeout;

	public WireTapSpec selector(String selectorExpression) {
		this.selector = new ExpressionEvaluatingSelector(PARSER.parseExpression(selectorExpression));
		return this;
	}

	public WireTapSpec selector(MessageSelector selector) {
		this.selector = selector;
		return this;
	}

	public WireTapSpec channel(MessageChannel tapChannel) {
		this.tapChannel = tapChannel;
		return this;
	}

	public WireTapSpec channel(String tapChannelName) {
		this.tapChannel = new MessageChannelReference(tapChannelName);
		return this;
	}

	public WireTapSpec timeout(long timeout) {
		this.timeout = timeout;
		return this;
	}

	@Override
	protected DslWireTap doGet() {
		Assert.state(this.tapChannel != null, "tapChannel is required");
		DslWireTap wireTap = new DslWireTap(this.tapChannel, this.selector);
		if (this.timeout != null) {
			wireTap.setTimeout(this.timeout);
		}
		this.target = wireTap;
		return wireTap;
	}

	@Override
	public Collection<Object> getComponentsToRegister() {
		if (this.selector != null) {
			return Arrays.asList(this.selector, this.target);
		}
		else {
			return Collections.singletonList(this.target);
		}
	}

}
