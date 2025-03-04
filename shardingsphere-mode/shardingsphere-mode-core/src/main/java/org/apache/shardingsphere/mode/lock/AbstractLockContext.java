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

import org.apache.shardingsphere.infra.lock.LockContext;
import org.apache.shardingsphere.infra.lock.LockDefinition;
import org.apache.shardingsphere.mode.lock.definition.DatabaseLockDefinition;

/**
 * Abstract lock context.
 */
public abstract class AbstractLockContext implements LockContext {
    
    @Override
    public boolean tryLock(final LockDefinition lockDefinition) {
        if (lockDefinition instanceof DatabaseLockDefinition) {
            return tryLock((DatabaseLockDefinition) lockDefinition);
        }
        throw new UnsupportedOperationException();
    }
    
    protected abstract boolean tryLock(DatabaseLockDefinition lockDefinition);
    
    @Override
    public boolean tryLock(final LockDefinition lockDefinition, final long timeoutMillis) {
        if (lockDefinition instanceof DatabaseLockDefinition) {
            return tryLock((DatabaseLockDefinition) lockDefinition, timeoutMillis);
        }
        throw new UnsupportedOperationException();
    }
    
    protected abstract boolean tryLock(DatabaseLockDefinition lockDefinition, long timeoutMillis);
    
    @Override
    public void unLock(final LockDefinition lockDefinition) {
        if (lockDefinition instanceof DatabaseLockDefinition) {
            this.unLock((DatabaseLockDefinition) lockDefinition);
            return;
        }
        throw new UnsupportedOperationException();
    }
    
    protected abstract void unLock(DatabaseLockDefinition lockDefinition);
    
    @Override
    public boolean isLocked(final LockDefinition lockDefinition) {
        if (lockDefinition instanceof DatabaseLockDefinition) {
            return isLocked((DatabaseLockDefinition) lockDefinition);
        }
        throw new UnsupportedOperationException();
    }
    
    protected abstract boolean isLocked(DatabaseLockDefinition lockDefinition);
}
