/*
* Copyright (C) 2014 The CyanogenMod Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.poupa.vinylmusicplayer.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.poupa.vinylmusicplayer.model.Song;

import java.util.ArrayList;
import java.util.List;

public class HistoryStore extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "history.db";
    private static final int VERSION = 1;
    @Nullable
    private static HistoryStore sInstance = null;

    public HistoryStore(final Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(@NonNull final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + RecentStoreColumns.NAME + " ("
                + RecentStoreColumns.ID + " LONG NOT NULL," + RecentStoreColumns.TIME_PLAYED
                + " LONG NOT NULL);");
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + RecentStoreColumns.NAME);
        onCreate(db);
    }

    @Override
    public void onDowngrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + RecentStoreColumns.NAME);
        onCreate(db);
    }

    @NonNull
    public static synchronized HistoryStore getInstance(@NonNull final Context context) {
        if (sInstance == null) {
            sInstance = new HistoryStore(context.getApplicationContext());
        }
        return sInstance;
    }

    public void addSongId(final long songId) {
        if (songId == Song.EMPTY_SONG.id) {
            return;
        }
        addSongIds(List.of(songId));
    }

    public void addSongIds(@NonNull List<Long> songIds) {
        if (songIds.isEmpty()) {
            return;
        }

        final SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();

        try {
            long importTime = System.currentTimeMillis();
            final ContentValues values = new ContentValues(2);

            for (long songId : songIds) {
                // remove previous entries
                removeSongId(database, songId);

                // add the entry
                values.clear();
                values.put(RecentStoreColumns.ID, songId);
                values.put(RecentStoreColumns.TIME_PLAYED, importTime);
                database.insert(RecentStoreColumns.NAME, null, values);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public void removeSongIds(@NonNull List<Long> missingIds) {
        if (missingIds.isEmpty()) return;

        final SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();
        try {
            for (long id : missingIds) {
                removeSongId(database, id);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private void removeSongId(@NonNull final SQLiteDatabase database, final long songId) {
        database.delete(RecentStoreColumns.NAME, RecentStoreColumns.ID + " = ?", new String[]{
                String.valueOf(songId)
        });
    }

    public void clear() {
        final SQLiteDatabase database = getWritableDatabase();
        database.delete(RecentStoreColumns.NAME, null, null);
    }

    public boolean contains(long id) {
        final SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(RecentStoreColumns.NAME,
                new String[]{RecentStoreColumns.ID},
                RecentStoreColumns.ID + "=?",
                new String[]{String.valueOf(id)},
                null, null, null, null);

        boolean containsId = cursor != null && cursor.moveToFirst();
        if (cursor != null) {
            cursor.close();
        }
        return containsId;
    }

    @NonNull
    public ArrayList<Long> getRecentIds(long cutoff) {
        try (Cursor cursor = queryRecentIds(cutoff)) {
            return StoreLoader.getIdsFromCursor(cursor, RecentStoreColumns.ID);
        }
    }

    private Cursor queryRecentIds(long cutoff) {
        final boolean noCutoffTime = (cutoff == 0);
        final boolean reverseOrder = (cutoff < 0);
        if (reverseOrder) cutoff = -cutoff;

        final SQLiteDatabase database = getReadableDatabase();

        return database.query(RecentStoreColumns.NAME,
                new String[]{RecentStoreColumns.ID},
                noCutoffTime ? null : RecentStoreColumns.TIME_PLAYED + (reverseOrder ? "<?" : ">?"),
                noCutoffTime ? null : new String[]{String.valueOf(cutoff)},
                null, null,
                RecentStoreColumns.TIME_PLAYED + (reverseOrder ? " ASC" : " DESC"));
    }

    public interface RecentStoreColumns {
        String NAME = "recent_history";

        String ID = "song_id";
        String TIME_PLAYED = "time_played";
    }
}
