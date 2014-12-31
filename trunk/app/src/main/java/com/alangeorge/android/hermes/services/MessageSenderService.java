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

public class MessageSenderService extends Service {
    private static final String TAG = "MessageSenderService";

    public static final int MSG_SEND_MESSAGE = 0;
    public static final int MSG_SEND_SUCCESS = 1;
    public static final int MSG_SEND_FAILED = 2;
    public static final String ARG_MESSAGE_TEXT = "arg_message_text";
    public static final String ARG_GCM_ID = "arg_gcm_id";
    public static final String ARG_ERROR_TEXT = "arg_error_text";

    private Looper serviceLooper;
    private Messenger serviceMessenger;

    public MessageSenderService() { }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("MessageSenderServiceThread", Process.THREAD_PRIORITY_DEFAULT);
        thread.start();

        serviceLooper = thread.getLooper();
        serviceMessenger = new Messenger(new InboundMessageHandler(serviceLooper));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return serviceMessenger.getBinder();
    }

    @SuppressWarnings("UnusedParameters")
    private void sendGcmMessage(String messageText, String gcmId, Messenger replyTo) {
        Log.d(TAG, "sendGcmMessage()");

        //TODO send GCM message
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

                    sendGcmMessage(messageText, gcmId, message.replyTo);

                    Message reply = Message.obtain(null, MSG_SEND_SUCCESS);
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
