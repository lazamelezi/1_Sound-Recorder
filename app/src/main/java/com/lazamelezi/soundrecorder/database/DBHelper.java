package com.lazamelezi.soundrecorder.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import androidx.annotation.Nullable;

import com.lazamelezi.soundrecorder.Helper.RecordingItem;
import com.lazamelezi.soundrecorder.listeners.OnDatabaseChangedListener;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "saved_recordings.db";
    public static final int DATABASE_VERSION = 1;
    public static final String COMMA_SEP = ",";
    public static final String TEXT_TYPE = " TEXT";
    public static final String INTEGER_TYPE = " INTEGER";
    private static final String SQL_CREATE_ENTRIES =

            "CREATE TABLE " + DBHelperItem.TABLE_NAME + " (" +

                    DBHelperItem._ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                    DBHelperItem.COLUMN_NAME_RECORDING_NAME + TEXT_TYPE + COMMA_SEP +
                    DBHelperItem.COLUMN_NAME_RECORDING_FILE_PATH + TEXT_TYPE + COMMA_SEP +
                    DBHelperItem.COLUMN_NAME_RECORDING_LENGTH + INTEGER_TYPE + COMMA_SEP +
                    DBHelperItem.COLUMN_NAME_RECORDING_TIME_ADDED + INTEGER_TYPE + ")";
    private static OnDatabaseChangedListener onDatabaseChangedListener;
    private Context mContext;


    public DBHelper(@Nullable Context mContext) {
        super(mContext, DATABASE_NAME, null, DATABASE_VERSION);

        this.mContext = mContext;
    }

    public static void setOnDatabaseChangedLister(OnDatabaseChangedListener listener) {
        onDatabaseChangedListener = listener;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public long addRecording(String recordingName, String filePath, long length) {

        SQLiteDatabase sqLiteDatabase = getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(DBHelperItem.COLUMN_NAME_RECORDING_NAME, recordingName);
        contentValues.put(DBHelperItem.COLUMN_NAME_RECORDING_FILE_PATH, filePath);
        contentValues.put(DBHelperItem.COLUMN_NAME_RECORDING_LENGTH, length);
        contentValues.put(DBHelperItem.COLUMN_NAME_RECORDING_TIME_ADDED, System.currentTimeMillis());

        long rowId = sqLiteDatabase.insert(DBHelperItem.TABLE_NAME, null, contentValues);

        if (onDatabaseChangedListener != null)
            onDatabaseChangedListener.onNewDatabaseEntryAdded();

        return rowId;
    }

    public int getCount() {
        SQLiteDatabase sqLiteDatabase = getReadableDatabase();
        String[] projection = {DBHelperItem._ID};

        Cursor c = sqLiteDatabase.query(DBHelperItem.TABLE_NAME, projection,
                null, null, null, null, null);

        int count = c.getCount();
        c.close();
        return count;
    }

    public RecordingItem getItemAt(int position) {

        SQLiteDatabase db = getReadableDatabase();

        String[] projection = {DBHelperItem._ID,
                DBHelperItem.COLUMN_NAME_RECORDING_NAME,
                DBHelperItem.COLUMN_NAME_RECORDING_FILE_PATH,
                DBHelperItem.COLUMN_NAME_RECORDING_LENGTH,
                DBHelperItem.COLUMN_NAME_RECORDING_TIME_ADDED};

        Cursor c = db.query(DBHelperItem.TABLE_NAME, projection, null, null, null, null, null);

        if (c.moveToPosition(position)) {
            RecordingItem item = new RecordingItem();
            item.setId(c.getInt(c.getColumnIndex(DBHelperItem._ID)));
            item.setName(c.getString(c.getColumnIndex(DBHelperItem.COLUMN_NAME_RECORDING_NAME)));
            item.setFilePath(c.getString(c.getColumnIndex(DBHelperItem.COLUMN_NAME_RECORDING_FILE_PATH)));
            item.setLength(c.getInt(c.getColumnIndex(DBHelperItem.COLUMN_NAME_RECORDING_LENGTH)));
            item.setTime(c.getLong(c.getColumnIndex(DBHelperItem.COLUMN_NAME_RECORDING_TIME_ADDED)));
            c.close();
            return item;
        }
        return null;
    }

    public void renameItem(RecordingItem item, String name, String filePath) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DBHelperItem.COLUMN_NAME_RECORDING_NAME, name);
        cv.put(DBHelperItem.COLUMN_NAME_RECORDING_FILE_PATH, filePath);

        db.update(DBHelperItem.TABLE_NAME, cv, DBHelperItem._ID + "=" + item.getId(), null);

        if (onDatabaseChangedListener != null)
            onDatabaseChangedListener.onDatabaseEntryRenamed();
    }

    public void removeItemWithId(int id) {
        SQLiteDatabase db = getWritableDatabase();
        String[] whereArgs = {String.valueOf(id)};
        db.delete(DBHelperItem.TABLE_NAME, "_ID=?", whereArgs);
    }

    public void deleteAllData() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("delete from " + DBHelperItem.TABLE_NAME);
    }

    public static abstract class DBHelperItem implements BaseColumns {
        public static final String TABLE_NAME = "saved_recordings";
        public static final String COLUMN_NAME_RECORDING_NAME = "recording_name";
        public static final String COLUMN_NAME_RECORDING_FILE_PATH = "file_path";
        public static final String COLUMN_NAME_RECORDING_LENGTH = "length";
        public static final String COLUMN_NAME_RECORDING_TIME_ADDED = "time_added";
    }
}


