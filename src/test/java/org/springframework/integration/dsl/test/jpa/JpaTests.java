/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.dsl.test.jpa;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.integration.dsl.test.jpa.entity.Gender;
import org.springframework.integration.dsl.test.jpa.entity.StudentDomain;
import org.springframework.integration.jpa.support.PersistMode;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.OnlyOnceTrigger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Artem Bilan
 * @since 1.2
 */
@RunWith(SpringRunner.class)
@DirtiesContext
@SpringBootTest(properties = "spring.datasource.initialize=false",
		webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class JpaTests {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private PollableChannel pollingResults;

	@Autowired
	@Qualifier("outboundAdapterFlow.input")
	private MessageChannel outboundAdapterFlowInput;

	@Autowired
	@Qualifier("updatingGatewayFlow.input")
	private MessageChannel updatingGatewayFlowInput;

	@Autowired
	private PollableChannel persistResults;

	@Autowired
	@Qualifier("retrievingGatewayFlow.input")
	private MessageChannel retrievingGatewayFlowInput;

	@Autowired
	private PollableChannel retrieveResults;

	@Test
	public void testInboundAdapterFlow() {
		Message<?> message = this.pollingResults.receive(10_000);
		assertNotNull(message);
		assertThat(message.getPayload(), instanceOf(StudentDomain.class));
		StudentDomain student = (StudentDomain) message.getPayload();
		assertEquals("First One", student.getFirstName());
	}

	@Test
	public void testOutboundAdapterFlow() {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(this.dataSource);

		List<?> results1 = jdbcTemplate.queryForList("Select * from Student");
		assertNotNull(results1);
		assertTrue(results1.size() == 3);

		Calendar dateOfBirth = Calendar.getInstance();
		dateOfBirth.set(1981, 9, 27);

		StudentDomain student = new StudentDomain()
				.withFirstName("Artem")
				.withLastName("Bilan")
				.withGender(Gender.MALE)
				.withDateOfBirth(dateOfBirth.getTime())
				.withLastUpdated(new Date());

		assertNull(student.getRollNumber());

		this.outboundAdapterFlowInput.send(MessageBuilder.withPayload(student).build());

		List<?> results2 = jdbcTemplate.queryForList("Select * from Student");
		assertNotNull(results2);
		assertTrue(results2.size() == 4);

		assertNotNull(student.getRollNumber());
	}

	@Test
	public void testUpdatingGatewayFlow() {
		Calendar dateOfBirth = Calendar.getInstance();
		dateOfBirth.set(1981, 9, 27);

		StudentDomain student = new StudentDomain()
				.withFirstName("Artem")
				.withLastName("Bilan")
				.withGender(Gender.MALE)
				.withDateOfBirth(dateOfBirth.getTime())
				.withLastUpdated(new Date());

		assertNull(student.getRollNumber());

		this.updatingGatewayFlowInput.send(MessageBuilder.withPayload(student).build());

		Message<?> receive = this.persistResults.receive(10_000);
		assertNotNull(receive);

		StudentDomain mergedStudent = (StudentDomain) receive.getPayload();
		assertEquals(student.getFirstName(), mergedStudent.getFirstName());
		assertNotNull(mergedStudent.getRollNumber());
		assertNull(student.getRollNumber());
	}

	@Test
	public void testRetrievingGatewayFlow() {
		this.retrievingGatewayFlowInput.send(MessageBuilder.withPayload(2L).build());
		Message<?> receive = this.retrieveResults.receive(10_000);
		assertNotNull(receive);
		assertThat(receive.getPayload(), instanceOf(StudentDomain.class));
		StudentDomain student = (StudentDomain) receive.getPayload();
		assertEquals("First Two", student.getFirstName());
		assertEquals(Gender.FEMALE, student.getGender());
	}


	@Configuration
	@Import({ DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
			IntegrationAutoConfiguration.class })
	@EntityScan(basePackageClasses = StudentDomain.class)
	public static class ContextConfiguration {

		@Autowired
		private EntityManagerFactory entityManagerFactory;

		@Bean
		public IntegrationFlow pollingAdapterFlow() {
			return IntegrationFlows
					.from(Jpa.inboundAdapter(this.entityManagerFactory)
									.entityClass(StudentDomain.class)
									.maxResults(1)
									.expectSingleResult(true),
							e -> e.poller(p -> p.trigger(new OnlyOnceTrigger())))
					.channel(c -> c.queue("pollingResults"))
					.get();
		}

		@Bean
		public IntegrationFlow outboundAdapterFlow() {
			return f -> f
					.handle(Jpa.outboundAdapter(this.entityManagerFactory)
									.entityClass(StudentDomain.class)
									.persistMode(PersistMode.PERSIST),
							e -> e.transactional(true));
		}

		@Bean
		public IntegrationFlow updatingGatewayFlow() {
			return f -> f
					.handle(Jpa.updatingGateway(this.entityManagerFactory),
							e -> e.transactional(true))
					.channel(c -> c.queue("persistResults"));
		}

		@Bean
		public IntegrationFlow retrievingGatewayFlow() {
			return f -> f
					.handle(Jpa.retrievingGateway(this.entityManagerFactory)
							.jpaQuery("from Student s where s.id = :id")
							.expectSingleResult(true)
							.parameterExpression("id", "payload"))
					.channel(c -> c.queue("retrieveResults"));
		}

	}

}
