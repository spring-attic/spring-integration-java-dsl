/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.dsl.jpa;

import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.support.PersistMode;

/**
 * A {@link JpaBaseOutboundEndpointSpec} extension for the {@code updating}
 * {@link org.springframework.integration.jpa.outbound.JpaOutboundGateway} mode.
 * The {@code outbound-channel-adapter} is achievable through an internal {@code producesReply} option.
 *
 * @author Artem Bilan
 * @since 1.2
 */
public class JpaUpdatingOutboundEndpointSpec extends JpaBaseOutboundEndpointSpec<JpaUpdatingOutboundEndpointSpec> {

	JpaUpdatingOutboundEndpointSpec(JpaExecutor jpaExecutor) {
		super(jpaExecutor);
	}

	JpaUpdatingOutboundEndpointSpec producesReply(boolean producesReply) {
		this.target.setProducesReply(producesReply);
		if (producesReply) {
			this.target.setRequiresReply(true);
		}
		return this;
	}

	public JpaUpdatingOutboundEndpointSpec persistMode(PersistMode persistMode) {
		this.jpaExecutor.setPersistMode(persistMode);
		return this;
	}

	public JpaUpdatingOutboundEndpointSpec flush(boolean flush) {
		this.jpaExecutor.setFlush(flush);
		return this;
	}

	public JpaUpdatingOutboundEndpointSpec flushSize(int flushSize) {
		this.jpaExecutor.setFlushSize(flushSize);
		return this;
	}

	public JpaUpdatingOutboundEndpointSpec clearOnFlush(boolean clearOnFlush) {
		this.jpaExecutor.setClearOnFlush(clearOnFlush);
		return this;
	}

}
