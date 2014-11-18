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

import org.springframework.integration.aggregator.ResequencingMessageGroupProcessor;
import org.springframework.integration.aggregator.ResequencingMessageHandler;

/**
 * @author Artem Bilan
 */
public class ResequencerSpec extends CorrelationHandlerSpec<ResequencerSpec, ResequencingMessageHandler> {

	private final ResequencingMessageHandler resequencingMessageHandler =
			new ResequencingMessageHandler(new ResequencingMessageGroupProcessor());

	ResequencerSpec() {
	}

	/**
	 * @param releasePartialSequences the releasePartialSequences
	 * @return the handler spec.
	 * @see ResequencingMessageHandler#setReleasePartialSequences(boolean)
	 */
	public ResequencerSpec releasePartialSequences(boolean releasePartialSequences) {
		this.resequencingMessageHandler.setReleasePartialSequences(releasePartialSequences);
		return _this();
	}

	@Override
	protected ResequencingMessageHandler doGet() {
		return this.configure(this.resequencingMessageHandler);
	}

}
