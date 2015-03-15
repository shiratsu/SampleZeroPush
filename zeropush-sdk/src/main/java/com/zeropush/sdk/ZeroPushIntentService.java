package com.zeropush.sdk;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
 * Created by stefan on 10/21/14.
 */
public class ZeroPushIntentService extends IntentService {
    public ZeroPushIntentService(){
        super("ZeroPushIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);

        onHandleIntent(intent, messageType, extras);
        //release the lock
        ZeroPushBroadcastReceiver.completeWakefulIntent(intent);
    }

    protected void onHandleIntent(Intent intent, String messageType, Bundle extras) {
        Log.i("ZP-SDK", messageType);
        Log.i("ZP-SDK", "Received:" + extras.toString());
    }
}
