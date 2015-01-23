/*
 * Copyright 2015 the original author or authors.
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
import java.util.Collection;
import java.util.Collections;

import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.dsl.core.ComponentsRegistration;
import org.springframework.integration.dsl.core.IntegrationComponentSpec;
import org.springframework.integration.filter.ExpressionEvaluatingSelector;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * The {@link IntegrationComponentSpec} implementation for the {@link WireTap} component.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @since 1.1.0
 *
 */
public class WireTapSpec extends IntegrationComponentSpec<WireTapSpec, WireTap> implements ComponentsRegistration {

	private final MessageChannel channel;

	private MessageSelector selector;

	private Long timeout;

	WireTapSpec(MessageChannel channel) {
		Assert.notNull(channel, "'channel' must not be null");
		this.channel = channel;
	}

	public WireTapSpec selector(String selectorExpression) {
		this.selector = new ExpressionEvaluatingSelector(PARSER.parseExpression(selectorExpression));
		return this;
	}

	public WireTapSpec selector(MessageSelector selector) {
		this.selector = selector;
		return this;
	}

	public WireTapSpec timeout(long timeout) {
		this.timeout = timeout;
		return this;
	}

	@Override
	protected WireTap doGet() {
		WireTap wireTap = new WireTap(this.channel, this.selector);
		if (this.timeout != null) {
			wireTap.setTimeout(this.timeout);
		}
		return wireTap;
	}

	@Override
	public Collection<Object> getComponentsToRegister() {
		if (this.selector != null) {
			return Arrays.asList(this.selector, this.target);
		}
		else {
			return Collections.<Object>singletonList(this.target);
		}
	}

}
