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
import java.util.Comparator;

import org.springframework.expression.Expression;
import org.springframework.integration.dsl.support.Function;
import org.springframework.messaging.Message;

/**
 * The Spring Integration File components Factory.
 *
 * @author Artem Bilan
 */
public abstract class Files {

	public static FileInboundChannelAdapterSpec inboundAdapter(File directory) {
		return inboundAdapter(directory, null);
	}

	public static FileInboundChannelAdapterSpec inboundAdapter(File directory,
			Comparator<File> receptionOrderComparator) {
		return new FileInboundChannelAdapterSpec(receptionOrderComparator).directory(directory);
	}

	public static FileWritingMessageHandlerSpec outboundAdapter(File destinationDirectory) {
		return new FileWritingMessageHandlerSpec(destinationDirectory).expectReply(false);
	}

	public static FileWritingMessageHandlerSpec outboundAdapter(String directoryExpression) {
		return new FileWritingMessageHandlerSpec(directoryExpression).expectReply(false);
	}

	/**
	 * @param directoryExpression an expression to evaluate the target directory.
	 * @return the FileWritingMessageHandlerSpec instance.
	 * @since 1.1.1
	 */
	public static FileWritingMessageHandlerSpec outboundAdapter(Expression directoryExpression) {
		return new FileWritingMessageHandlerSpec(directoryExpression).expectReply(false);
	}

	public static <P> FileWritingMessageHandlerSpec outboundAdapter(Function<Message<P>, ?> directoryFunction) {
		return new FileWritingMessageHandlerSpec(directoryFunction).expectReply(false);
	}

	public static FileWritingMessageHandlerSpec outboundGateway(File destinationDirectory) {
		return new FileWritingMessageHandlerSpec(destinationDirectory).expectReply(true);
	}

	public static FileWritingMessageHandlerSpec outboundGateway(String directoryExpression) {
		return new FileWritingMessageHandlerSpec(directoryExpression).expectReply(true);
	}

	/**
	 * @param directoryExpression an expression to evaluate the target directory.
	 * @return the FileWritingMessageHandlerSpec instance.
	 * @since 1.1.1
	 */
	public static FileWritingMessageHandlerSpec outboundGateway(Expression directoryExpression) {
		return new FileWritingMessageHandlerSpec(directoryExpression).expectReply(true);
	}

	public static <P> FileWritingMessageHandlerSpec outboundGateway(Function<Message<P>, ?> directoryFunction) {
		return new FileWritingMessageHandlerSpec(directoryFunction).expectReply(true);
	}

	public static TailAdapterSpec tailAdapter(File file) {
		return new TailAdapterSpec().file(file);
	}

	/**
	 * The {@link FileSplitterSpec} builder factory method with default arguments.
	 * @return the {@link FileSplitterSpec} builder.
	 * @since 1.1
	 */
	public static FileSplitterSpec splitter() {
		return splitter(true);
	}

	/**
	 * The {@link FileSplitterSpec} builder factory method with {@code iterator} flag specified.
	 * @param iterator the {@code boolean} flag to specify the {@code iterator} mode or not.
	 * @return the {@link FileSplitterSpec} builder.
	 * @since 1.1
	 */
	public static FileSplitterSpec splitter(boolean iterator) {
		return splitter(iterator, false);
	}

	/**
	 * The {@link FileSplitterSpec} builder factory method with {@code iterator} and {@code markers}
	 * flags specified.
	 * @param iterator the {@code boolean} flag to specify the {@code iterator} mode or not.
	 * @param markers true to emit start of file/end of file marker messages before/after the data.
	 * @return the {@link FileSplitterSpec} builder.
	 * @since 1.1
	 */
	public static FileSplitterSpec splitter(boolean iterator, boolean markers) {
		return new FileSplitterSpec(iterator, markers);
	}

}
