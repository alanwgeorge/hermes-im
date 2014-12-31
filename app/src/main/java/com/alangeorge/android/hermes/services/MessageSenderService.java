package com.alangeorge.android.hermes.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import com.alangeorge.android.hermes.App;
import com.alangeorge.android.hermes.R;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

import java.io.IOException;

public class MessageSenderService extends Service {
    private static final String TAG = "MessageSenderService";

    public static final int MSG_SEND_MESSAGE = 0;
    public static final int MSG_SEND_SUCCESS = 1;
    public static final int MSG_SEND_FAILED = 2;
    public static final String ARG_MESSAGE_TEXT = "arg_message_text";
    public static final String ARG_GCM_ID = "arg_gcm_id";
    public static final String ARG_ERROR_TEXT = "arg_error_text";

    private Messenger serviceMessenger;
    private boolean isRunning = false;

    public MessageSenderService() { }

    @Override
    public void onCreate() {
        super.onCreate();

        HandlerThread thread = new HandlerThread("MessageSenderServiceThread", Process.THREAD_PRIORITY_DEFAULT);
        thread.start();

        serviceMessenger = new Messenger(new InboundMessageHandler(thread.getLooper()));

        isRunning = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return serviceMessenger.getBinder();
    }

    private Result sendGcmMessage(String messageText, String gcmId) throws IOException {
        Log.d(TAG, "sendGcmMessage()");

        Sender sender = new Sender(App.context.getResources().getString(R.string.gcm_sender_key));
        com.google.android.gcm.server.Message.Builder builder = new com.google.android.gcm.server.Message.Builder();
        builder.addData(App.context.getString(R.string.gcm_message_field_message), messageText);
        com.google.android.gcm.server.Message message = builder.build();
        Result result = sender.send(message, gcmId, 5);

        Log.d(TAG, "sent PN with result: " + result);

        return result;
    }

    public boolean isRunning() {
        return isRunning;
    }

    private class InboundMessageHandler extends Handler {

        private InboundMessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(final Message message) {
            switch (message.what) {
                case MSG_SEND_MESSAGE:
                    final String messageText = message.getData().getString(ARG_MESSAGE_TEXT, null);
                    final String gcmId = message.getData().getString(ARG_GCM_ID, null);
                    String errorText = null;

                    if (messageText == null | "".equals(messageText)) {
                        errorText = "messageText either null or empty";
                    }

                    if (gcmId == null | "".equals(gcmId)) {
                        errorText = "gcmId either null or empty";
                    }

                    if (errorText != null) {
                        Message reply = Message.obtain(null, MSG_SEND_FAILED);
                        reply.setData(message.getData());
                        reply.getData().putString(ARG_ERROR_TEXT, errorText);
                        try {
                            message.replyTo.send(reply);
                        } catch (RemoteException e) {
                            Log.e(TAG, "failed to send reply: " + reply.toString());
                        }
                        return;
                    }

                    Message reply = Message.obtain(null, MSG_SEND_SUCCESS);
                    Result result = null;
                    try {
                        result = sendGcmMessage(messageText, gcmId);
                    } catch (IOException e) {
                        Log.e(TAG, "failed to send GCM message: IOException", e);
                        reply = Message.obtain(null, MSG_SEND_FAILED);
                    }

                    if (result != null && result.getErrorCodeName() != null) {
                        Log.e(TAG, "failed to send GCM message: " + result.getErrorCodeName());
                        reply = Message.obtain(null, MSG_SEND_FAILED);
                    }

                    if (result != null && result.getCanonicalRegistrationId() != null) {
                        Log.i(TAG, "Canonical ID returned");
                        //TODO update contact with new GCM ID
                    }

                    reply.getData().putString(ARG_MESSAGE_TEXT, messageText);
                    reply.getData().putString(ARG_GCM_ID, gcmId);
                    try {
                        message.replyTo.send(reply);
                    } catch (RemoteException e) {
                        Log.e(TAG, "error communicating message send requester", e);
                    }

                    break;
                default:
                    super.handleMessage(message);
            }
        }
    }
}
