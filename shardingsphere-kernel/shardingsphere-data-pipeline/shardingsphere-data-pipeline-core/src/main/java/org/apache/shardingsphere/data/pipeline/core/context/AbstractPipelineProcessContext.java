/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.data.pipeline.core.context;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.shardingsphere.data.pipeline.core.execute.ExecuteEngine;
import org.apache.shardingsphere.data.pipeline.core.ingest.channel.memory.MemoryPipelineChannelCreator;
import org.apache.shardingsphere.data.pipeline.spi.ingest.channel.PipelineChannelCreator;
import org.apache.shardingsphere.data.pipeline.spi.ingest.channel.PipelineChannelCreatorFactory;
import org.apache.shardingsphere.data.pipeline.spi.ratelimit.JobRateLimitAlgorithm;
import org.apache.shardingsphere.data.pipeline.spi.ratelimit.JobRateLimitAlgorithmFactory;
import org.apache.shardingsphere.infra.config.algorithm.AlgorithmConfiguration;
import org.apache.shardingsphere.infra.config.rule.data.pipeline.PipelineInputConfiguration;
import org.apache.shardingsphere.infra.config.rule.data.pipeline.PipelineOutputConfiguration;
import org.apache.shardingsphere.infra.config.rule.data.pipeline.PipelineProcessConfiguration;
import org.apache.shardingsphere.infra.yaml.config.pojo.algorithm.YamlAlgorithmConfiguration;
import org.apache.shardingsphere.infra.yaml.config.pojo.data.pipeline.YamlPipelineInputConfiguration;
import org.apache.shardingsphere.infra.yaml.config.pojo.data.pipeline.YamlPipelineOutputConfiguration;
import org.apache.shardingsphere.infra.yaml.config.pojo.data.pipeline.YamlPipelineProcessConfiguration;
import org.apache.shardingsphere.infra.yaml.config.swapper.rule.data.pipeline.YamlPipelineProcessConfigurationSwapper;

import java.util.Properties;

/**
 * Abstract pipeline process context.
 */
@Getter
@Slf4j
public abstract class AbstractPipelineProcessContext {
    
    private static final YamlPipelineProcessConfigurationSwapper SWAPPER = new YamlPipelineProcessConfigurationSwapper();
    
    private final PipelineProcessConfiguration pipelineProcessConfig;
    
    private final JobRateLimitAlgorithm inputRateLimitAlgorithm;
    
    private final JobRateLimitAlgorithm outputRateLimitAlgorithm;
    
    private final PipelineChannelCreator pipelineChannelCreator;
    
    private final LazyInitializer<ExecuteEngine> inventoryDumperExecuteEngineLazyInitializer;
    
    private final LazyInitializer<ExecuteEngine> incrementalDumperExecuteEngineLazyInitializer;
    
    private final LazyInitializer<ExecuteEngine> importerExecuteEngineLazyInitializer;
    
    public AbstractPipelineProcessContext(final String jobId, final PipelineProcessConfiguration originalProcessConfig) {
        PipelineProcessConfiguration processConfig = convertProcessConfig(originalProcessConfig);
        this.pipelineProcessConfig = processConfig;
        PipelineInputConfiguration inputConfig = processConfig.getInput();
        AlgorithmConfiguration inputRateLimiter = inputConfig.getRateLimiter();
        inputRateLimitAlgorithm = null != inputRateLimiter ? JobRateLimitAlgorithmFactory.newInstance(inputRateLimiter) : null;
        PipelineOutputConfiguration outputConfig = processConfig.getOutput();
        AlgorithmConfiguration outputRateLimiter = outputConfig.getRateLimiter();
        outputRateLimitAlgorithm = null != outputRateLimiter ? JobRateLimitAlgorithmFactory.newInstance(outputRateLimiter) : null;
        AlgorithmConfiguration streamChannel = processConfig.getStreamChannel();
        pipelineChannelCreator = PipelineChannelCreatorFactory.newInstance(streamChannel);
        inventoryDumperExecuteEngineLazyInitializer = new LazyInitializer<ExecuteEngine>() {
            
            @Override
            protected ExecuteEngine initialize() {
                return ExecuteEngine.newFixedThreadInstance(inputConfig.getWorkerThread(), "Inventory-" + jobId);
            }
        };
        incrementalDumperExecuteEngineLazyInitializer = new LazyInitializer<ExecuteEngine>() {
            
            @Override
            protected ExecuteEngine initialize() {
                return ExecuteEngine.newCachedThreadInstance("Incremental-" + jobId);
            }
        };
        importerExecuteEngineLazyInitializer = new LazyInitializer<ExecuteEngine>() {
            
            @Override
            protected ExecuteEngine initialize() {
                return ExecuteEngine.newFixedThreadInstance(outputConfig.getWorkerThread(), "Importer-" + jobId);
            }
        };
    }
    
    private PipelineProcessConfiguration convertProcessConfig(final PipelineProcessConfiguration originalProcessConfig) {
        YamlPipelineProcessConfiguration yamlActionConfig = SWAPPER.swapToYamlConfiguration(originalProcessConfig);
        if (null == yamlActionConfig.getInput()) {
            yamlActionConfig.setInput(YamlPipelineInputConfiguration.buildWithDefaultValue());
        } else {
            yamlActionConfig.getInput().fillInNullFieldsWithDefaultValue();
        }
        if (null == yamlActionConfig.getOutput()) {
            yamlActionConfig.setOutput(YamlPipelineOutputConfiguration.buildWithDefaultValue());
        } else {
            yamlActionConfig.getOutput().fillInNullFieldsWithDefaultValue();
        }
        if (null == yamlActionConfig.getStreamChannel()) {
            yamlActionConfig.setStreamChannel(new YamlAlgorithmConfiguration(MemoryPipelineChannelCreator.TYPE, new Properties()));
        }
        return SWAPPER.swapToObject(yamlActionConfig);
    }
    
    /**
     * Get inventory dumper execute engine.
     *
     * @return inventory dumper execute engine
     */
    @SneakyThrows(ConcurrentException.class)
    public ExecuteEngine getInventoryDumperExecuteEngine() {
        return inventoryDumperExecuteEngineLazyInitializer.get();
    }
    
    /**
     * Get incremental dumper execute engine.
     *
     * @return incremental dumper execute engine
     */
    @SneakyThrows(ConcurrentException.class)
    public ExecuteEngine getIncrementalDumperExecuteEngine() {
        return incrementalDumperExecuteEngineLazyInitializer.get();
    }
    
    /**
     * Get importer execute engine.
     *
     * @return importer execute engine
     */
    @SneakyThrows(ConcurrentException.class)
    public ExecuteEngine getImporterExecuteEngine() {
        return importerExecuteEngineLazyInitializer.get();
    }
}
