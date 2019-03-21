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

package org.springframework.integration.dsl.core;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.Trigger;

/**
 * An {@code Adapter} class for the {@link Pollers} factory.
 * Typically used with a Java 8 Lambda expression:
 * <pre class="code">
 * {@code
 *  c -> c.poller(p -> p.fixedRate(100))
 * }
 * </pre>
 *
 * @author Artem Bilan
 */
public class PollerFactory {

	public PollerSpec trigger(Trigger trigger) {
		return Pollers.trigger(trigger);
	}

	public PollerSpec cron(String cronExpression) {
		return Pollers.cron(cronExpression);
	}

	public PollerSpec cron(String cronExpression, TimeZone timeZone) {
		return Pollers.cron(cronExpression, timeZone);
	}

	public PollerSpec fixedRate(long period) {
		return Pollers.fixedRate(period);
	}

	public PollerSpec fixedRate(long period, TimeUnit timeUnit) {
		return Pollers.fixedRate(period, timeUnit);
	}

	public PollerSpec fixedRate(long period, long initialDelay) {
		return Pollers.fixedRate(period, initialDelay);
	}

	public PollerSpec fixedDelay(long period, TimeUnit timeUnit, long initialDelay) {
		return Pollers.fixedDelay(period, timeUnit, initialDelay);
	}

	public PollerSpec fixedRate(long period, TimeUnit timeUnit, long initialDelay) {
		return Pollers.fixedRate(period, timeUnit, initialDelay);
	}

	public PollerSpec fixedDelay(long period, TimeUnit timeUnit) {
		return Pollers.fixedDelay(period, timeUnit);
	}

	public PollerSpec fixedDelay(long period, long initialDelay) {
		return Pollers.fixedDelay(period, initialDelay);
	}

	public PollerSpec fixedDelay(long period) {
		return Pollers.fixedDelay(period);
	}

	PollerFactory() {
	}

}
