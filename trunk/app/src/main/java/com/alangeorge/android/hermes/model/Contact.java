package com.alangeorge.android.hermes.model;

import android.database.Cursor;
import android.net.Uri;

import com.alangeorge.android.hermes.App;

import java.util.Date;

import static com.alangeorge.android.hermes.model.dao.DBHelper.CONTACT_ALL_COLUMNS;
import static com.alangeorge.android.hermes.model.provider.HermesContentProvider.CONTACTS_CONTENT_URI;

@SuppressWarnings("UnusedDeclaration")
public class Contact {
    private static final String TAG = "Contact";

    private long id;
    private String name;
    private String publicKey;
    private String gcmId;
    private Date createTime;

    public Contact() { }

    public Contact(long id) {
        Uri locationUri = Uri.parse(CONTACTS_CONTENT_URI + "/" + id);

        Cursor cursor = App.context.getContentResolver().query(
                locationUri,
                CONTACT_ALL_COLUMNS,
                null,
                null,
                null
        );

        if (cursor != null) {
            cursor.moveToFirst();
            setId(cursor.getLong(0));
            setName(cursor.getString(1));
            setPublicKey(cursor.getString(2));
            setGcmId(cursor.getString(3));
            setCreateTime(new Date(cursor.getLong(4)));
            cursor.close();
        }
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

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
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

    /**
     * Takes a {@link android.database.Cursor} and converts it to a model object, {@link com.alangeorge.android.hermes.model.Contact}
     *
     * @param cursor the {@link android.database.Cursor} to convert
     * @return the resulting {@link com.alangeorge.android.hermes.model.Contact}
     */
    private static Contact cursorToContact(Cursor cursor) {
        Contact contact = new Contact();

        contact.setId(cursor.getLong(0));
        contact.setName(cursor.getString(1));
        contact.setPublicKey(cursor.getString(2));
        contact.setGcmId(cursor.getString(3));
        contact.setCreateTime(new Date(cursor.getLong(4)));

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
