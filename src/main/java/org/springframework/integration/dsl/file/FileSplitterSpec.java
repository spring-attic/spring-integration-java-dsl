/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.integration.dsl.file;

import java.nio.charset.Charset;

import org.springframework.integration.dsl.core.MessageHandlerSpec;
import org.springframework.integration.file.splitter.FileSplitter;

/**
 * The {@link MessageHandlerSpec} for the {@link FileSplitter}.
 * @author Artem Bilan
 * @since 1.1
 */
public class FileSplitterSpec extends MessageHandlerSpec<FileSplitterSpec, FileSplitter> {

	private final boolean iterator;

	private boolean markers;

	private boolean markersJson;

	private Charset charset;

	private boolean applySequence;

	FileSplitterSpec() {
		this(true);
	}

	FileSplitterSpec(boolean iterator) {
		this(iterator, false);
	}

	FileSplitterSpec(boolean iterator, boolean markers) {
		this.iterator = iterator;
		this.markers = markers;
	}

	public FileSplitterSpec charset(String charset) {
		return charset(Charset.forName(charset));
	}

	public FileSplitterSpec charset(Charset charset) {
		this.charset = charset;
		return this;
	}

	/**
	 * Specify if {@link FileSplitter} should emit
	 * {@link org.springframework.integration.file.splitter.FileSplitter.FileMarker}s
	 * Defaults to {@code false}.
	 * @return the FileSplitterSpec
	 * @since 1.2
	 * @see FileSplitter
	 */
	public FileSplitterSpec markers() {
		return markers(false);
	}

	/**
	 * Specify if {@link FileSplitter} should emit
	 * {@link org.springframework.integration.file.splitter.FileSplitter.FileMarker}s
	 * and if they should be converted to the JSON string representation.
	 * Defaults to {@code false} for markers and {@code false} for markersJson.
	 * @param asJson the asJson flag to use.
	 * @return the FileSplitterSpec
	 * @since 1.2
	 * @see FileSplitter
	 */
	public FileSplitterSpec markers(boolean asJson) {
		this.markers = true;
		this.markersJson = asJson;
		return this;
	}

	/**
	 * A {@code boolean} flag to indicate if {@code sequenceDetails} should be
	 * applied for messages based on the lines from file.
	 * Defaults to {@code false}.
	 * @param applySequence the applySequence flag to use.
	 * @return the FileSplitterSpec
	 * @since 1.2
	 * @see org.springframework.integration.splitter.AbstractMessageSplitter#setApplySequence(boolean)
	 */
	public FileSplitterSpec applySequence(boolean applySequence) {
		this.applySequence = applySequence;
		return this;
	}


	@Override
	protected FileSplitter doGet() {
		FileSplitter fileSplitter = new FileSplitter(this.iterator, this.markers, this.markersJson);
		fileSplitter.setApplySequence(this.applySequence);
		fileSplitter.setCharset(this.charset);
		return fileSplitter;
	}

}
