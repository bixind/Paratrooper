package com.pcom.paratrooper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.pcom.paratrooper.EventsContract.EventEntry;

public class EventDatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "EVENT_DATABASE";
    private static final String EVENT_TABLE_CREATE =
            "CREATE TABLE " + EventEntry.TABLE_NAME + " (" +
                    EventEntry._ID + " INTEGER PRIMARY KEY," +
                    EventEntry.COLUMN_NAME_TIME + " INT, " +
                    EventEntry.COLUMN_NAME_TYPE + " INT, " +
                    EventEntry.COLUMN_NAME_USER_ID + " INT, " +
                    EventEntry.COLUMN_NAME_EXTRA + " INT);";

    private static final String EVENT_TABLE_DELETE =
            "DROP TABLE IF EXISTS " + EventEntry.TABLE_NAME;

    EventDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(EVENT_TABLE_DELETE);
        onCreate(db);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(EVENT_TABLE_CREATE);
    }
}
