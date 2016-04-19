/*
Copyright 2009-2016 Urban Airship Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE URBAN AIRSHIP INC ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
EVENT SHALL URBAN AIRSHIP INC OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.urbanairship.analytics;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.urbanairship.json.JsonMap;
import com.urbanairship.push.PushMessage;

/**
 * An event that captures information regarding an interactive notification action open.
 *
 * @hide
 */
public class InteractiveNotificationEvent extends Event {

    private static final String SEND_ID_KEY = "send_id";
    private static final String BUTTON_GROUP_KEY = "button_group";
    private static final String BUTTON_ID_KEY = "button_id";
    private static final String BUTTON_DESCRIPTION_KEY = "button_description";
    private static final String FOREGROUND_KEY = "foreground";
    private static final String TYPE = "interactive_notification_action";
    private static final String USER_INPUT = "user_input";

    private final String sendId;
    private final String buttonGroupId;
    private final String buttonId;
    private final String buttonDescription;
    private final boolean isForeground;
    private final Bundle remoteInput;

    /**
     * Creates an interactive notification event.
     * @param message The PushMessage that displayed the notification.
     * @param buttonId The button ID.
     * @param buttonDescription The button description.
     * @param isForeground If the action is foreground or not.
     * @param remoteInput The action button remote input.
     */
    public InteractiveNotificationEvent(@NonNull PushMessage message, @NonNull String buttonId, @Nullable String buttonDescription, boolean isForeground, @Nullable Bundle remoteInput) {
        this.sendId = message.getSendId();
        this.buttonGroupId = message.getInteractiveNotificationType();
        this.buttonId = buttonId;
        this.buttonDescription = buttonDescription;
        this.isForeground = isForeground;
        this.remoteInput = remoteInput;
    }

    @Override
    public final String getType() {
        return TYPE;
    }

    @Override
    protected final JsonMap getEventData() {
        JsonMap.Builder builder = JsonMap.newBuilder()
                .put(SEND_ID_KEY, sendId)
                .put(BUTTON_GROUP_KEY, buttonGroupId)
                .put(BUTTON_ID_KEY, buttonId)
                .put(BUTTON_DESCRIPTION_KEY, buttonDescription)
                .put(FOREGROUND_KEY, isForeground);

        if (remoteInput != null && !remoteInput.isEmpty()) {

            JsonMap.Builder remoteInputBuilder = JsonMap.newBuilder();
            for (String key : remoteInput.keySet()) {
                remoteInputBuilder.put(key, remoteInput.getString(key));
            }

            builder.put(USER_INPUT, remoteInputBuilder.build());
        }

        return builder.build();
    }
}
