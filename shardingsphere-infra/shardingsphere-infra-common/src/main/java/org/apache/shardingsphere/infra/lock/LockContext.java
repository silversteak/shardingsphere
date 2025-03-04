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

package org.apache.shardingsphere.infra.lock;

import org.apache.shardingsphere.infra.instance.InstanceContext;

/**
 * Lock context.
 */
public interface LockContext {
    
    /**
     * Init lock state.
     *
     * @param instanceContext instance context
     */
    default void initLockState(InstanceContext instanceContext) {
    }
    
    /**
     * Try lock.
     *
     * @param lockDefinition lock definition
     * @return is locked or not
     */
    boolean tryLock(LockDefinition lockDefinition);
    
    /**
     * Try Lock.
     *
     * @param lockDefinition lock definition
     * @param timeoutMillis timeout milliseconds
     * @return is locked or not
     */
    boolean tryLock(LockDefinition lockDefinition, long timeoutMillis);
    
    /**
     * Un lock.
     *
     * @param lockDefinition lock definition
     */
    void unLock(LockDefinition lockDefinition);
    
    /**
     *  Is locked.
     *
     * @param lockDefinition lock definition
     * @return is locked or not
     */
    boolean isLocked(LockDefinition lockDefinition);
    
    /**
     * Get lock.
     *
     * @return lock
     * @deprecated remove me when the distributed lock refactoring was completed
     */
    @Deprecated
    default ShardingSphereLock getLock() {
        throw new UnsupportedOperationException();
    }
}
