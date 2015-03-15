package com.zeropush.sdk;

/**
 * Created by Stefan Natchev on 10/17/14.
 */


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.Header;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;
import org.json.*;
import com.loopj.android.http.*;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class ZeroPush {

    public static final String ZeroPushAPIHost = "https://api.zeropush.com";
    public static final String Version = "1.0.2";
    static final String TAG = "ZeroPush-GCM-SDK";

    static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static final String UserAgent = TAG + "/" + Version;
    public static final AsyncHttpClient httpClient = new AsyncHttpClient();

    private static ZeroPush sharedInstance;

    GoogleCloudMessaging gcm;

    public static ZeroPush getInstance() {
        return sharedInstance;
    }

    private String apiKey;
    private String senderId;
    private String deviceToken;
    private Activity delegate;

    public ZeroPush(String apiKey, String senderId, Activity delegate){
        this.apiKey = apiKey;
        this.senderId = senderId;
        this.delegate = delegate;
        httpClient.setUserAgent(UserAgent);
        sharedInstance = this;
    }

    public void registerForRemoteNotifications() {
        if (!isGooglePlayServicesAvailable()){
            Log.e(TAG, "No valid Google Play Services APK found.");
            return;
        }
        doRegistrationInBackground();
    }

    private void doRegistrationInBackground() {
        new AsyncTask<Void,Void,String>() {
            @Override
            protected String doInBackground(Void... params) {
                String token = getDeviceToken();

                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(delegate.getApplicationContext());
                    }

                    if(token == null || token.isEmpty()) {
                        token = gcm.register(senderId);
                        setDeviceToken(token);
                        Log.d(TAG, "Received token: " + token);
                    } else {
                        Log.d(TAG, "Reusing token: " + token);
                    }
                } catch (IOException ex) {
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                    Log.e(TAG, ex.getMessage());
                }
                return token;
            }

            protected void onPostExecute(String deviceToken) {
                registerDeviceToken(deviceToken);
            }
        }.execute(null, null, null);
    }

    /**
     * Get the stored device token either from the instance variable or check in the stored preferences
     *
     * @return String the device token
     */
    public String getDeviceToken(){
        //is it in the instance variable?
        if (deviceToken != null && !deviceToken.isEmpty()) {
            return deviceToken;
        }

        //check is the app settings
        SharedPreferences preferences = delegate.getSharedPreferences(TAG, delegate.getApplicationContext().MODE_PRIVATE);
        deviceToken = preferences.getString(getDeviceTokenKey(), "");

        return deviceToken;
    }

    /**
     * Set the device token. Also stores in the application shared preferences.
     *
     * @param deviceToken
     */
    private void setDeviceToken(String deviceToken) {
        if(deviceToken != null) {
            this.deviceToken = deviceToken;
            SharedPreferences preferences = delegate.getSharedPreferences(TAG, delegate.getApplicationContext().MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(getDeviceTokenKey(), deviceToken);
            editor.apply();
        }
    }

    /**
     * Returns the key used to look up SharedPreferences for this module.
     *
     * @return The key used to look up saved preferences
     */
    private String getDeviceTokenKey() {
        Context context = delegate.getApplicationContext();
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return String.format("com.zeropush.api.deviceToken:%s", packageInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    public void registerDeviceToken(String deviceToken) {
        registerDeviceTokenToChannel(deviceToken, null);
    }

    /**
     * Registers a device token with ZeroPush. If a channel is passed, then to subscribes the device token to that channel.
     *
     * @param deviceToken - Device token returned by the
     * @param channel - Optional channel to subscribe the device token
     */
    public void registerDeviceTokenToChannel(String deviceToken, String channel) {
        RequestParams params = new RequestParams();
        params.put("device_token", this.deviceToken);
        params.put("auth_token", this.apiKey);
        if(channel != null) {
            params.put("channel", channel);
        }

        asyncRequest("POST", "/register", params, null);
    }

    public void verifyCredentials(ZeroPushResponseHandler handler) {
        RequestParams params = new RequestParams("auth_token", this.apiKey);
        asyncRequest("GET", "/verify_credentials", params, handler);
    }

    /**
     * Subscribe the device to a channel.
     *
     * @param channel
     */
    public void subscribeToChannel(String channel) {
        subscribeToChannel(channel, null);
    }

    public void subscribeToChannel(String channel, ZeroPushResponseHandler handler) {
        RequestParams params = new RequestParams("auth_token", this.apiKey, "device_token", this.deviceToken, "channel", channel);
        asyncRequest("POST", "/subscribe", params, handler);
    }

    /**
     * Unsubscribe the device from a channel
     *
     * @param channel
     */
    public void unsubscribeFromChannel(String channel) {
        unsubscribeFromChannel(channel, null);
    }
    public void unsubscribeFromChannel(String channel, ZeroPushResponseHandler handler) {
        RequestParams params = new RequestParams("auth_token", this.apiKey, "device_token", this.deviceToken, "channel", channel);
        asyncRequest("POST", "/unsubscribe", params, handler);
    }

    /**
     * Unsubscribe the device from all channels.
     *
     */
    public void unsubscribeFromAllChannels() {
        unsubscribeFromAllChannels(null);
    }
    public void unsubscribeFromAllChannels(ZeroPushResponseHandler handler) {
        String url = String.format("/devices/%s", this.deviceToken);
        RequestParams params = new RequestParams("auth_token", this.apiKey, "channel_list", "");
        asyncRequest("PUT", url, params, handler);
    }

    public void getChannels(ZeroPushResponseHandler handler ) {
        String url = String.format("/devices/%s", this.deviceToken);
        RequestParams params = new RequestParams("auth_token", this.apiKey);
        asyncRequest("GET", url, params, handler);
    }

    /**
     * Replace the current channel subscriptions with the provided list.
     *
     * @param channels
     */
    public void setChannels(List<String> channels) {
        setChannels(channels, null);
    }
    public void setChannels(List<String> channels, ZeroPushResponseHandler handler) {
        String url = String.format("/devices/%s", this.deviceToken);
        RequestParams params = new RequestParams("auth_token", this.apiKey, "channel_list", join(channels.iterator(), ","));
        asyncRequest("PUT", url, params, handler);
    }

    private void asyncRequest(String verb, String url, RequestParams params, ZeroPushResponseHandler handler) {
        if(handler == null) {
            handler = new ZeroPushResponseHandler();
        }

        java.lang.reflect.Method method;
        try {
            Class[] types = new Class[]{String.class, RequestParams.class, ResponseHandlerInterface.class};
            method = httpClient.getClass().getMethod(verb.toLowerCase(), types);
            method.invoke(
                    httpClient,
                    ZeroPushAPIHost + url,
                    params,
                    handler);

        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private boolean isGooglePlayServicesAvailable() {

        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(delegate);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, delegate, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                delegate.finish();
            }
            return false;
        }
        return true;
    }

    //Helper Method:
    private static String join(final Iterator<String> iterator, final String separator) {
        // handle null, zero and one elements before building a buffer
        if (iterator == null) {
            return null;
        }
        if (!iterator.hasNext()) {
            return "";
        }
        final String first = iterator.next();
        if (!iterator.hasNext()) {
            return first;
        }

        // two or more elements
        final StringBuilder buf = new StringBuilder(256); // Java default is 16, probably too small
        if (first != null) {
            buf.append(first);
        }

        while (iterator.hasNext()) {
            if (separator != null) {
                buf.append(separator);
            }
            final Object obj = iterator.next();
            if (obj != null) {
                buf.append(obj);
            }
        }
        return buf.toString();
    }
}
