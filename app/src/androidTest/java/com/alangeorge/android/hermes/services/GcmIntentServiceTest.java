package com.alangeorge.android.hermes.services;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.test.ServiceTestCase;
import android.util.Log;

import com.alangeorge.android.hermes.R;
import com.alangeorge.android.hermes.TestingUtils;
import com.alangeorge.android.hermes.model.Contact;
import com.alangeorge.android.hermes.model.Message;

/**
 * From adb you can send a message to the GcmBroadcastReceiver that will trigger an Intent to the GcmIntentService.
 * You have to remove the com.google.android.c2dm.permission.SEND permission from the Manifest for this to work
 * adb shell
 * am broadcast -a com.google.android.c2dm.intent.RECEIVE -n com.alangeorge.android.hermes/com.alangeorge.android.hermes.services.GcmBroadcastReceiver --es "hermes_message" '{"body":{"gcmRegistrationId":"APA91bEtwCaTGLQoACaRkkzTCdeTtsNQHa14wgyfukoJ0y4V8E2QXbjfH40UHI8Edq-_DmkdwSA4s4M9gasZxnSQZfN0cZwa9rbkslkICFOOYgrRdsjWc3ppbzH37aBWlannpTFvzVSH-1lrsy8YNempDhfbDPNKQJYolYLbPEs2u0rR-ld8HIc","message":"sAgnljVyAEGWCFgKcOMe8A\u003d\u003d","messageKey":"eJ7N/4RM6twfHBljsAcpgtah1yyVt3TN1xPlJV3bRtzzjtUSJkSPk4qgHhIw4RbefBIoVqSdRZq4\nNEDm2SMl9efCBORCQHQTq5+MagY3sfQYerEzT4tCMzZZO+qbNC3QkxtnmHk2OuUZV1ojf+7JeEyV\n3uTXjFioadVRcYKtHxZsM5cDFHMduMFRCZwSSiUcHMK6fqbNrabn8ZuaqP92F6Dylvv7twox0iKt\nwVMoRFh6sNwA+lZzizwpxX15uW1tur8J8EwQ3z+RvtO//DcHzUxDGy0itfH63Sf5c59UQyTlLvQK\nZ08qsWH7tM/VvESDJryM4zYvBdfvPrcAIn4GHA\n","publicKey":"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAx/xTfOAkpYsBCjVCrQr5damjrBjUtPWewElM9E2jDcOar4CZ7uVKkqnUG0KF/aOcjjm2xPaUCwC0AXVr7Ds4qB7bDzQxQ2sxmtk6i4jnCSNJ3JTNzIljbEUuC6o2rB1oL+sZgI+8ZBqLp9GNzutH5wfBp+An4gdajTSN8C2TnlWBcZ8K+XcPE5PtqNfbkMtgMB8uhZlGeyLHXLheVZvtYkgH0fFO+2uceoQN9H7u5DejrM7oYWE8gHG+72JhjTKXQOQ6tcG73nMcq/63NQvDFnF8dcICXdbnHt/39YmDZRhrqjKqkX/MtdIqoINyBkVuuhey/C0BT12fxOGG+vhDcQIDAQAB"},"signature":"uStQQS9EktTkdjX3GVGg4vd79ahNhIwtzur3VbeOOJL79ZVy3+o4HjTU1/hTYhCjYchEEZzTntSfCfTsQI0vNu36ZTT7X1/Pz6fNu5GVvBlkFRpHcBj0nXEr7lhyz/ZRgKOlDS5D436vXOt+bGD/2kuwua+HnDIB5ikFzC2TEOQrapVoAOtjDv+HTMn8vlLpq6KNIIDZuMcqnhZVw/NajreE1xsr3sLrWwm12fxAaK7qqPOsPSy5VB+dov89VCIQhqkW2NQ/RSRPeIb9FJZcxrtjDJHVYKcBGUJu1bSsyH+knLfBGP70o9M1ilcc89CXJ3xj8K9l3uOqbRj7mglvLQ\u003d\u003d"}'
 */
public class GcmIntentServiceTest extends ServiceTestCase<GcmIntentService> {
    @SuppressWarnings("UnusedDeclaration")
    private static final String TAG = "Hermes.GcmIntentServiceTest";

