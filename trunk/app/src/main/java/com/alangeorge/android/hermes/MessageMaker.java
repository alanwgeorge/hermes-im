package com.alangeorge.android.hermes;

import android.util.Base64;
import android.util.Log;

import com.alangeorge.android.hermes.model.Contact;
import com.alangeorge.android.hermes.model.Message;

import java.security.KeyPair;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;

import static com.alangeorge.android.hermes.App.DEFAULT_AES_CIPHER;
import static com.alangeorge.android.hermes.App.DEFAULT_AES_SECURITY_PROVIDER;
import static com.alangeorge.android.hermes.App.DEFAULT_CHARACTER_SET;
import static com.alangeorge.android.hermes.App.DEFAULT_RSA_CIPHER;
import static com.alangeorge.android.hermes.App.DEFAULT_RSA_SECURITY_PROVIDER;

public class MessageMaker {
    private static final String TAG = "Hermes.MessageMaker";

    /**
     * Constructs a {@link com.alangeorge.android.hermes.model.Message} that is ready to be sent
     *
     * @param messageText message to send
     * @param to recipient of message as a Contact
     * @param fromGcmId sender GCM registration id
     * @param fromKeyPair sender key pair
     * @return the constructed {@link com.alangeorge.android.hermes.model.Message}
     * @throws MessageMakerException
     */
    public Message make(String messageText, Contact to,  String fromGcmId, KeyPair fromKeyPair) throws MessageMakerException {
        Message message = new Message();
        Message.Body body = new Message.Body();

        body.setGcmRegistrationId(fromGcmId);
        body.setSenderPublicKey(Base64.encodeToString(fromKeyPair.getPublic().getEncoded(), Base64.NO_WRAP));

        SecretKeySpec symmetricKey = generateSymmetricKey();

        // encode the message with symmetric key
        byte[] theTestTextInEncodedBytes;
        try {
            Cipher cipher = Cipher.getInstance(DEFAULT_AES_CIPHER, DEFAULT_AES_SECURITY_PROVIDER);
            cipher.init(Cipher.ENCRYPT_MODE, symmetricKey);
            theTestTextInEncodedBytes = cipher.doFinal(messageText.getBytes(DEFAULT_CHARACTER_SET));
        } catch (Exception e) {
            throw new MessageMakerException(e);
        }

        body.setMessage(Base64.encodeToString(theTestTextInEncodedBytes, Base64.NO_WRAP));

        // encode symmetricKey with receivers public key
        byte[] symmetricKeyEncodedBytes;
        try {
            Cipher cipher = Cipher.getInstance(DEFAULT_RSA_CIPHER, DEFAULT_RSA_SECURITY_PROVIDER);
            cipher.init(Cipher.ENCRYPT_MODE, to.getPublicKey());
            symmetricKeyEncodedBytes = cipher.doFinal(symmetricKey.getEncoded());
        } catch (Exception e) {
            throw new MessageMakerException(e);
        }

        body.setMessageKey(Base64.encodeToString(symmetricKeyEncodedBytes, Base64.NO_PADDING));
        message.setBody(body);
        message.sign(fromKeyPair.getPrivate());

        String messageJson = message.toJson();
        Log.d(TAG, "message json: " + messageJson);
        Log.d(TAG, "message json length: " + messageJson.length());

        return message;
    }

    private SecretKeySpec generateSymmetricKey() throws MessageMakerException {
        SecretKeySpec secretKeySpec;
        try {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(256, App.secureRandom);
            secretKeySpec = new SecretKeySpec((kg.generateKey()).getEncoded(), "AES");
        } catch (Exception e) {
            throw new MessageMakerException(e);
        }

        return secretKeySpec;
    }
}
