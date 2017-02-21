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

package fi.nuumio.netsync.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Log {
    public static final Level WTF = new LogLevel("WTF", 1000);
    public static final Level ERROR = new LogLevel("E", 950);
    public static final Level WARNING = new LogLevel("W", 900);
    public static final Level INFO = new LogLevel("I", 800);
    public static final Level DEBUG = new LogLevel("D", 500);
    public static final Level VERBOSE = new LogLevel("V", 400);
    public static final Logger sLogger = Logger.getLogger("");

    public static void d(final String message) {
        sLogger.log(DEBUG, message);
    }

    public static void d(final String message, final Exception e) {
        sLogger.log(DEBUG, message, e);
    }

    public static void e(final String message) {
        sLogger.log(ERROR, message);
    }

    public static void e(final String message, final Exception e) {
        sLogger.log(ERROR, message, e);
    }

    public static void i(final String message) {
        sLogger.log(INFO, message);
    }

    public static void i(final String message, final Exception e) {
        sLogger.log(INFO, message, e);
    }

    public static void setLevel(final Level level) {
        sLogger.setLevel(level);
        for (final Handler handler : sLogger.getHandlers()) {
            handler.setLevel(level);
            handler.setFormatter(new LogFormatter());
        }
    }

    public static void v(final String message) {
        sLogger.log(VERBOSE, message);
    }

    public static void v(final String message, final Exception e) {
        sLogger.log(VERBOSE, message, e);
    }

    public static void w(final String message) {
        sLogger.log(WARNING, message);
    }

    public static void w(final String message, final Exception e) {
        sLogger.log(WARNING, message, e);
    }

    public static synchronized void wtf(final String message) {
        sLogger.log(WTF, message);
    }

    public static synchronized void wtf(final String message, final Exception e) {
        sLogger.log(WTF, message, e);
    }

    private static class LogFormatter extends Formatter {
        private final SimpleDateFormat mDateFormat =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        private final StringBuilder mSb = new StringBuilder();
        private final StringBuilder mSbName = new StringBuilder();
        private final java.util.Formatter mFormatter = new java.util.Formatter(mSbName);

        @Override
        public synchronized String format(final LogRecord record) {
            final String threadName = LoggableThread.getName(record.getThreadID());
            mSb.setLength(0);
            mSbName.setLength(0);
            mFormatter.format("[%1$18.18s]", threadName.length() > 18 ?
                    threadName.substring(threadName.length() - 18) : threadName);
            mSb.append(record.getLevel())
                    .append(" ")
                    .append(mSbName)
                    .append(" ")
                    .append(mDateFormat.format(new Date(record.getMillis())))
                    .append(" ")
                    .append(formatMessage(record))
                    .append("\n");
            return mSb.toString();
        }
    }

    private static class LogLevel extends Level {
        private LogLevel(final String name, final int value) {
            super(name, value);
        }
    }
}
