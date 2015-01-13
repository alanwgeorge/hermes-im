package com.alangeorge.android.hermes;

import android.content.ContentValues;
import android.net.Uri;
import android.util.Log;

import com.alangeorge.android.hermes.model.Contact;
import com.alangeorge.android.hermes.model.dao.DBHelper;
import com.alangeorge.android.hermes.model.provider.HermesContentProvider;

public class TestingUtils {
    private static final String TAG = "TestingUtils";

    public static Contact persistContact(Contact contact) throws Exception {
        ContentValues contactValues = new ContentValues();
        contactValues.put(DBHelper.CONTACT_COLUMN_NAME, contact.getName());
        contactValues.put(DBHelper.CONTACT_COLUMN_GCM_ID, contact.getGcmId());
        contactValues.put(DBHelper.CONTACT_COLUMN_PUBLIC_KEY, contact.getPublicKeyEncoded());

        Uri insertedContactUri = App.context.getContentResolver().insert(HermesContentProvider.CONTACTS_CONTENT_URI, contactValues);
        contact = new Contact(insertedContactUri);
        Log.d(TAG, "contact = " + contact);

        return contact;
    }

    public static int deleteContact(long id) throws Exception {
        Uri contactUri = Uri.parse(HermesContentProvider.CONTACTS_CONTENT_URI + "/" + id);
        return App.context.getContentResolver().delete(contactUri, null, null);
    }

}
