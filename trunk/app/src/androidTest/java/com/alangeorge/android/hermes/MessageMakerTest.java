package com.alangeorge.android.hermes;

import android.content.Intent;
import android.test.ActivityUnitTestCase;
import android.util.Base64;
import android.util.Log;
import android.view.ContextThemeWrapper;

import com.alangeorge.android.hermes.model.Contact;
import com.alangeorge.android.hermes.model.Message;

import java.security.KeyPair;
import java.util.Date;

public class MessageMakerTest extends ActivityUnitTestCase<MainActivity> {
    private static final String TAG = "MessageMakerTest";

    private static final String fromKeyPairAlias = "test_from_alias";
    private static final String toKeyPairAlias = "test_to_alias";

    private String gcmId;
    private Contact contact1;
    private KeyPair fromKeyPair1;
    private KeyPair toKeyPair1;


    public MessageMakerTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // http://stackoverflow.com/questions/22364433/activityunittestcase-and-startactivity-with-actionbaractivity
        ContextThemeWrapper context = new ContextThemeWrapper(getInstrumentation().getTargetContext(), R.style.AppTheme);
        setActivityContext(context);

        // onCreate() should register for us GCM, must be on device/emulator with google play services
        startActivity(new Intent(getInstrumentation().getTargetContext(), MainActivity.class), null, null);
        getInstrumentation().waitForIdleSync();

        gcmId = App.getGcmRegistrationId();

        assertNotSame("GCM id not found", gcmId, "");

        fromKeyPair1 = App.makeKeyPair(fromKeyPairAlias);
        toKeyPair1 = App.makeKeyPair(toKeyPairAlias);

        contact1 = new Contact();
        contact1.setId(100);
        contact1.setGcmId(gcmId); // since we are not sending message, to and from gcm ids can be the same here
        contact1.setCreateTime(new Date());
        contact1.setName("Micky Mouse");
        contact1.setPublicKeyEncoded(Base64.encodeToString(toKeyPair1.getPublic().getEncoded(), Base64.NO_WRAP));

        assertNotNull("fromKeyPair1 is null", fromKeyPair1);
        assertNotNull("toKeyPair1 is null", toKeyPair1);
    }

    @Override
    public void tearDown() throws Exception {
        App.deleteKeyPair(fromKeyPairAlias);
        App.deleteKeyPair(toKeyPairAlias);

        super.tearDown();
    }

    // tests creating a Message which includes encryption and signing
    // verifies the message both before and after Json marshaling
    // finally decrypts the message and compares it to the original
    public void testMakeMessage() throws Exception {
        Log.d(TAG, "testMakeMessage()");
        MessageMaker messageMaker = new MessageMaker();

        String messageText = "RSA stands for Ron Rivest, Adi Shamir and Leonard Adleman. They developed the algorithm by using the large integer factorization technique in 1977. It has since become so popular that we almost depend on similar technologies used in everyday life, such as banking, messaging, etc. As we briefly mentioned before, this type of algorithm uses a pair of keys used for encryption and decryption respectively.";

        Message message = messageMaker.make(messageText, contact1, gcmId, fromKeyPair1);

        // decode message with sender key
        assertTrue("sent and received message not equal, outbound decoding", messageText.equals(message.getOutboundMessageClearText(fromKeyPair1.getPrivate())));

        assertTrue("message failed to verifySignature()", message.verifySignature());

        String messageJson = message.toJson();

        //noinspection UnusedAssignment
        message = null;

        // create new message from previous message's Json
        message = new Message(messageJson);

        assertTrue("new message failed to verifySignature() after Json marshalling", message.verifySignature());

        // now we attempt to retrieve our message
        assertTrue("sent and received message not equal, inbound decoding", messageText.equals(message.getInboundMessageClearText(toKeyPair1.getPrivate())));
    }
}