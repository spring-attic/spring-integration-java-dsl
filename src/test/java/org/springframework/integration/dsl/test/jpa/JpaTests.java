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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.junit.Assert;
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
@SpringBootTest(properties = "spring.datasource.initialize=false", webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class JpaTests {

	@Autowired
	private PollableChannel pollingResults;

	@Autowired
	@Qualifier("outboundAdapterFlow.input")
	private MessageChannel outboundAdapterFlowInput;

	@Autowired
	private DataSource dataSource;

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
		Assert.assertNotNull(results1);
		Assert.assertTrue(results1.size() == 3);

		Calendar dateOfBirth = Calendar.getInstance();
		dateOfBirth.set(1981, 9, 27);

		StudentDomain student = new StudentDomain()
				.withFirstName("Artem")
				.withLastName("Bilan")
				.withGender(Gender.MALE)
				.withDateOfBirth(dateOfBirth.getTime())
				.withLastUpdated(new Date());

		Assert.assertNull(student.getRollNumber());

		this.outboundAdapterFlowInput.send(MessageBuilder.withPayload(student).build());

		List<?> results2 = jdbcTemplate.queryForList("Select * from Student");
		Assert.assertNotNull(results2);
		Assert.assertTrue(results2.size() == 4);

		Assert.assertNotNull(student.getRollNumber());
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

	}

}
