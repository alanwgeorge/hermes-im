package com.alangeorge.android.hermes.model.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 *
 * Below are example commands (OSX) to access the database of a device with BloodHound installed
 * <p>
 * <pre>
 * {@code
 * $ adb -s 10.0.1.28:5555 backup -f data.ab -noapk com.alangeorge.android.hermes
 * $ dd if=data.ab bs=1 skip=24 | python -c "import zlib,sys;sys.stdout.write(zlib.decompress(sys.stdin.read()))" | tar -xvf -
 * $ sqlite3 apps/com.alangeorge.android.hermes/db/hermes.db
 * sqlite> select * from contact;
 * sqlite> select * from message;
 * }
 * </pre>
 */
public class DBHelper extends SQLiteOpenHelper {
    private static final String TAG = "DBHelper";

    private static final String DATABASE_NAME = "hermes.db";
    private static final int DATABASE_VERSION = 2;

    // contact Table
    public static final String TABLE_CONTACT = "contact";
    public static final String CONTACT_COLUMN_ID = "_id";
    public static final String CONTACT_COLUMN_NAME = "name";
    public static final String CONTACT_COLUMN_PUBLIC_KEY = "public_key";
    public static final String CONTACT_COLUMN_GCM_ID = "gcm_id";
    public static final String CONTACT_COLUMN_CREATE_TIME = "createtime";

    public static final String[] CONTACT_ALL_COLUMNS = {
            CONTACT_COLUMN_ID,
            CONTACT_COLUMN_NAME,
            CONTACT_COLUMN_PUBLIC_KEY,
            CONTACT_COLUMN_GCM_ID,
            CONTACT_COLUMN_CREATE_TIME
    };

    @SuppressWarnings("UnusedDeclaration")
    public static final String CONTACT_DATABASE_CREATE = "create table " + TABLE_CONTACT + " (" + CONTACT_COLUMN_ID
            + " integer primary key autoincrement, " + CONTACT_COLUMN_NAME + " text not null, " + CONTACT_COLUMN_PUBLIC_KEY
            + " text not null, " + CONTACT_COLUMN_GCM_ID + " text not null, " + CONTACT_COLUMN_CREATE_TIME + " integer not null);";

    // message Table
    public static final String TABLE_MESSAGE = "message";
    public static final String MESSAGE_COLUMN_ID = "_id";
    public static final String MESSAGE_COLUMN_CONTACT_ID = "contact_id";
    public static final String MESSAGE_COLUMN_MESSAGE_TEXT = "message_text";
    public static final String MESSAGE_COLUMN_READ_TIME = "read_time"; // -1 == not yet read
    public static final String MESSAGE_COLUMN_CREATE_TIME = "createtime";

    @SuppressWarnings("UnusedDeclaration")
    public static final String[] MESSAGE_ALL_COLUMNS = {
            MESSAGE_COLUMN_ID,
            MESSAGE_COLUMN_CONTACT_ID,
            MESSAGE_COLUMN_MESSAGE_TEXT,
            MESSAGE_COLUMN_READ_TIME,
            MESSAGE_COLUMN_CREATE_TIME
    };

    public static final String MESSAGE_DATABASE_CREATE = "create table " + TABLE_MESSAGE + " (" + MESSAGE_COLUMN_ID
            + " integer primary key autoincrement, " + MESSAGE_COLUMN_CONTACT_ID + " integer not null, " + MESSAGE_COLUMN_MESSAGE_TEXT
            + " text not null, " + MESSAGE_COLUMN_READ_TIME + " integer, " + MESSAGE_COLUMN_CREATE_TIME + " integer not null, foreign key("
            + MESSAGE_COLUMN_CONTACT_ID + ") references " + TABLE_CONTACT + "(" + CONTACT_COLUMN_ID + "));";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate()");
        db.execSQL(MESSAGE_DATABASE_CREATE);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        Log.d(TAG, "onOpen()");
        super.onOpen(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGE);
        onCreate(db);
    }
}
