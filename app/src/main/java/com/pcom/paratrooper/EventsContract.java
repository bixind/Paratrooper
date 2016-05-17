package com.pcom.paratrooper;

import android.provider.BaseColumns;

public final class EventsContract {
    public EventsContract() {}

    public static abstract class EventEntry implements BaseColumns {
        public static final String TABLE_NAME = "events";
        public static final String COLUMN_NAME_TIME = "time";
        public static final String COLUMN_NAME_TYPE = "type";
        public static final String COLUMN_NAME_USER_ID = "user_id";
        public static final String COLUMN_NAME_EXTRA = "extra";
    }
}
