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

public class HermesContentProvider extends ContentProvider {
    private static final String TAG = "Hermes.HermesContentProvider";

    // used for the UriMatcher
    public static final int CONTACTS_ALL = 10;
    public static final int CONTACT_BY_ID = 20;
    public static final int CONTACT_BY_CONTACT_PRIVATE_KEY = 80;
    public static final int MESSAGES_ALL = 30;
    public static final int MESSAGE_BY_ID = 40;
    public static final int MESSAGES_CONTACT_ALL = 50;
    public static final int MESSAGES_CONTACT_BY_ID = 60;
    public static final int MESSAGES_CONTACT_BY_CONTACT_PRIVATE_KEY = 70;


    private static final String CONTACTS_PATH = "contacts";
    private static final String MESSAGES_PATH = "messages";
    private static final String MESSAGES_CONTACT_JOIN_PATH = "messagescontact";
    private static final String CONTACT_BY_PUBLIC_KEY_URI_PART = "/findby/contact/publickey";

    public static final String AUTHORITY = "com.alangeorge.android.hermes.contentprovider";
    public static final Uri CONTACTS_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + CONTACTS_PATH);
    public static final Uri CONTACTS_CONTENT_BY_PUBLIC_KEY_URI = Uri.parse("content://" + AUTHORITY + "/" + CONTACTS_PATH + CONTACT_BY_PUBLIC_KEY_URI_PART);
    public static final Uri MESSAGES_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + MESSAGES_PATH);
    public static final Uri MESSAGES_CONTACT_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + MESSAGES_CONTACT_JOIN_PATH);
    public static final Uri MESSAGES_CONTACT_BY_PUBLIC_KEY_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + MESSAGES_CONTACT_JOIN_PATH + CONTACT_BY_PUBLIC_KEY_URI_PART);

    public static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        URI_MATCHER.addURI(AUTHORITY, CONTACTS_PATH, CONTACTS_ALL);
        URI_MATCHER.addURI(AUTHORITY, CONTACTS_PATH + "/#", CONTACT_BY_ID);
        URI_MATCHER.addURI(AUTHORITY, MESSAGES_PATH, MESSAGES_ALL);
        URI_MATCHER.addURI(AUTHORITY, MESSAGES_PATH + "/#", MESSAGE_BY_ID);
        URI_MATCHER.addURI(AUTHORITY, MESSAGES_CONTACT_JOIN_PATH, MESSAGES_CONTACT_ALL);
        URI_MATCHER.addURI(AUTHORITY, MESSAGES_CONTACT_JOIN_PATH + "/#", MESSAGES_CONTACT_BY_ID);
        URI_MATCHER.addURI(AUTHORITY, MESSAGES_CONTACT_JOIN_PATH + CONTACT_BY_PUBLIC_KEY_URI_PART + "/*", MESSAGES_CONTACT_BY_CONTACT_PRIVATE_KEY);
        URI_MATCHER.addURI(AUTHORITY, CONTACTS_PATH + CONTACT_BY_PUBLIC_KEY_URI_PART + "/*", CONTACT_BY_CONTACT_PRIVATE_KEY);
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
    public String getType(Uri uri) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Uri result;
        int uriType = URI_MATCHER.match(uri);
        SQLiteDatabase sqlDB = dbHelper.getWritableDatabase();

        long id;
        switch (uriType) {
            case CONTACTS_ALL:
                values.put(DBHelper.CONTACT_COLUMN_CREATE_TIME, new Date().getTime());
                id = sqlDB.insertOrThrow(DBHelper.TABLE_CONTACT, null, values);
                getContext().getContentResolver().notifyChange(CONTACTS_CONTENT_URI, null);
                getContext().getContentResolver().notifyChange(MESSAGES_CONTACT_CONTENT_URI, null);
                result = id == -1 ? null : Uri.parse("content://" + AUTHORITY + "/" + CONTACTS_PATH + "/" + id);
                break;
            case MESSAGES_ALL:
                values.put(DBHelper.MESSAGE_COLUMN_CREATE_TIME, new Date().getTime());
                values.put(DBHelper.MESSAGE_COLUMN_READ_TIME, -1); // == not read
                id = sqlDB.insertOrThrow(DBHelper.TABLE_MESSAGE, null, values);
                getContext().getContentResolver().notifyChange(MESSAGES_CONTENT_URI, null);
                getContext().getContentResolver().notifyChange(MESSAGES_CONTACT_CONTENT_URI, null);
                result = id == -1 ? null : Uri.parse("content://" + AUTHORITY + "/" + MESSAGES_PATH + "/" + id);
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

        int uriType = URI_MATCHER.match(uri);
        switch (uriType) {
            case CONTACT_BY_ID:
            case CONTACT_BY_CONTACT_PRIVATE_KEY:
            case CONTACTS_ALL:
                checkColumnsContact(projection);
                switch (uriType) {
                    case CONTACT_BY_ID:
                        queryBuilder.appendWhere(DBHelper.CONTACT_COLUMN_ID + "=" + uri.getLastPathSegment());
                        break;
                    case CONTACT_BY_CONTACT_PRIVATE_KEY:
                        queryBuilder.appendWhere(DBHelper.CONTACT_COLUMN_PUBLIC_KEY + "='" + uri.getLastPathSegment() + "'");// getLastPathSegment() is already decoded
                        break;
                }
                queryBuilder.setTables(DBHelper.TABLE_CONTACT);
                break;
            case MESSAGE_BY_ID:
                // adding the ID to the original query
                queryBuilder.appendWhere(DBHelper.MESSAGE_COLUMN_ID + "=" + uri.getLastPathSegment());
            case MESSAGES_ALL:
                checkColumnsMessage(projection);
                queryBuilder.setTables(DBHelper.TABLE_MESSAGE);
                break;
            case MESSAGES_CONTACT_BY_ID:
            case MESSAGES_CONTACT_BY_CONTACT_PRIVATE_KEY:
            case MESSAGES_CONTACT_ALL:
                checkColumnsMessagesContactJoin(DBHelper.MESSAGE_CONTACT_ALL_COLUMNS);
                switch (uriType) {
                    case MESSAGES_CONTACT_BY_ID:
                        queryBuilder.appendWhere(DBHelper.MESSAGE_COLUMN_CONTACT_ID + "=" + uri.getLastPathSegment());
                        break;
                    case MESSAGES_CONTACT_BY_CONTACT_PRIVATE_KEY:
                        queryBuilder.appendWhere(DBHelper.MESSAGE_CONTACT_COLUMN_CONTACT_PUBLIC_KEY + "='" + uri.getLastPathSegment() + "'"); // getLastPathSegment() is already decoded
                        break;
                }
                queryBuilder.setTables(DBHelper.TABLE_JOIN_MESSAGE_CONTACT);
                queryBuilder.setProjectionMap(DBHelper.MESSAGE_CONTACT_JOIN_PROJECTION_MAP);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        cursor = queryBuilder.query(dbHelper.getReadableDatabase(), projection, selection, selectionArgs, null, null, sortOrder);

        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Unsupported operation");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = URI_MATCHER.match(uri);
        SQLiteDatabase sqlDB = dbHelper.getWritableDatabase();

        int rowsDeleted;

        switch (uriType) {
            case CONTACT_BY_ID:
                String contactId = uri.getLastPathSegment();
                rowsDeleted = sqlDB.delete(DBHelper.TABLE_CONTACT, DBHelper.CONTACT_COLUMN_ID + "=" + contactId, null);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(CONTACTS_CONTENT_URI, null);
        getContext().getContentResolver().notifyChange(MESSAGES_CONTACT_CONTENT_URI, null);

        return rowsDeleted;
    }

    private void checkColumnsContact(String[] projection) {
        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<>(Arrays.asList(DBHelper.CONTACT_ALL_COLUMNS));
            // check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }

    private void checkColumnsMessage(String[] projection) {
        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<>(Arrays.asList(DBHelper.MESSAGE_ALL_COLUMNS));
            // check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }

    private void checkColumnsMessagesContactJoin(String[] projection) {
        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<>(Arrays.asList(projection));
            HashSet<String> availableColumns = new HashSet<>(Arrays.asList(DBHelper.MESSAGE_CONTACT_ALL_COLUMNS));
            // check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }
}
