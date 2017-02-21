/*
 * Copyright 2017 Jari Hämäläinen / https://github.com/nuumio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fi.nuumio.netsync.integration.util;

import fi.nuumio.netsync.util.TimeUtils;

import static fi.nuumio.netsync.integration.util.TestUtil.DEFAULT_TIMEOUT;

public class BlockingReturn<T> {
    private final Blocker<T> mBlocker;
    private final Thread mThread;
    private T mReturn = null;
    private boolean mStarted = false;
    private boolean mFinished = false;
    private Throwable mBlockerTrowable = null;

    public BlockingReturn(final Blocker<T> blocker) {
        mBlocker = blocker;
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BlockingReturn.this.mReturn = mBlocker.getValue();
                } catch (final Throwable t) {
                    BlockingReturn.this.mBlockerTrowable = t;
                }
                synchronized (BlockingReturn.this) {
                    BlockingReturn.this.mFinished = true;
                    BlockingReturn.this.notifyAll();
                }
            }
        });
    }

    public synchronized T get() {
        if (!mStarted) {
            throw new BlockingRetvalException("Not started");
        }
        final TimeUtils.StopWatch watch = new TimeUtils.StopWatch();
        while (!mFinished && watch.hasTimeLeft(DEFAULT_TIMEOUT)) {
            try {
                wait(watch.getTimeLeft(DEFAULT_TIMEOUT));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (!mFinished) {
            throw new BlockingRetvalException("Timed out");
        }
        if (mBlockerTrowable != null) {
            throw new BlockingRetvalException("Blocker exception caught", mBlockerTrowable);
        }
        return mReturn;
    }

    public synchronized void start() {
        mThread.start();
        mStarted = true;
    }

    public interface Blocker<T> {
        T getValue() throws Throwable;
    }

    private static class BlockingRetvalException extends RuntimeException {
        BlockingRetvalException(final String message) {
            super(message);
        }

        BlockingRetvalException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
