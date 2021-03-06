package com.alangeorge.android.hermes;

import android.support.annotation.NonNull;
import android.util.Base64;

import com.alangeorge.android.hermes.model.Contact;
import com.alangeorge.android.hermes.model.Message;

import java.security.KeyPair;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;

import timber.log.Timber;

public class MessageMaker {
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
    public @NonNull Message make(@NonNull String messageText, @NonNull Contact to,  @NonNull String fromGcmId, @NonNull KeyPair fromKeyPair) throws MessageMakerException {
        Message message = new Message();
        Message.Body body = new Message.Body();

        body.setSenderGcmRegistrationId(fromGcmId);
        body.setSenderPublicKey(Base64.encodeToString(fromKeyPair.getPublic().getEncoded(), Base64.NO_WRAP));

        SecretKeySpec symmetricKey = generateSymmetricKey();

        // encode the message with symmetric key
        byte[] messageSymmetricEncodedBytes;
        try {
            Cipher cipher = Cipher.getInstance(App.DEFAULT_AES_CIPHER, App.DEFAULT_AES_SECURITY_PROVIDER);
            cipher.init(Cipher.ENCRYPT_MODE, symmetricKey);
            messageSymmetricEncodedBytes = cipher.doFinal(messageText.getBytes(App.DEFAULT_CHARACTER_SET));
        } catch (Exception e) {
            throw new MessageMakerException(e);
        }

        body.setMessage(Base64.encodeToString(messageSymmetricEncodedBytes, Base64.NO_WRAP));

        // encode symmetricKey with receivers public key
        byte[] symmetricKeyEncodedReceiverBytes;
        try {
            Cipher cipher = Cipher.getInstance(App.DEFAULT_RSA_CIPHER, App.DEFAULT_RSA_SECURITY_PROVIDER);
            cipher.init(Cipher.ENCRYPT_MODE, to.getPublicKey());
            symmetricKeyEncodedReceiverBytes = cipher.doFinal(symmetricKey.getEncoded());
        } catch (Exception e) {
            throw new MessageMakerException(e);
        }

        body.setReceiverEncodedSymKey(Base64.encodeToString(symmetricKeyEncodedReceiverBytes, Base64.NO_PADDING));

        // encode symmetricKey with senders public key
        byte[] symmetricKeyEncodedSenderBytes;
        try {
            Cipher cipher = Cipher.getInstance(App.DEFAULT_RSA_CIPHER, App.DEFAULT_RSA_SECURITY_PROVIDER);
            cipher.init(Cipher.ENCRYPT_MODE, fromKeyPair.getPublic());
            symmetricKeyEncodedSenderBytes = cipher.doFinal(symmetricKey.getEncoded());
        } catch (Exception e) {
            throw new MessageMakerException(e);
        }

        message.setSenderEncodedSymKey(Base64.encodeToString(symmetricKeyEncodedSenderBytes, Base64.NO_PADDING));

        message.setBody(body);
        message.sign(fromKeyPair.getPrivate());

        String messageJson = message.toJson();
        Timber.d("message json: " + messageJson);
        Timber.d("message json length: " + messageJson.length());

        return message;
    }

    private SecretKeySpec generateSymmetricKey() throws MessageMakerException {
        SecretKeySpec secretKeySpec;
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256, App.secureRandom);
            secretKeySpec = new SecretKeySpec((keyGenerator.generateKey()).getEncoded(), "AES");
        } catch (Exception e) {
            throw new MessageMakerException(e);
        }

        return secretKeySpec;
    }
}
