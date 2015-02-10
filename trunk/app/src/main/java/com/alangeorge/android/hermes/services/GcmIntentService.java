package com.alangeorge.android.hermes.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.alangeorge.android.hermes.App;
import com.alangeorge.android.hermes.MainActivity;
import com.alangeorge.android.hermes.R;
import com.alangeorge.android.hermes.model.Contact;
import com.alangeorge.android.hermes.model.Message;
import com.alangeorge.android.hermes.model.ModelException;
import com.alangeorge.android.hermes.model.dao.DBHelper;
import com.alangeorge.android.hermes.model.provider.HermesContentProvider;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Set;

public class GcmIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    private static final String TAG = "GcmIntentService";
    public static final String ACTION_INCOMING_MESSAGE_STATUS = "action_incoming_message_status";
    public static final String ARG_INCOMING_MESSAGE_STATUS = "arg_incoming_message_status";
    public static final String ARG_INCOMING_MESSAGE = "arg_incoming_message";
    public static final int INCOMING_MESSAGE_SUCCESSFUL = 0;
    public static final int INCOMING_MESSAGE_ERROR_NO_HERMES_MESSAGE_FOUND = 1;
    public static final int INCOMING_MESSAGE_ERROR_FAILED_SIGNATURE_VERIFY = 2;
    public static final int INCOMING_MESSAGE_ERROR_NOT_FOUND_CONTACT = 3;
    public static final int INCOMING_MESSAGE_ERROR_UNMARSHALING_CONTACT = 4;
    public static final int INCOMING_MESSAGE_UNSUPPORTED_CHARACTER_SET = 5;


    public GcmIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent(" + intent + ")");
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);


        String messageType = gcm.getMessageType(intent);
        if (extras != null && !extras.isEmpty()) {
            switch (messageType) {
                case GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR:
                    sendNotification(this, "Send error: " + extras.toString());
                    break;
                case GoogleCloudMessaging.MESSAGE_TYPE_DELETED:
                    sendNotification(this, "Deleted messages on server: " + extras.toString());
                    // If it's a regular GCM message, do some work.
                    break;
                case GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE:

                    Set<String> bundleKeys = extras.keySet();
                    for (String key : bundleKeys) {
                        Log.d(TAG, "intent bundle key:value " + key + ":" + extras.get(key).toString());
                    }

                    String hermesMessageJson = extras.getString(getResources().getString(R.string.gcm_message_field_message), null);

                    if (hermesMessageJson == null || "".equals(hermesMessageJson)) {
                        Log.e(TAG, "received PN with no hermes message");
                        Intent errorIntent = new Intent(ACTION_INCOMING_MESSAGE_STATUS);
                        errorIntent.putExtra(ARG_INCOMING_MESSAGE_STATUS, INCOMING_MESSAGE_ERROR_NO_HERMES_MESSAGE_FOUND);
                        LocalBroadcastManager.getInstance(this).sendBroadcast(errorIntent);
                        return;
                    }

                    LocalBroadcastManager.getInstance(this).sendBroadcast(handleIncomingHermesMessage(hermesMessageJson));
                    break;
                default:
                    Log.e(TAG, "unknown GCM message type: " + messageType);
                    break;
            }
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

    private Intent handleIncomingHermesMessage(String messageJson) {
        Intent statusBroadcastIntent = new Intent(ACTION_INCOMING_MESSAGE_STATUS);
        statusBroadcastIntent.putExtra(ARG_INCOMING_MESSAGE, messageJson);

        Message message = new Message(messageJson);

        // verify message signature
        if (! message.verifySignature()) {
            Log.e(TAG, "message failed signature verification");
            statusBroadcastIntent.putExtra(ARG_INCOMING_MESSAGE_STATUS, INCOMING_MESSAGE_ERROR_FAILED_SIGNATURE_VERIFY);
            return statusBroadcastIntent;
        }

        // look up Contact
        try {
            Uri contactUri = Uri.parse(HermesContentProvider.CONTACTS_CONTENT_BY_PUBLIC_KEY_URI + "/" + URLEncoder.encode(message.getBody().getSenderPublicKey(), App.DEFAULT_CHARACTER_SET.displayName()));
            Cursor contactCursor = App.context.getContentResolver().query(contactUri, DBHelper.CONTACT_ALL_COLUMNS, null, null, null);

            if (contactCursor.getCount() == 0) {
                statusBroadcastIntent.putExtra(ARG_INCOMING_MESSAGE_STATUS, INCOMING_MESSAGE_ERROR_NOT_FOUND_CONTACT);
                return statusBroadcastIntent;
            }

            try {
                Contact contact = Contact.cursorToContact(contactCursor);
                message.setContact(contact);
            } catch (ModelException e) {
                statusBroadcastIntent.putExtra(ARG_INCOMING_MESSAGE_STATUS, INCOMING_MESSAGE_ERROR_UNMARSHALING_CONTACT);
                return statusBroadcastIntent;
            }
            contactCursor.close();
        } catch (UnsupportedEncodingException e) {
            statusBroadcastIntent.putExtra(ARG_INCOMING_MESSAGE_STATUS, INCOMING_MESSAGE_UNSUPPORTED_CHARACTER_SET);
            return statusBroadcastIntent;
        }

        // insert message
        ContentValues messageValues = new ContentValues();
        messageValues.put(DBHelper.MESSAGE_COLUMN_CONTACT_ID, message.getContact().getId());
        messageValues.put(DBHelper.MESSAGE_COLUMN_IS_INBOUND, true);
        messageValues.put(DBHelper.MESSAGE_COLUMN_MESSAGE_JSON, messageJson);

        getContentResolver().insert(HermesContentProvider.MESSAGES_CONTENT_URI, messageValues);

        statusBroadcastIntent.putExtra(ARG_INCOMING_MESSAGE_STATUS, INCOMING_MESSAGE_SUCCESSFUL);

        return statusBroadcastIntent;
    }
}
