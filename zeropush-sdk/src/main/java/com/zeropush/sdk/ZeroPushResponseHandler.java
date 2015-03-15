package com.zeropush.sdk;

import com.loopj.android.http.JsonHttpResponseHandler;
import android.util.Log;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by stefan on 12/17/14.
 */

public class ZeroPushResponseHandler extends JsonHttpResponseHandler {
    @Override
    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
        Error error;
        //prefer errorResponse
        if (errorResponse != null) {
            error = new Error(errorResponse.toString(), throwable);
        }
        else {
            error = new Error(throwable.getMessage(), throwable);
        }

        handle((JSONObject)null, statusCode, error);
    }
    @Override
    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
        handle(response, statusCode, null);
    }
    @Override
    public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
        handle(response, statusCode, null);
    }

    /**
     * Default handlers just do some logging
     *
     * @param success
     * @param statusCode
     * @param error
     */
    public void handle(JSONObject success, int statusCode, Error error) {
        if(error != null) {
            Log.e(ZeroPush.TAG, error.getMessage(), error.getCause());
            return;
        }
        Log.d(ZeroPush.TAG, success.toString());
    }

    /**
     * Default handlers just do some logging
     *
     * @param success
     * @param statusCode
     * @param error
     */
    public void handle(JSONArray success, int statusCode, Error error) {
        if(error != null) {
            Log.e(ZeroPush.TAG, error.getMessage(), error.getCause());
            return;
        }
        Log.d(ZeroPush.TAG, success.toString());
    }
}