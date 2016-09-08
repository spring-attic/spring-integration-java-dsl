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

package org.springframework.integration.dsl.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.core.JpaOperations;
import org.springframework.integration.jpa.support.PersistMode;

/**
 * @author Artem Bilan
 * @since 1.2
 */
public class JpaOutboundChannelAdapterSpec extends JpaBaseOutboundEndpointSpec<JpaOutboundChannelAdapterSpec> {

	JpaOutboundChannelAdapterSpec(EntityManagerFactory entityManagerFactory) {
		this(new JpaExecutor(entityManagerFactory));
	}

	JpaOutboundChannelAdapterSpec(EntityManager entityManager) {
		this(new JpaExecutor(entityManager));
	}

	JpaOutboundChannelAdapterSpec(JpaOperations jpaOperations) {
		this(new JpaExecutor(jpaOperations));
	}

	private JpaOutboundChannelAdapterSpec(JpaExecutor jpaExecutor) {
		super(jpaExecutor, false);
	}

	public JpaOutboundChannelAdapterSpec persistMode(PersistMode persistMode) {
		this.jpaExecutor.setPersistMode(persistMode);
		return this;
	}

	public JpaOutboundChannelAdapterSpec flushSize(int flushSize) {
		this.jpaExecutor.setFlushSize(flushSize);
		return this;
	}

	public JpaOutboundChannelAdapterSpec clearOnFlush(boolean clearOnFlush) {
		this.jpaExecutor.setClearOnFlush(clearOnFlush);
		return this;
	}

	public JpaOutboundChannelAdapterSpec flush(boolean flush) {
		this.jpaExecutor.setFlush(flush);
		return this;
	}

}