    private boolean didMessageStatusMessageArrive = false;
    private boolean didMessageSuccess = false;
    private final Object waitLock = new Object();
    private String message1 = "{\"body\":{\"gcmRegistrationId\":\"APA91bEtwCaTGLQoACaRkkzTCdeTtsNQHa14wgyfukoJ0y4V8E2QXbjfH40UHI8Edq-_DmkdwSA4s4M9gasZxnSQZfN0cZwa9rbkslkICFOOYgrRdsjWc3ppbzH37aBWlannpTFvzVSH-1lrsy8YNempDhfbDPNKQJYolYLbPEs2u0rR-ld8HIc\",\"message\":\"cYTr2JyNi8SA/Ibl5r63vQ\\u003d\\u003d\",\"messageKey\":\"o2Io1SbH56Np2HvtKo4YPPs5TdKh1zsHCzjg9NwU1q0CG4UMF/h8s452S0rINJgy1tTqqYgIffEU\\n5vdf7KbASza+tcrfVv/1RzNXjJtZQUca/9rC+PLDDPXRl45c3I079Ygr3cfyEOX//8/S8hDe+kkV\\nfM2XQuvWAujoc8IYTce84+nUBMhtRxcGIU0IhaN9vD/+s0SXb4sDjGBTBzpbgRjVFVrYN8Uafew4\\nD3tUODNzq4yKDDPhJmt5BUsNk8fiARzDXWpDejEY+n7lNToiS3uqHUeI0Anor8y9SAXD4jYCfwZh\\nMdO2jNC3qlxvSOLrg/qmdlkOMyX5PQL120yxPw\\n\",\"senderPublicKey\":\"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAx/xTfOAkpYsBCjVCrQr5damjrBjUtPWewElM9E2jDcOar4CZ7uVKkqnUG0KF/aOcjjm2xPaUCwC0AXVr7Ds4qB7bDzQxQ2sxmtk6i4jnCSNJ3JTNzIljbEUuC6o2rB1oL+sZgI+8ZBqLp9GNzutH5wfBp+An4gdajTSN8C2TnlWBcZ8K+XcPE5PtqNfbkMtgMB8uhZlGeyLHXLheVZvtYkgH0fFO+2uceoQN9H7u5DejrM7oYWE8gHG+72JhjTKXQOQ6tcG73nMcq/63NQvDFnF8dcICXdbnHt/39YmDZRhrqjKqkX/MtdIqoINyBkVuuhey/C0BT12fxOGG+vhDcQIDAQAB\"},\"signature\":\"YDW+dlDYuPwnVzRcTKeL99VeZOCluwg1VkQqTvME1gzL6amBxLTMrTS0erUTbPK0+toZwoCHyF7s8rXYoOqtlkQv5tIOwB6JvXpnnr6C0aST15iz2ob+cA7nHz+UokH1/zUPwZAKF+xJKia6VkkGrl0nqzRjTpsyLEso+qqXDpw5X7gw7x1+eZTojOC8MyUJg2G8yYtmaVeR3Z8xHLdjU8wqNdXMG9joEbHKHQbvsYL5Tbhlh3QORYSU47WLItojl6chuleVc2eJ4VuNgTkLEADafD/vZrQ3JWHjeGBQ1L6FyYYaCmiT8z/BQrqYXSRKJrGDU5r36NMhJS0tv7SWeA\\u003d\\u003d\"}";
    @SuppressWarnings("FieldCanBeLocal")
    private String badMessage1 = "{\"body\":{\"gcmRegistrationId\":\"ZPA91bEtwCaTGLQoACaRkkzTCdeTtsNQHa14wgyfukoJ0y4V8E2QXbjfH40UHI8Edq-_DmkdwSA4s4M9gasZxnSQZfN0cZwa9rbkslkICFOOYgrRdsjWc3ppbzH37aBWlannpTFvzVSH-1lrsy8YNempDhfbDPNKQJYolYLbPEs2u0rR-ld8HIc\",\"message\":\"cYTr2JyNi8SA/Ibl5r63vQ\\u003d\\u003d\",\"messageKey\":\"o2Io1SbH56Np2HvtKo4YPPs5TdKh1zsHCzjg9NwU1q0CG4UMF/h8s452S0rINJgy1tTqqYgIffEU\\n5vdf7KbASza+tcrfVv/1RzNXjJtZQUca/9rC+PLDDPXRl45c3I079Ygr3cfyEOX//8/S8hDe+kkV\\nfM2XQuvWAujoc8IYTce84+nUBMhtRxcGIU0IhaN9vD/+s0SXb4sDjGBTBzpbgRjVFVrYN8Uafew4\\nD3tUODNzq4yKDDPhJmt5BUsNk8fiARzDXWpDejEY+n7lNToiS3uqHUeI0Anor8y9SAXD4jYCfwZh\\nMdO2jNC3qlxvSOLrg/qmdlkOMyX5PQL120yxPw\\n\",\"senderPublicKey\":\"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAx/xTfOAkpYsBCjVCrQr5damjrBjUtPWewElM9E2jDcOar4CZ7uVKkqnUG0KF/aOcjjm2xPaUCwC0AXVr7Ds4qB7bDzQxQ2sxmtk6i4jnCSNJ3JTNzIljbEUuC6o2rB1oL+sZgI+8ZBqLp9GNzutH5wfBp+An4gdajTSN8C2TnlWBcZ8K+XcPE5PtqNfbkMtgMB8uhZlGeyLHXLheVZvtYkgH0fFO+2uceoQN9H7u5DejrM7oYWE8gHG+72JhjTKXQOQ6tcG73nMcq/63NQvDFnF8dcICXdbnHt/39YmDZRhrqjKqkX/MtdIqoINyBkVuuhey/C0BT12fxOGG+vhDcQIDAQAB\"},\"signature\":\"YDW+dlDYuPwnVzRcTKeL99VeZOCluwg1VkQqTvME1gzL6amBxLTMrTS0erUTbPK0+toZwoCHyF7s8rXYoOqtlkQv5tIOwB6JvXpnnr6C0aST15iz2ob+cA7nHz+UokH1/zUPwZAKF+xJKia6VkkGrl0nqzRjTpsyLEso+qqXDpw5X7gw7x1+eZTojOC8MyUJg2G8yYtmaVeR3Z8xHLdjU8wqNdXMG9joEbHKHQbvsYL5Tbhlh3QORYSU47WLItojl6chuleVc2eJ4VuNgTkLEADafD/vZrQ3JWHjeGBQ1L6FyYYaCmiT8z/BQrqYXSRKJrGDU5r36NMhJS0tv7SWeA\\u003d\\u003d\"}";
    private Intent intent1;
    private int lastGcmIntentServiceStatusReceived = -1;

