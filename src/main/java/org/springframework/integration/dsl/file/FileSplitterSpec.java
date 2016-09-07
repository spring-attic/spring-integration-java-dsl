/*
 * Copyright 2015-2016 the original author or authors
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

package org.springframework.integration.dsl.file;

import java.nio.charset.Charset;

import org.springframework.integration.dsl.core.MessageHandlerSpec;
import org.springframework.integration.file.splitter.FileSplitter;
import org.springframework.util.Assert;

/**
 * The {@link MessageHandlerSpec} for the {@link FileSplitter}.
 * @author Artem Bilan
 * @since 1.1
 */
public class FileSplitterSpec extends MessageHandlerSpec<FileSplitterSpec, FileSplitter> {

	FileSplitterSpec() {
		this(true);
	}

	FileSplitterSpec(boolean iterator) {
		this(iterator, false);
	}

	FileSplitterSpec(boolean iterator, boolean markers) {
		this.target = new FileSplitter(iterator, markers);
	}

	public FileSplitterSpec charset(String charset) {
		Assert.hasText(charset);
		return charset(Charset.forName(charset));
	}

	public FileSplitterSpec charset(Charset charset) {
		this.target.setCharset(charset);
		return _this();
	}

}
