package com.alangeorge.android.hermes.model;

import android.database.Cursor;
import android.net.Uri;
import android.util.Base64;

import com.alangeorge.android.hermes.App;
import com.alangeorge.android.hermes.model.dao.DBHelper;
import com.alangeorge.android.hermes.model.provider.HermesContentProvider;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import timber.log.Timber;

@SuppressWarnings("UnusedDeclaration")
public class Message {
    private Gson gson;

    @Expose
    private String signature;
    @Expose
    private Body body;

    private long id;
    private boolean isInbound;
    private long contactId;
    private Date createTime;
    private Date readTime;
    private String senderEncodedSymKey;
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

    public boolean isInbound() {
        return isInbound;
    }

    public void setInbound(boolean isInbound) {
        this.isInbound = isInbound;
    }

    // contact.getId() is canonical
    public long getContactId() {
        return contact != null ? contact.getId() : contactId;
    }

    public void setContactId(long contactId) {
        if (contact != null) {
            this.contactId = contact.getId();
        } else {
            this.contactId = contactId;
        }
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

    public String getSenderEncodedSymKey() {
        return senderEncodedSymKey;
    }

    public void setSenderEncodedSymKey(String senderEncodedSymKey) {
        this.senderEncodedSymKey = senderEncodedSymKey;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
        if (contact != null) {
            this.contactId = contact.getId();
        }
    }

    public void sign(PrivateKey senderPrivateKey) {
        if (body == null) {
            Timber.e( "message body null, not signing");
            return;
        }

        try {
            Signature secretKeySigner = Signature.getInstance(App.DEFAULT_SIGNATURE_ALGORITHM, App.DEFAULT_RSA_SECURITY_PROVIDER);
            secretKeySigner.initSign(senderPrivateKey);
            secretKeySigner.update(body.getSenderGcmRegistrationId().getBytes());
            secretKeySigner.update(body.getSenderPublicKey().getBytes());
            secretKeySigner.update(body.getReceiverEncodedSymKey().getBytes());
            secretKeySigner.update(body.getMessage().getBytes());

            signature = Base64.encodeToString(secretKeySigner.sign(), Base64.NO_WRAP);

//                } catch (NoSuchAlgorithmException e) {
//                } catch (InvalidKeyException e) {
//                } catch (SignatureException e) {
        } catch (Exception e) {
            Timber.e( "SHA512 signing error", e);
        }
    }

    public boolean verifySignature() {
        if (body == null) {
            Timber.e( "invalid message, message body is null");
            return false;
        }

        if (body.getSenderPublicKey() == null || "".equals(body.getSenderPublicKey())) {
            Timber.e( "sender public key is null or empty");
            return false;
        }

        try {
            Signature secretKeyVerifier = Signature.getInstance(App.DEFAULT_SIGNATURE_ALGORITHM, App.DEFAULT_RSA_SECURITY_PROVIDER);
            secretKeyVerifier.initVerify(Contact.decodePublicKey(body.getSenderPublicKey()));
            secretKeyVerifier.update(body.getSenderGcmRegistrationId().getBytes());
            secretKeyVerifier.update(body.getSenderPublicKey().getBytes());
            secretKeyVerifier.update(body.getReceiverEncodedSymKey().getBytes());
            secretKeyVerifier.update(body.getMessage().getBytes());
            return secretKeyVerifier.verify(Base64.decode(signature, Base64.NO_WRAP));

//                } catch (NoSuchAlgorithmException e) {
//                } catch (InvalidKeyException e) {
//                } catch (SignatureException e) {
        } catch (Exception e) {
            Timber.e( "SHA512 signing error", e);
        }

        return false;
    }

    public String getInboundMessageClearText(PrivateKey privateKey) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException {
        byte[] symmetricKeyDecodedBytes;
        Cipher cipher = Cipher.getInstance(App.DEFAULT_RSA_CIPHER, App.DEFAULT_RSA_SECURITY_PROVIDER);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        symmetricKeyDecodedBytes = cipher.doFinal(Base64.decode(getBody().getReceiverEncodedSymKey(), Base64.NO_WRAP));

        return getMessageClearText(symmetricKeyDecodedBytes);
    }

    public String getOutboundMessageClearText(PrivateKey privateKey) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException {
        byte[] symmetricKeyDecodedBytes;
        Cipher cipher = Cipher.getInstance(App.DEFAULT_RSA_CIPHER, App.DEFAULT_RSA_SECURITY_PROVIDER);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        symmetricKeyDecodedBytes = cipher.doFinal(Base64.decode(getSenderEncodedSymKey(), Base64.NO_WRAP));

        return getMessageClearText(symmetricKeyDecodedBytes);
    }

    private String getMessageClearText(byte[] symmetricKeyDecodedBytes) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        SecretKeySpec symmetricKeyFromMessage = new SecretKeySpec(symmetricKeyDecodedBytes, App.DEFAULT_AES_CIPHER);

        // decrypt our message with our decrypted symmetric key
        byte[] decodedBytes;
        Cipher cipher = Cipher.getInstance(App.DEFAULT_AES_CIPHER, App.DEFAULT_AES_SECURITY_PROVIDER);
        cipher.init(Cipher.DECRYPT_MODE, symmetricKeyFromMessage);
        decodedBytes = cipher.doFinal(Base64.decode(getBody().getMessage(), Base64.NO_WRAP));

        return new String(decodedBytes, App.DEFAULT_CHARACTER_SET);
    }

