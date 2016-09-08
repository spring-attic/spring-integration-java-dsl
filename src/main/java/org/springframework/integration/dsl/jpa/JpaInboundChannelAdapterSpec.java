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

import java.util.Collection;
import java.util.Collections;

import org.springframework.expression.Expression;
import org.springframework.integration.dsl.core.ComponentsRegistration;
import org.springframework.integration.dsl.core.MessageSourceSpec;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.inbound.JpaPollingChannelAdapter;
import org.springframework.integration.jpa.support.parametersource.ParameterSource;

/**
 * A {@link MessageSourceSpec} for a {@link JpaPollingChannelAdapter}.
 *
 * @author Artem Bilan
 *
 * @since 1.2
 */
public class JpaInboundChannelAdapterSpec
		extends MessageSourceSpec<JpaInboundChannelAdapterSpec, JpaPollingChannelAdapter>
		implements ComponentsRegistration {

	private final JpaExecutor jpaExecutor;

	JpaInboundChannelAdapterSpec(JpaExecutor jpaExecutor) {
		this.jpaExecutor = jpaExecutor;
		this.target = new JpaPollingChannelAdapter(this.jpaExecutor);
	}

	public JpaInboundChannelAdapterSpec entityClass(Class<?> entityClass) {
		this.jpaExecutor.setEntityClass(entityClass);
		return this;
	}

	public JpaInboundChannelAdapterSpec jpaQuery(String jpaQuery) {
		this.jpaExecutor.setJpaQuery(jpaQuery);
		return this;
	}

	public JpaInboundChannelAdapterSpec nativeQuery(String nativeQuery) {
		this.jpaExecutor.setNativeQuery(nativeQuery);
		return this;
	}

	public JpaInboundChannelAdapterSpec namedQuery(String namedQuery) {
		this.jpaExecutor.setNamedQuery(namedQuery);
		return this;
	}

	public JpaInboundChannelAdapterSpec deleteAfterPoll(boolean deleteAfterPoll) {
		this.jpaExecutor.setDeleteAfterPoll(deleteAfterPoll);
		return this;
	}

	public JpaInboundChannelAdapterSpec deleteInBatch(boolean deleteInBatch) {
		this.jpaExecutor.setDeleteInBatch(deleteInBatch);
		return this;
	}

	public JpaInboundChannelAdapterSpec flushAfterDelete(boolean flush) {
		this.jpaExecutor.setFlush(flush);
		return this;
	}

	public JpaInboundChannelAdapterSpec parameterSource(ParameterSource parameterSource) {
		this.jpaExecutor.setParameterSource(parameterSource);
		return this;
	}

	public JpaInboundChannelAdapterSpec expectSingleResult(boolean expectSingleResult) {
		this.jpaExecutor.setExpectSingleResult(expectSingleResult);
		return this;
	}

	/*public JpaInboundChannelAdapterSpec firstResultExpression(Expression firstResultExpression) {
		this.jpaExecutor.setFirstResultExpression(firstResultExpression);
		return this;
	}*/

/*	public JpaInboundChannelAdapterSpec idExpression(Expression idExpression) {
		this.jpaExecutor.setIdExpression(idExpression);
		return this;
	}*/

	public JpaInboundChannelAdapterSpec maxResultsExpression(Expression maxResultsExpression) {
		this.jpaExecutor.setMaxResultsExpression(maxResultsExpression);
		return this;
	}

	public JpaInboundChannelAdapterSpec maxResultsExpression(String maxResultsExpression) {
		return maxResultsExpression(PARSER.parseExpression(maxResultsExpression));
	}

	public JpaInboundChannelAdapterSpec maxResults(int maxResults) {
		return maxResultsExpression(new ValueExpression<Integer>(maxResults));
	}

	@Override
	public Collection<Object> getComponentsToRegister() {
		return Collections.<Object>singletonList(this.jpaExecutor);
	}

}
