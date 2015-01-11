package com.alangeorge.android.hermes.model;

import android.database.Cursor;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import com.alangeorge.android.hermes.App;
import com.alangeorge.android.hermes.model.dao.DBHelper;
import com.alangeorge.android.hermes.model.provider.HermesContentProvider;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.security.PrivateKey;
import java.security.Signature;
import java.util.Date;

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
    private Contact contact;

    public Message() {
        GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithoutExposeAnnotation();
        gson = builder.create();
    }

    public Message(long id) throws ModelException {
        this();
        Uri uri = Uri.parse(HermesContentProvider.MESSAGES_CONTACT_CONTENT_URI + "/" + id);
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

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public void sign(PrivateKey senderPrivateKey) {
        if (body == null) {
            Log.e(TAG, "message body null, not signing");
            return;
        }

        try {
            Signature secretKeySigner = Signature.getInstance(App.DEFAULT_SIGNATURE_ALGORITHM, App.DEFAULT_RSA_SECURITY_PROVIDER);
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
            Signature secretKeyVerifier = Signature.getInstance(App.DEFAULT_SIGNATURE_ALGORITHM, App.DEFAULT_RSA_SECURITY_PROVIDER);
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
                ", id=" + id +
                ", contactId=" + contactId +
                ", createTime=" + createTime +
                ", readTime=" + readTime +
                ", contact=" + contact +
                '}';
    }

    public String toJson() {
        return gson.toJson(this);
    }

    private void load(Uri uri) throws ModelException {
        int uriType = HermesContentProvider.URI_MATCHER.match(uri);

        Cursor cursor;

        switch (uriType) {
            case HermesContentProvider.MESSAGES: // loads first message
            case HermesContentProvider.MESSAGE_ID:
                cursor = App.context.getContentResolver().query(
                        uri,
                        DBHelper.MESSAGE_ALL_COLUMNS,
                        null,
                        null,
                        null
                );
                break;
            case HermesContentProvider.MESSAGES_CONTACT: // loads first messages/contact join
            case HermesContentProvider.MESSAGES_CONTACT_ID:
                cursor = App.context.getContentResolver().query(
                        uri,
                        DBHelper.MESSAGE_CONTACT_ALL_COLUMNS,
                        null,
                        null,
                        null
                );
                break;
            default:
                throw new ModelException("unknown uriType: " + uri);
        }

        cursor.moveToFirst();
        load(cursor, true);
    }

    private void load(Cursor cursor, boolean closeCursor) throws ModelException {
        if (cursor != null && cursor.getCount() > 0) {
            try {
                setId(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.MESSAGE_ALL_COLUMNS[0])));
                setContactId(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.MESSAGE_ALL_COLUMNS[1])));
                Message temp = new Message(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.MESSAGE_ALL_COLUMNS[2])));
                setSignature(temp.getSignature());
                setBody(temp.getBody());
                setReadTime(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.MESSAGE_ALL_COLUMNS[3]))));
                setCreateTime(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.MESSAGE_ALL_COLUMNS[4]))));
                if (cursor.getColumnCount() > 5) { // this is a messages/contact join
                    Contact contact = new Contact();
                    contact.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.MESSAGE_CONTACT_ALL_COLUMNS[5])));
                    contact.setName(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.MESSAGE_CONTACT_ALL_COLUMNS[6])));
                    contact.setPublicKeyEncoded(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.MESSAGE_CONTACT_ALL_COLUMNS[7])));
                    contact.setGcmId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.MESSAGE_CONTACT_ALL_COLUMNS[8])));
                    contact.setCreateTime(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.MESSAGE_CONTACT_ALL_COLUMNS[9]))));
                    setContact(contact);
                }
            } catch (Exception e) {
                throw new ModelException("unable to load message cursor", e);
            }
            if (closeCursor) cursor.close();
        } else {
            throw new ModelException("unable to load: cursor null or count is 0");
        }
    }

    public static Message cursorToMessage(Cursor cursor) throws ModelException {
        if (cursor == null || cursor.getCount() == 0) {
            Log.e(TAG, "cursor null or empty");
            return null;
        }

        if (cursor.isBeforeFirst()) {
            cursor.moveToFirst();
        }

        Message message = new Message();
        message.load(cursor, true);

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
