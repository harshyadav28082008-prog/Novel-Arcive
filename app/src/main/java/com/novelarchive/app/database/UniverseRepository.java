
package com.novelarchive.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class UniverseRepository {

    private final DatabaseHelper helper;

    public UniverseRepository(Context context) {
        helper = new DatabaseHelper(context.getApplicationContext());
    }

    // --------------------------------------------------
    // DATA MODELS
    // --------------------------------------------------

    public static class Universe {
        public final long id;
        public final String name;
        public final String description;
        public final long createdAt;
        public final long updatedAt;

        public Universe(
            long id,
            String name,
            String description,
            long createdAt,
            long updatedAt
        ) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
    }

    public static class Novel {
        public final long id;
        public final long universeId;
        public final String title;
        public final String description;
        public final long createdAt;
        public final long updatedAt;

        public Novel(
            long id,
            long universeId,
            String title,
            String description,
            long createdAt,
            long updatedAt
        ) {
            this.id = id;
            this.universeId = universeId;
            this.title = title;
            this.description = description;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
    }

    // --------------------------------------------------
    // UNIVERSE OPERATIONS
    // --------------------------------------------------

    public long createUniverse(
        String name,
        String description
    ) {
        name = validateName(name, "Universe name");

        long now = System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("description", cleanDescription(description));
        values.put("created_at", now);
        values.put("updated_at", now);

        return helper.getWritableDatabase().insertOrThrow(
            "universes",
            null,
            values
        );
    }

    public List<Universe> getAllUniverses() {
        List<Universe> result = new ArrayList<>();

        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cursor = db.query(
            "universes",
            new String[]{
                "id",
                "name",
                "description",
                "created_at",
                "updated_at"
            },
            null,
            null,
            null,
            null,
            "updated_at DESC, id DESC"
        );

        try {
            while (cursor.moveToNext()) {
                result.add(readUniverse(cursor));
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    public Universe getUniverseById(long universeId) {
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cursor = db.query(
            "universes",
            new String[]{
                "id",
                "name",
                "description",
                "created_at",
                "updated_at"
            },
            "id = ?",
            new String[]{String.valueOf(universeId)},
            null,
            null,
            null
        );

        try {
            if (cursor.moveToFirst()) {
                return readUniverse(cursor);
            }

            return null;
        } finally {
            cursor.close();
        }
    }

    public boolean updateUniverse(
        long universeId,
        String name,
        String description
    ) {
        name = validateName(name, "Universe name");

        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("description", cleanDescription(description));
        values.put("updated_at", System.currentTimeMillis());

        int rows = helper.getWritableDatabase().update(
            "universes",
            values,
            "id = ?",
            new String[]{String.valueOf(universeId)}
        );

        return rows > 0;
    }

    // Returns false if the universe does not exist
    // or still contains novels.
    public boolean deleteUniverse(long universeId) {
        try {
            int rows = helper.getWritableDatabase().delete(
                "universes",
                "id = ?",
                new String[]{String.valueOf(universeId)}
            );

            return rows > 0;

        } catch (SQLiteConstraintException e) {
            return false;
        }
    }

    // --------------------------------------------------
    // NOVEL OPERATIONS
    // --------------------------------------------------

    public long createNovel(
        long universeId,
        String title,
        String description
    ) {
        title = validateName(title, "Novel title");

        long now = System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put("universe_id", universeId);
        values.put("title", title);
        values.put("description", cleanDescription(description));
        values.put("created_at", now);
        values.put("updated_at", now);

        return helper.getWritableDatabase().insertOrThrow(
            "novels",
            null,
            values
        );
    }

    public List<Novel> getNovelsForUniverse(long universeId) {
        List<Novel> result = new ArrayList<>();

        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cursor = db.query(
            "novels",
            new String[]{
                "id",
                "universe_id",
                "title",
                "description",
                "created_at",
                "updated_at"
            },
            "universe_id = ?",
            new String[]{String.valueOf(universeId)},
            null,
            null,
            "updated_at DESC, id DESC"
        );

        try {
            while (cursor.moveToNext()) {
                result.add(readNovel(cursor));
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    public Novel getNovelById(long novelId) {
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cursor = db.query(
            "novels",
            new String[]{
                "id",
                "universe_id",
                "title",
                "description",
                "created_at",
                "updated_at"
            },
            "id = ?",
            new String[]{String.valueOf(novelId)},
            null,
            null,
            null
        );

        try {
            if (cursor.moveToFirst()) {
                return readNovel(cursor);
            }

            return null;
        } finally {
            cursor.close();
        }
    }

    public boolean updateNovel(
        long novelId,
        String title,
        String description
    ) {
        title = validateName(title, "Novel title");

        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("description", cleanDescription(description));
        values.put("updated_at", System.currentTimeMillis());

        int rows = helper.getWritableDatabase().update(
            "novels",
            values,
            "id = ?",
            new String[]{String.valueOf(novelId)}
        );

        return rows > 0;
    }

    // Deleting a novel also deletes its dependent records
    // according to the ON DELETE CASCADE rules in the schema.
    public boolean deleteNovel(long novelId) {
        int rows = helper.getWritableDatabase().delete(
            "novels",
            "id = ?",
            new String[]{String.valueOf(novelId)}
        );

        return rows > 0;
    }

    // --------------------------------------------------
    // INTERNAL HELPERS
    // --------------------------------------------------

    private Universe readUniverse(Cursor cursor) {
        return new Universe(
            cursor.getLong(0),
            cursor.getString(1),
            cursor.getString(2),
            cursor.getLong(3),
            cursor.getLong(4)
        );
    }

    private Novel readNovel(Cursor cursor) {
        return new Novel(
            cursor.getLong(0),
            cursor.getLong(1),
            cursor.getString(2),
            cursor.getString(3),
            cursor.getLong(4),
            cursor.getLong(5)
        );
    }

    private String validateName(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                fieldName + " cannot be empty."
            );
        }

        return value.trim();
    }

    private String cleanDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return null;
        }

        return description.trim();
    }

    public void close() {
        helper.close();
    }
}
