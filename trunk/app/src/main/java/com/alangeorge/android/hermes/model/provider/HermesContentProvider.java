package com.alangeorge.android.hermes.model.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import com.alangeorge.android.hermes.model.dao.DBHelper;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

import static com.alangeorge.android.hermes.model.dao.DBHelper.CONTACT_ALL_COLUMNS;
import static com.alangeorge.android.hermes.model.dao.DBHelper.CONTACT_COLUMN_CREATE_TIME;
import static com.alangeorge.android.hermes.model.dao.DBHelper.CONTACT_COLUMN_ID;
import static com.alangeorge.android.hermes.model.dao.DBHelper.MESSAGE_COLUMN_CREATE_TIME;
import static com.alangeorge.android.hermes.model.dao.DBHelper.MESSAGE_COLUMN_READ_TIME;
import static com.alangeorge.android.hermes.model.dao.DBHelper.TABLE_CONTACT;
import static com.alangeorge.android.hermes.model.dao.DBHelper.TABLE_MESSAGE;

public class HermesContentProvider extends ContentProvider {
    private static final String TAG = "Hermes.HermesContentProvider";

    // used for the UriMatcher
    private static final int CONTACTS = 10;
    private static final int CONTACT_ID = 20;
    private static final int MESSAGES = 30;
    private static final int MESSAGE_ID = 40;

    private static final String CONTACTS_PATH = "contacts";
    private static final String MESSAGES_PATH = "messages";

    public static final String AUTHORITY = "com.alangeorge.android.hermes.contentprovider";
    public static final Uri CONTACTS_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + CONTACTS_PATH);
    public static final Uri MESSAGES_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + MESSAGES_PATH);

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, CONTACTS_PATH, CONTACTS);
        sURIMatcher.addURI(AUTHORITY, CONTACTS_PATH + "/#", CONTACT_ID);
        sURIMatcher.addURI(AUTHORITY, MESSAGES_PATH, MESSAGES);
        sURIMatcher.addURI(AUTHORITY, MESSAGES_PATH + "/#", MESSAGE_ID);
    }

    private DBHelper dbHelper;

    public HermesContentProvider() { }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Unsupported operation");
    }

    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = dbHelper.getWritableDatabase();
        Uri result;

        long id;

        switch (uriType) {
            case CONTACTS:
                values.put(CONTACT_COLUMN_CREATE_TIME, new Date().getTime());
                id = sqlDB.insert(TABLE_CONTACT, null, values);
                getContext().getContentResolver().notifyChange(CONTACTS_CONTENT_URI, null);
                result = Uri.parse("content://" + AUTHORITY + "/" + CONTACTS_PATH + "/" + id);
                break;
            case MESSAGES:
                values.put(MESSAGE_COLUMN_CREATE_TIME, new Date().getTime());
                values.put(MESSAGE_COLUMN_READ_TIME, -1); // == not read
                id = sqlDB.insert(TABLE_MESSAGE, null, values);
                getContext().getContentResolver().notifyChange(MESSAGES_CONTENT_URI, null);
                result = Uri.parse("content://" + AUTHORITY + "/" + MESSAGES_PATH + "/" + id);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        return result;
    }

    @Override
    public boolean onCreate() {
        Log.d(TAG, "onCreate()");
        dbHelper = new DBHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        Cursor cursor;
        SQLiteDatabase db;

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case CONTACT_ID:
                // adding the ID to the original query
                queryBuilder.appendWhere(CONTACT_COLUMN_ID + "=" + uri.getLastPathSegment());
            case CONTACTS:
                checkColumnsContact(projection);
                queryBuilder.setTables(TABLE_CONTACT);
                db = dbHelper.getWritableDatabase();
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);

        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Unsupported operation");
    }

    private void checkColumnsContact(String[] projection) {
        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<>(Arrays.asList(CONTACT_ALL_COLUMNS));
            // check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }

}
