
package com.novelarchive.app.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "novel_archive.db";
    private static final int DATABASE_VERSION = 2;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // 1. Universes
        db.execSQL(
            "CREATE TABLE universes (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "name TEXT NOT NULL," +
            "description TEXT," +
            "created_at INTEGER NOT NULL," +
            "updated_at INTEGER NOT NULL)"
        );

        // 2. Novels
        // RESTRICT prevents accidental deletion of a universe
        // that still contains novels.
        db.execSQL(
            "CREATE TABLE novels (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "universe_id INTEGER NOT NULL," +
            "title TEXT NOT NULL," +
            "description TEXT," +
            "created_at INTEGER NOT NULL," +
            "updated_at INTEGER NOT NULL," +
            "cover_image_path TEXT," +
            "FOREIGN KEY(universe_id) REFERENCES universes(id) " +
            "ON DELETE RESTRICT)"
        );

        // 3. Shared Name Bank
        db.execSQL(
            "CREATE TABLE names (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "name TEXT NOT NULL COLLATE NOCASE UNIQUE," +
            "gender TEXT NOT NULL DEFAULT 'Unknown' " +
            "CHECK(gender IN ('Male','Female','Unknown'))," +
            "never_reuse INTEGER NOT NULL DEFAULT 0 " +
            "CHECK(never_reuse IN (0,1))," +
            "notes TEXT," +
            "created_at INTEGER NOT NULL," +
            "updated_at INTEGER NOT NULL)"
        );

        // 4. Name usage across novels
        db.execSQL(
            "CREATE TABLE name_usage (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "name_id INTEGER NOT NULL," +
            "novel_id INTEGER NOT NULL," +
            "created_at INTEGER NOT NULL," +
            "UNIQUE(name_id, novel_id)," +
            "FOREIGN KEY(name_id) REFERENCES names(id) " +
            "ON DELETE CASCADE," +
            "FOREIGN KEY(novel_id) REFERENCES novels(id) " +
            "ON DELETE CASCADE)"
        );

        // 5. Categories
        // scope_type: UNIVERSAL, UNIVERSE, STORY
        db.execSQL(
            "CREATE TABLE categories (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "name TEXT NOT NULL COLLATE NOCASE," +
            "scope_type TEXT NOT NULL " +
            "CHECK(scope_type IN ('UNIVERSAL','UNIVERSE','STORY'))," +
            "scope_id INTEGER," +
            "importance INTEGER NOT NULL DEFAULT 3 " +
            "CHECK(importance BETWEEN 1 AND 5)," +
            "created_at INTEGER NOT NULL," +
            "updated_at INTEGER NOT NULL)"
        );

        // 6. Category assignments
        // Entity references are validated by application code
        // because this table supports multiple entity types.
        db.execSQL(
            "CREATE TABLE category_assignments (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "category_id INTEGER NOT NULL," +
            "entity_type TEXT NOT NULL " +
            "CHECK(entity_type IN ('NAME','CHARACTER','EVENT'))," +
            "entity_id INTEGER NOT NULL," +
            "UNIQUE(category_id, entity_type, entity_id)," +
            "FOREIGN KEY(category_id) REFERENCES categories(id) " +
            "ON DELETE CASCADE)"
        );

        // 7. Character profiles
        // UNIQUE(id, novel_id) allows related records to enforce
        // that records belong to the same novel.
        db.execSQL(
            "CREATE TABLE characters (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "novel_id INTEGER NOT NULL," +
            "name_id INTEGER," +
            "name TEXT NOT NULL," +
            "gender TEXT NOT NULL DEFAULT 'Unknown' " +
            "CHECK(gender IN ('Male','Female','Unknown'))," +
            "age TEXT," +
            "date_of_birth TEXT," +
            "role TEXT," +
            "description TEXT," +
            "personality TEXT," +
            "appearance TEXT," +
            "background TEXT," +
            "created_at INTEGER NOT NULL," +
            "updated_at INTEGER NOT NULL," +
            "UNIQUE(id, novel_id)," +
            "FOREIGN KEY(novel_id) REFERENCES novels(id) " +
            "ON DELETE CASCADE," +
            "FOREIGN KEY(name_id) REFERENCES names(id) " +
            "ON DELETE SET NULL)"
        );

        // 8. Custom character field definitions
        db.execSQL(
            "CREATE TABLE character_fields (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "novel_id INTEGER NOT NULL," +
            "field_name TEXT NOT NULL," +
            "field_type TEXT NOT NULL DEFAULT 'TEXT'," +
            "display_order INTEGER NOT NULL DEFAULT 0," +
            "created_at INTEGER NOT NULL," +
            "UNIQUE(id, novel_id)," +
            "FOREIGN KEY(novel_id) REFERENCES novels(id) " +
            "ON DELETE CASCADE)"
        );

        // 9. Custom character field values
        // Both the character and field must belong to novel_id.
        db.execSQL(
            "CREATE TABLE character_field_values (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "novel_id INTEGER NOT NULL," +
            "character_id INTEGER NOT NULL," +
            "field_id INTEGER NOT NULL," +
            "field_value TEXT," +
            "UNIQUE(character_id, field_id)," +
            "FOREIGN KEY(character_id, novel_id) " +
            "REFERENCES characters(id, novel_id) ON DELETE CASCADE," +
            "FOREIGN KEY(field_id, novel_id) " +
            "REFERENCES character_fields(id, novel_id) ON DELETE CASCADE)"
        );

        // 10. Character relationships
        // Both characters must belong to the same novel.
        db.execSQL(
            "CREATE TABLE character_relationships (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "novel_id INTEGER NOT NULL," +
            "character_id INTEGER NOT NULL," +
            "related_character_id INTEGER NOT NULL," +
            "relationship_type TEXT NOT NULL," +
            "description TEXT," +
            "CHECK(character_id != related_character_id)," +
            "UNIQUE(character_id, related_character_id, relationship_type)," +
            "FOREIGN KEY(character_id, novel_id) " +
            "REFERENCES characters(id, novel_id) ON DELETE CASCADE," +
            "FOREIGN KEY(related_character_id, novel_id) " +
            "REFERENCES characters(id, novel_id) ON DELETE CASCADE)"
        );

        // 11. Fictional calendars
        db.execSQL(
            "CREATE TABLE calendars (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "novel_id INTEGER NOT NULL UNIQUE," +
            "calendar_name TEXT NOT NULL," +
            "era_name TEXT," +
            "year_label TEXT DEFAULT 'Year'," +
            "has_weekdays INTEGER NOT NULL DEFAULT 0 " +
            "CHECK(has_weekdays IN (0,1))," +
            "days_per_week INTEGER NOT NULL DEFAULT 7 " +
            "CHECK(days_per_week > 0)," +
            "leap_year_rule TEXT," +
            "created_at INTEGER NOT NULL," +
            "updated_at INTEGER NOT NULL," +
            "FOREIGN KEY(novel_id) REFERENCES novels(id) " +
            "ON DELETE CASCADE)"
        );

        // 12. Calendar months
        db.execSQL(
            "CREATE TABLE calendar_months (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "calendar_id INTEGER NOT NULL," +
            "month_index INTEGER NOT NULL CHECK(month_index >= 0)," +
            "month_name TEXT NOT NULL," +
            "days_count INTEGER NOT NULL CHECK(days_count > 0)," +
            "UNIQUE(calendar_id, month_index)," +
            "FOREIGN KEY(calendar_id) REFERENCES calendars(id) " +
            "ON DELETE CASCADE)"
        );

        // 13. Timeline events
        db.execSQL(
            "CREATE TABLE timeline_events (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "novel_id INTEGER NOT NULL," +
            "title TEXT NOT NULL," +
            "year_number INTEGER NOT NULL," +
            "month_number INTEGER NOT NULL," +
            "day_number INTEGER NOT NULL," +
            "date_sort_key INTEGER," +
            "time_text TEXT," +
            "description TEXT," +
            "location TEXT," +
            "importance INTEGER NOT NULL DEFAULT 3 " +
            "CHECK(importance BETWEEN 1 AND 5)," +
            "created_at INTEGER NOT NULL," +
            "updated_at INTEGER NOT NULL," +
            "UNIQUE(id, novel_id)," +
            "FOREIGN KEY(novel_id) REFERENCES novels(id) " +
            "ON DELETE CASCADE)"
        );

        // 14. Characters involved in events
        // The event and character must belong to the same novel.
        db.execSQL(
            "CREATE TABLE event_characters (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "novel_id INTEGER NOT NULL," +
            "event_id INTEGER NOT NULL," +
            "character_id INTEGER NOT NULL," +
            "UNIQUE(event_id, character_id)," +
            "FOREIGN KEY(event_id, novel_id) " +
            "REFERENCES timeline_events(id, novel_id) ON DELETE CASCADE," +
            "FOREIGN KEY(character_id, novel_id) " +
            "REFERENCES characters(id, novel_id) ON DELETE CASCADE)"
        );

        // Original database indexes
        createIndexes(db);

        // Version 2 tables and indexes
        createNovelStructureTables(db);
    }

    private void createIndexes(SQLiteDatabase db) {

        db.execSQL(
            "CREATE INDEX index_novels_universe " +
            "ON novels(universe_id)"
        );

        db.execSQL(
            "CREATE INDEX index_name_usage_novel " +
            "ON name_usage(novel_id)"
        );

        db.execSQL(
            "CREATE INDEX index_characters_novel " +
            "ON characters(novel_id)"
        );

        db.execSQL(
            "CREATE INDEX index_character_fields_novel " +
            "ON character_fields(novel_id)"
        );

        db.execSQL(
            "CREATE INDEX index_relationships_novel " +
            "ON character_relationships(novel_id)"
        );

        db.execSQL(
            "CREATE INDEX index_events_novel_date " +
            "ON timeline_events(novel_id, date_sort_key)"
        );

        db.execSQL(
            "CREATE INDEX index_event_characters_character " +
            "ON event_characters(character_id)"
        );

        db.execSQL(
            "CREATE INDEX index_category_assignments_entity " +
            "ON category_assignments(entity_type, entity_id)"
        );

        db.execSQL(
            "CREATE INDEX index_categories_scope " +
            "ON categories(scope_type, scope_id)"
        );
    }

    @Override
    public void onUpgrade(
        SQLiteDatabase db,
        int oldVersion,
        int newVersion
    ) {

        db.beginTransaction();

        try {

            if (oldVersion < 2) {
                migrateToVersion2(db);
            }

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
        }
    }

    /**
     * Upgrades an existing version 1 database to version 2.
     */
    private void migrateToVersion2(SQLiteDatabase db) {

        // Version 1 databases do not have this column.
        db.execSQL(
            "ALTER TABLE novels ADD COLUMN cover_image_path TEXT"
        );

        // Create the same v2 tables used by fresh installations.
        createNovelStructureTables(db);
    }

    /**
     * Creates all version 2 novel-structure tables and indexes.
     *
     * Used by:
     * - onCreate() for fresh installations.
     * - migrateToVersion2() for existing version 1 installations.
     */
    private void createNovelStructureTables(SQLiteDatabase db) {

        // 15. Volumes
        db.execSQL(
            "CREATE TABLE volumes (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "novel_id INTEGER NOT NULL," +
            "title TEXT NOT NULL," +
            "volume_number INTEGER NOT NULL," +
            "description TEXT," +
            "created_at INTEGER NOT NULL," +
            "updated_at INTEGER NOT NULL," +
            "UNIQUE(novel_id, volume_number)," +
            "FOREIGN KEY(novel_id) REFERENCES novels(id) " +
            "ON DELETE CASCADE)"
        );

        // 16. Chapters
        db.execSQL(
            "CREATE TABLE chapters (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "volume_id INTEGER NOT NULL," +
            "title TEXT NOT NULL," +
            "chapter_number INTEGER NOT NULL," +
            "description TEXT," +
            "created_at INTEGER NOT NULL," +
            "updated_at INTEGER NOT NULL," +
            "UNIQUE(volume_id, chapter_number)," +
            "FOREIGN KEY(volume_id) REFERENCES volumes(id) " +
            "ON DELETE CASCADE)"
        );

        // 17. Independent chapter flowcharts
        db.execSQL(
            "CREATE TABLE chapter_flowcharts (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "chapter_id INTEGER NOT NULL," +
            "title TEXT NOT NULL," +
            "display_order INTEGER NOT NULL DEFAULT 0," +
            "created_at INTEGER NOT NULL," +
            "updated_at INTEGER NOT NULL," +
            "FOREIGN KEY(chapter_id) REFERENCES chapters(id) " +
            "ON DELETE CASCADE)"
        );

        // 18. Event nodes
        db.execSQL(
            "CREATE TABLE chapter_events (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "flowchart_id INTEGER NOT NULL," +
            "title TEXT NOT NULL," +
            "description TEXT," +
            "position_x REAL NOT NULL DEFAULT 0," +
            "position_y REAL NOT NULL DEFAULT 0," +
            "display_order INTEGER NOT NULL DEFAULT 0," +
            "created_at INTEGER NOT NULL," +
            "updated_at INTEGER NOT NULL," +
            "FOREIGN KEY(flowchart_id) REFERENCES chapter_flowcharts(id) " +
            "ON DELETE CASCADE)"
        );

        // 19. Directed connections between event nodes
        db.execSQL(
            "CREATE TABLE chapter_event_connections (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "flowchart_id INTEGER NOT NULL," +
            "from_event_id INTEGER NOT NULL," +
            "to_event_id INTEGER NOT NULL," +
            "label TEXT," +
            "created_at INTEGER NOT NULL," +
            "UNIQUE(flowchart_id, from_event_id, to_event_id)," +
            "CHECK(from_event_id != to_event_id)," +
            "FOREIGN KEY(flowchart_id) REFERENCES chapter_flowcharts(id) " +
            "ON DELETE CASCADE," +
            "FOREIGN KEY(from_event_id) REFERENCES chapter_events(id) " +
            "ON DELETE CASCADE," +
            "FOREIGN KEY(to_event_id) REFERENCES chapter_events(id) " +
            "ON DELETE CASCADE)"
        );

        // Version 2 indexes
        db.execSQL(
            "CREATE INDEX index_volumes_novel " +
            "ON volumes(novel_id, volume_number)"
        );

        db.execSQL(
            "CREATE INDEX index_chapters_volume " +
            "ON chapters(volume_id, chapter_number)"
        );

        db.execSQL(
            "CREATE INDEX index_flowcharts_chapter " +
            "ON chapter_flowcharts(chapter_id)"
        );

        db.execSQL(
            "CREATE INDEX index_chapter_events_flowchart " +
            "ON chapter_events(flowchart_id)"
        );

        db.execSQL(
            "CREATE INDEX index_event_connections_flowchart " +
            "ON chapter_event_connections(flowchart_id)"
        );
    }
}
