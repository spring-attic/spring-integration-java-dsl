/*
 * Copyright 2015 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.dsl.test.reactivestreams;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.reactivestreams.Publisher;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.Channels;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import reactor.rx.Streams;


/**
 * @author Artem Bilan
 * @since 1.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class ReactiveStreamsTests {

	@Autowired
	@Qualifier("reactiveFlow")
	private Publisher<Message<String>> publisher;

	@Autowired
	@Qualifier("pollableReactiveFlow")
	private Publisher<Message<Integer>> pollablePublisher;

	@Autowired
	@Qualifier("reactiveSteamsMessageSource")
	private Lifecycle messageSource;

	@Autowired
	@Qualifier("inputChannel")
	private MessageChannel inputChannel;

	@Test
	public void testReactiveFlow() throws InterruptedException {
		List<String> results = new ArrayList<>();
		CountDownLatch latch = new CountDownLatch(6);
		Streams.wrap(this.publisher)
				.map(m -> m.getPayload().toUpperCase())
				.consume(p -> {
					results.add(p);
					latch.countDown();
				});
		this.messageSource.start();
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		String[] strings = results.toArray(new String[results.size()]);
		assertArrayEquals(new String[]{"A", "B", "C", "D", "E", "F"}, strings);
	}

	@Test
	public void testPollableReactiveFlow() throws InterruptedException, TimeoutException, ExecutionException {
		this.inputChannel.send(new GenericMessage<>("1,2,3,4,5"));

		CountDownLatch latch = new CountDownLatch(6);

		Streams.wrap(this.pollablePublisher)
				.filter(m -> m.getHeaders().containsKey(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER))
				.observe(p -> latch.countDown())
				.consume(6);

		Future<List<Integer>> future =
				Executors.newSingleThreadExecutor().submit(() ->
						Streams.from(new String[]{"11,12,13"})
								.map(v -> v.split(","))
								.map(Arrays::asList)
								.<String>split()
								.map(Integer::parseInt)
								.<Message<Integer>>map(GenericMessage<Integer>::new)
								.concatWith(this.pollablePublisher)
								.map(Message::getPayload)
								.toList(7)
								.await(5, TimeUnit.SECONDS));

		this.inputChannel.send(new GenericMessage<>("6,7,8,9,10"));

		assertTrue(latch.await(10, TimeUnit.SECONDS));
		List<Integer> integers = future.get(10, TimeUnit.SECONDS);
		assertNotNull(integers);
		assertEquals(7, integers.size());
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		private final AtomicBoolean invoked = new AtomicBoolean();

		@Bean
		public Publisher<Message<String>> reactiveFlow() {
			return IntegrationFlows
					.from(() -> new GenericMessage<>("a,b,c,d,e,f"),
							e -> e.poller(p -> p.trigger(ctx -> this.invoked.getAndSet(true) ? null : new Date()))
									.autoStartup(false)
									.id("reactiveSteamsMessageSource"))
					.split(String.class, p -> p.split(","))
					.toReactivePublisher();
		}

		@Bean
		public Publisher<Message<Integer>> pollableReactiveFlow() {
			return IntegrationFlows
					.from("inputChannel")
					.split(e -> e.get().getT2().setDelimiters(","))
					.<String, Integer>transform(Integer::parseInt)
					.channel(Channels::queue)
					.toReactivePublisher();
		}

	}

}
