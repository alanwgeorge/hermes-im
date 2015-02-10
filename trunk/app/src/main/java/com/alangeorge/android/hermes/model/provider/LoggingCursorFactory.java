package com.alangeorge.android.hermes.model.provider;

import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQuery;
import android.util.Log;

public class LoggingCursorFactory implements SQLiteDatabase.CursorFactory {
    private static final String TAG = "LoggingCursorFactory";
    @Override
    public Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery, String editTable, SQLiteQuery query) {
        Log.d(TAG, "query:" + query.toString());

        return new SQLiteCursor(masterQuery, editTable, query);
    }
}
