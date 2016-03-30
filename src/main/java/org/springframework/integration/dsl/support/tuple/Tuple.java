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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * A {@literal Tuple} is an immutable {@link java.util.Collection} of objects,
 * each of which can be of an arbitrary type.
 *
 * @author Jon Brisbin
 * @author Stephane Maldini
 * @author Artem Bilan
 */
public class Tuple implements Iterable<Object>, Serializable {

	private static final long serialVersionUID = 8777121214502020842L;

	static final Object[] emptyArray       = new Object[0];
	static final Tuple    empty            = new Tuple(0);


	protected final int size;

	/**
	 * Creates a new {@code Tuple} that holds the given {@code values}.
	 * @param size The number of values to hold
	 */
	protected Tuple(int size) {
		this.size = size;
	}


	public Object get(int index) {
		return null;
	}

	/**
	 * Turn this {@literal Tuple} into a plain Object array.
	 * @return A new Object array.
	 */
	public Object[] toArray() {
		return emptyArray;
	}

	/**
	 * Turn this {@literal Tuple} into a plain Object list.
	 * @return A new Object list.
	 */
	public List<Object> toList() {
		return Arrays.asList(toArray());
	}

	/**
	 * Return the number of elements in this {@literal Tuple}.
	 * @return The size of this {@literal Tuple}.
	 */
	public int size() {
		return size;
	}

	@Override
	public Iterator<Object> iterator() {
		return toList().iterator();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Tuple tuple = (Tuple) o;

		return this.size == tuple.size;

	}

	@Override
	public int hashCode() {
		return this.size;
	}

}
