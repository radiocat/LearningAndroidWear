/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.radiocat.wearable.yokaiwatch;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;


public class YokaiWikipediaListenerService extends WearableListenerService {

    private static final String TAG = "YokaiWikipedia";

    private static final String MESSAGE_PATH = "/yokai_wikipedia";

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onDataChanged: " + dataEvents + " for " + getPackageName());
        }
        dataEvents.close();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onMessageReceived: " + messageEvent + " for " + getPackageName());
        }

        if (!messageEvent.getPath().equals(MESSAGE_PATH)) {
            Log.d(TAG, "different path : " + messageEvent.getPath());
            return;
        }
        // DataItemから検索キーワードを取得する
        String keyword = new String(messageEvent.getData());
        try {
            String param = URLEncoder.encode(keyword, "utf-8");
            Uri uri = Uri.parse("http://ja.wikipedia.org/wiki/" + param);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (UnsupportedEncodingException e) {
            Log.d(TAG, "Faild in encode " + keyword + " : "+ e.getMessage());
        }
    }

}
