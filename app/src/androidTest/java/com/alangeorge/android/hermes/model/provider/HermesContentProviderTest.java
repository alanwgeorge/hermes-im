package com.alangeorge.android.hermes.model.provider;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.test.ProviderTestCase2;
import android.util.Log;

import com.alangeorge.android.hermes.model.Contact;
import com.alangeorge.android.hermes.model.dao.DBHelper;

import static com.alangeorge.android.hermes.model.dao.DBHelper.CONTACT_COLUMN_GCM_ID;
import static com.alangeorge.android.hermes.model.dao.DBHelper.CONTACT_COLUMN_NAME;
import static com.alangeorge.android.hermes.model.dao.DBHelper.CONTACT_COLUMN_PUBLIC_KEY;
import static com.alangeorge.android.hermes.model.provider.HermesContentProvider.AUTHORITY;
import static com.alangeorge.android.hermes.model.provider.HermesContentProvider.CONTACTS_CONTENT_URI;

public class HermesContentProviderTest extends ProviderTestCase2<HermesContentProvider> {
    private static final String TAG = "Hermes.HermesContentProviderTest";

    public HermesContentProviderTest() {
        super(HermesContentProvider.class, AUTHORITY);
    }

    public void setUp() throws Exception {
        super.setUp();
        Log.d(TAG, "setup()");
    }

    public void tearDown() throws Exception {
        Log.d(TAG, "tearDown()");

        // Clean up DB
        DBHelper dbHelper = new DBHelper(getContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        dbHelper.onUpgrade(db, 0, 0);

        super.tearDown();
    }

    public void testContactBasicInsertAndFetch() throws Exception {
        Log.d(TAG, "testContactBasicInsert()");

        String name = "Micky Mouse";

        ContentValues values = new ContentValues();
        values.put(CONTACT_COLUMN_NAME, name);
        values.put(CONTACT_COLUMN_GCM_ID, "fakeGcmID");
        values.put(CONTACT_COLUMN_PUBLIC_KEY, "fakeEncodedPublicKey");

        Uri insertedContactUri = getContext().getContentResolver().insert(CONTACTS_CONTENT_URI, values);
        Contact contact = new Contact(insertedContactUri);
        Log.d(TAG, "contact = " + contact);

        assertNotNull("contact is null from constructor", contact);
        assertEquals("contact name not as expected from constructor", contact.getName(), name);

        Cursor contactCursor = getContext().getContentResolver().query(insertedContactUri, DBHelper.CONTACT_ALL_COLUMNS, null, null, null);
        contact = Contact.cursorToContact(contactCursor);

        assertNotNull("contact is null from cursor", contact);
        assertEquals("contact name not as expected from cursor", contact.getName(), name);
    }
}