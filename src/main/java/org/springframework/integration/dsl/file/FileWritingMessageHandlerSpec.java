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

package org.springframework.integration.dsl.file;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import org.springframework.expression.Expression;
import org.springframework.integration.dsl.core.ComponentsRegistration;
import org.springframework.integration.dsl.core.MessageHandlerSpec;
import org.springframework.integration.dsl.support.Function;
import org.springframework.integration.dsl.support.FunctionExpression;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * @author Artem Bilan
 */
public class FileWritingMessageHandlerSpec
		extends MessageHandlerSpec<FileWritingMessageHandlerSpec, FileWritingMessageHandler>
		implements ComponentsRegistration {

	private FileNameGenerator fileNameGenerator;

	private DefaultFileNameGenerator defaultFileNameGenerator;

	FileWritingMessageHandlerSpec(File destinationDirectory) {
		this.target = new FileWritingMessageHandler(destinationDirectory);
	}

	FileWritingMessageHandlerSpec(String directoryExpression) {
		this(PARSER.parseExpression(directoryExpression));
	}

	<P> FileWritingMessageHandlerSpec(Function<Message<P>, ?> directoryFunction) {
		this(new FunctionExpression<Message<P>>(directoryFunction));
	}

	FileWritingMessageHandlerSpec(Expression directoryExpression) {
		this.target = new FileWritingMessageHandler(directoryExpression);
	}

	FileWritingMessageHandlerSpec expectReply(boolean expectReply) {
		this.target.setExpectReply(expectReply);
		if (expectReply) {
			this.target.setRequiresReply(true);
		}
		return _this();
	}

	public FileWritingMessageHandlerSpec autoCreateDirectory(boolean autoCreateDirectory) {
		this.target.setAutoCreateDirectory(autoCreateDirectory);
		return _this();
	}

	public FileWritingMessageHandlerSpec temporaryFileSuffix(String temporaryFileSuffix) {
		this.target.setTemporaryFileSuffix(temporaryFileSuffix);
		return _this();
	}

	public FileWritingMessageHandlerSpec fileExistsMode(FileExistsMode fileExistsMode) {
		this.target.setFileExistsMode(fileExistsMode);
		return _this();
	}

	public FileWritingMessageHandlerSpec fileNameGenerator(FileNameGenerator fileNameGenerator) {
		this.fileNameGenerator = fileNameGenerator;
		this.target.setFileNameGenerator(fileNameGenerator);
		return _this();
	}

	public FileWritingMessageHandlerSpec fileNameExpression(String fileNameExpression) {
		Assert.isNull(this.fileNameGenerator,
				"'fileNameGenerator' and 'fileNameGeneratorExpression' are mutually exclusive.");
		this.defaultFileNameGenerator = new DefaultFileNameGenerator();
		this.defaultFileNameGenerator.setExpression(fileNameExpression);
		return fileNameGenerator(this.defaultFileNameGenerator);
	}


	public FileWritingMessageHandlerSpec deleteSourceFiles(boolean deleteSourceFiles) {
		this.target.setDeleteSourceFiles(deleteSourceFiles);
		return _this();
	}

	public FileWritingMessageHandlerSpec charset(String charset) {
		this.target.setCharset(charset);
		return _this();
	}

	/**
	 * @param appendNewLine true if a new-line should be written to the file after payload is written.
	 * @return the spec.
	 * @since 1.1.1
	 * @see FileWritingMessageHandler#setAppendNewLine(boolean)
	 */
	public FileWritingMessageHandlerSpec appendNewLine(boolean appendNewLine) {
		this.target.setAppendNewLine(appendNewLine);
		return this;
	}

	@Override
	public Collection<Object> getComponentsToRegister() {
		if (this.defaultFileNameGenerator != null) {
			return Collections.<Object>singletonList(this.defaultFileNameGenerator);
		}
		return null;
	}

	@Override
	protected FileWritingMessageHandler doGet() {
		throw new UnsupportedOperationException();
	}

}
