
package com.novelarchive.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class NovelStructureRepository {

    private final DatabaseHelper helper;
    private final SQLiteDatabase db;

    public NovelStructureRepository(Context context) {
        helper = new DatabaseHelper(context.getApplicationContext());
        db = helper.getWritableDatabase();
    }

    private long now() {
        return System.currentTimeMillis();
    }

    private void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be empty.");
        }
    }

    private void requireId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Invalid ID.");
        }
    }

    private boolean exists(String table, long id) {
        Cursor c = db.rawQuery(
            "SELECT 1 FROM " + table + " WHERE id=? LIMIT 1",
            new String[]{String.valueOf(id)}
        );

        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    private boolean belongsTo(
        String table,
        long id,
        String parentColumn,
        long parentId
    ) {
        Cursor c = db.rawQuery(
            "SELECT 1 FROM " + table +
            " WHERE id=? AND " + parentColumn + "=? LIMIT 1",
            new String[]{
                String.valueOf(id),
                String.valueOf(parentId)
            }
        );

        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    // ---------------------------------------------------------
    // MODELS
    // ---------------------------------------------------------

    public static class Volume {
        public long id;
        public long novelId;
        public String title;
        public int number;
        public String description;
        public long createdAt;
        public long updatedAt;
    }

    public static class Chapter {
        public long id;
        public long volumeId;
        public String title;
        public int number;
        public String description;
        public long createdAt;
        public long updatedAt;
    }

    public static class Flowchart {
        public long id;
        public long chapterId;
        public String title;
        public int order;
        public long createdAt;
        public long updatedAt;
    }

    public static class Event {
        public long id;
        public long flowchartId;
        public String title;
        public String description;
        public float x;
        public float y;
        public int order;
        public long createdAt;
        public long updatedAt;
    }

    public static class Connection {
        public long id;
        public long flowchartId;
        public long fromEventId;
        public long toEventId;
        public String label;
        public long createdAt;
    }

    // ---------------------------------------------------------
    // VOLUMES
    // ---------------------------------------------------------

    public long createVolume(
        long novelId,
        String title,
        int number,
        String description
    ) {
        requireId(novelId);
        requireText(title, "Volume title");

        if (!exists("novels", novelId)) {
            throw new IllegalArgumentException("Novel not found.");
        }

        if (number < 1) {
            throw new IllegalArgumentException(
                "Volume number must be at least 1."
            );
        }

        long time = now();

        ContentValues values = new ContentValues();
        values.put("novel_id", novelId);
        values.put("title", title.trim());
        values.put("volume_number", number);
        values.put("description", description);
        values.put("created_at", time);
        values.put("updated_at", time);

        return db.insertOrThrow("volumes", null, values);
    }

    public List<Volume> getVolumes(long novelId) {
        List<Volume> result = new ArrayList<>();

        Cursor c = db.query(
            "volumes",
            null,
            "novel_id=?",
            new String[]{String.valueOf(novelId)},
            null,
            null,
            "volume_number ASC"
        );

        try {
            while (c.moveToNext()) {
                Volume item = new Volume();
                item.id = c.getLong(c.getColumnIndexOrThrow("id"));
                item.novelId = c.getLong(c.getColumnIndexOrThrow("novel_id"));
                item.title = c.getString(c.getColumnIndexOrThrow("title"));
                item.number = c.getInt(c.getColumnIndexOrThrow("volume_number"));
                item.description = c.getString(c.getColumnIndexOrThrow("description"));
                item.createdAt = c.getLong(c.getColumnIndexOrThrow("created_at"));
                item.updatedAt = c.getLong(c.getColumnIndexOrThrow("updated_at"));
                result.add(item);
            }
        } finally {
            c.close();
        }

        return result;
    }

    public void updateVolume(
        long volumeId,
        String title,
        int number,
        String description
    ) {
        requireId(volumeId);
        requireText(title, "Volume title");

        if (number < 1) {
            throw new IllegalArgumentException("Invalid volume number.");
        }

        ContentValues values = new ContentValues();
        values.put("title", title.trim());
        values.put("volume_number", number);
        values.put("description", description);
        values.put("updated_at", now());

        int count = db.update(
            "volumes",
            values,
            "id=?",
            new String[]{String.valueOf(volumeId)}
        );

        if (count == 0) {
            throw new IllegalArgumentException("Volume not found.");
        }
    }

    public void deleteVolume(long volumeId) {
        requireId(volumeId);
        db.delete(
            "volumes",
            "id=?",
            new String[]{String.valueOf(volumeId)}
        );
    }

    // ---------------------------------------------------------
    // CHAPTERS
    // ---------------------------------------------------------

    public long createChapter(
        long volumeId,
        String title,
        int number,
        String description
    ) {
        requireId(volumeId);
        requireText(title, "Chapter title");

        if (!exists("volumes", volumeId)) {
            throw new IllegalArgumentException("Volume not found.");
        }

        if (number < 1) {
            throw new IllegalArgumentException(
                "Chapter number must be at least 1."
            );
        }

        long time = now();

        ContentValues values = new ContentValues();
        values.put("volume_id", volumeId);
        values.put("title", title.trim());
        values.put("chapter_number", number);
        values.put("description", description);
        values.put("created_at", time);
        values.put("updated_at", time);

        return db.insertOrThrow("chapters", null, values);
    }

    public List<Chapter> getChapters(long volumeId) {
        List<Chapter> result = new ArrayList<>();

        Cursor c = db.query(
            "chapters",
            null,
            "volume_id=?",
            new String[]{String.valueOf(volumeId)},
            null,
            null,
            "chapter_number ASC"
        );

        try {
            while (c.moveToNext()) {
                Chapter item = new Chapter();
                item.id = c.getLong(c.getColumnIndexOrThrow("id"));
                item.volumeId = c.getLong(c.getColumnIndexOrThrow("volume_id"));
                item.title = c.getString(c.getColumnIndexOrThrow("title"));
                item.number = c.getInt(c.getColumnIndexOrThrow("chapter_number"));
                item.description = c.getString(c.getColumnIndexOrThrow("description"));
                item.createdAt = c.getLong(c.getColumnIndexOrThrow("created_at"));
                item.updatedAt = c.getLong(c.getColumnIndexOrThrow("updated_at"));
                result.add(item);
            }
        } finally {
            c.close();
        }

        return result;
    }

    public void updateChapter(
        long chapterId,
        String title,
        int number,
        String description
    ) {
        requireId(chapterId);
        requireText(title, "Chapter title");

        if (number < 1) {
            throw new IllegalArgumentException("Invalid chapter number.");
        }

        ContentValues values = new ContentValues();
        values.put("title", title.trim());
        values.put("chapter_number", number);
        values.put("description", description);
        values.put("updated_at", now());

        int count = db.update(
            "chapters",
            values,
            "id=?",
            new String[]{String.valueOf(chapterId)}
        );

        if (count == 0) {
            throw new IllegalArgumentException("Chapter not found.");
        }
    }

    public void deleteChapter(long chapterId) {
        requireId(chapterId);
        db.delete(
            "chapters",
            "id=?",
            new String[]{String.valueOf(chapterId)}
        );
    }

    // ---------------------------------------------------------
    // FLOWCHARTS
    // ---------------------------------------------------------

    public long createFlowchart(long chapterId, String title) {
        requireId(chapterId);
        requireText(title, "Flowchart title");

        if (!exists("chapters", chapterId)) {
            throw new IllegalArgumentException("Chapter not found.");
        }

        ContentValues values = new ContentValues();
        values.put("chapter_id", chapterId);
        values.put("title", title.trim());
        values.put("display_order", getFlowcharts(chapterId).size());
        values.put("created_at", now());
        values.put("updated_at", now());

        return db.insertOrThrow("chapter_flowcharts", null, values);
    }

    public List<Flowchart> getFlowcharts(long chapterId) {
        List<Flowchart> result = new ArrayList<>();

        Cursor c = db.query(
            "chapter_flowcharts",
            null,
            "chapter_id=?",
            new String[]{String.valueOf(chapterId)},
            null,
            null,
            "display_order ASC, id ASC"
        );

        try {
            while (c.moveToNext()) {
                Flowchart item = new Flowchart();
                item.id = c.getLong(c.getColumnIndexOrThrow("id"));
                item.chapterId = c.getLong(c.getColumnIndexOrThrow("chapter_id"));
                item.title = c.getString(c.getColumnIndexOrThrow("title"));
                item.order = c.getInt(c.getColumnIndexOrThrow("display_order"));
                item.createdAt = c.getLong(c.getColumnIndexOrThrow("created_at"));
                item.updatedAt = c.getLong(c.getColumnIndexOrThrow("updated_at"));
                result.add(item);
            }
        } finally {
            c.close();
        }

        return result;
    }

    public void renameFlowchart(long flowchartId, String title) {
        requireId(flowchartId);
        requireText(title, "Flowchart title");

        ContentValues values = new ContentValues();
        values.put("title", title.trim());
        values.put("updated_at", now());

        int count = db.update(
            "chapter_flowcharts",
            values,
            "id=?",
            new String[]{String.valueOf(flowchartId)}
        );

        if (count == 0) {
            throw new IllegalArgumentException("Flowchart not found.");
        }
    }

    public void deleteFlowchart(long flowchartId) {
        requireId(flowchartId);
        db.delete(
            "chapter_flowcharts",
            "id=?",
            new String[]{String.valueOf(flowchartId)}
        );
    }

    // ---------------------------------------------------------
    // EVENT NODES
    // ---------------------------------------------------------

    public long createEvent(
        long flowchartId,
        String title,
        String description,
        float x,
        float y
    ) {
        requireId(flowchartId);
        requireText(title, "Event title");

        if (!exists("chapter_flowcharts", flowchartId)) {
            throw new IllegalArgumentException("Flowchart not found.");
        }

        ContentValues values = new ContentValues();
        values.put("flowchart_id", flowchartId);
        values.put("title", title.trim());
        values.put("description", description);
        values.put("position_x", x);
        values.put("position_y", y);
        values.put("display_order", getEvents(flowchartId).size());
        values.put("created_at", now());
        values.put("updated_at", now());

        return db.insertOrThrow("chapter_events", null, values);
    }

    public List<Event> getEvents(long flowchartId) {
        List<Event> result = new ArrayList<>();

        Cursor c = db.query(
            "chapter_events",
            null,
            "flowchart_id=?",
            new String[]{String.valueOf(flowchartId)},
            null,
            null,
            "display_order ASC, id ASC"
        );

        try {
            while (c.moveToNext()) {
                Event item = new Event();
                item.id = c.getLong(c.getColumnIndexOrThrow("id"));
                item.flowchartId = c.getLong(c.getColumnIndexOrThrow("flowchart_id"));
                item.title = c.getString(c.getColumnIndexOrThrow("title"));
                item.description = c.getString(c.getColumnIndexOrThrow("description"));
                item.x = c.getFloat(c.getColumnIndexOrThrow("position_x"));
                item.y = c.getFloat(c.getColumnIndexOrThrow("position_y"));
                item.order = c.getInt(c.getColumnIndexOrThrow("display_order"));
                item.createdAt = c.getLong(c.getColumnIndexOrThrow("created_at"));
                item.updatedAt = c.getLong(c.getColumnIndexOrThrow("updated_at"));
                result.add(item);
            }
        } finally {
            c.close();
        }

        return result;
    }

    public void updateEvent(
        long eventId,
        String title,
        String description
    ) {
        requireId(eventId);
        requireText(title, "Event title");

        ContentValues values = new ContentValues();
        values.put("title", title.trim());
        values.put("description", description);
        values.put("updated_at", now());

        int count = db.update(
            "chapter_events",
            values,
            "id=?",
            new String[]{String.valueOf(eventId)}
        );

        if (count == 0) {
            throw new IllegalArgumentException("Event not found.");
        }
    }

    public void moveEvent(long eventId, float x, float y) {
        requireId(eventId);

        ContentValues values = new ContentValues();
        values.put("position_x", x);
        values.put("position_y", y);
        values.put("updated_at", now());

        int count = db.update(
            "chapter_events",
            values,
            "id=?",
            new String[]{String.valueOf(eventId)}
        );

        if (count == 0) {
            throw new IllegalArgumentException("Event not found.");
        }
    }

    public void deleteEvent(long eventId) {
        requireId(eventId);
        db.delete(
            "chapter_events",
            "id=?",
            new String[]{String.valueOf(eventId)}
        );
    }

    // ---------------------------------------------------------
    // EVENT CONNECTIONS
    // ---------------------------------------------------------

    public long connectEvents(
        long flowchartId,
        long fromEventId,
        long toEventId,
        String label
    ) {
        requireId(flowchartId);
        requireId(fromEventId);
        requireId(toEventId);

        if (fromEventId == toEventId) {
            throw new IllegalArgumentException(
                "An event cannot connect to itself."
            );
        }

        if (!belongsTo(
            "chapter_events",
            fromEventId,
            "flowchart_id",
            flowchartId
        )) {
            throw new IllegalArgumentException(
                "Starting event does not belong to this flowchart."
            );
        }

        if (!belongsTo(
            "chapter_events",
            toEventId,
            "flowchart_id",
            flowchartId
        )) {
            throw new IllegalArgumentException(
                "Destination event does not belong to this flowchart."
            );
        }

        ContentValues values = new ContentValues();
        values.put("flowchart_id", flowchartId);
        values.put("from_event_id", fromEventId);
        values.put("to_event_id", toEventId);
        values.put("label", label);
        values.put("created_at", now());

        return db.insertOrThrow(
            "chapter_event_connections",
            null,
            values
        );
    }

    public List<Connection> getConnections(long flowchartId) {
        List<Connection> result = new ArrayList<>();

        Cursor c = db.query(
            "chapter_event_connections",
            null,
            "flowchart_id=?",
            new String[]{String.valueOf(flowchartId)},
            null,
            null,
            "id ASC"
        );

        try {
            while (c.moveToNext()) {
                Connection item = new Connection();
                item.id = c.getLong(c.getColumnIndexOrThrow("id"));
                item.flowchartId = c.getLong(c.getColumnIndexOrThrow("flowchart_id"));
                item.fromEventId = c.getLong(c.getColumnIndexOrThrow("from_event_id"));
                item.toEventId = c.getLong(c.getColumnIndexOrThrow("to_event_id"));
                item.label = c.getString(c.getColumnIndexOrThrow("label"));
                item.createdAt = c.getLong(c.getColumnIndexOrThrow("created_at"));
                result.add(item);
            }
        } finally {
            c.close();
        }

        return result;
    }

    public void updateConnectionLabel(long connectionId, String label) {
        requireId(connectionId);

        ContentValues values = new ContentValues();
        values.put("label", label);

        int count = db.update(
            "chapter_event_connections",
            values,
            "id=?",
            new String[]{String.valueOf(connectionId)}
        );

        if (count == 0) {
            throw new IllegalArgumentException("Connection not found.");
        }
    }

    public void deleteConnection(long connectionId) {
        requireId(connectionId);
        db.delete(
            "chapter_event_connections",
            "id=?",
            new String[]{String.valueOf(connectionId)}
        );
    }

    // ---------------------------------------------------------
    // MERGE FLOWCHARTS
    // ---------------------------------------------------------

    public void mergeFlowcharts(
        long destinationFlowchartId,
        long sourceFlowchartId
    ) {
        requireId(destinationFlowchartId);
        requireId(sourceFlowchartId);

        if (destinationFlowchartId == sourceFlowchartId) {
            throw new IllegalArgumentException(
                "Choose two different flowcharts."
            );
        }

        db.beginTransaction();

        try {
            Cursor c = db.rawQuery(
                "SELECT d.chapter_id, s.chapter_id " +
                "FROM chapter_flowcharts d, chapter_flowcharts s " +
                "WHERE d.id=? AND s.id=?",
                new String[]{
                    String.valueOf(destinationFlowchartId),
                    String.valueOf(sourceFlowchartId)
                }
            );

            boolean valid;

            try {
                valid = c.moveToFirst()
                    && c.getLong(0) == c.getLong(1);
            } finally {
                c.close();
            }

            if (!valid) {
                throw new IllegalArgumentException(
                    "Both flowcharts must belong to the same chapter."
                );
            }

            // Copy source connections into the destination.
            // INSERT OR IGNORE handles connections that already exist.
            db.execSQL(
                "INSERT OR IGNORE INTO chapter_event_connections " +
                "(flowchart_id, from_event_id, to_event_id, label, created_at) " +
                "SELECT ?, from_event_id, to_event_id, label, created_at " +
                "FROM chapter_event_connections WHERE flowchart_id=?",
                new Object[]{
                    destinationFlowchartId,
                    sourceFlowchartId
                }
            );

            // Remove old source connections.
            db.delete(
                "chapter_event_connections",
                "flowchart_id=?",
                new String[]{String.valueOf(sourceFlowchartId)}
            );

            // Move source nodes without changing their IDs or positions.
            ContentValues values = new ContentValues();
            values.put("flowchart_id", destinationFlowchartId);

            db.update(
                "chapter_events",
                values,
                "flowchart_id=?",
                new String[]{String.valueOf(sourceFlowchartId)}
            );

            // Remove the now-empty source flowchart.
            db.delete(
                "chapter_flowcharts",
                "id=?",
                new String[]{String.valueOf(sourceFlowchartId)}
            );

            ContentValues updated = new ContentValues();
            updated.put("updated_at", now());

            db.update(
                "chapter_flowcharts",
                updated,
                "id=?",
                new String[]{String.valueOf(destinationFlowchartId)}
            );

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
        }
    }

    public void close() {
        helper.close();
    }
}
