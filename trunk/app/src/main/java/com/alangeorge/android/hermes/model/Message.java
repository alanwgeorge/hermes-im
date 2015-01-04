package com.alangeorge.android.hermes.model;

import android.database.Cursor;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import com.alangeorge.android.hermes.model.provider.HermesContentProvider;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.security.PrivateKey;
import java.security.Signature;
import java.util.Date;

import static com.alangeorge.android.hermes.App.DEFAULT_RSA_SECURITY_PROVIDER;
import static com.alangeorge.android.hermes.App.DEFAULT_SIGNATURE_ALGORITHM;
import static com.alangeorge.android.hermes.App.context;
import static com.alangeorge.android.hermes.model.dao.DBHelper.MESSAGE_ALL_COLUMNS;

@SuppressWarnings("UnusedDeclaration")
public class Message {
    private static final String TAG = "Hermes.Message";

    private Gson gson;

    @Expose
    private String signature;
    @Expose
    private Body body;

    private long id;
    private long contactId;
    private Date createTime;
    private Date readTime;

    public Message() {
        GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithoutExposeAnnotation();
        gson = builder.create();
    }

    public Message(long id) throws ModelException {
        this();
        Uri uri = Uri.parse(HermesContentProvider.MESSAGES_CONTENT_URI + "/" + id);
        load(uri);
    }

    public Message(String json) {
        this();
        Message temp = gson.fromJson(json, getClass());
        this.setBody(temp.getBody());
        this.setSignature(temp.getSignature());
    }

    public Message(Uri messageUri) throws ModelException {
        this();
        load(messageUri);
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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getContactId() {
        return contactId;
    }

    public void setContactId(long contactId) {
        this.contactId = contactId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getReadTime() {
        return readTime;
    }

    public void setReadTime(Date readTime) {
        this.readTime = readTime;
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

    private void load(Uri uri) throws ModelException {
        Cursor cursor = context.getContentResolver().query(
                uri,
                MESSAGE_ALL_COLUMNS,
                null,
                null,
                null
        );
        cursor.moveToFirst();
        load(cursor, true);
    }

    private void load(Cursor cursor, boolean closeCursor) throws ModelException {
        if (cursor != null && cursor.getCount() > 0) {
            setId(cursor.getLong(0));
            setContactId(cursor.getLong(1));
            Message temp = new Message(cursor.getString(2));
            setSignature(temp.getSignature());
            setBody(temp.getBody());
            setReadTime(new Date(cursor.getLong(3)));
            setCreateTime(new Date(cursor.getLong(4)));
            if (closeCursor) cursor.close();
        } else {
            throw new ModelException("unable to load: cursor null or count is 0");
        }
    }

    public static Message cursorToMessage(Cursor cursor) {
        if (cursor == null || cursor.getCount() == 0) {
            Log.e(TAG, "cursor null or empty");
            return null;
        }

        if (cursor.isBeforeFirst()) {
            cursor.moveToFirst();
        }

        Message message = new Message(cursor.getString(2));

        message.setId(cursor.getLong(0));
        message.setContactId(cursor.getLong(1));
        message.setReadTime(new Date(cursor.getLong(3)));
        message.setCreateTime(new Date(cursor.getLong(4)));

        return message;
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
