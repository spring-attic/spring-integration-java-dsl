/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.dsl.ftp;

import org.apache.commons.net.ftp.FTPFile;

import org.springframework.integration.dsl.file.FileTransferringMessageHandlerSpec;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileExistsMode;

/**
 * @author Artem Bilan
 */
public class FtpMessageHandlerSpec extends FileTransferringMessageHandlerSpec<FTPFile, FtpMessageHandlerSpec> {

	FtpMessageHandlerSpec(SessionFactory<FTPFile> sessionFactory) {
		super(sessionFactory);
	}

	FtpMessageHandlerSpec(RemoteFileTemplate<FTPFile> remoteFileTemplate) {
		super(remoteFileTemplate);
	}

	FtpMessageHandlerSpec(RemoteFileTemplate<FTPFile> remoteFileTemplate, FileExistsMode fileExistsMode) {
		super(remoteFileTemplate, fileExistsMode);
	}

}