    @Override
    public String toString() {
        return "Message{" +
                "signature='" + signature + '\'' +
                ", body=" + body +
                ", id=" + id +
                ", isInbound=" + isInbound +
                ", contactId=" + contactId +
                ", createTime=" + createTime +
                ", readTime=" + readTime +
                ", senderEncodedSymKey='" + senderEncodedSymKey + '\'' +
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
            case HermesContentProvider.MESSAGES_ALL: // loads first message
            case HermesContentProvider.MESSAGE_BY_ID:
                cursor = App.context.getContentResolver().query(
                        uri,
                        DBHelper.MESSAGE_ALL_COLUMNS,
                        null,
                        null,
                        null
                );
                break;
            case HermesContentProvider.MESSAGES_CONTACT_ALL: // loads first messages/contact join
            case HermesContentProvider.MESSAGES_CONTACT_BY_ID:
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
                setInbound(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.MESSAGE_ALL_COLUMNS[1])) == 0);
                setContactId(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.MESSAGE_ALL_COLUMNS[2])));
                setSenderEncodedSymKey(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.MESSAGE_ALL_COLUMNS[3])));
                Message temp = new Message(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.MESSAGE_ALL_COLUMNS[4])));
                setSignature(temp.getSignature());
                setBody(temp.getBody());
                setReadTime(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.MESSAGE_ALL_COLUMNS[5]))));
                setCreateTime(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.MESSAGE_ALL_COLUMNS[6]))));
                if (cursor.getColumnCount() > 7) { // this is a messages/contact join
                    Contact contact = new Contact();
                    contact.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.MESSAGE_CONTACT_ALL_COLUMNS[7])));
                    contact.setName(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.MESSAGE_CONTACT_ALL_COLUMNS[8])));
                    contact.setPublicKeyEncoded(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.MESSAGE_CONTACT_ALL_COLUMNS[9])));
                    contact.setGcmId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.MESSAGE_CONTACT_ALL_COLUMNS[10])));
                    contact.setCreateTime(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.MESSAGE_CONTACT_ALL_COLUMNS[11]))));
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
            Timber.e("cursor null or empty");
            return null;
        }

        if (cursor.isBeforeFirst()) {
            cursor.moveToFirst();
        }

        Message message = new Message();
        message.load(cursor, false);

        return message;
    }

    public static class Body {
        @Expose
        private String senderGcmRegistrationId;
        @Expose
        private String senderPublicKey;
        @Expose
        private String receiverEncodedSymKey;
        @Expose
        private String message;

        public String getSenderGcmRegistrationId() {
            return senderGcmRegistrationId;
        }

        public void setSenderGcmRegistrationId(String senderGcmRegistrationId) {
            this.senderGcmRegistrationId = senderGcmRegistrationId;
        }

        public String getSenderPublicKey() {
            return senderPublicKey;
        }

        public void setSenderPublicKey(String publicKey) {
            this.senderPublicKey = publicKey;
        }

        public String getReceiverEncodedSymKey() {
            return receiverEncodedSymKey;
        }

        public void setReceiverEncodedSymKey(String receiverEncodedSymKey) {
            this.receiverEncodedSymKey = receiverEncodedSymKey;
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
                    "senderGcmRegistrationId='" + senderGcmRegistrationId + '\'' +
                    ", senderPublicKey='" + senderPublicKey + '\'' +
                    ", receiverEncodedSymKey='" + receiverEncodedSymKey + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}
