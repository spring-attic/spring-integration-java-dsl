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

package org.springframework.integration.dsl.test.jpa;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import javax.persistence.EntityManagerFactory;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.jpa.Jpa;
import org.springframework.integration.dsl.test.jpa.entity.StudentDomain;
import org.springframework.integration.test.util.OnlyOnceTrigger;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Artem Bilan
 * @since 1.2
 */
@RunWith(SpringRunner.class)
@DirtiesContext
@SpringBootTest(properties = "spring.datasource.initialize=false", webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class JpaTests {

	@Autowired
	private PollableChannel pollingResults;

	@Test
	public void testInboundAdapterFlow() {
		Message<?> message = this.pollingResults.receive(10_000);
		assertNotNull(message);
		assertThat(message.getPayload(), instanceOf(StudentDomain.class));
		StudentDomain student = (StudentDomain) message.getPayload();
		assertEquals("First One", student.getFirstName());
	}

	@Configuration
	@Import({ DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
			IntegrationAutoConfiguration.class })
	@EntityScan(basePackageClasses = StudentDomain.class)
	public static class ContextConfiguration {

		@Bean
		public IntegrationFlow pollingAdapterFlow(EntityManagerFactory entityManagerFactory) {
			return IntegrationFlows
					.from(Jpa.inboundAdapter(entityManagerFactory)
									.entityClass(StudentDomain.class)
									.maxResults(1)
									.expectSingleResult(true),
							e -> e.poller(p -> p.trigger(new OnlyOnceTrigger())))
					.channel(c -> c.queue("pollingResults"))
					.get();
		}

	}

}
