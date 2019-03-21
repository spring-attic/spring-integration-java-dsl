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

import org.springframework.expression.Expression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.support.OutboundGatewayType;

/**
 * A {@link JpaBaseOutboundEndpointSpec} extension for the
 * {@link org.springframework.integration.jpa.outbound.JpaOutboundGateway} with
 * {@link org.springframework.integration.jpa.support.OutboundGatewayType#RETRIEVING} mode.
 *
 * @author Artem Bilan
 * @since 1.2
 */
public class JpaRetrievingOutboundGatewaySpec extends JpaBaseOutboundEndpointSpec<JpaRetrievingOutboundGatewaySpec> {

	JpaRetrievingOutboundGatewaySpec(JpaExecutor jpaExecutor) {
		super(jpaExecutor);
		this.target.setGatewayType(OutboundGatewayType.RETRIEVING);
		this.target.setRequiresReply(true);
	}


	public JpaRetrievingOutboundGatewaySpec expectSingleResult(boolean expectSingleResult) {
		this.jpaExecutor.setExpectSingleResult(expectSingleResult);
		return this;
	}

	public JpaRetrievingOutboundGatewaySpec firstResult(int firstResult) {
		return firstResultExpression(new ValueExpression<Integer>(firstResult));
	}

	public JpaRetrievingOutboundGatewaySpec firstResultExpression(String firstResultExpression) {
		return firstResultExpression(PARSER.parseExpression(firstResultExpression));
	}

	public JpaRetrievingOutboundGatewaySpec firstResultExpression(Expression firstResultExpression) {
		this.jpaExecutor.setFirstResultExpression(firstResultExpression);
		return this;
	}

	public JpaRetrievingOutboundGatewaySpec idExpression(String idExpression) {
		return idExpression(PARSER.parseExpression(idExpression));
	}

	public JpaRetrievingOutboundGatewaySpec idExpression(Expression idExpression) {
		this.jpaExecutor.setIdExpression(idExpression);
		return this;
	}

	public JpaRetrievingOutboundGatewaySpec maxResults(int maxResults) {
		return maxResultsExpression(new ValueExpression<Integer>(maxResults));
	}

	public JpaRetrievingOutboundGatewaySpec maxResultsExpression(String maxResultsExpression) {
		return maxResultsExpression(PARSER.parseExpression(maxResultsExpression));
	}

	public JpaRetrievingOutboundGatewaySpec maxResultsExpression(Expression maxResultsExpression) {
		this.jpaExecutor.setMaxResultsExpression(maxResultsExpression);
		return this;
	}

	public JpaRetrievingOutboundGatewaySpec deleteAfterPoll(boolean deleteAfterPoll) {
		this.jpaExecutor.setDeleteAfterPoll(deleteAfterPoll);
		return this;
	}

	public JpaRetrievingOutboundGatewaySpec deleteInBatch(boolean deleteInBatch) {
		this.jpaExecutor.setDeleteInBatch(deleteInBatch);
		return this;
	}

	public JpaRetrievingOutboundGatewaySpec flushAfterDelete(boolean flush) {
		this.jpaExecutor.setFlush(flush);
		return this;
	}

}
