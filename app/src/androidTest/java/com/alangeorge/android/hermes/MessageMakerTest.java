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

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import static com.alangeorge.android.hermes.App.DEFAULT_AES_CIPHER;
import static com.alangeorge.android.hermes.App.DEFAULT_AES_SECURITY_PROVIDER;
import static com.alangeorge.android.hermes.App.DEFAULT_CHARACTER_SET;
import static com.alangeorge.android.hermes.App.DEFAULT_RSA_CIPHER;
import static com.alangeorge.android.hermes.App.DEFAULT_RSA_SECURITY_PROVIDER;
import static com.alangeorge.android.hermes.App.deleteKeyPair;
import static com.alangeorge.android.hermes.App.getGcmRegistrationId;
import static com.alangeorge.android.hermes.App.makeKeyPair;

public class MessageMakerTest extends ActivityUnitTestCase<MainActivity> {
    private static final String TAG = "Hermes.MessageMakerTest";

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

        // onCreate() should register for us GCM
        startActivity(new Intent(getInstrumentation().getTargetContext(), MainActivity.class), null, null);

        gcmId = getGcmRegistrationId();

        assertNotSame("GCM id not found", gcmId, "");

        fromKeyPair1 = makeKeyPair(fromKeyPairAlias);
        toKeyPair1 = makeKeyPair(toKeyPairAlias);

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
        deleteKeyPair(fromKeyPairAlias);
        deleteKeyPair(toKeyPairAlias);

        super.tearDown();
    }

    public void testMakeMessage() throws Exception {
        Log.d(TAG, "testMakeMessage()");
        MessageMaker messageMaker = new MessageMaker();

        String messageText = "RSA stands for Ron Rivest, Adi Shamir and Leonard Adleman. They developed the algorithm by using the large integer factorization technique in 1977. It has since become so popular that we almost depend on similar technologies used in everyday life, such as banking, messaging, etc. As we briefly mentioned before, this type of algorithm uses a pair of keys used for encryption and decryption respectively.";

        Message message = messageMaker.make(messageText, contact1, gcmId, fromKeyPair1);

        assertTrue("message failed to verifySignature()", message.verifySignature());

        String messageJson = message.toJson();

        //noinspection UnusedAssignment
        message = null;

        // create new message from previous message's Json
        message = new Message(messageJson);

        assertTrue("new message failed to verifySignature() after Json marshalling", message.verifySignature());

        // now we attempt to retrieve our message

        // decode the symmetricKey using receivers private key
        byte[] symmetricKeyDecodedBytes;
        Cipher cipher = Cipher.getInstance(DEFAULT_RSA_CIPHER, DEFAULT_RSA_SECURITY_PROVIDER);
        cipher.init(Cipher.DECRYPT_MODE, toKeyPair1.getPrivate());
        symmetricKeyDecodedBytes = cipher.doFinal(Base64.decode(message.getBody().getMessageKey(), Base64.NO_WRAP));

        // turn our decrypted bytes into a key
        SecretKeySpec symmetricKeyFromMessage = new SecretKeySpec(symmetricKeyDecodedBytes, DEFAULT_AES_CIPHER);

        // decrypt our message with our decrypted symmetric key
        byte[] theTestTextInDecodedBytes;
        cipher = Cipher.getInstance(DEFAULT_AES_CIPHER, DEFAULT_AES_SECURITY_PROVIDER);
        cipher.init(Cipher.DECRYPT_MODE, symmetricKeyFromMessage);
        theTestTextInDecodedBytes = cipher.doFinal(Base64.decode(message.getBody().getMessage(), Base64.NO_WRAP));

        String receivedMessageText = new String(theTestTextInDecodedBytes, DEFAULT_CHARACTER_SET);

        assertTrue("sent and received message not equal", receivedMessageText.equals(messageText));
    }
}