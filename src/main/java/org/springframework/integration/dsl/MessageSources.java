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

package org.springframework.integration.dsl;

import java.io.File;
import java.net.URL;
import java.util.Comparator;

import javax.jms.ConnectionFactory;

import org.apache.commons.net.ftp.FTPFile;

import org.springframework.core.io.Resource;
import org.springframework.integration.dsl.feed.Feed;
import org.springframework.integration.dsl.feed.FeedEntryMessageSourceSpec;
import org.springframework.integration.dsl.file.FileInboundChannelAdapterSpec;
import org.springframework.integration.dsl.file.Files;
import org.springframework.integration.dsl.ftp.Ftp;
import org.springframework.integration.dsl.ftp.FtpInboundChannelAdapterSpec;
import org.springframework.integration.dsl.jms.Jms;
import org.springframework.integration.dsl.jms.JmsInboundChannelAdapterSpec;
import org.springframework.integration.dsl.mail.ImapMailInboundChannelAdapterSpec;
import org.springframework.integration.dsl.mail.Mail;
import org.springframework.integration.dsl.mail.Pop3MailInboundChannelAdapterSpec;
import org.springframework.integration.dsl.scripting.ScriptMessageSourceSpec;
import org.springframework.integration.dsl.sftp.Sftp;
import org.springframework.integration.dsl.sftp.SftpInboundChannelAdapterSpec;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.jms.core.JmsTemplate;

import com.jcraft.jsch.ChannelSftp;

/**
 * Provides various factory methods for {@code MessageSourceSpec}s from Namespace Factories.
 * *
 * @author Artem Bilan
 * *
 * @see IntegrationFlows#from(IntegrationFlows.MessageSourcesFunction)
 */
public class MessageSources {

	/**
	 * Factory for the {@link FileInboundChannelAdapterSpec}
	 * @param directory the based directory to poll
	 * @return the FileInboundChannelAdapterSpec instance
	 */
	public FileInboundChannelAdapterSpec file(File directory) {
		return file(directory, null);
	}

	/**
	 * Factory for the {@link FileInboundChannelAdapterSpec}
	 * @param directory the based directory to poll
	 * @param receptionOrderComparator the {@link Comparator} to order the files
	 *          in the internal queue
	 * @return the FileInboundChannelAdapterSpec instance
	 */
	public FileInboundChannelAdapterSpec file(File directory, Comparator<File> receptionOrderComparator) {
		return Files.inboundAdapter(directory, receptionOrderComparator);
	}

	/**
	 * Factory for the {@link FtpInboundChannelAdapterSpec}
	 * @param sessionFactory the {@link SessionFactory} for FTP
	 * @return the FtpInboundChannelAdapterSpec instance
	 */
	public FtpInboundChannelAdapterSpec ftp(SessionFactory<FTPFile> sessionFactory) {
		return ftp(sessionFactory, null);
	}

	/**
	 * Factory for the {@link FtpInboundChannelAdapterSpec}
	 * @param sessionFactory the {@link SessionFactory} for FTP
 	 * @param receptionOrderComparator the {@link Comparator} to order the files
	 *          in the internal queue
	 * @return the FtpInboundChannelAdapterSpec instance
	 */
	public FtpInboundChannelAdapterSpec ftp(SessionFactory<FTPFile> sessionFactory,
			Comparator<File> receptionOrderComparator) {
		return Ftp.inboundAdapter(sessionFactory, receptionOrderComparator);
	}

	/**
	 * Factory for the {@link SftpInboundChannelAdapterSpec}
	 * @param sessionFactory the {@link SessionFactory} for SFTP
	 * @return the SftpInboundChannelAdapterSpec instance
	 */
	public SftpInboundChannelAdapterSpec sftp(SessionFactory<ChannelSftp.LsEntry> sessionFactory) {
		return sftp(sessionFactory, null);
	}

	/**
	 * Factory for the {@link SftpInboundChannelAdapterSpec}
	 * @param sessionFactory the {@link SessionFactory} for SFTP
	 * @param receptionOrderComparator the {@link Comparator} to order the files
	 *          in the internal queue
	 * @return the SftpInboundChannelAdapterSpec instance
	 */
	public SftpInboundChannelAdapterSpec sftp(SessionFactory<ChannelSftp.LsEntry> sessionFactory,
			Comparator<File> receptionOrderComparator) {
		return Sftp.inboundAdapter(sessionFactory, receptionOrderComparator);
	}

