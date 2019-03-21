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
import org.springframework.integration.dsl.core.MessageSourceSpec;
import org.springframework.integration.dsl.support.Function;
import org.springframework.integration.dsl.support.FunctionExpression;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizer;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizingMessageSource;

/**
 * A {@link MessageSourceSpec} for an {@link AbstractInboundFileSynchronizingMessageSource}.
 *
 * @author Artem Bilan
 */
public abstract class RemoteFileInboundChannelAdapterSpec<F, S extends RemoteFileInboundChannelAdapterSpec<F, S, MS>,
		MS extends AbstractInboundFileSynchronizingMessageSource<F>>
		extends MessageSourceSpec<S, MS> implements ComponentsRegistration {

	protected final AbstractInboundFileSynchronizer<F> synchronizer;

	private CompositeFileListFilter<F> filter;

	protected RemoteFileInboundChannelAdapterSpec(AbstractInboundFileSynchronizer<F> synchronizer) {
		this.synchronizer = synchronizer;
	}

	/**
	 * Configure whether the local directory should be created by the adapter.
	 * @param autoCreateLocalDirectory the autoCreateLocalDirectory
	 * @return the spec.
	 */
	public S autoCreateLocalDirectory(boolean autoCreateLocalDirectory) {
		this.target.setAutoCreateLocalDirectory(autoCreateLocalDirectory);
		return _this();
	}

	/**
	 * Configure the local directory to copy files to.
	 * @param localDirectory the localDirectory.
	 * @return the spec.
	 */
	public S localDirectory(File localDirectory) {
		this.target.setLocalDirectory(localDirectory);
		return _this();
	}

	/**
	 * @param localFileListFilter the localFileListFilter.
	 * @return the spec.
	 * @see AbstractInboundFileSynchronizingMessageSource#setLocalFilter(FileListFilter)
	 */
	public S localFilter(FileListFilter<File> localFileListFilter) {
		this.target.setLocalFilter(localFileListFilter);
		return _this();
	}

	/**
	 * Configure the file name path separator used by the remote system. Defaults to '/'.
	 * @param remoteFileSeparator the remoteFileSeparator.
	 * @return the spec.
	 */
	public S remoteFileSeparator(String remoteFileSeparator) {
		this.synchronizer.setRemoteFileSeparator(remoteFileSeparator);
		return _this();
	}

	/**
	 * Configure a SpEL expression to generate the local file name; the root object for
	 * the evaluation is the remote file name.
	 * @param localFilenameExpression the localFilenameExpression.
	 * @return the spec.
	 */
	public S localFilenameExpression(String localFilenameExpression) {
		return localFilenameExpression(PARSER.parseExpression(localFilenameExpression));
	}

	/**
	 * Configure a {@link Function} to be invoked to generate the local file name;
	 * argument passed to the {@code apply} method is the remote file name.
	 * @param localFilenameFunction the localFilenameFunction.
	 * @return the spec.
	 * @see FunctionExpression
	 */
	public S localFilename(Function<String, String> localFilenameFunction) {
		return localFilenameExpression(new FunctionExpression<String>(localFilenameFunction));
	}

	/**
	 * Configure a SpEL expression to generate the local file name; the root object for
	 * the evaluation is the remote file name.
	 * @param localFilenameExpression the localFilenameExpression.
	 * @return the spec.
	 * @since 1.1.1
	 */
	public S localFilenameExpression(Expression localFilenameExpression) {
		this.synchronizer.setLocalFilenameGeneratorExpression(localFilenameExpression);
		return _this();
	}


	/**
	 * Configure a suffix to temporarily apply to the local filename; when copied the
	 * file is renamed to its final name. Default: '.writing'.
	 * @param temporaryFileSuffix the temporaryFileSuffix.
	 * @return the spec.
	 */
	public S temporaryFileSuffix(String temporaryFileSuffix) {
		this.synchronizer.setTemporaryFileSuffix(temporaryFileSuffix);
		return _this();
	}

	/**
	 * @param remoteDirectory the remoteDirectory.
	 * @return the spec.
	 * @see AbstractInboundFileSynchronizer#setRemoteDirectory(String)
	 */
	public S remoteDirectory(String remoteDirectory) {
		this.synchronizer.setRemoteDirectory(remoteDirectory);
		return _this();
	}

	/**
	 * Specify an expression that evaluates to the full path to the remote directory.
	 * @param remoteDirectoryExpression The remote directory expression.
	 * @return the spec.
	 * @since 1.1.1
	 */
	public S remoteDirectoryExpression(Expression remoteDirectoryExpression) {
		this.synchronizer.setRemoteDirectoryExpression(remoteDirectoryExpression);
		return _this();
	}

	/**
	 * Configure a {@link FileListFilter} to be applied to the remote files before
	 * copying them.
	 * @param filter the filter.
	 * @return the spec.
	 */
	public S filter(FileListFilter<F> filter) {
		if (this.filter == null) {
			if (filter instanceof CompositeFileListFilter) {
				this.filter = (CompositeFileListFilter<F>) filter;
			}
			else {
				this.filter = new CompositeFileListFilter<F>();
				this.filter.addFilter(filter);
			}
			this.synchronizer.setFilter(this.filter);
		}
		else {
			this.filter.addFilter(filter);
		}
		return _this();
	}

	/**
	 * Configure a simple pattern filter (e.g. '*.txt').
	 * @param pattern the pattern.
	 * @return the spec.
	 * @see #filter(FileListFilter)
	 */
	public abstract S patternFilter(String pattern);

	/**
	 * Configure a regex pattern filter (e.g. '[0-9].*.txt').
	 * @param regex the regex.
	 * @return the spec.
	 * @see #filter(FileListFilter)
	 */
	public abstract S regexFilter(String regex);

	public S deleteRemoteFiles(boolean deleteRemoteFiles) {
		this.synchronizer.setDeleteRemoteFiles(deleteRemoteFiles);
		return _this();
	}

	public S preserveTimestamp(boolean preserveTimestamp) {
		this.synchronizer.setPreserveTimestamp(preserveTimestamp);
		return _this();
	}

	@Override
	public Collection<Object> getComponentsToRegister() {
		return Collections.<Object>singletonList(this.synchronizer);
	}

	@Override
	protected MS doGet() {
		throw new UnsupportedOperationException();
	}

}
