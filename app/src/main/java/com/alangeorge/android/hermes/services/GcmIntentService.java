package com.alangeorge.android.hermes.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.alangeorge.android.hermes.MainActivity;
import com.alangeorge.android.hermes.R;
import com.alangeorge.android.hermes.model.Message;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.util.Set;

public class GcmIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    private static final String TAG = "Hermes.GcmIntentService";
    public static final String ACTION_INCOMING_MESSAGE_STATUS = "action_incoming_message_status";
    public static final String ARG_INCOMING_MESSAGE_STATUS = "arg_incoming_message_status";
    public static final int INCOMING_MESSAGE_SUCCESSFUL = 0;
    public static final int INCOMING_MESSAGE_ERROR_NO_HERMES_MESSAGE_FOUND = 1;
    public static final int INCOMING_MESSAGE_ERROR_FAILED_SIGNATURE_VERIFY = 2;

    public GcmIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent(" + intent + ")");
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);

        Intent statusBroadcastIntent = new Intent(ACTION_INCOMING_MESSAGE_STATUS);

        String messageType = gcm.getMessageType(intent);
        if (extras != null && !extras.isEmpty()) {  // has effect of unparcelling Bundle
            switch (messageType) {
                case GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR:
                    sendNotification(this, "Send error: " + extras.toString());
                    break;
                case GoogleCloudMessaging.MESSAGE_TYPE_DELETED:
                    sendNotification(this, "Deleted messages on server: " + extras.toString());
                    // If it's a regular GCM message, do some work.
                    break;
                case GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE:
                    // Post notification of received message.

                    Set<String> bundleKeys = extras.keySet();
                    for (String key : bundleKeys) {
                        Log.d(TAG, "intent bundle key:value " + key + ":" + extras.get(key).toString());
                    }

                    String hermesMessageJson = extras.getString(getResources().getString(R.string.gcm_message_field_message), null);

                    if (hermesMessageJson == null || "".equals(hermesMessageJson)) {
                        Log.e(TAG, "received PN with no hermes message");
                        statusBroadcastIntent.putExtra(ARG_INCOMING_MESSAGE_STATUS, INCOMING_MESSAGE_ERROR_NO_HERMES_MESSAGE_FOUND);
                        break;
                    }

                    Message message = new Message(hermesMessageJson);

                    if (! message.verifySignature()) {
                        Log.e(TAG, "message failed signature verification");
                        statusBroadcastIntent.putExtra(ARG_INCOMING_MESSAGE_STATUS, INCOMING_MESSAGE_ERROR_FAILED_SIGNATURE_VERIFY);
                        break;
                    }
                    statusBroadcastIntent.putExtra(ARG_INCOMING_MESSAGE_STATUS, INCOMING_MESSAGE_SUCCESSFUL);
                    break;
                default:
                    Log.e(TAG, "unknown GCM message type: " + messageType);
                    break;
            }

            LocalBroadcastManager.getInstance(this).sendBroadcast(statusBroadcastIntent);
        }

        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    public static  void sendNotification(Context context, String msg) {
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("GCM Notification")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}
