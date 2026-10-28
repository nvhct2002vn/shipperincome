package com.example.shipperincome.deliverynotes;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public final class NotesDb extends SQLiteOpenHelper {
    private static final String DB_NAME = "delivery_notes.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE = "location_notes";

    private static volatile NotesDb instance;

    /** Mot instance cho ca app (UI va service) de khong mo nhieu ket noi toi cung mot file. */
    public static NotesDb get(Context context) {
        NotesDb local = instance;
        if (local == null) {
            synchronized (NotesDb.class) {
                local = instance;
                if (local == null) {
                    local = new NotesDb(context.getApplicationContext());
                    instance = local;
                }
            }
        }
        return local;
    }

    private NotesDb(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        setWriteAheadLoggingEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "note TEXT NOT NULL," +
                "latitude REAL NOT NULL," +
                "longitude REAL NOT NULL," +
                "radius_meters INTEGER NOT NULL," +
                "created_at INTEGER NOT NULL," +
                "last_notified_at INTEGER NOT NULL DEFAULT 0" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Nang cap tung buoc, khong bao gio DROP bang (se mat het ghi chu cua nguoi dung).
        // Khi tang DB_VERSION, them mot nhanh moi, vi du:
        // if (oldVersion < 2) db.execSQL("ALTER TABLE " + TABLE + " ADD COLUMN ...");
    }

    public long addNote(String name, String note, double latitude, double longitude, int radiusMeters) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("note", note);
        values.put("latitude", latitude);
        values.put("longitude", longitude);
        values.put("radius_meters", radiusMeters);
        values.put("created_at", System.currentTimeMillis());
        values.put("last_notified_at", 0);
        return getWritableDatabase().insert(TABLE, null, values);
    }

    /** Them nhieu diem trong mot transaction: hoac vao het, hoac khong vao diem nao. */
    public void addNotes(List<LocationNote> notes) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (LocationNote n : notes) {
                addNote(n.name, n.note, n.latitude, n.longitude, n.radiusMeters);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void updateNote(long id, String name, String note, int radiusMeters) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("note", note);
        values.put("radius_meters", radiusMeters);
        getWritableDatabase().update(TABLE, values, "id=?", new String[]{String.valueOf(id)});
    }

    public List<LocationNote> getAllNotes() {
        ArrayList<LocationNote> notes = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                TABLE,
                null,
                null,
                null,
                null,
                null,
                "created_at DESC")) {
            while (cursor.moveToNext()) {
                notes.add(fromCursor(cursor));
            }
        }
        return notes;
    }

    public void deleteNote(long id) {
        getWritableDatabase().delete(TABLE, "id=?", new String[]{String.valueOf(id)});
    }

    public void markNotified(long id, long whenMillis) {
        ContentValues values = new ContentValues();
        values.put("last_notified_at", whenMillis);
        getWritableDatabase().update(TABLE, values, "id=?", new String[]{String.valueOf(id)});
    }

    private static LocationNote fromCursor(Cursor cursor) {
        return new LocationNote(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("name")),
                cursor.getString(cursor.getColumnIndexOrThrow("note")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
                cursor.getInt(cursor.getColumnIndexOrThrow("radius_meters")),
                cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                cursor.getLong(cursor.getColumnIndexOrThrow("last_notified_at")));
    }
}
