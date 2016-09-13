/*
 * Copyright 2014-2016 the original author or authors.
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

package org.springframework.integration.dsl.test.file;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageProducers;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.dsl.core.Pollers;
import org.springframework.integration.dsl.file.Files;
import org.springframework.integration.dsl.support.Transformers;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.tail.ApacheCommonsFileTailingMessageProducer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StreamUtils;

/**
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class FileTests {

	@ClassRule
	public static final TemporaryFolder tmpDir = new TemporaryFolder();

	@Autowired
	private ListableBeanFactory beanFactory;

	@Autowired
	private ControlBusGateway controlBus;

	@Autowired
	@Qualifier("fileFlow1Input")
	private MessageChannel fileFlow1Input;

	@Autowired
	@Qualifier("fileWriting.handler")
	private MessageHandler fileWritingMessageHandler;

	@Autowired
	@Qualifier("tailChannel")
	private PollableChannel tailChannel;

	@Autowired
	private ApacheCommonsFileTailingMessageProducer tailer;

	@Autowired
	@Qualifier("fileReadingResultChannel")
	private PollableChannel fileReadingResultChannel;

	@Autowired
	@Qualifier("fileWritingInput")
	private MessageChannel fileWritingInput;

	@Autowired
	@Qualifier("fileWritingResultChannel")
	private PollableChannel fileWritingResultChannel;

	@Autowired
	@Qualifier("fileSplittingResultChannel")
	private PollableChannel fileSplittingResultChannel;

	@Test
	public void testFileHandler() throws Exception {
		Message<?> message = MessageBuilder.withPayload("foo").setHeader(FileHeaders.FILENAME, "foo").build();
		try {
			this.fileFlow1Input.send(message);
			fail("NullPointerException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(MessageHandlingException.class));
			assertThat(e.getCause(), instanceOf(NullPointerException.class));
		}
		DefaultFileNameGenerator fileNameGenerator = new DefaultFileNameGenerator();
		fileNameGenerator.setBeanFactory(this.beanFactory);
		Object targetFileWritingMessageHandler = this.fileWritingMessageHandler;
		if (this.fileWritingMessageHandler instanceof Advised) {
			TargetSource targetSource = ((Advised) this.fileWritingMessageHandler).getTargetSource();
			if (targetSource != null) {
				targetFileWritingMessageHandler = targetSource.getTarget();
			}
		}
		DirectFieldAccessor dfa = new DirectFieldAccessor(targetFileWritingMessageHandler);
		dfa.setPropertyValue("fileNameGenerator", fileNameGenerator);
		this.fileFlow1Input.send(message);

		assertTrue(new File(tmpDir.getRoot(), "foo").exists());
	}

	@Test
	public void testMessageProducerFlow() throws Exception {
		FileOutputStream file = new FileOutputStream(new File(tmpDir.getRoot(), "TailTest"));
		for (int i = 0; i < 50; i++) {
			file.write((i + "\n").getBytes());
		}
		this.tailer.start();
		for (int i = 0; i < 50; i++) {
			Message<?> message = this.tailChannel.receive(5000);
			assertNotNull(message);
			assertEquals("hello " + i, message.getPayload());
		}
		assertNull(this.tailChannel.receive(1));

		this.controlBus.send("@tailer.stop()");
		file.close();
	}


	@Test
	public void testFileReadingFlow() throws Exception {
		List<Integer> evens = new ArrayList<>(25);
		for (int i = 0; i < 50; i++) {
			boolean even = i % 2 == 0;
			String extension = even ? ".sitest" : ".foofile";
			if (even) {
				evens.add(i);
			}
			FileOutputStream file = new FileOutputStream(new File(tmpDir.getRoot(), i + extension));
			file.write(("" + i).getBytes());
			file.flush();
			file.close();
		}

		Message<?> message = fileReadingResultChannel.receive(60000);
		assertNotNull(message);
		Object payload = message.getPayload();
		assertThat(payload, instanceOf(List.class));
		@SuppressWarnings("unchecked")
		List<String> result = (List<String>) payload;
		assertEquals(25, result.size());
		result.forEach(s -> assertTrue(evens.contains(Integer.parseInt(s))));
	}

	@Test
	public void testFileWritingFlow() throws Exception {
		String payload = "Spring Integration";
		this.fileWritingInput.send(new GenericMessage<>(payload));
		Message<?> receive = this.fileWritingResultChannel.receive(1000);
		assertNotNull(receive);
		assertThat(receive.getPayload(), instanceOf(File.class));
		File resultFile = (File) receive.getPayload();
		assertThat(resultFile.getAbsolutePath(),
				endsWith(TestUtils.applySystemFileSeparator("fileWritingFlow/foo.write")));
		String fileContent = StreamUtils.copyToString(new FileInputStream(resultFile), Charset.defaultCharset());
		assertEquals(payload, fileContent);
	}

	@Test
	public void testFileSplitterFlow() throws Exception {
		FileOutputStream file = new FileOutputStream(new File(tmpDir.getRoot(), "foo.tmp"));
		file.write(("HelloWorld\näöüß").getBytes(Charset.defaultCharset()));
		file.flush();
		file.close();

		Message<?> receive = this.fileSplittingResultChannel.receive(10000);
		assertNotNull(receive); //HelloWorld
		assertEquals(0, receive.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE));
		receive = this.fileSplittingResultChannel.receive(10000);
		assertNotNull(receive); //äöüß
		assertNull(this.fileSplittingResultChannel.receive(1));
	}

	@Autowired
	@Qualifier("dynamicAdaptersResult")
	PollableChannel dynamicAdaptersResult;

	@Autowired
	private MyService myService;

	@Test
	public void testDynamicFileFlows() throws Exception {
		File newFolder1 = tmpDir.newFolder();
		FileOutputStream file = new FileOutputStream(new File(newFolder1, "foo"));
		file.write(("foo").getBytes());
		file.flush();
		file.close();

		File newFolder2 = tmpDir.newFolder();
		file = new FileOutputStream(new File(newFolder2, "bar"));
		file.write(("bar").getBytes());
		file.flush();
		file.close();

		this.myService.pollDirectories(newFolder1, newFolder2);

		Set<String> payloads = new TreeSet<>();
		Message<?> receive = this.dynamicAdaptersResult.receive(10000);
		assertNotNull(receive);
		payloads.add((String) receive.getPayload());
		receive = this.dynamicAdaptersResult.receive(10000);
		assertNotNull(receive);
		payloads.add((String) receive.getPayload());

		assertArrayEquals(new String[] { "bar", "foo" }, payloads.toArray());
	}

	@MessagingGateway(defaultRequestChannel = "controlBus.input")
	private interface ControlBusGateway {

		void send(String command);

	}

	@Configuration
	@EnableIntegration
	@ComponentScan
	@IntegrationComponentScan
	public static class ContextConfiguration {

		@Bean
		public IntegrationFlow controlBus() {
			return IntegrationFlowDefinition::controlBus;
		}

		@Bean
		public IntegrationFlow fileFlow1() {
			return IntegrationFlows.from("fileFlow1Input")
					.handleWithAdapter(h -> h.file(tmpDir.getRoot()).fileNameGenerator(message -> null),
							c -> c.id("fileWriting"))
					.get();
		}

		@Bean
		public IntegrationFlow tailFlow() {
			return IntegrationFlows.from((MessageProducers p) ->
					p.tail(new File(tmpDir.getRoot(), "TailTest"))
							.delay(500)
							.end(false)
							.id("tailer")
							.autoStartup(false))
					.transform("hello "::concat)
					.channel(MessageChannels.queue("tailChannel"))
					.get();
		}

		@Bean
		public IntegrationFlow fileReadingFlow() {
			return IntegrationFlows
					.from(s -> s.file(tmpDir.getRoot())
									.patternFilter("*.sitest")
									.useWatchService(true)
									.watchEvents(FileReadingMessageSource.WatchEventType.CREATE,
											FileReadingMessageSource.WatchEventType.MODIFY),
							e -> e.poller(Pollers.fixedDelay(100)))
					.transform(Transformers.fileToString())
					.aggregate(a -> a.correlationExpression("1")
							.releaseStrategy(g -> g.size() == 25))
					.channel(MessageChannels.queue("fileReadingResultChannel"))
					.get();
		}

		@Bean
		public IntegrationFlow fileWritingFlow() {
			return IntegrationFlows.from("fileWritingInput")
					.enrichHeaders(h -> h.header(FileHeaders.FILENAME, "foo.write")
							.header("directory", new File(tmpDir.getRoot(), "fileWritingFlow")))
					.handleWithAdapter(a -> a.fileGateway(m -> m.getHeaders().get("directory")))
					.channel(MessageChannels.queue("fileWritingResultChannel"))
					.get();
		}


		@Bean
		public IntegrationFlow fileSplitterFlow() {
			return IntegrationFlows
					.from(s -> s.file(tmpDir.getRoot())
									.patternFilter("foo.tmp"),
							e -> e.poller(p -> p.fixedDelay(100)))
					.split(Files.splitter())
					.channel(c -> c.queue("fileSplittingResultChannel"))
					.get();
		}

		@Bean
		public PollableChannel dynamicAdaptersResult() {
			return new QueueChannel();
		}

	}

	@Service
	public static class MyService {

		@Autowired
		private AutowireCapableBeanFactory beanFactory;

		@Autowired
		@Qualifier("dynamicAdaptersResult")
		PollableChannel dynamicAdaptersResult;

		public void pollDirectories(File... directories) {
			for (File directory : directories) {
				StandardIntegrationFlow integrationFlow = IntegrationFlows
						.from(s -> s.file(directory),
								e -> e.poller(p -> p.fixedDelay(1000))
										.id(directory.getName() + ".adapter"))
						.transform(Transformers.fileToString(),
								e -> e.id(directory.getName() + ".transformer"))
						.channel(this.dynamicAdaptersResult)
						.get();
				this.beanFactory.initializeBean(integrationFlow, directory.getName());
				this.beanFactory.getBean(directory.getName() + ".transformer", Lifecycle.class).start();
				this.beanFactory.getBean(directory.getName() + ".adapter", Lifecycle.class).start();
			}
		}

	}

}