    public GcmIntentServiceTest() {
        super(GcmIntentService.class);
    }

    private final BroadcastReceiver incomingMessageBroadcastReceiver = new BroadcastReceiver() {
        private static final String TAG = "Hermes.GcmIntentServiceTest.incomingMessageBroadcastReceiver";
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive(" + intent + ")");
            didMessageStatusMessageArrive = true;

            lastGcmIntentServiceStatusReceived = intent.getIntExtra(GcmIntentService.ARG_INCOMING_MESSAGE_STATUS, -1);

            switch (lastGcmIntentServiceStatusReceived) {
                case GcmIntentService.INCOMING_MESSAGE_SUCCESSFUL:
                    didMessageSuccess = true;
                    break;
                default:
                    didMessageSuccess = false;
            }

            synchronized (waitLock) {
                waitLock.notifyAll();
            }
        }
    };

    @Override
    public void setUp() throws Exception {
        super.setUp();
        didMessageStatusMessageArrive = false;
        didMessageSuccess = false;

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(incomingMessageBroadcastReceiver, new IntentFilter(GcmIntentService.ACTION_INCOMING_MESSAGE_STATUS));

        intent1 = new Intent(getContext(), GcmIntentService.class);
        intent1.setAction("com.google.android.c2dm.intent.RECEIVE");
        ComponentName comp = new ComponentName(getContext().getPackageName(), GcmIntentService.class.getName());
        intent1.setComponent(comp);
    }

    @Override
    public void tearDown() throws Exception {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(incomingMessageBroadcastReceiver);
        super.tearDown();
    }

    public void testGcmIntentDeliveryHappyPath() throws Exception {
        intent1.putExtra(getContext().getString(R.string.gcm_message_field_message), message1);

        Contact contact = new Contact();
        contact.setName("Micky Mouse");
        contact.setGcmId("dummyGcmId");
        Message message = new Message(message1);
        contact.setPublicKeyEncoded(message.getBody().getSenderPublicKey());
        contact = TestingUtils.persistContact(contact);

        startService(intent1);

        Log.d(TAG, "wait()ing thread for up to 5 seconds for message receive status to arrive from GcmIntentService");
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

        assertTrue("contact delete unsuccessful", TestingUtils.deleteContact(contact.getId()) == 1);

        assertTrue("incoming message status message did not arrive from GcmIntentService", didMessageStatusMessageArrive);
        assertTrue("incoming message status was not success", didMessageSuccess);
    }

    public void testGcmIntentDeliveryFailValidation() throws Exception {
        intent1.putExtra(getContext().getString(R.string.gcm_message_field_message), badMessage1);

        startService(intent1);

        Log.d(TAG, "wait()ing thread for up to 5 seconds for message receive status to arrive from GcmIntentService");
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

        assertTrue("incoming message status message did not arrive from GcmIntentService", didMessageStatusMessageArrive);
        assertFalse("incoming message status was success, expected failure", didMessageSuccess);
        assertEquals("incorrect error code", lastGcmIntentServiceStatusReceived, GcmIntentService.INCOMING_MESSAGE_ERROR_FAILED_SIGNATURE_VERIFY);
    }

    public void testGcmIntentDeliveryFailNoHermesMessage() throws Exception {
        intent1.putExtra("message", "HelloWorld");

        startService(intent1);

        Log.d(TAG, "wait()ing thread for up to 5 seconds for message receive status to arrive from GcmIntentService");
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

        assertTrue("incoming message status message did not arrive from GcmIntentService", didMessageStatusMessageArrive);
        assertFalse("incoming message status was success, expected failure", didMessageSuccess);
        assertEquals("incorrect error code", lastGcmIntentServiceStatusReceived, GcmIntentService.INCOMING_MESSAGE_ERROR_NO_HERMES_MESSAGE_FOUND);
    }

    public void testGcmIntentDeliveryFailNoContact() throws Exception {
        intent1.putExtra(getContext().getString(R.string.gcm_message_field_message), message1);

        startService(intent1);

        Log.d(TAG, "wait()ing thread for up to 5 seconds for message receive status to arrive from GcmIntentService");
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

        assertTrue("incoming message status message did not arrive from GcmIntentService", didMessageStatusMessageArrive);
        assertFalse("incoming message status was success, expected failure", didMessageSuccess);
        assertEquals("incorrect error code", lastGcmIntentServiceStatusReceived, GcmIntentService.INCOMING_MESSAGE_ERROR_NOT_FOUND_CONTACT);
    }

    public void testBroadcastHermesGcmMessageHappyPath() throws Exception {
        Intent intent = new Intent(getContext(), GcmBroadcastReceiver.class);
        intent.setAction("com.google.android.c2dm.intent.RECEIVE");
        Bundle extras = new Bundle();
        extras.putString(getContext().getResources().getString(R.string.gcm_message_field_message), message1);
        intent.putExtras(extras);
        ComponentName comp = new ComponentName(getContext().getPackageName(), GcmBroadcastReceiver.class.getName());
        intent.setComponent(comp);

        Contact contact = new Contact();
        contact.setName("Micky Mouse");
        contact.setGcmId("dummyGcmId");
        Message message = new Message(message1);
        contact.setPublicKeyEncoded(message.getBody().getSenderPublicKey());
        contact = TestingUtils.persistContact(contact);

        getContext().sendOrderedBroadcast(intent, null);

        Log.d(TAG, "wait()ing thread for up to 5 seconds for message receive status to arrive from GcmIntentService");
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

        assertTrue("contact delete unsuccessful", TestingUtils.deleteContact(contact.getId()) == 1);

        assertTrue("incoming message status message did not arrive from GcmIntentService", didMessageStatusMessageArrive);
        assertTrue("incoming message status was not success", didMessageSuccess);
    }

}