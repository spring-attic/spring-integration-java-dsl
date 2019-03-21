/*
 * Copyright 2014-2017 the original author or authors.
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

package org.springframework.integration.dsl.file;

import java.io.File;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.dsl.core.MessageProducerSpec;
import org.springframework.integration.file.config.FileTailInboundChannelAdapterFactoryBean;
import org.springframework.integration.file.tail.FileTailingMessageProducerSupport;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

/**
 * A {@link MessageProducerSpec} for file tailing adapters.
 *
 * @author Artem Bilan
 */
public class TailAdapterSpec extends MessageProducerSpec<TailAdapterSpec, FileTailingMessageProducerSupport> {

	private final FileTailInboundChannelAdapterFactoryBean factoryBean = new FileTailInboundChannelAdapterFactoryBean();

	private MessageChannel outputChannel;

	private MessageChannel errorChannel;

	TailAdapterSpec() {
		super(null);
		this.factoryBean.setBeanFactory(new DefaultListableBeanFactory());
	}

	TailAdapterSpec file(File file) {
		Assert.notNull(file, "'file' must not be null");
		this.factoryBean.setFile(file);
		return _this();
	}

	/**
	 * @param nativeOptions the nativeOptions.
	 * @return the spec.
	 * @see org.springframework.integration.file.tail.OSDelegatingFileTailingMessageProducer#setOptions(String)
	 */
	public TailAdapterSpec nativeOptions(String nativeOptions) {
		this.factoryBean.setNativeOptions(nativeOptions);
		return _this();
	}

	/**
	 * Configure a task executor. Defaults to a
	 * {@link org.springframework.core.task.SimpleAsyncTaskExecutor}.
	 * @param taskExecutor the taskExecutor.
	 * @return the spec.
	 */
	public TailAdapterSpec taskExecutor(TaskExecutor taskExecutor) {
		this.factoryBean.setTaskExecutor(taskExecutor);
		return _this();
	}

	/**
	 * Set a task scheduler - defaults to the integration 'taskScheduler'.
	 * @param taskScheduler the taskScheduler.
	 * @return the spec.
	 */
	public TailAdapterSpec taskScheduler(TaskScheduler taskScheduler) {
		this.factoryBean.setTaskScheduler(taskScheduler);
		return _this();
	}

	/**
	 * @param delay the delay.
	 * @return the spec.
	 * @see org.springframework.integration.file.tail.ApacheCommonsFileTailingMessageProducer#setPollingDelay(long)
	 */
	public TailAdapterSpec delay(long delay) {
		this.factoryBean.setDelay(delay);
		return _this();
	}

	/**
	 * @param fileDelay the fileDelay.
	 * @return the spec.
	 * @see FileTailingMessageProducerSupport#setTailAttemptsDelay(long)
	 */
	public TailAdapterSpec fileDelay(long fileDelay) {
		this.factoryBean.setFileDelay(fileDelay);
		return _this();
	}

	/**
	 * @param end the end.
	 * @return the spec.
	 * @see org.springframework.integration.file.tail.ApacheCommonsFileTailingMessageProducer#setEnd(boolean)
	 */
	public TailAdapterSpec end(boolean end) {
		this.factoryBean.setEnd(end);
		return _this();
	}

	/**
	 * @param reopen the reopen.
	 * @return the spec.
	 * @see org.springframework.integration.file.tail.ApacheCommonsFileTailingMessageProducer#setReopen(boolean)
	 */
	public TailAdapterSpec reopen(boolean reopen) {
		this.factoryBean.setReopen(reopen);
		return _this();
	}

	@Override
	public TailAdapterSpec id(String id) {
		this.factoryBean.setBeanName(id);
		return _this();
	}

	@Override
	public TailAdapterSpec phase(int phase) {
		this.factoryBean.setPhase(phase);
		return _this();
	}

	@Override
	public TailAdapterSpec autoStartup(boolean autoStartup) {
		this.factoryBean.setAutoStartup(autoStartup);
		return _this();
	}

	@Override
	public TailAdapterSpec outputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
		return _this();
	}

	@Override
	public TailAdapterSpec errorChannel(MessageChannel errorChannel) {
		this.errorChannel = errorChannel;
		return _this();
	}

	@Override
	protected FileTailingMessageProducerSupport doGet() {
		if (this.outputChannel == null) {
			this.factoryBean.setOutputChannel(new NullChannel());
		}
		FileTailingMessageProducerSupport tailingMessageProducerSupport = null;
		try {
			this.factoryBean.afterPropertiesSet();
			tailingMessageProducerSupport = this.factoryBean.getObject();
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
		if (this.errorChannel != null) {
			tailingMessageProducerSupport.setErrorChannel(this.errorChannel);
		}
		tailingMessageProducerSupport.setOutputChannel(this.outputChannel);
		return tailingMessageProducerSupport;
	}

}
