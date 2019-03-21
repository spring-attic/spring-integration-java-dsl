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

package org.springframework.integration.dsl.support.tuple;

/**
 * A tuple that holds two values
 *
 * @param <T1> The type of the first value held by this tuple
 * @param <T2> The type of the second value held by this tuple
 *
 * @author Jon Brisbin
 */
public class Tuple2<T1, T2> extends Tuple1<T1> {

	private static final long serialVersionUID = -565933838909569191L;

	Tuple2(Object... values) {
		super(values);
	}

	/**
	 * Type-safe way to get the second object of this {@link Tuple}.
	 *
	 * @return The second object, cast to the correct type.
	 */
	@SuppressWarnings("unchecked")
	public T2 getT2() {
		return (T2) get(1);
	}

}
