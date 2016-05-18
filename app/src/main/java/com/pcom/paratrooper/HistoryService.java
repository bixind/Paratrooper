package com.pcom.paratrooper;

import com.pcom.paratrooper.EventsContract.EventEntry;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

public class HistoryService extends Service {

    public static final String ACTION_START = "com.pcom.paratrooper.action.start";
    public static final String ACTION_STOP = "com.pcom.paratrooper.action.stop";
    public static final String TAG = "HistoryService";

    private HistoryThread mt;
    private String key;
    private int ts;
    private String server;
    private volatile int c = 0;
    public final static long dt = (long) 1000 * 60 * 60 * 24;


    private String getUpdates() throws IOException {
        InputStream is = null;
        StringBuilder urlgen = new StringBuilder();
        final int waitTime = 25;
        urlgen.append("http://").append(server).append("?act=a_check").append("&key=").append(key).append("&ts=").append(ts).append("&wait=").append(waitTime).append("&mode=2");
        String myurl = urlgen.toString();
        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(30000 /* milliseconds */);
            conn.setConnectTimeout(30000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            is = conn.getInputStream();
            String content;
            StringBuilder sb = new StringBuilder();
            while (is.available() > 0)
            {
                int i = is.read();
                sb.append((char) i);
            }
            content = sb.toString();
            return content;
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private class HistoryThread extends Thread{

        private volatile Boolean isReady = false;
        private Long lastClearance;
        private final Long clearanceDelta = 1000 * 60 * 5L;

        @Override
        public void run(){
            Log.d(TAG, "hello");
            try {
                lastClearance = 0L;
                while(!this.isInterrupted()) {
                        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                        if (!(networkInfo != null && networkInfo.isConnected()))
                            break;
                        EventDatabaseHelper evHelper = StartActivity.eventHelper;
                        SQLiteDatabase evdb = evHelper.getWritableDatabase();
                        isReady = false;
                        VKRequest request = new VKRequest("messages.getLongPollServer");
                        request.executeWithListener(new VKRequest.VKRequestListener() {
                            @Override
                            public void onComplete(VKResponse response) {
//                                System.out.println(response.json);
                                try {
                                    JSONObject r = response.json.getJSONObject("response");
                                    ts = r.getInt("ts");
                                    key = r.getString("key");
                                    server = r.getString("server");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                synchronized (isReady) {
                                    isReady = true;
                                }
                            }
                        });
                        while (!isReady) {
                            Thread.sleep(300);
                        }
//                        System.out.println("rangers go!");
                        while (!this.isInterrupted()) {
                            try {
                                Long now = new Date().getTime();
                                if (now > clearanceDelta + lastClearance) {
                                    evdb.delete(EventEntry.TABLE_NAME, EventEntry.COLUMN_NAME_TIME + "<" + ((Long) (now - dt)).toString(), null);
                                    lastClearance = now;
                                }
                                JSONObject r = (JSONObject) new JSONTokener(getUpdates()).nextValue();
//                                System.out.println(r.toString());
                                if (r.has("failed"))
                                    break;
                                ts = r.getInt("ts");
                                JSONArray updates = r.getJSONArray("updates");
                                for (int i = 0; i < updates.length(); i++) {
                                    JSONArray upd = updates.optJSONArray(i);
                                    int type = upd.optInt(0);
                                    if (type == 8 || type == 9) {
//                                        System.out.println(upd);
                                        ContentValues v = new ContentValues();
                                        v.put(EventEntry.COLUMN_NAME_TIME, now);
                                        v.put(EventEntry.COLUMN_NAME_TYPE, upd.optInt(0));
                                        v.put(EventEntry.COLUMN_NAME_USER_ID, -upd.optInt(1));
                                        v.put(EventEntry.COLUMN_NAME_EXTRA, upd.optInt(2));
                                        evdb.insert(EventEntry.TABLE_NAME, null, v);
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                break;
                            }
                        }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Log.d(TAG, "bye");
            stopSelf();
            }
        }


    @Override
    public void onCreate() {
        mt = new HistoryThread();
        mt.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        System.out.println("service start");
        if (intent.getAction().equals(ACTION_STOP))
        {
            mt.interrupt();
            Toast.makeText(this, "Сервис отключается", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(this, "Сервис активен", Toast.LENGTH_SHORT).show();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
    }
}
