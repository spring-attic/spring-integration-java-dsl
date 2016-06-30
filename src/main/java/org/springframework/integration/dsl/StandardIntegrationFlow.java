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

package org.springframework.integration.dsl;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.Lifecycle;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.dsl.core.EndpointSpec;
import org.springframework.integration.endpoint.AbstractEndpoint;

/**
* @author Artem Bilan
*/
public class StandardIntegrationFlow implements IntegrationFlow, SmartLifecycle {

	private final Set<Object> integrationComponents;

	private final Set<Lifecycle> lifecycles = new HashSet<Lifecycle>();

	private boolean registerComponents = true;

	private boolean running;

	@SuppressWarnings("unchecked")
	StandardIntegrationFlow(Set<Object> integrationComponents) {
		this.integrationComponents = new LinkedHashSet<Object>(integrationComponents);
		for (Object integrationComponent : integrationComponents) {
			if (integrationComponent instanceof AbstractEndpoint) {
				this.lifecycles.add((Lifecycle) integrationComponent);
			}
			else if (integrationComponent instanceof EndpointSpec) {
				BeanNameAware endpoint = ((EndpointSpec<?, BeanNameAware, ?>) integrationComponent).get().getT1();
				this.lifecycles.add((Lifecycle) endpoint);
			}
		}
	}

	//TODO Figure out some custom DestinationResolver when we don't register singletons
	/*public void setRegisterComponents(boolean registerComponents) {
		this.registerComponents = registerComponents;
	}*/

	public boolean isRegisterComponents() {
		return registerComponents;
	}

	public Set<Object> getIntegrationComponents() {
		return integrationComponents;
	}

	@Override
	public void configure(IntegrationFlowDefinition<?> flow) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void start() {
		if (!this.running) {
			for (Lifecycle lifecycle : this.lifecycles) {
				lifecycle.start();
			}
			this.running = true;
		}
	}

	@Override
	public void stop(Runnable callback) {
		stop();
		if (callback != null) {
			callback.run();
		}
	}

	@Override
	public void stop() {
		if (!this.running) {
			for (Lifecycle lifecycle : this.lifecycles) {
				lifecycle.stop();
			}
			this.running = false;
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public boolean isAutoStartup() {
		return false;
	}

	@Override
	public int getPhase() {
		return 0;
	}

}
