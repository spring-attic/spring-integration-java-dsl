/*
 * Copyright 2015 the original author or authors
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

package org.springframework.integration.dsl.scripting;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.core.ComponentsRegistration;
import org.springframework.integration.dsl.core.MessageSourceSpec;
import org.springframework.integration.dsl.support.MapBuilder;
import org.springframework.integration.dsl.support.MessageProcessorMessageSource;
import org.springframework.integration.scripting.ScriptVariableGenerator;

/**
 * @author Artem Bilan
 * @since 1.1
 */
public class ScriptMessageSourceSpec extends MessageSourceSpec<ScriptMessageSourceSpec, MessageSource<?>>
		implements ComponentsRegistration {

	private final ScriptSpec delegate;

	public ScriptMessageSourceSpec(Resource scriptResource) {
		this.delegate = new ScriptSpec(scriptResource);
	}

	public ScriptMessageSourceSpec(String scriptLocation) {
		this.delegate = new ScriptSpec(scriptLocation);
	}

	public ScriptMessageSourceSpec lang(String lang) {
		this.delegate.lang(lang);
		return this;
	}

	public ScriptMessageSourceSpec variableGenerator(ScriptVariableGenerator variableGenerator) {
		this.delegate.variableGenerator(variableGenerator);
		return this;
	}

	public ScriptMessageSourceSpec variables(MapBuilder<?, String, Object> variables) {
		this.delegate.variables(variables);
		return this;
	}

	public ScriptMessageSourceSpec variable(String name, Object value) {
		this.delegate.variable(name, value);
		return this;
	}

	public ScriptMessageSourceSpec refreshCheckDelay(long refreshCheckDelay) {
		this.delegate.refreshCheckDelay(refreshCheckDelay);
		return this;
	}

	public ScriptMessageSourceSpec variables(Map<String, Object> variables) {
		this.delegate.variables(variables);
		return this;
	}

	@Override
	protected MessageSource<?> doGet() {
		return new MessageProcessorMessageSource(this.delegate.get());
	}

	@Override
	public Collection<Object> getComponentsToRegister() {
		return Arrays.<Object>asList(this.delegate.get());
	}
	
}
