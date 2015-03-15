package jp.co.shiratsu.samplezeropush;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.zeropush.sdk.ZeroPushBroadcastReceiver;

/**
 * Created by shiratsu on 15/03/10.
 */
public class IntentReceiver extends ZeroPushBroadcastReceiver {
    @Override
    public void onPushReceived(Context context, Intent intent, Bundle extras) {

        NotificationManager manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0);

        Notification notification = new Notification.Builder(context)
                .setContentTitle("Got it!")
                .setContentText(extras.toString())
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();

        manager.notify(1, notification);
    }
}
