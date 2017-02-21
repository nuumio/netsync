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

import fi.nuumio.netsync.client.GroupSyncException;
import fi.nuumio.netsync.client.SyncGroup;
import fi.nuumio.netsync.util.ClientSettings;
import fi.nuumio.netsync.util.TimeUtils;

public class TestUtil {
    public static final int DEFAULT_TIMEOUT = 2000;
    private static final long COMM_FINISH_WAIT_TIME = 5;
    private static final ClientSettings sClientSettings = new ClientSettings();

    public static void sleep(final long timeout) {
        TimeUtils.StopWatch watch = new TimeUtils.StopWatch();
        while (watch.hasTimeLeft(timeout)) {
            try {
                Thread.yield();
                Thread.sleep(watch.getTimeLeft(timeout));
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public static void awaitCommunicationFinish() {
        // This is kinda hack but hopefully makes other threads to finish pending comms from
        // sockets. Better option could be to have mechanism for checking if GroupNotify has
        // arrived lately.
        sleep(COMM_FINISH_WAIT_TIME);
    }

    public static boolean waitMemberCount(final int count, final SyncGroup ... groups)
            throws GroupSyncException {
        boolean allOk = true;
        for(final SyncGroup group: groups) {
            try {
                allOk = allOk && group.waitMembers(SyncGroup.MemberCount.equalTo(group, count),
                        sClientSettings.getClientGroupMessageTimeout());
            } catch (final GroupSyncException e) {
                allOk = false;
                throw e;
            }
        }
        return allOk;
    }

}
