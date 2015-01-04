package com.alangeorge.android.hermes.model.provider;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.test.ProviderTestCase2;
import android.util.Log;

import com.alangeorge.android.hermes.model.Contact;
import com.alangeorge.android.hermes.model.Message;
import com.alangeorge.android.hermes.model.dao.DBHelper;

import static com.alangeorge.android.hermes.model.dao.DBHelper.CONTACT_COLUMN_GCM_ID;
import static com.alangeorge.android.hermes.model.dao.DBHelper.CONTACT_COLUMN_NAME;
import static com.alangeorge.android.hermes.model.dao.DBHelper.CONTACT_COLUMN_PUBLIC_KEY;
import static com.alangeorge.android.hermes.model.dao.DBHelper.MESSAGE_COLUMN_CONTACT_ID;
import static com.alangeorge.android.hermes.model.dao.DBHelper.MESSAGE_COLUMN_MESSAGE_JSON;
import static com.alangeorge.android.hermes.model.provider.HermesContentProvider.AUTHORITY;
import static com.alangeorge.android.hermes.model.provider.HermesContentProvider.CONTACTS_CONTENT_URI;
import static com.alangeorge.android.hermes.model.provider.HermesContentProvider.MESSAGES_CONTENT_URI;

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
        String name = "Micky Mouse";
        String contactPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAx/xTfOAkpYsBCjVCrQr5damjrBjUtPWewElM9E2jDcOar4CZ7uVKkqnUG0KF/aOcjjm2xPaUCwC0AXVr7Ds4qB7bDzQxQ2sxmtk6i4jnCSNJ3JTNzIljbEUuC6o2rB1oL+sZgI+8ZBqLp9GNzutH5wfBp+An4gdajTSN8C2TnlWBcZ8K+XcPE5PtqNfbkMtgMB8uhZlGeyLHXLheVZvtYkgH0fFO+2uceoQN9H7u5DejrM7oYWE8gHG+72JhjTKXQOQ6tcG73nMcq/63NQvDFnF8dcICXdbnHt/39YmDZRhrqjKqkX/MtdIqoINyBkVuuhey/C0BT12fxOGG+vhDcQIDAQAB";

        ContentValues values = new ContentValues();
        values.put(CONTACT_COLUMN_NAME, name);
        values.put(CONTACT_COLUMN_GCM_ID, "fakeGcmID");
        values.put(CONTACT_COLUMN_PUBLIC_KEY, contactPublicKey);

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

    public void testMessageBasicInsertAndFetch() throws Exception {
        String contactName = "Micky Mouse";
        String contactPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAx/xTfOAkpYsBCjVCrQr5damjrBjUtPWewElM9E2jDcOar4CZ7uVKkqnUG0KF/aOcjjm2xPaUCwC0AXVr7Ds4qB7bDzQxQ2sxmtk6i4jnCSNJ3JTNzIljbEUuC6o2rB1oL+sZgI+8ZBqLp9GNzutH5wfBp+An4gdajTSN8C2TnlWBcZ8K+XcPE5PtqNfbkMtgMB8uhZlGeyLHXLheVZvtYkgH0fFO+2uceoQN9H7u5DejrM7oYWE8gHG+72JhjTKXQOQ6tcG73nMcq/63NQvDFnF8dcICXdbnHt/39YmDZRhrqjKqkX/MtdIqoINyBkVuuhey/C0BT12fxOGG+vhDcQIDAQAB";
        String messageJson = "{\"body\":{\"gcmRegistrationId\":\"APA91bEtwCaTGLQoACaRkkzTCdeTtsNQHa14wgyfukoJ0y4V8E2QXbjfH40UHI8Edq-_DmkdwSA4s4M9gasZxnSQZfN0cZwa9rbkslkICFOOYgrRdsjWc3ppbzH37aBWlannpTFvzVSH-1lrsy8YNempDhfbDPNKQJYolYLbPEs2u0rR-ld8HIc\",\"message\":\"cYTr2JyNi8SA/Ibl5r63vQ\\u003d\\u003d\",\"messageKey\":\"o2Io1SbH56Np2HvtKo4YPPs5TdKh1zsHCzjg9NwU1q0CG4UMF/h8s452S0rINJgy1tTqqYgIffEU\\n5vdf7KbASza+tcrfVv/1RzNXjJtZQUca/9rC+PLDDPXRl45c3I079Ygr3cfyEOX//8/S8hDe+kkV\\nfM2XQuvWAujoc8IYTce84+nUBMhtRxcGIU0IhaN9vD/+s0SXb4sDjGBTBzpbgRjVFVrYN8Uafew4\\nD3tUODNzq4yKDDPhJmt5BUsNk8fiARzDXWpDejEY+n7lNToiS3uqHUeI0Anor8y9SAXD4jYCfwZh\\nMdO2jNC3qlxvSOLrg/qmdlkOMyX5PQL120yxPw\\n\",\"senderPublicKey\":\"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAx/xTfOAkpYsBCjVCrQr5damjrBjUtPWewElM9E2jDcOar4CZ7uVKkqnUG0KF/aOcjjm2xPaUCwC0AXVr7Ds4qB7bDzQxQ2sxmtk6i4jnCSNJ3JTNzIljbEUuC6o2rB1oL+sZgI+8ZBqLp9GNzutH5wfBp+An4gdajTSN8C2TnlWBcZ8K+XcPE5PtqNfbkMtgMB8uhZlGeyLHXLheVZvtYkgH0fFO+2uceoQN9H7u5DejrM7oYWE8gHG+72JhjTKXQOQ6tcG73nMcq/63NQvDFnF8dcICXdbnHt/39YmDZRhrqjKqkX/MtdIqoINyBkVuuhey/C0BT12fxOGG+vhDcQIDAQAB\"},\"signature\":\"YDW+dlDYuPwnVzRcTKeL99VeZOCluwg1VkQqTvME1gzL6amBxLTMrTS0erUTbPK0+toZwoCHyF7s8rXYoOqtlkQv5tIOwB6JvXpnnr6C0aST15iz2ob+cA7nHz+UokH1/zUPwZAKF+xJKia6VkkGrl0nqzRjTpsyLEso+qqXDpw5X7gw7x1+eZTojOC8MyUJg2G8yYtmaVeR3Z8xHLdjU8wqNdXMG9joEbHKHQbvsYL5Tbhlh3QORYSU47WLItojl6chuleVc2eJ4VuNgTkLEADafD/vZrQ3JWHjeGBQ1L6FyYYaCmiT8z/BQrqYXSRKJrGDU5r36NMhJS0tv7SWeA\\u003d\\u003d\"}";


        ContentValues contactValues = new ContentValues();
        contactValues.put(CONTACT_COLUMN_NAME, contactName);
        contactValues.put(CONTACT_COLUMN_GCM_ID, "fakeGcmID");
        contactValues.put(CONTACT_COLUMN_PUBLIC_KEY, contactPublicKey);

        Uri insertedContactUri = getContext().getContentResolver().insert(CONTACTS_CONTENT_URI, contactValues);
        Contact contact = new Contact(insertedContactUri);
        Log.d(TAG, "contact = " + contact);

        assertNotNull("contact is null from constructor", contact);
        assertEquals("contact name not as expected from constructor", contact.getName(), contactName);

        ContentValues messageValues = new ContentValues();
        messageValues.put(MESSAGE_COLUMN_CONTACT_ID, contact.getId());
        messageValues.put(MESSAGE_COLUMN_MESSAGE_JSON, messageJson);

        Uri insertedMessageUri = getContext().getContentResolver().insert(MESSAGES_CONTENT_URI, messageValues);

        Message message = new Message(insertedMessageUri);
        assertNotNull("message is null from constructor", message);
        assertNotNull("message body is null from constructor", message.getBody());
        assertEquals("message sender public key not as expected from constructor", message.getBody().getSenderPublicKey(), contactPublicKey);
        assertTrue("message failed signature verification from constructor", message.verifySignature());

        Cursor messageCursor = getContext().getContentResolver().query(insertedMessageUri, DBHelper.MESSAGE_ALL_COLUMNS, null, null, null);
        message = Message.cursorToMessage(messageCursor);
        assertNotNull("message is null from cursor", message);
        assertNotNull("message body is null from cursor", message.getBody());
        assertEquals("message sender public key not as expected from cursor", message.getBody().getSenderPublicKey(), contactPublicKey);
        assertTrue("message failed signature verification from cursor", message.verifySignature());
    }
}