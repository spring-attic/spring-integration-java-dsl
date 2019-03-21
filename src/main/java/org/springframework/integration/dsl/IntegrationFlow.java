/*
 * Copyright 2014-2015 the original author or authors.
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

package org.springframework.integration.dsl;

/**
 * The main Integration DSL abstraction.
 * <p>
 * The {@link StandardIntegrationFlow} implementation (produced by {@link IntegrationFlowBuilder})
 * represents a container for the integration components, which will be registered
 * in the application context. Typically is used as {@code &#64;Bean} definition:
 * <pre class="code">
 *  &#64;Bean
 *  public IntegrationFlow fileReadingFlow() {
 *      return IntegrationFlows
 *             .from(s -&gt; s.file(tmpDir.getRoot()), e -&gt; e.poller(Pollers.fixedDelay(100)))
 *             .transform(Transformers.fileToString())
 *             .channel(MessageChannels.queue("fileReadingResultChannel"))
 *             .get();
 *  }
 * </pre>
 * <p>
 * Also this interface can be implemented directly to encapsulate the integration logic
 * in the target service:
 * <pre class="code">
 *  &#64;Component
 *  public class MyFlow implements IntegrationFlow {
 *
 *        &#64;Override
 *        public void configure(IntegrationFlowDefinition&lt;?&gt; f) {
 *                f.&lt;String, String&gt;transform(String::toUpperCase);
 *        }
 *
 *  }
 * </pre>
 *
 * @author Artem Bilan
 *
 * @see IntegrationFlowBuilder
 * @see StandardIntegrationFlow
 * @see IntegrationFlowAdapter
 */
public interface IntegrationFlow {

	void configure(IntegrationFlowDefinition<?> flow);

}
