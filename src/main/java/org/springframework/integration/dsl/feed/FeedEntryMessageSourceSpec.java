/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.integration.dsl.feed;

import java.net.URL;

import org.springframework.integration.dsl.core.MessageSourceSpec;
import org.springframework.integration.feed.inbound.FeedEntryMessageSource;
import org.springframework.integration.metadata.MetadataStore;

/**
 * A {@link MessageSourceSpec} for a {@link FeedEntryMessageSource}.
 * @author Artem Bilan
 * @since 1.1
 */
@SuppressWarnings("deprecation")
public class FeedEntryMessageSourceSpec extends MessageSourceSpec<FeedEntryMessageSourceSpec, FeedEntryMessageSource> {

	private final URL feedUrl;

	private final String metadataKey;

	private com.rometools.fetcher.FeedFetcher feedFetcher;

	private MetadataStore metadataStore;

	FeedEntryMessageSourceSpec(URL feedUrl, String metadataKey) {
		this.feedUrl = feedUrl;
		this.metadataKey = metadataKey;
	}

	public FeedEntryMessageSourceSpec feedFetcher(com.rometools.fetcher.FeedFetcher feedFetcher) {
		this.feedFetcher = feedFetcher;
		return this;
	}

	public FeedEntryMessageSourceSpec metadataStore(MetadataStore metadataStore) {
		this.metadataStore = metadataStore;
		return this;
	}

	@Override
	protected FeedEntryMessageSource doGet() {
		FeedEntryMessageSource feedEntryMessageSource;
		if (this.feedFetcher == null) {
			feedEntryMessageSource = new FeedEntryMessageSource(this.feedUrl, this.metadataKey);
		}
		else {
			feedEntryMessageSource = new FeedEntryMessageSource(this.feedUrl, this.metadataKey, this.feedFetcher);
		}
		if (this.metadataStore != null) {
			feedEntryMessageSource.setMetadataStore(this.metadataStore);
		}
		return feedEntryMessageSource;
	}

}
