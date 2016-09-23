/*
 * Copyright 2012 - 2016 Splice Machine, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 */

package com.splicemachine.db.client.cluster;

import java.util.concurrent.Semaphore;

/**
 * A Fixed pool size, which forces new entrants to block
 * @author Scott Fines
 *         Date: 8/23/16
 */
public class BoundedBlockingPoolSize implements PoolSizingStrategy{
    private final Semaphore permits;
    private final int maxPermitsPerServer;

    public BoundedBlockingPoolSize(int maxPoolSize){
        this.permits=new Semaphore(maxPoolSize);
        this.maxPermitsPerServer=Math.max(maxPoolSize/10,1); //TODO -sf- make this automatically adjust based on server discovery
    }

    @Override
    public void acquirePermit() throws InterruptedException{
        permits.acquire();
    }

    @Override
    public void releasePermit(){
        permits.release();
    }

    @Override
    public int singleServerPoolSize(){
        return maxPermitsPerServer;
    }
}