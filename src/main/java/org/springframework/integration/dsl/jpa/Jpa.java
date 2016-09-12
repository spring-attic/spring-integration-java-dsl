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

/**
 * Factory class for JPA components.
 *
 * @author Artem Bilan
 *
 * @since 1.2
 */
public final class Jpa {

	public static JpaInboundChannelAdapterSpec inboundAdapter(EntityManagerFactory entityManagerFactory) {
		return inboundAdapter(new JpaExecutor(entityManagerFactory));
	}

	public static JpaInboundChannelAdapterSpec inboundAdapter(EntityManager entityManager) {
		return inboundAdapter(new JpaExecutor(entityManager));
	}

	public static JpaInboundChannelAdapterSpec inboundAdapter(JpaOperations jpaOperations) {
		return inboundAdapter(new JpaExecutor(jpaOperations));
	}

	private static JpaInboundChannelAdapterSpec inboundAdapter(JpaExecutor jpaExecutor) {
		return new JpaInboundChannelAdapterSpec(jpaExecutor);
	}

	public static JpaUpdatingOutboundEndpointSpec outboundAdapter(EntityManagerFactory entityManagerFactory) {
		return outboundAdapter(new JpaExecutor(entityManagerFactory));
	}

	public static JpaUpdatingOutboundEndpointSpec outboundAdapter(EntityManager entityManager) {
		return outboundAdapter(new JpaExecutor(entityManager));
	}

	public static JpaUpdatingOutboundEndpointSpec outboundAdapter(JpaOperations jpaOperations) {
		return outboundAdapter(new JpaExecutor(jpaOperations));
	}

	private static JpaUpdatingOutboundEndpointSpec outboundAdapter(JpaExecutor jpaExecutor) {
		return new JpaUpdatingOutboundEndpointSpec(jpaExecutor)
				.producesReply(false);
	}

	public static JpaUpdatingOutboundEndpointSpec updatingGateway(EntityManagerFactory entityManagerFactory) {
		return updatingGateway(new JpaExecutor(entityManagerFactory));
	}

	public static JpaUpdatingOutboundEndpointSpec updatingGateway(EntityManager entityManager) {
		return updatingGateway(new JpaExecutor(entityManager));
	}

	public static JpaUpdatingOutboundEndpointSpec updatingGateway(JpaOperations jpaOperations) {
		return updatingGateway(new JpaExecutor(jpaOperations));
	}

	private static JpaUpdatingOutboundEndpointSpec updatingGateway(JpaExecutor jpaExecutor) {
		return new JpaUpdatingOutboundEndpointSpec(jpaExecutor)
				.producesReply(true);
	}

	public static JpaRetrievingOutboundGatewaySpec retrievingGateway(EntityManagerFactory entityManagerFactory) {
		return retrievingGateway(new JpaExecutor(entityManagerFactory));
	}

	public static JpaRetrievingOutboundGatewaySpec retrievingGateway(EntityManager entityManager) {
		return retrievingGateway(new JpaExecutor(entityManager));
	}

	public static JpaRetrievingOutboundGatewaySpec retrievingGateway(JpaOperations jpaOperations) {
		return retrievingGateway(new JpaExecutor(jpaOperations));
	}

	private static JpaRetrievingOutboundGatewaySpec retrievingGateway(JpaExecutor jpaExecutor) {
		return new JpaRetrievingOutboundGatewaySpec(jpaExecutor);
	}


	private Jpa() {
	}

}
