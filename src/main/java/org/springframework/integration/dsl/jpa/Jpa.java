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

import org.springframework.integration.jpa.core.JpaOperations;

/**
 * Factory class for JPA components.
 *
 * @author Artem Bilan
 *
 * @since 1.2
 */
public final class Jpa {

	public static JpaInboundChannelAdapterSpec inboundAdapter(EntityManagerFactory entityManagerFactory) {
		return new JpaInboundChannelAdapterSpec(entityManagerFactory);
	}

	public static JpaInboundChannelAdapterSpec inboundAdapter(EntityManager entityManager) {
		return new JpaInboundChannelAdapterSpec(entityManager);
	}

	public static JpaInboundChannelAdapterSpec inboundAdapter(JpaOperations jpaOperations) {
		return new JpaInboundChannelAdapterSpec(jpaOperations);
	}

	private Jpa() {
	}

}
