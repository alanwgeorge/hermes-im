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
import static com.alangeorge.android.hermes.model.dao.DBHelper.MESSAGE_ALL_COLUMNS;
import static com.alangeorge.android.hermes.model.dao.DBHelper.MESSAGE_COLUMN_CONTACT_ID;
import static com.alangeorge.android.hermes.model.dao.DBHelper.MESSAGE_COLUMN_CREATE_TIME;
import static com.alangeorge.android.hermes.model.dao.DBHelper.MESSAGE_COLUMN_ID;
import static com.alangeorge.android.hermes.model.dao.DBHelper.MESSAGE_COLUMN_READ_TIME;
import static com.alangeorge.android.hermes.model.dao.DBHelper.MESSAGE_CONTACT_ALL_COLUMNS;
import static com.alangeorge.android.hermes.model.dao.DBHelper.TABLE_CONTACT;
import static com.alangeorge.android.hermes.model.dao.DBHelper.TABLE_JOIN_MESSAGE_CONTACT;
import static com.alangeorge.android.hermes.model.dao.DBHelper.TABLE_MESSAGE;

public class HermesContentProvider extends ContentProvider {
    private static final String TAG = "Hermes.HermesContentProvider";

    // used for the UriMatcher
    public static final int CONTACTS = 10;
    public static final int CONTACT_ID = 20;
    public static final int MESSAGES = 30;
    public static final int MESSAGE_ID = 40;
    public static final int MESSAGES_CONTACT = 50;
    public static final int MESSAGES_CONTACT_ID = 60;
    public static final int MESSAGES_CONTACT_BY_CONTACT_PRIVATE_KEY = 70;

    private static final String CONTACTS_PATH = "contacts";
    private static final String MESSAGES_PATH = "messages";
    private static final String MESSAGES_CONTACT_JOIN_PATH = "messagescontact";

    public static final String AUTHORITY = "com.alangeorge.android.hermes.contentprovider";
    public static final Uri CONTACTS_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + CONTACTS_PATH);
    public static final Uri MESSAGES_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + MESSAGES_PATH);
    public static final Uri MESSAGES_CONTACT_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + MESSAGES_CONTACT_JOIN_PATH);

    public static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        URI_MATCHER.addURI(AUTHORITY, CONTACTS_PATH, CONTACTS);
        URI_MATCHER.addURI(AUTHORITY, CONTACTS_PATH + "/#", CONTACT_ID);
        URI_MATCHER.addURI(AUTHORITY, MESSAGES_PATH, MESSAGES);
        URI_MATCHER.addURI(AUTHORITY, MESSAGES_PATH + "/#", MESSAGE_ID);
        URI_MATCHER.addURI(AUTHORITY, MESSAGES_CONTACT_JOIN_PATH, MESSAGES_CONTACT);
        URI_MATCHER.addURI(AUTHORITY, MESSAGES_CONTACT_JOIN_PATH + "/#", MESSAGES_CONTACT_ID);
        URI_MATCHER.addURI(AUTHORITY, MESSAGES_CONTACT_JOIN_PATH + "/findby/contact/privatekey/*", MESSAGES_CONTACT_BY_CONTACT_PRIVATE_KEY);
    }

    private DBHelper dbHelper;

    public HermesContentProvider() { }

    @Override
    public boolean onCreate() {
        Log.d(TAG, "onCreate()");
        dbHelper = new DBHelper(getContext());
        return true;
    }

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
        int uriType = URI_MATCHER.match(uri);
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
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        Cursor cursor;
        SQLiteDatabase db;

        int uriType = URI_MATCHER.match(uri);
        switch (uriType) {
            case CONTACT_ID:
                // adding the ID to the original query
                queryBuilder.appendWhere(CONTACT_COLUMN_ID + "=" + uri.getLastPathSegment());
            case CONTACTS:
                checkColumnsContact(projection);
                queryBuilder.setTables(TABLE_CONTACT);
                db = dbHelper.getWritableDatabase();
                break;
            case MESSAGE_ID:
                // adding the ID to the original query
                queryBuilder.appendWhere(MESSAGE_COLUMN_ID + "=" + uri.getLastPathSegment());
            case MESSAGES:
                checkColumnsMessage(projection);
                queryBuilder.setTables(TABLE_MESSAGE);
                db = dbHelper.getWritableDatabase();
                break;
            case MESSAGES_CONTACT_ID:
                // adding the ID to the original query
                queryBuilder.appendWhere(MESSAGE_COLUMN_CONTACT_ID + "=" + uri.getLastPathSegment());
            case MESSAGES_CONTACT:
                checkColumnsMessagesContactJoin(MESSAGE_CONTACT_ALL_COLUMNS);
                queryBuilder.setTables(TABLE_JOIN_MESSAGE_CONTACT);
                queryBuilder.setProjectionMap(DBHelper.MESSAGE_CONTACT_JOIN_PROJECTION_MAP);
//                queryBuilder.setProjectionMap(MESSAGE_CONTACT_JOIN_PROJECTION_MAP);
                db = dbHelper.getWritableDatabase();
                break;
//            case MESSAGES_CONTACT_BY_CONTACT_PRIVATE_KEY:
//                queryBuilder.appendWhere(MES);
//                checkColumnsMessage(projection);
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

    private void checkColumnsMessage(String[] projection) {
        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<>(Arrays.asList(MESSAGE_ALL_COLUMNS));
            // check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }

    private void checkColumnsMessagesContactJoin(String[] projection) {
        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<>(Arrays.asList(MESSAGE_CONTACT_ALL_COLUMNS));
            // check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }
}
