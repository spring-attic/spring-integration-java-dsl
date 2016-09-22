/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.dsl.samples.filesplitter2aggregator;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.ReleaseStrategy;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.core.Pollers;
import org.springframework.integration.dsl.file.Files;
import org.springframework.integration.dsl.support.Transformers;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.splitter.FileSplitter;
import org.springframework.integration.xml.transformer.ResultToStringTransformer;
import org.springframework.messaging.Message;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

/**
 * @author Artem Bilan
 * @since 1.2
 */
@SpringBootApplication
public class SplitFileAndAggregate {

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(SplitFileAndAggregate.class)
				.web(false)
				.run(args);
		System.out.println("Put a file with a .txt extension in /tmp/in with names,"
				+ "\nthe file will be read, names started with 'X' will be filtered and "
				+ "the result will be stored in XML format in the "
				+ "\n/tmp/out"
				+ "\n\nHit enter to terminate");
		System.in.read();
		context.close();
	}

	@Bean
	public Marshaller jaxbMarshaller() {
		Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
		jaxb2Marshaller.setPackagesToScan(Names.class.getPackage().getName());
		jaxb2Marshaller.setMarshallerProperties(Collections
				.singletonMap(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, true));
		return jaxb2Marshaller;
	}

	@Bean
	public IntegrationFlow fileSplitterAggregator() {
		return IntegrationFlows
				.from(Files.inboundAdapter(new File("/tmp/in"))
								.autoCreateDirectory(true)
								.patternFilter("*.txt"),
						e -> e.poller(Pollers.fixedDelay(5000)))
				.split(Files.splitter()
						.markers()
						.applySequence(true))
				.filter(p -> !(p instanceof FileSplitter.FileMarker),
						e -> e.discardChannel("aggregatorChannel"))
				.<String, Name>transform(Name::new)
				.<Name>filter(p -> !p.getValue().startsWith("X"))
				.channel("aggregatorChannel")
				.aggregate(a -> a.processor(new FileMarkerAggregator()))
				.<List<Name>, Names>transform(Names::new)
				.transform(Transformers.marshaller(jaxbMarshaller(),
						new ResultToStringTransformer()))
				.handle(Files.outboundAdapter("'/tmp/out'")
						.fileNameGenerator(m -> m
								.getHeaders()
								.get(FileHeaders.FILENAME, String.class)
								.replace(".txt", ".xml"))
						.autoCreateDirectory(true))
				.get();
	}

	private static class FileMarkerAggregator {

		@ReleaseStrategy
		public boolean release(List<Message<?>> messages) {
			return FileSplitter.FileMarker.Mark.END.name()
					.equals(messages.get(messages.size() - 1)
							.getHeaders()
							.get(FileHeaders.MARKER));
		}

		@Aggregator
		public List<Object> aggregate(List<Object> payloads) {
			return payloads.stream()
					.filter(p -> !(p instanceof FileSplitter.FileMarker))
					.collect(Collectors.toList());
		}

	}

}
