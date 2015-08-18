/*
 * Copyright 2014-2015 the original author or authors.
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

package org.springframework.integration.dsl.file;

import java.io.File;
import java.util.Comparator;

import org.springframework.integration.dsl.core.MessageSourceSpec;
import org.springframework.integration.file.DirectoryScanner;
import org.springframework.integration.file.FileLocker;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.filters.IgnoreHiddenFileListFilter;
import org.springframework.integration.file.filters.RegexPatternFileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.integration.file.locking.NioFileLocker;
import org.springframework.util.Assert;

/**
 * A {@link MessageSourceSpec} for a {@link FileReadingMessageSource}.
 *
 * @author Artem Bilan
 */
public class FileInboundChannelAdapterSpec
		extends MessageSourceSpec<FileInboundChannelAdapterSpec, FileReadingMessageSource> {

	private CompositeFileListFilter<File> filter;

	private FileLocker locker;

	FileInboundChannelAdapterSpec() {
		this.target = new FileReadingMessageSource();
	}

	FileInboundChannelAdapterSpec(Comparator<File> receptionOrderComparator) {
		this.target = new FileReadingMessageSource(receptionOrderComparator);
	}

	/**
	 * @param directory the directory.
	 * @return the spec.
	 * @see FileReadingMessageSource#setDirectory(File)
	 */
	FileInboundChannelAdapterSpec directory(File directory) {
		this.target.setDirectory(directory);
		return _this();
	}

	/**
	 * @param scanner the scanner.
	 * @return the spec.
	 * @see FileReadingMessageSource#setScanner(DirectoryScanner)
	 */
	public FileInboundChannelAdapterSpec scanner(DirectoryScanner scanner) {
		this.target.setScanner(scanner);
		return _this();
	}

	/**
	 * @param autoCreateDirectory the autoCreateDirectory.
	 * @return the spec.
	 * @see FileReadingMessageSource#setAutoCreateDirectory(boolean)
	 */
	public FileInboundChannelAdapterSpec autoCreateDirectory(boolean autoCreateDirectory) {
		this.target.setAutoCreateDirectory(autoCreateDirectory);
		return _this();
	}

	/**
	 * Configure the filter.
	 * @param filter the filter.
	 * @return the spec.
	 * @see FileReadingMessageSource#setFilter(FileListFilter)
	 */
	public FileInboundChannelAdapterSpec filter(FileListFilter<File> filter) {
		if (this.filter == null) {
			if (filter instanceof CompositeFileListFilter) {
				this.filter = (CompositeFileListFilter<File>) filter;
			}
			else {
				this.filter = new CompositeFileListFilter<File>();
				this.filter.addFilter(filter);
			}
			this.target.setFilter(this.filter);
		}
		else {
			this.filter.addFilter(filter);
		}
		return _this();
	}

	/**
	 * Configure the filter; if {@code preventDuplicates == true}, the filter is combined with an
	 * {@link AcceptOnceFileListFilter} in a {@link CompositeFileListFilter}.
	 * @param filter the filter.
	 * @param preventDuplicates true to prevent duplicates.
	 * @return the spec.
	 * @see CompositeFileListFilter
	 * @see AcceptOnceFileListFilter
	 * @deprecated since 1.1 in favor of the bunch of methods usage.
	 */
	@Deprecated
	public FileInboundChannelAdapterSpec filter(FileListFilter<File> filter, boolean preventDuplicates) {
		if (preventDuplicates) {
			filter(new AcceptOnceFileListFilter<File>());
		}
		return filter(filter);
	}

	/**
	 * Configure an {@link AcceptOnceFileListFilter} if {@code preventDuplicates == true},
	 * otherwise nothing changed.
	 * @param preventDuplicates true to configure an {@link AcceptOnceFileListFilter}.
	 * @return the spec.
	 * @see #preventDuplicates
	 * @deprecated since 1.1 in favor of the bunch of methods usage.
	 */
	@Deprecated
	public FileInboundChannelAdapterSpec preventDuplicatesFilter(boolean preventDuplicates) {
		if (preventDuplicates) {
			return filter(new AcceptOnceFileListFilter<File>());
		}
		return _this();
	}

	/**
	 * Configure an {@link AcceptOnceFileListFilter}.
	 * @return the spec.
	 * @since 1.1
	 */
	public FileInboundChannelAdapterSpec preventDuplicates() {
		return filter(new AcceptOnceFileListFilter<File>());
	}

	/**
	 * Configure an {@link IgnoreHiddenFileListFilter}.
	 * @return the spec.
	 * @since 1.1
	 */
	public FileInboundChannelAdapterSpec ignoreHidden() {
		return filter(new IgnoreHiddenFileListFilter());
	}

	/**
	 * Configure a {@link SimplePatternFileListFilter}.
	 * @param pattern The pattern.
	 * @return the spec.
	 * @see FileReadingMessageSource#setFilter(FileListFilter)
	 * @see #filter(FileListFilter, boolean)
	 */
	public FileInboundChannelAdapterSpec patternFilter(String pattern) {
		return filter(new SimplePatternFileListFilter(pattern));
	}

	/**
	 * Configure a {@link SimplePatternFileListFilter}.
	 * @param pattern The pattern.
	 * @param preventDuplicates the preventDuplicates.
	 * @return the spec.
	 * @see FileReadingMessageSource#setFilter(FileListFilter)
	 * @see #filter(FileListFilter, boolean)
	 * @see #preventDuplicates
	 * @deprecated since 1.1 in favor of the bunch of methods usage.
	 */
	@Deprecated
	public FileInboundChannelAdapterSpec patternFilter(String pattern, boolean preventDuplicates) {
		if (preventDuplicates) {
			preventDuplicates();
		}
		return patternFilter(pattern);
	}

	/**
	 * Configure a {@link RegexPatternFileListFilter}.
	 * @param regex The regex.
	 * @return the spec.
	 * @see FileReadingMessageSource#setFilter(FileListFilter)
	 * @see #filter(FileListFilter, boolean)
	 */
	public FileInboundChannelAdapterSpec regexFilter(String regex) {
		return filter(new RegexPatternFileListFilter(regex));
	}

	/**
	 * Configure a {@link RegexPatternFileListFilter}.
	 * @param regex The regex.
	 * @param preventDuplicates the preventDuplicates.
	 * @return the spec.
	 * @see FileReadingMessageSource#setFilter(FileListFilter)
	 * @see #filter(FileListFilter, boolean)
	 * @see #preventDuplicates
	 * @deprecated since 1.1 in favor of the bunch of methods usage.
	 */
	@Deprecated
	public FileInboundChannelAdapterSpec regexFilter(String regex, boolean preventDuplicates) {
		if (preventDuplicates) {
			preventDuplicates();
		}
		return regexFilter(regex);
	}

	/**
	 * @param locker the locker.
	 * @return the spec.
	 * @see FileReadingMessageSource#setLocker(FileLocker)
	 */
	public FileInboundChannelAdapterSpec locker(FileLocker locker) {
		Assert.isNull(this.locker,
				"The 'locker' (" + this.locker + ") is already configured for the FileReadingMessageSource");
		this.locker = locker;
		this.target.setLocker(locker);
		return _this();
	}

	/**
	 * Configure an {@link NioFileLocker}.
	 * @return the spec.
	 * @see #locker(FileLocker)
	 */
	public FileInboundChannelAdapterSpec nioLocker() {
		return locker(new NioFileLocker());
	}

	/**
	 * @param scanEachPoll the scanEachPoll.
	 * @return the spec.
	 * @see FileReadingMessageSource#setScanEachPoll(boolean)
	 */
	public FileInboundChannelAdapterSpec scanEachPoll(boolean scanEachPoll) {
		this.target.setScanEachPoll(scanEachPoll);
		return _this();
	}

	@Override
	protected FileReadingMessageSource doGet() {
		throw new UnsupportedOperationException();
	}

}
