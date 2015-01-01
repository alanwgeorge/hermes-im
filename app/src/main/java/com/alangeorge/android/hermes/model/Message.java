package com.alangeorge.android.hermes.model;

import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.security.PrivateKey;
import java.security.Signature;

import static com.alangeorge.android.hermes.App.DEFAULT_RSA_SECURITY_PROVIDER;
import static com.alangeorge.android.hermes.App.DEFAULT_SIGNATURE_ALGORITHM;

public class Message {
    private static final String TAG = "Hermes.Message";

    private Gson gson;

    @Expose
    private String signature;
    @Expose
    private Body body;

    public Message() {
        GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithoutExposeAnnotation();
        gson = builder.create();
    }

    public Message(String json) {
        this();
        Message temp = gson.fromJson(json, getClass());
        this.setBody(temp.getBody());
        this.setSignature(temp.getSignature());
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    public void sign(PrivateKey senderPrivateKey) {
        if (body == null) {
            Log.e(TAG, "message body null, not signing");
            return;
        }

        try {
            Signature secretKeySigner = Signature.getInstance(DEFAULT_SIGNATURE_ALGORITHM, DEFAULT_RSA_SECURITY_PROVIDER);
            secretKeySigner.initSign(senderPrivateKey);
            secretKeySigner.update(body.getGcmRegistrationId().getBytes());
            secretKeySigner.update(body.getSenderPublicKey().getBytes());
            secretKeySigner.update(body.getMessageKey().getBytes());
            secretKeySigner.update(body.getMessage().getBytes());

            signature = Base64.encodeToString(secretKeySigner.sign(), Base64.NO_WRAP);

//                } catch (NoSuchAlgorithmException e) {
//                } catch (InvalidKeyException e) {
//                } catch (SignatureException e) {
        } catch (Exception e) {
            Log.e(TAG, "SHA512 signing error", e);
        }
    }

    public boolean verifySignature() {
        if (body == null) {
            Log.e(TAG, "invalid message, message body is null");
            return false;
        }

        if (body.getSenderPublicKey() == null || "".equals(body.getSenderPublicKey())) {
            Log.e(TAG, "sender public key is null or empty");
            return false;
        }

        try {
            Signature secretKeyVerifier = Signature.getInstance(DEFAULT_SIGNATURE_ALGORITHM, DEFAULT_RSA_SECURITY_PROVIDER);
            secretKeyVerifier.initVerify(Contact.decodePublicKey(body.getSenderPublicKey()));
            secretKeyVerifier.update(body.getGcmRegistrationId().getBytes());
            secretKeyVerifier.update(body.getSenderPublicKey().getBytes());
            secretKeyVerifier.update(body.getMessageKey().getBytes());
            secretKeyVerifier.update(body.getMessage().getBytes());
            return secretKeyVerifier.verify(Base64.decode(signature, Base64.NO_WRAP));

//                } catch (NoSuchAlgorithmException e) {
//                } catch (InvalidKeyException e) {
//                } catch (SignatureException e) {
        } catch (Exception e) {
            Log.e(TAG, "SHA512 signing error", e);
        }

        return false;
    }

    @Override
    public String toString() {
        return "Message{" +
                "signature='" + signature + '\'' +
                ", body=" + body +
                '}';
    }

    public String toJson() {
        return gson.toJson(this);
    }

    public static class Body {
        @Expose
        private String gcmRegistrationId;
        @Expose
        private String senderPublicKey;
        @Expose
        private String messageKey;
        @Expose
        private String message;

        public String getGcmRegistrationId() {
            return gcmRegistrationId;
        }

        public void setGcmRegistrationId(String gcmRegistrationId) {
            this.gcmRegistrationId = gcmRegistrationId;
        }

        public String getSenderPublicKey() {
            return senderPublicKey;
        }

        public void setSenderPublicKey(String publicKey) {
            this.senderPublicKey = publicKey;
        }

        public String getMessageKey() {
            return messageKey;
        }

        public void setMessageKey(String messageKey) {
            this.messageKey = messageKey;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return "Body{" +
                    "gcmRegistrationId='" + gcmRegistrationId + '\'' +
                    ", senderPublicKey='" + senderPublicKey + '\'' +
                    ", messageKey='" + messageKey + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}
