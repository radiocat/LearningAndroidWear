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

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 音声入力で妖怪の名前を取得するActivity
 */
public class YokaiSearchActivity extends Activity {

    private static final String TAG = YokaiSearchActivity.class.getName();

    private static final String MESSAGE_PATH = "/yokai_wikipedia";

    private static final long CONNECTION_TIME_OUT_MS = 100;

    private GoogleApiClient mGoogleApiClient;
    private String keyword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d(TAG, "onConnected");
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.d(TAG, "onConnectionSuspended");
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.d(TAG, "onConnectionFailed");
                    }
                })
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

        // 音声認識のアクティビティを呼び出し
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        startActivityForResult(intent, 0);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            // 音声認識の結果を取得できた場合はMessageAPIでHandheldに送信する
            if (results != null && results.size() > 0) {
                keyword = results.get(0);
                Log.d(TAG, " kyeword : " + keyword);
                sendMessage();
            }
        }
    }

    /**
     * キーワードをセットしてHandheldにメッセージを送信する
     */
    private void sendMessage() {

        // MessageAPIはUIスレッドから実行できないので非同期で送信する
        new AsyncTask<String, Integer, Long>() {
            @Override
            protected Long doInBackground(String... params) {
                Log.d(TAG, "start backgroud");
                NodeApi.GetConnectedNodesResult result =
                        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                List<Node> nodes = result.getNodes();
                if (result.getStatus().isSuccess()) {
                        Log.d(TAG, "nodes : " + nodes.toString());
                        mGoogleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                        Wearable.MessageApi.sendMessage(mGoogleApiClient, pickBestNodeId(nodes), MESSAGE_PATH, keyword.getBytes());
                        Log.d(TAG, "keywordを送信 : " + keyword);
                    mGoogleApiClient.disconnect();
                } else if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "onHandleIntent: failed to connect node.");
                }

                return null;
            }

            @Override
            protected void onPostExecute(Long result) {
                // 処理終了後にActivityを閉じる
                Log.d(TAG, "onPostExecute - " + result);
                finish();
            }

        }.execute();

    }

    /**
     * 最良のNodeを取得してNodeのIDを返す
     *
     * @see https://developer.android.com/training/wearables/data-layer/messages.html
     * @param nodes Nodeのリスト
     * @return NodeのID
     *
     */
    private String pickBestNodeId(List<Node> nodes) {
        String bestNodeId = null;
        // Find a nearby node or pick one arbitrarily
        for (Node node : nodes) {
            if (node.isNearby()) {
                return node.getId();
            }
            bestNodeId = node.getId();
        }
        return bestNodeId;
    }

}
