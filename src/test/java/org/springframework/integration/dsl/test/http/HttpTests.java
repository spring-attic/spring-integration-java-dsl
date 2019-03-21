/*
 * Copyright 2015 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.dsl.test.http;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessagingGateways;
import org.springframework.integration.dsl.http.Http;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @author Artem Bilan
 * @since 1.1
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration
@WebIntegrationTest
public class HttpTests {

	@Value("${local.server.port}")
	private String port;

	@Test
	public void testHttpProxyFlow() {
		String result = new RestTemplate()
				.getForObject("http://localhost:" + this.port + "/service/?name={name}", String.class, "foo");
		assertEquals("FOO", result);
	}


	@Configuration
	@Import({PropertyPlaceholderAutoConfiguration.class, ServerPropertiesAutoConfiguration.class,
			EmbeddedServletContainerAutoConfiguration.class, DispatcherServletAutoConfiguration.class})
	@EnableIntegration
	public static class ContextConfiguration {

		@Autowired
		private Environment environment;

		@Bean
		public IntegrationFlow httpInternalServiceFlow() {
			return IntegrationFlows
					.from(Http.inboundGateway("/service/internal")
							.requestMapping(r -> r.params("name"))
							.payloadExpression("#requestParams.name"))
					.<List<String>, String>transform(p -> p.get(0).toUpperCase())
					.get();
		}

		@Bean
		public IntegrationFlow httpProxyFlow() {
			return IntegrationFlows
					.from((MessagingGateways g) ->
							g.httpGateway("/service")
									.requestMapping(r -> r.params("name"))
									.payloadFunction(httpEntity ->
											((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
													.getRequest()
													.getQueryString()))
					.handleWithAdapter(a ->
							a.httpGateway(m ->
									String.format("http://localhost:%s/service/internal?%s",
											this.environment.getProperty("local.server.port"), m.getPayload()))
									.expectedResponseType(String.class))
					.get();
		}

	}


}
