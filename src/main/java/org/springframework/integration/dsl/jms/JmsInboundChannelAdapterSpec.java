/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.dsl.jms;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import org.springframework.integration.dsl.core.MessageSourceSpec;
import org.springframework.integration.dsl.support.Consumer;
import org.springframework.integration.jms.JmsDestinationPollingSource;
import org.springframework.integration.jms.JmsHeaderMapper;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.util.Assert;

/**
 * A {@link MessageSourceSpec} for a {@link JmsDestinationPollingSource}.
 *
 * @author Artem Bilan
 */
public class JmsInboundChannelAdapterSpec<S extends JmsInboundChannelAdapterSpec<S>>
		extends MessageSourceSpec<S, JmsDestinationPollingSource> {

	final JmsTemplateSpec jmsTemplateSpec = new JmsTemplateSpec();

	JmsInboundChannelAdapterSpec(JmsTemplate jmsTemplate) {
		this.target = new JmsDestinationPollingSource(jmsTemplate);
	}

	private JmsInboundChannelAdapterSpec(ConnectionFactory connectionFactory) {
		this.target = new JmsDestinationPollingSource(this.jmsTemplateSpec.connectionFactory(connectionFactory).get());
	}

	/**
	 * @param messageSelector the messageSelector.
	 * @return the spec.
	 * @see JmsDestinationPollingSource#setMessageSelector(String)
	 */
	public S messageSelector(String messageSelector) {
		this.target.setMessageSelector(messageSelector);
		return _this();
	}

	/**
	 * Configure a {@link JmsHeaderMapper} to map from JMS headers and properties to
	 * Spring Integration headers.
	 * @param headerMapper the headerMapper.
	 * @return the spec.
	 */
	public S headerMapper(JmsHeaderMapper headerMapper) {
		this.target.setHeaderMapper(headerMapper);
		return _this();
	}

	/**
	 * Configure the destination from which to receive messages.
	 * @param destination the destination.
	 * @return the spec.
	 */
	public S destination(Destination destination) {
		this.target.setDestination(destination);
		return _this();
	}

	/**
	 * Configure the name of destination from which to receive messages.
	 * @param destination the destination.
	 * @return the spec.
	 */
	public S destination(String destination) {
		this.target.setDestinationName(destination);
		return _this();
	}

	@Override
	protected JmsDestinationPollingSource doGet() {
		throw new UnsupportedOperationException();
	}

	public static class JmsInboundChannelSpecTemplateAware extends
			JmsInboundChannelAdapterSpec<JmsInboundChannelSpecTemplateAware> {

		JmsInboundChannelSpecTemplateAware(ConnectionFactory connectionFactory) {
			super(connectionFactory);
		}

		/**
		 * Configure the channel adapter to use the template specification created by invoking the
		 * {@link Consumer} callback, passing in a {@link JmsTemplateSpec}.
		 * @param configurer the configurer.
		 * @return the spec.
		 */
		public JmsInboundChannelSpecTemplateAware configureJmsTemplate(Consumer<JmsTemplateSpec> configurer) {
			Assert.notNull(configurer);
			configurer.accept(this.jmsTemplateSpec);
			return _this();
		}

	}

}
