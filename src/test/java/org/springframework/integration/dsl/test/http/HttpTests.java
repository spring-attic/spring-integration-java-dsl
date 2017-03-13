/*
 * Copyright 2015-2017 the original author or authors.
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

package org.springframework.integration.dsl.test.http;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessagingGateways;
import org.springframework.integration.dsl.http.Http;
import org.springframework.integration.security.channel.ChannelSecurityInterceptor;
import org.springframework.integration.security.channel.SecuredChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.access.vote.RoleVoter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @author Artem Bilan
 * @since 1.1
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HttpTests {

	@LocalServerPort
	private String port;

	@Test
	public void testHttpProxyFlow() {
		String result = new TestRestTemplate("guest", "guest")
				.getForObject("http://localhost:" + this.port + "/service/?name={name}", String.class, "foo");
		assertEquals("FOO", result);
	}


	@Configuration
	@ImportAutoConfiguration({PropertyPlaceholderAutoConfiguration.class, ServerPropertiesAutoConfiguration.class,
			EmbeddedServletContainerAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
			SecurityAutoConfiguration.class })
	@EnableIntegration
	@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
	public static class ContextConfiguration extends WebSecurityConfigurerAdapter {

		@Autowired
		private Environment environment;


		@Override
		protected void configure(AuthenticationManagerBuilder auth) throws Exception {
			auth.inMemoryAuthentication()
					.withUser("guest")
					.password("guest")
					.roles("ADMIN");
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.authorizeRequests()
					.anyRequest().hasRole("ADMIN")
					.and()
					.httpBasic()
					.and()
					.csrf().disable()
					.anonymous().disable();
		}

		@Bean
		@SecuredChannel(interceptor = "channelSecurityInterceptor", sendAccess = "ROLE_ADMIN")
		public MessageChannel transformSecuredChannel() {
			return new DirectChannel();
		}

		@Bean
		public IntegrationFlow httpInternalServiceFlow() {
			return IntegrationFlows
					.from(Http.inboundGateway("/service/internal")
							.requestMapping(r -> r.params("name"))
							.payloadExpression("#requestParams.name"))
					.channel(transformSecuredChannel())
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

		@Bean
		public AccessDecisionManager accessDecisionManager() {
			return new AffirmativeBased(Collections.singletonList(new RoleVoter()));
		}

		@Bean
		public ChannelSecurityInterceptor channelSecurityInterceptor(AuthenticationManager authenticationManager,
				AccessDecisionManager accessDecisionManager) {
			ChannelSecurityInterceptor channelSecurityInterceptor = new ChannelSecurityInterceptor();
			channelSecurityInterceptor.setAuthenticationManager(authenticationManager);
			channelSecurityInterceptor.setAccessDecisionManager(accessDecisionManager);
			return channelSecurityInterceptor;
		}

	}


}
