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

import org.springframework.integration.config.SourcePollingChannelAdapterFactoryBean;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.core.EndpointSpec;
import org.springframework.integration.scheduling.PollerMetadata;

/**
 * @author Artem Bilan

 */
public final class SourcePollingChannelAdapterSpec extends
		EndpointSpec<SourcePollingChannelAdapterSpec, SourcePollingChannelAdapterFactoryBean, MessageSource<?>> {

	SourcePollingChannelAdapterSpec(MessageSource<?> messageSource) {
		super(messageSource);
		this.target.getT1().setSource(messageSource);
	}

	public SourcePollingChannelAdapterSpec phase(int phase) {
		this.target.getT1().setPhase(phase);
		return _this();
	}

	public SourcePollingChannelAdapterSpec autoStartup(boolean autoStartup) {
		this.target.getT1().setAutoStartup(autoStartup);
		return _this();
	}

	public SourcePollingChannelAdapterSpec poller(PollerMetadata pollerMetadata) {
		if (pollerMetadata != null) {
			if (PollerMetadata.MAX_MESSAGES_UNBOUNDED == pollerMetadata.getMaxMessagesPerPoll()) {
				pollerMetadata.setMaxMessagesPerPoll(1);
			}
			this.target.getT1().setPollerMetadata(pollerMetadata);
		}
		return _this();
	}

}
