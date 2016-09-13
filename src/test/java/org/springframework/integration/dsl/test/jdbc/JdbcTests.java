/*
 * Copyright 2014-2016 the original author or authors.
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

package org.springframework.integration.dsl.test.jdbc;

import static org.junit.Assert.assertNotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artem Bilan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class JdbcTests {

	@Autowired
	@Qualifier("jdbcSplitter.input")
	private MessageChannel jdbcSplitterChannel;

	@Autowired
	private PollableChannel splitResultsChannel;

	@Test
	public void testJdbcSplitter() {
		this.jdbcSplitterChannel.send(new GenericMessage<>("foo"));

		for (int i = 0; i < 10; i++) {
			Message<?> result = this.splitResultsChannel.receive(1000);
			assertNotNull(result);
		}
	}

	@Configuration
	@Import(DataSourceAutoConfiguration.class)
	@EnableIntegration
	public static class ContextConfiguration {

		@Autowired
		private DataSource dataSource;

		@Bean
		public IntegrationFlow jdbcSplitter() {
			return f ->
					f.<String>split(this::iterator, e -> e.applySequence(false))
							.channel(c -> c.queue("splitResultsChannel"));
		}

		private ResultSetIterator<?> iterator(Object payload) {
			SQLExceptionTranslator exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(this.dataSource);
			String sql = "SELECT * from FOO";
			PreparedStatement preparedStatement = null;
			try {
				Connection connection = this.dataSource.getConnection();
				preparedStatement = connection.prepareStatement(sql);
				ResultSet resultSet = preparedStatement.executeQuery();
				return new ResultSetIterator<>(connection, resultSet, (rs, rowNum) ->
						new Foo(rs.getInt(1), rs.getString(2)));
			}
			catch (SQLException e) {
				throw exceptionTranslator.translate("PreparedStatement", sql, e);
			}
		}
	}

	private static final class Foo {

		@SuppressWarnings("unused")
		private final int id;

		@SuppressWarnings("unused")
		private final String name;

		private Foo(int id, String name) {
			this.id = id;
			this.name = name;
		}

	}

	private static final class ResultSetIterator<T> implements Iterator<T> {

		private final Connection connection;

		private final ResultSet rs;

		private final RowMapper<T> rowMapper;

		private ResultSetIterator(Connection connection, ResultSet rs, RowMapper<T> rowMapper) {
			this.connection = connection;
			this.rs = rs;
			this.rowMapper = rowMapper;
		}

		@Override
		public boolean hasNext() {
			try {
				boolean hasNext = !this.rs.isLast();
				if (!hasNext) {
					JdbcUtils.closeResultSet(this.rs);
					JdbcUtils.closeConnection(this.connection);
				}
				return hasNext;
			}
			catch (SQLException e) {
				throw new InvalidResultSetAccessException(e);
			}
		}

		@Override
		public T next() {
			try {
				this.rs.next();
				return this.rowMapper.mapRow(this.rs, this.rs.getRow());
			}
			catch (SQLException e) {
				throw new InvalidResultSetAccessException(e);
			}
		}

	}

}
