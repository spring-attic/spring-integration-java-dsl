/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.integration.dsl.samples.file2file1;

import java.io.File;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.core.Pollers;
import org.springframework.integration.dsl.file.Files;
import org.springframework.integration.dsl.support.Transformers;

/**
 * Simple file to file, converting CRLF to LF.
 *
 * @author Gary Russell
 * @since 1.1
 *
 */
@Configuration
@EnableIntegration
@EnableAutoConfiguration
public class FileChangeLineSeparator {

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(FileChangeLineSeparator.class)
				.web(false)
				.run(args);
		System.out.println("Put a windows file with a .txt extension in /tmp/in,"
				+ "\nthe file will be converted to Un*x and placed in"
				+ "\n/tmp/out"
				+ "\n\nHit enter to terminate");
		System.in.read();
		context.close();
	}

	@Bean
	public IntegrationFlow fileToFile() {
		return IntegrationFlows.from(Files.inboundAdapter(new File("/tmp/in"))
										.autoCreateDirectory(true)
										.patternFilter("*.txt"),
											e -> e.poller(Pollers.fixedDelay(5000)))
				.transform(Transformers.fileToString())
				.transform("payload.replaceAll('\r\n', '\n')")
				.handle(Files.outboundAdapter("'/tmp/out'")
						.autoCreateDirectory(true))
				.get();
	}

}