	/**
	 * Factory for the {@link JmsInboundChannelAdapterSpec}
	 * @param jmsTemplate the {@link JmsTemplate} to use
	 * @return JmsInboundChannelAdapterSpec instance
	 */
	public JmsInboundChannelAdapterSpec<? extends JmsInboundChannelAdapterSpec<?>> jms(JmsTemplate jmsTemplate) {
		return Jms.inboundAdapter(jmsTemplate);
	}

	/**
	 * Factory for the {@link JmsInboundChannelAdapterSpec}
	 * @param connectionFactory the {@link ConnectionFactory} to use
	 * @return JmsInboundChannelAdapterSpec instance
	 */
	public JmsInboundChannelAdapterSpec.JmsInboundChannelSpecTemplateAware jms(ConnectionFactory connectionFactory) {
		return Jms.inboundAdapter(connectionFactory);
	}

	/**
	 * Factory for the {@link ImapMailInboundChannelAdapterSpec}
	 * @return ImapMailInboundChannelAdapterSpec instance
	 */
	public ImapMailInboundChannelAdapterSpec imap() {
		return Mail.imapInboundAdapter();
	}

	/**
	 * Factory for the {@link ImapMailInboundChannelAdapterSpec}
	 * @param url the IMAP url
	 * @return ImapMailInboundChannelAdapterSpec instance
	 */
	public ImapMailInboundChannelAdapterSpec imap(String url) {
		return Mail.imapInboundAdapter(url);
	}

	/**
	 * Factory for the {@link Pop3MailInboundChannelAdapterSpec}
	 * @return Pop3MailInboundChannelAdapterSpec instance
	 */
	public Pop3MailInboundChannelAdapterSpec pop3() {
		return Mail.pop3InboundAdapter();
	}

	/**
	 * Factory for the {@link Pop3MailInboundChannelAdapterSpec}
	 * @param url the POP3 url
	 * @return Pop3MailInboundChannelAdapterSpec instance
	 */
	public Pop3MailInboundChannelAdapterSpec pop3(String url) {
		return Mail.pop3InboundAdapter(url);
	}

	/**
	 * Factory for the {@link Pop3MailInboundChannelAdapterSpec}
	 * @param host the POP3 host
	 * @param username the user name to connect to POP3
	 * @param password the password to connect to POP3
	 * @return Pop3MailInboundChannelAdapterSpec instance
	 */
	public Pop3MailInboundChannelAdapterSpec pop3(String host, String username, String password) {
		return pop3(host, -1, username, password);
	}

	/**
	 * Factory for the {@link Pop3MailInboundChannelAdapterSpec}
	 * @param host the POP3 host
	 * @param port the POP3 port
	 * @param username the user name to connect to POP3
	 * @param password the password to connect to POP3
	 * @return Pop3MailInboundChannelAdapterSpec instance
	 */
	public Pop3MailInboundChannelAdapterSpec pop3(String host, int port, String username, String password) {
		return Mail.pop3InboundAdapter(host, port, username, password);
	}

	/**
	 * Factory for the {@link ScriptMessageSourceSpec} based on the {@link Resource}.
	 * The {@link Resource} must represent the real file and can be injected like:
	 * <pre class="code">
	 *  &#064;Value("com/my/project/scripts/FilterScript.groovy")
	 *  private Resource filterScript;
	 * </pre>
	 * @param scriptResource the script {@link Resource}
	 * @return the {@link ScriptMessageSourceSpec}
	 * @since 1.1
	 */
	public ScriptMessageSourceSpec script(Resource scriptResource) {
		return new ScriptMessageSourceSpec(scriptResource);
	}

	/**
	 * Factory for the {@link ScriptMessageSourceSpec} based on the script location.
	 * @param scriptLocation the path to the script file.
	 *         {@code file:}, {@code ftp:}, {@code s3:} etc.
	 *         The {@code classpath:} can be omitted.
	 * @return the {@link ScriptMessageSourceSpec}
	 * @since 1.1
	 */
	public ScriptMessageSourceSpec script(String scriptLocation) {
		return new ScriptMessageSourceSpec(scriptLocation);
	}

	/**
	 * Factory for the {@link FeedEntryMessageSourceSpec} based on the {@code feedUrl} and {@code metadataKey}.
	 * @param feedUrl the {@link URL} for Feed resource.
	 * @param metadataKey the metadata key to the last entry after fetching.
	 * @return the {@link FeedEntryMessageSourceSpec}
	 * @since 1.1
	 */
	public FeedEntryMessageSourceSpec feed(URL feedUrl, String metadataKey) {
		return Feed.inboundAdapter(feedUrl, metadataKey);
	}

	MessageSources() {
	}

}
