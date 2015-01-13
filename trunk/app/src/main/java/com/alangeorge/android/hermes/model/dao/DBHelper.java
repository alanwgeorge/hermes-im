package com.alangeorge.android.hermes.model.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.alangeorge.android.hermes.BuildConfig;
import com.alangeorge.android.hermes.model.provider.LoggingCursorFactory;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * Below are example commands (OSX) to access the database of a device with Hermes installed
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
    private static final String TAG = "Hermes.DBHelper";

    private static final String DATABASE_NAME = "hermes.db";
    private static final int DATABASE_VERSION = 3;

    // contact Table
    public static final String TABLE_CONTACT = "contact";
    public static final String CONTACT_COLUMN_ID = "_id";
    public static final String CONTACT_COLUMN_NAME = "name";
    public static final String CONTACT_COLUMN_PUBLIC_KEY = "public_key";
    public static final String CONTACT_COLUMN_GCM_ID = "gcm_id";
    @SuppressWarnings("SpellCheckingInspection")
    public static final String CONTACT_COLUMN_CREATE_TIME = "createtime";

    public static final String[] CONTACT_ALL_COLUMNS = {
            CONTACT_COLUMN_ID,
            CONTACT_COLUMN_NAME,
            CONTACT_COLUMN_PUBLIC_KEY,
            CONTACT_COLUMN_GCM_ID,
            CONTACT_COLUMN_CREATE_TIME
    };

    @SuppressWarnings("UnusedDeclaration")
    public static final String CONTACT_TABLE_CREATE = "create table " + TABLE_CONTACT + " (" + CONTACT_COLUMN_ID
            + " integer primary key autoincrement, " + CONTACT_COLUMN_NAME + " text not null, " + CONTACT_COLUMN_PUBLIC_KEY
            + " text not null, " + CONTACT_COLUMN_GCM_ID + " text not null, " + CONTACT_COLUMN_CREATE_TIME + " integer not null);";

    // message Table
    public static final String TABLE_MESSAGE = "message";
    public static final String INDEX_MESSAGE_CONTACT_ID = "contact_id_index";
    public static final String MESSAGE_COLUMN_ID = "_id";
    public static final String MESSAGE_COLUMN_CONTACT_ID = "contact_id";
    public static final String MESSAGE_COLUMN_MESSAGE_JSON = "message_json";
    public static final String MESSAGE_COLUMN_READ_TIME = "read_time"; // -1 == not yet read
    @SuppressWarnings("SpellCheckingInspection")
    public static final String MESSAGE_COLUMN_CREATE_TIME = "createtime";

    public static final String[] MESSAGE_ALL_COLUMNS = {
            MESSAGE_COLUMN_ID, // 0
            MESSAGE_COLUMN_CONTACT_ID, // 1
            MESSAGE_COLUMN_MESSAGE_JSON, // 2
            MESSAGE_COLUMN_READ_TIME, // 3
            MESSAGE_COLUMN_CREATE_TIME // 4
    };

    private static final String MESSAGE_TABLE_CREATE = "create table " + TABLE_MESSAGE + " (" + MESSAGE_COLUMN_ID
            + " integer primary key autoincrement, " + MESSAGE_COLUMN_CONTACT_ID + " integer not null, " + MESSAGE_COLUMN_MESSAGE_JSON
            + " text not null, " + MESSAGE_COLUMN_READ_TIME + " integer, " + MESSAGE_COLUMN_CREATE_TIME + " integer not null, foreign key("
            + MESSAGE_COLUMN_CONTACT_ID + ") references " + TABLE_CONTACT + "(" + CONTACT_COLUMN_ID + ") on delete cascade);";

    public static final String MESSAGE_INDEX_CONTACT_ID_CREATE = "create index " + INDEX_MESSAGE_CONTACT_ID + " on " + TABLE_MESSAGE + "(" + MESSAGE_COLUMN_CONTACT_ID + ");";

    // message/contact join
    private static final String MESSAGE_PREFIX = "m";
    private static final String CONTACT_PREFIX = "c";

    public static final String MESSAGE_CONTACT_COLUMN_MESSAGE_ID = MESSAGE_COLUMN_ID;
    public static final String MESSAGE_CONTACT_COLUMN_MESSAGE_CONTACT_ID = MESSAGE_COLUMN_CONTACT_ID;
    public static final String MESSAGE_CONTACT_COLUMN_MESSAGE_MESSAGE_JSON = MESSAGE_COLUMN_MESSAGE_JSON;
    public static final String MESSAGE_CONTACT_COLUMN_MESSAGE_READ_TIME = MESSAGE_COLUMN_READ_TIME;
    public static final String MESSAGE_CONTACT_COLUMN_MESSAGE_CREATE_TIME = MESSAGE_COLUMN_CREATE_TIME;
    public static final String MESSAGE_CONTACT_COLUMN_CONTACT_ID = CONTACT_PREFIX + "_" + CONTACT_PREFIX + CONTACT_COLUMN_ID;
    public static final String MESSAGE_CONTACT_COLUMN_CONTACT_NAME = CONTACT_PREFIX + "_" + CONTACT_COLUMN_NAME;
    public static final String MESSAGE_CONTACT_COLUMN_CONTACT_PUBLIC_KEY = CONTACT_PREFIX + "_" + CONTACT_COLUMN_PUBLIC_KEY;
    public static final String MESSAGE_CONTACT_COLUMN_CONTACT_GCM_ID = CONTACT_PREFIX + "_" + CONTACT_COLUMN_GCM_ID;
    public static final String MESSAGE_CONTACT_COLUMN_CONTACT_CREATE_TIME = CONTACT_PREFIX + "_" + CONTACT_COLUMN_CREATE_TIME;

    public static final String TABLE_JOIN_MESSAGE_CONTACT = TABLE_MESSAGE + " " + MESSAGE_PREFIX + " INNER JOIN "
            + TABLE_CONTACT + " " + CONTACT_PREFIX + " ON (" + MESSAGE_PREFIX + "." + MESSAGE_COLUMN_CONTACT_ID
            + " = " + CONTACT_PREFIX + "." + CONTACT_COLUMN_ID + ")";

    //SELECT m._id as _id, m.contact_id as contact_id, m.message_json as message_json, m.read_time as read_time, m.createtime as createtime, c._id as c_c_id, c.name as c_name, c.public_key as c_public_key, c.gcm_id as c_gcm_id, c.createtime as c_createtime FROM message m INNER JOIN contact c ON (m.contact_id = c._id) WHERE (contact_id=1)
    public static final Map<String, String > MESSAGE_CONTACT_JOIN_PROJECTION_MAP;
    static {
        MESSAGE_CONTACT_JOIN_PROJECTION_MAP = new HashMap<>();
        MESSAGE_CONTACT_JOIN_PROJECTION_MAP.put(MESSAGE_CONTACT_COLUMN_MESSAGE_ID, MESSAGE_PREFIX + "." + MESSAGE_COLUMN_ID + " as " + MESSAGE_COLUMN_ID);
        MESSAGE_CONTACT_JOIN_PROJECTION_MAP.put(MESSAGE_CONTACT_COLUMN_MESSAGE_CONTACT_ID, MESSAGE_PREFIX + "." + MESSAGE_COLUMN_CONTACT_ID + " as " + MESSAGE_COLUMN_CONTACT_ID);
        MESSAGE_CONTACT_JOIN_PROJECTION_MAP.put(MESSAGE_CONTACT_COLUMN_MESSAGE_MESSAGE_JSON, MESSAGE_PREFIX + "." + MESSAGE_COLUMN_MESSAGE_JSON + " as " + MESSAGE_COLUMN_MESSAGE_JSON);
        MESSAGE_CONTACT_JOIN_PROJECTION_MAP.put(MESSAGE_CONTACT_COLUMN_MESSAGE_READ_TIME, MESSAGE_PREFIX + "." + MESSAGE_COLUMN_READ_TIME + " as " + MESSAGE_COLUMN_READ_TIME);
        MESSAGE_CONTACT_JOIN_PROJECTION_MAP.put(MESSAGE_CONTACT_COLUMN_MESSAGE_CREATE_TIME, MESSAGE_PREFIX + "." + MESSAGE_COLUMN_CREATE_TIME + " as " + MESSAGE_COLUMN_CREATE_TIME);
        MESSAGE_CONTACT_JOIN_PROJECTION_MAP.put(MESSAGE_CONTACT_COLUMN_CONTACT_ID, CONTACT_PREFIX + "." + CONTACT_COLUMN_ID + " as " + CONTACT_PREFIX + "_" + CONTACT_PREFIX + CONTACT_COLUMN_ID);
        MESSAGE_CONTACT_JOIN_PROJECTION_MAP.put(MESSAGE_CONTACT_COLUMN_CONTACT_NAME, CONTACT_PREFIX + "." + CONTACT_COLUMN_NAME + " as " + CONTACT_PREFIX + "_" + CONTACT_COLUMN_NAME);
        MESSAGE_CONTACT_JOIN_PROJECTION_MAP.put(MESSAGE_CONTACT_COLUMN_CONTACT_PUBLIC_KEY, CONTACT_PREFIX + "." + CONTACT_COLUMN_PUBLIC_KEY + " as " + CONTACT_PREFIX + "_" + CONTACT_COLUMN_PUBLIC_KEY);
        MESSAGE_CONTACT_JOIN_PROJECTION_MAP.put(MESSAGE_CONTACT_COLUMN_CONTACT_GCM_ID, CONTACT_PREFIX + "." + CONTACT_COLUMN_GCM_ID + " as " + CONTACT_PREFIX + "_" + CONTACT_COLUMN_GCM_ID);
        MESSAGE_CONTACT_JOIN_PROJECTION_MAP.put(MESSAGE_CONTACT_COLUMN_CONTACT_CREATE_TIME, CONTACT_PREFIX + "." + CONTACT_COLUMN_CREATE_TIME + " as " + CONTACT_PREFIX + "_" + CONTACT_COLUMN_CREATE_TIME);
    }

    public static final String[] MESSAGE_CONTACT_ALL_COLUMNS = {
            MESSAGE_CONTACT_COLUMN_MESSAGE_ID, // 0
            MESSAGE_CONTACT_COLUMN_MESSAGE_CONTACT_ID, // 1
            MESSAGE_CONTACT_COLUMN_MESSAGE_MESSAGE_JSON, // 2
            MESSAGE_CONTACT_COLUMN_MESSAGE_READ_TIME, // 3
            MESSAGE_CONTACT_COLUMN_MESSAGE_CREATE_TIME, // 4
            MESSAGE_CONTACT_COLUMN_CONTACT_ID, // 5
            MESSAGE_CONTACT_COLUMN_CONTACT_NAME, // 6
            MESSAGE_CONTACT_COLUMN_CONTACT_PUBLIC_KEY, // 7
            MESSAGE_CONTACT_COLUMN_CONTACT_GCM_ID, // 8
            MESSAGE_CONTACT_COLUMN_CONTACT_CREATE_TIME // 9

    };

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, BuildConfig.DEBUG ? new LoggingCursorFactory() : null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate()");
        db.execSQL(CONTACT_TABLE_CREATE);
        db.execSQL(MESSAGE_TABLE_CREATE);
        db.execSQL(MESSAGE_INDEX_CONTACT_ID_CREATE);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        Log.d(TAG, "onOpen()");
        super.onOpen(db);
        if (!db.isReadOnly()) {
            // Enable foreign key constraints
            db.execSQL("PRAGMA foreign_keys = ON;");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        db.execSQL("PRAGMA foreign_keys = OFF;");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACT);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGE);
        db.execSQL("DROP INDEX IF EXISTS " + INDEX_MESSAGE_CONTACT_ID);
        onCreate(db);

        if (newVersion == 0) {  // special case for test cases, creates fresh DB
            Log.d(TAG, "dropping and recreating database tables, newVersion is 0...");
            db.execSQL("PRAGMA foreign_keys = OFF;");
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACT);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGE);
            onCreate(db);
        }

        db.execSQL("PRAGMA foreign_keys = ON;");
    }
}
