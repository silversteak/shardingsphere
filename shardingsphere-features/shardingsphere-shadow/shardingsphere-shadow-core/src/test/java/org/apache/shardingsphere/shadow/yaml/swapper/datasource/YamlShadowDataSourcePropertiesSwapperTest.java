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

package org.apache.shardingsphere.shadow.yaml.swapper.datasource;

import org.apache.shardingsphere.shadow.api.config.datasource.ShadowDataSourceConfiguration;
import org.apache.shardingsphere.shadow.yaml.config.datasource.YamlShadowDataSourceConfiguration;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public final class YamlShadowDataSourcePropertiesSwapperTest {
    
    @Test
    public void assertSwapToYamlConfiguration() {
        ShadowDataSourceConfiguration shadowDataSourceConfig = new ShadowDataSourceConfiguration("ds", "ds-shadow");
        YamlShadowDataSourceConfigurationSwapper swapper = new YamlShadowDataSourceConfigurationSwapper();
        YamlShadowDataSourceConfiguration yamlConfig = swapper.swapToYamlConfiguration(shadowDataSourceConfig);
        assertThat(shadowDataSourceConfig.getSourceDataSourceName(), is(yamlConfig.getSourceDataSourceName()));
        assertThat(shadowDataSourceConfig.getShadowDataSourceName(), is(yamlConfig.getShadowDataSourceName()));
    }
    
    @Test
    public void assertSwapToObject() {
        YamlShadowDataSourceConfiguration yamlConfig = new YamlShadowDataSourceConfiguration();
        yamlConfig.setShadowDataSourceName("ds-shadow");
        yamlConfig.setSourceDataSourceName("ds");
        YamlShadowDataSourceConfigurationSwapper swapper = new YamlShadowDataSourceConfigurationSwapper();
        ShadowDataSourceConfiguration dataSourceConfig = swapper.swapToObject(yamlConfig);
        assertThat(yamlConfig.getSourceDataSourceName(), is(dataSourceConfig.getSourceDataSourceName()));
        assertThat(yamlConfig.getShadowDataSourceName(), is(dataSourceConfig.getShadowDataSourceName()));
    }
}
