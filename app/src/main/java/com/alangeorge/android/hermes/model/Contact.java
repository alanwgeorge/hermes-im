package com.alangeorge.android.hermes.model;

import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.alangeorge.android.hermes.App;
import com.alangeorge.android.hermes.model.dao.DBHelper;
import com.alangeorge.android.hermes.model.provider.HermesContentProvider;
import com.google.gson.annotations.Expose;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;

@SuppressWarnings("UnusedDeclaration")
public class Contact {
    private static final String TAG = "Contact";

    private long id;
    @Expose
    private String name;
    @Expose
    private String publicKey;
    @Expose
    private String gcmId;
    private Date createTime;

    public Contact() { }

    public Contact(long id) throws ModelException {
        Uri uri = Uri.parse(HermesContentProvider.CONTACTS_CONTENT_URI + "/" + id);
        load(uri);
    }

    public Contact(Uri uri) throws ModelException {
        load(uri);
    }

    public Contact(Cursor cursor) throws ModelException {
        load(cursor, false);
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPublicKeyEncoded() {
        return publicKey;
    }

    public void setPublicKeyEncoded(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getGcmId() {
        return gcmId;
    }

    public void setGcmId(String gcmId) {
        this.gcmId = gcmId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public boolean validateForInsert() {
        boolean result = true;

        if (TextUtils.isEmpty(gcmId)) {
            Log.d(TAG, "gcmId isEmpty");
            result = false;
        }

        if (TextUtils.isEmpty(publicKey)) {
            Log.d(TAG, "publicKey isEmpty");
            result = false;
        }

        if (TextUtils.isEmpty(name)) {
            Log.d(TAG, "name isEmpty");
            result = false;
        }


        return result;
    }

    public PublicKey getPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        return Contact.decodePublicKey(getPublicKeyEncoded());
    }

    public static PublicKey decodePublicKey(String base64EncodedKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return KeyFactory.getInstance(App.DEFAULT_KEYPAIR_ALGORITHM).generatePublic(new X509EncodedKeySpec(Base64.decode(base64EncodedKey, Base64.NO_WRAP)));
    }

    private void load(Uri uri) throws ModelException {
        Cursor cursor = App.context.getContentResolver().query(
                uri,
                DBHelper.CONTACT_ALL_COLUMNS,
                null,
                null,
                null
        );
        cursor.moveToFirst();
        load(cursor, true);
    }

    private void load(Cursor cursor, boolean closeCursor) throws ModelException {
        if (cursor != null && cursor.getCount() > 0) {
            try {
                setId(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.CONTACT_ALL_COLUMNS[0])));
                setName(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.CONTACT_ALL_COLUMNS[1])));
                setPublicKeyEncoded(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.CONTACT_ALL_COLUMNS[2])));
                setGcmId(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.CONTACT_ALL_COLUMNS[3])));
                setCreateTime(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.CONTACT_ALL_COLUMNS[4]))));
                if (closeCursor) cursor.close();
            } catch (Exception e) {
                throw new ModelException("unable to load contact from cursor", e);
            }
        } else {
            throw new ModelException("unable to load: cursor null or count is 0");
        }
    }

    /**
     * Takes a {@link android.database.Cursor} and converts it to a model object, {@link com.alangeorge.android.hermes.model.Contact}
     *
     * @param cursor the {@link android.database.Cursor} to convert
     * @return the resulting {@link com.alangeorge.android.hermes.model.Contact}, null if Cursor is empty
     */
    public static Contact cursorToContact(Cursor cursor) throws ModelException {
        if (cursor == null || cursor.getCount() == 0) {
            Log.e(TAG, "cursor null or empty");
            return null;
        }

        if (cursor.isBeforeFirst()) {
            cursor.moveToFirst();
        }

        Contact contact = new Contact();
        contact.load(cursor, false);
//        contact.setId(cursor.getLong(0));
//        contact.setName(cursor.getString(1));
//        contact.setPublicKeyEncoded(cursor.getString(2));
//        contact.setGcmId(cursor.getString(3));
//        contact.setCreateTime(new Date(cursor.getLong(4)));

        return contact;
    }


    @Override
    public String toString() {
        return "Contact{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", publicKey='" + publicKey + '\'' +
                ", gcmId='" + gcmId + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}
