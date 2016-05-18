package com.pcom.paratrooper;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKAccessTokenTracker;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class StartActivity extends AppCompatActivity {

    public static volatile EventDatabaseHelper eventHelper;
    private static final String[] scope = new String[] {
            VKScope.FRIENDS,
            VKScope.MESSAGES
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!VKSdk.wakeUpSession(this)) {
            VKSdk.login(this, scope);
        }
        else
        {
            Intent intent = new Intent(this, HistoryService.class);
            intent.setAction(HistoryService.ACTION_START);
            startService(intent);
        }
        eventHelper = new EventDatabaseHelper(this);
        setContentView(R.layout.activity_start);
    }

    public void startService(View view)
    {
        Intent intent = new Intent(this, HistoryService.class);
        intent.setAction(HistoryService.ACTION_START);
        startService(intent);

    }

    public void stopService(View view)
    {
        Intent intent = new Intent(this, HistoryService.class);
        intent.setAction(HistoryService.ACTION_STOP);
        startService(intent);
    }

    public void showGraph(View view)
    {
        Intent intent = new Intent(this, GraphActivity.class);
        startActivity(intent);
    }


}
