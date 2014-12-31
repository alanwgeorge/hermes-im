package com.alangeorge.android.hermes.services;

import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.os.RemoteException;
import android.test.ServiceTestCase;
import android.util.Log;

import com.alangeorge.android.hermes.App;

import static com.alangeorge.android.hermes.services.MessageSenderService.ARG_GCM_ID;
import static com.alangeorge.android.hermes.services.MessageSenderService.ARG_MESSAGE_TEXT;
import static com.alangeorge.android.hermes.services.MessageSenderService.MSG_SEND_FAILED;
import static com.alangeorge.android.hermes.services.MessageSenderService.MSG_SEND_MESSAGE;
import static com.alangeorge.android.hermes.services.MessageSenderService.MSG_SEND_SUCCESS;

public class MessageSenderServiceTest extends ServiceTestCase<MessageSenderService> {
    private static final String TAG = "MessageSenderServiceTest";
    private Messenger contactDetailFragmentMessenger = new Messenger(new MessageSenderServiceInboundMessageHandler());
    private boolean didMessageStatusMessageArrive = false;
    private boolean didMessageSuccess = false;
    private final Object waitLock = new Object();

    public MessageSenderServiceTest() {
        super(MessageSenderService.class);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testSendMessage() throws Exception {
        Log.d(TAG, "testSendMessage()");

        IBinder iBinder = bindService(new Intent(getContext(), MessageSenderService.class));
        assertNotNull("iBinder from bindService call is null", iBinder);
        Messenger messageSenderServiceMessenger = new Messenger(iBinder);

        assertEquals("service not running after bind call", getService().isRunning(), true);

        android.os.Message serviceMessage = android.os.Message.obtain(null, MSG_SEND_MESSAGE);
        serviceMessage.getData().putString(ARG_MESSAGE_TEXT, "HelloWorld");
        serviceMessage.getData().putString(ARG_GCM_ID, App.getGcmRegistrationId());
        serviceMessage.replyTo = contactDetailFragmentMessenger;
        try {
            messageSenderServiceMessenger.send(serviceMessage);
        } catch (RemoteException e) {
            Log.e(TAG, "error communicating with MessageSenderService", e);
            fail("error communicating with MessageSenderService");
        }

        Log.d(TAG, "waiting 5 seconds for message send status to arrive");
        long startWait = 0;
        long endWait = 0;
        synchronized (waitLock) {
            if (! didMessageStatusMessageArrive) {
                startWait = System.currentTimeMillis();
                waitLock.wait(5000);
                endWait = System.currentTimeMillis();
            } else {
                Log.d(TAG, "no wait");
            }
        }
        Log.d(TAG, "wait time was " + (endWait - startWait) + " ms");

        assertTrue("message send status message did not arrive", didMessageStatusMessageArrive);
        assertTrue("message send status was not success", didMessageSuccess);
    }

    private class MessageSenderServiceInboundMessageHandler extends Handler {
        @Override
        public void handleMessage(android.os.Message msg) {
            didMessageStatusMessageArrive = true;
            switch (msg.what) {
                case MSG_SEND_SUCCESS:
                    Log.d(TAG, "Message send success");
                    didMessageSuccess = true;
                    synchronized (waitLock) {
                        waitLock.notifyAll();
                    }
                    break;
                case MSG_SEND_FAILED:
                    Log.e(TAG, "Message send failed");
                    break;
                default:
                    Log.d(TAG, "unknown message : " + msg.what);
                    super.handleMessage(msg);
            }
        }
    }
}