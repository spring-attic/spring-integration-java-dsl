/*
 * Copyright 2015 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.dsl.http;

import org.springframework.integration.http.inbound.HttpRequestHandlingController;

/**
 * The {@link BaseHttpInboundEndpointSpec} implementation for the {@link HttpRequestHandlingController}.
 *
 * @author Artem Bilan
 *
 * @since 1.1
 * @see HttpRequestHandlingController
 */
public class HttpControllerEndpointSpec
		extends BaseHttpInboundEndpointSpec<HttpControllerEndpointSpec, HttpRequestHandlingController> {

	HttpControllerEndpointSpec(HttpRequestHandlingController controller, String... path) {
		super(controller, path);
	}

	public HttpControllerEndpointSpec replyKey(String replyKey) {
		this.target.setReplyKey(replyKey);
		return this;
	}

	public HttpControllerEndpointSpec errorsKey(String errorsKey) {
		this.target.setErrorsKey(errorsKey);
		return this;
	}

	public HttpControllerEndpointSpec errorCode(String errorCode) {
		this.target.setErrorCode(errorCode);
		return this;
	}

}
