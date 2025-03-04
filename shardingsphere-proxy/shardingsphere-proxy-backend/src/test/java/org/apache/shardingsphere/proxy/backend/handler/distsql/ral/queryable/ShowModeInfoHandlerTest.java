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

package org.apache.shardingsphere.proxy.backend.handler.distsql.ral.queryable;

import org.apache.shardingsphere.distsql.parser.statement.ral.queryable.ShowModeInfoStatement;
import org.apache.shardingsphere.infra.config.mode.ModeConfiguration;
import org.apache.shardingsphere.infra.instance.InstanceContext;
import org.apache.shardingsphere.mode.manager.ContextManager;
import org.apache.shardingsphere.mode.repository.cluster.ClusterPersistRepositoryConfiguration;
import org.apache.shardingsphere.proxy.backend.context.ProxyContext;
import org.apache.shardingsphere.proxy.backend.util.ProxyContextRestorer;
import org.junit.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class ShowModeInfoHandlerTest extends ProxyContextRestorer {
    
    @Test
    public void assertExecutor() throws SQLException {
        ContextManager contextManager = mock(ContextManager.class, RETURNS_DEEP_STUBS);
        InstanceContext instanceContext = createInstanceContext();
        when(contextManager.getInstanceContext()).thenReturn(instanceContext);
        ShowModeInfoHandler handler = new ShowModeInfoHandler();
        handler.init(new ShowModeInfoStatement(), null);
        ProxyContext.init(contextManager);
        handler.execute();
        handler.next();
        List<Object> data = handler.getRowData().getData();
        assertThat(data.size(), is(4));
        assertThat(data.get(0), is("Cluster"));
        assertThat(data.get(1), is("ZooKeeper"));
        assertThat(data.get(2), is("{\"key\":\"value1,value2\"}"));
        assertThat(data.get(3), is(Boolean.FALSE.toString()));
    }
    
    private InstanceContext createInstanceContext() {
        InstanceContext result = mock(InstanceContext.class, RETURNS_DEEP_STUBS);
        when(result.getInstance().getMetaData().getId()).thenReturn("127.0.0.1@3309");
        when(result.getModeConfiguration()).thenReturn(new ModeConfiguration("Cluster",
                new ClusterPersistRepositoryConfiguration("ZooKeeper", "governance_ds", "127.0.0.1:2181", createProperties("key", "value1,value2")), false));
        return result;
    }
    
    private Properties createProperties(final String key, final String value) {
        Properties result = new Properties();
        result.put(key, value);
        return result;
    }
}
