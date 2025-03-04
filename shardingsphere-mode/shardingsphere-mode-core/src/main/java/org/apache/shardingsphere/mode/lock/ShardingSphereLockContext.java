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

package org.apache.shardingsphere.mode.lock;

import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.infra.lock.LockContext;
import org.apache.shardingsphere.infra.lock.LockDefinition;
import org.apache.shardingsphere.mode.lock.manager.LockManager;
import org.apache.shardingsphere.mode.lock.util.TimeoutMilliseconds;

/**
 * Lock context of ShardingSphere.
 */
@RequiredArgsConstructor
public final class ShardingSphereLockContext implements LockContext {
    
    private final LockManager lockManager;
    
    @Override
    public boolean tryLock(final LockDefinition lockDefinition) {
        return lockManager.tryLock(lockDefinition, TimeoutMilliseconds.MAX_TRY_LOCK);
    }
    
    @Override
    public boolean tryLock(final LockDefinition lockDefinition, final long timeoutMillis) {
        return lockManager.tryLock(lockDefinition, timeoutMillis);
    }
    
    @Override
    public void unLock(final LockDefinition lockDefinition) {
        lockManager.unLock(lockDefinition);
    }
    
    @Override
    public boolean isLocked(final LockDefinition lockDefinition) {
        return lockManager.isLocked(lockDefinition);
    }
}
