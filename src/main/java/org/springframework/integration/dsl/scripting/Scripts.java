/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.integration.dsl.scripting;

import org.springframework.core.io.Resource;

/**
 * The factory for Dynamic Language Scripts (Groovy, Ruby, Python, JavaScript etc.)
 *
 * @author Artem Bilan
 * @since 1.1
 */
public abstract class Scripts {

	/**
	 * The factory method to produce {@link ScriptSpec} based on the {@link Resource}.
	 * The {@link Resource} must represent the real file and can be injected like:
	 * <pre class="code">
	 *  &#064;Value("com/my/project/scripts/FilterScript.groovy")
	 *  private Resource filterScript;
	 * </pre>
	 * @param scriptResource the script file {@link Resource}
	 * @return the ScriptSpec instance
	 */
	public static ScriptSpec script(Resource scriptResource) {
		return new ScriptSpec(scriptResource);
	}

	/**
	 * The factory method to produce {@link ScriptSpec} based on the script file location.
	 * @param scriptLocation the path to the script file.
	 *               {@code file:}, {@code ftp:}, {@code s3:} etc.
	 *               The {@code classpath:} can be omitted.
	 * @return the ScriptSpec instance
	 */
	public static ScriptSpec script(String scriptLocation) {
		return new ScriptSpec(scriptLocation);
	}

	private Scripts() {
	}

}
