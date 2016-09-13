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

package org.springframework.integration.dsl.support.tuple;

/**
 * A tuple that holds a single value.
 *
 * @param <T1> The type held by this tuple
 *
 * @author Jon Brisbin
 * @author Artem Bilan
 */
public class Tuple1<T1> extends Tuple {

	private static final long serialVersionUID = -1467756857377152573L;

	protected final T1 t1;

	Tuple1(int size, T1 t1) {
		super(1);
		this.t1 = t1;
	}

	/**
	 * Type-safe way to get the first object of this {@link Tuple}.
	 * @return The first object, cast to the correct type.
	 */
	public T1 getT1() {
		return this.t1;
	}

	@Override
	public Object get(int index) {
		switch (index) {
			case 0:
				return this.t1;
			default:
				return null;
		}
	}

	@Override
	public Object[] toArray() {
		return new Object[] {this.t1};
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}

		Tuple1<?> tuple1 = (Tuple1<?>) o;

		return this.t1 != null ? this.t1.equals(tuple1.t1) : tuple1.t1 == null;

	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (this.t1 != null ? this.t1.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return super.toString() +
				(this.t1 != null ? "," + this.t1.toString() : "");
	}

}
