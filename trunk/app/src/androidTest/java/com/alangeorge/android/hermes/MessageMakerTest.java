package com.alangeorge.android.hermes;

import android.test.ActivityUnitTestCase;
import android.util.Base64;
import android.util.Log;

import com.alangeorge.android.hermes.model.Contact;
import com.alangeorge.android.hermes.model.Message;

import java.security.KeyPair;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import static com.alangeorge.android.hermes.App.*;
import static com.alangeorge.android.hermes.App.DEFAULT_AES_CIPHER;
import static com.alangeorge.android.hermes.App.DEFAULT_AES_SECURITY_PROVIDER;
import static com.alangeorge.android.hermes.App.DEFAULT_CHARACTER_SET;
import static com.alangeorge.android.hermes.App.DEFAULT_RSA_CIPHER;
import static com.alangeorge.android.hermes.App.DEFAULT_RSA_SECURITY_PROVIDER;

public class MessageMakerTest extends ActivityUnitTestCase<MainActivity> {
    private static final String TAG = "MessageMakerTest";

    public MessageMakerTest() {
        super(MainActivity.class);
    }

    public void testMake() throws Exception {
        Log.d(TAG, "testMake()");
        String fromKeyPairAlias = "test_from_alias";
        String toKeyPairAlias = "test_to_alias";
        MessageMaker messageMaker = new MessageMaker();
        String gcmId = getGcmRegistrationId();
        KeyPair fromKeyPair = makeKeyPair(fromKeyPairAlias);
        KeyPair toKeyPair = makeKeyPair(toKeyPairAlias);
//        toKeyPair = fromKeyPair;

        String messageText = "RSA stands for Ron Rivest, Adi Shamir and Leonard Adleman. They developed the algorithm by using the large integer factorization technique in 1977. It has since become so popular that we almost depend on similar technologies used in everyday life, such as banking, messaging, etc. As we briefly mentioned before, this type of algorithm uses a pair of keys used for encryption and decryption respectively.";

        assertNotSame("GCM id not found", gcmId, "");
        assertNotNull("fromKeyPair is null", fromKeyPair);

        Contact to = new Contact();
        to.setId(100);
        to.setGcmId(gcmId); // to and from gcm the same here
        to.setCreateTime(new Date());
        to.setName("Micky Mouse");
        to.setPublicKeyEncoded(Base64.encodeToString(toKeyPair.getPublic().getEncoded(), Base64.NO_WRAP));

        Message message = messageMaker.make(messageText, to, gcmId, fromKeyPair);

        assertTrue("message failed to verify()", message.verify(fromKeyPair.getPublic()));

        String messageJson = message.toJson();

        //noinspection UnusedAssignment
        message = null;

        // create new message from previous message's Json
        message = new Message(messageJson);

        assertTrue("new message failed to verify() after Json marshalling", message.verify(fromKeyPair.getPublic()));

        // now we attempt to retrieve our message

        // decode the symmetricKey using receivers private key
        byte[] symmetricKeyDecodedBytes;
        Cipher cipher = Cipher.getInstance(DEFAULT_RSA_CIPHER, DEFAULT_RSA_SECURITY_PROVIDER);
        cipher.init(Cipher.DECRYPT_MODE, toKeyPair.getPrivate());
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

        deleteKeyPair(fromKeyPairAlias);
        deleteKeyPair(toKeyPairAlias);
    }
}