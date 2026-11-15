
package com.novelarchive.app.database;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class NovelCoverRepository {

    private final Context context;
    private final DatabaseHelper helper;
    private final SQLiteDatabase db;

    public NovelCoverRepository(Context context) {
        this.context = context.getApplicationContext();
        helper = new DatabaseHelper(this.context);
        db = helper.getWritableDatabase();
    }

    /**
     * Copies a selected image into private app storage
     * and saves its path in the novel record.
     */
    public String saveCover(long novelId, Uri imageUri) throws Exception {

        if (novelId <= 0 || imageUri == null) {
            throw new IllegalArgumentException("Invalid novel or image.");
        }

        if (!novelExists(novelId)) {
            throw new IllegalArgumentException("Novel not found.");
        }

        File directory = new File(
            context.getFilesDir(),
            "novel_covers"
        );

        if (!directory.exists() && !directory.mkdirs()) {
            throw new Exception("Could not create cover directory.");
        }

        String extension = getImageExtension(imageUri);

        String filename = "novel_" + novelId + "_" +
            System.currentTimeMillis() + "." + extension;

        File temporaryFile = new File(directory, filename + ".tmp");
        File finalFile = new File(directory, filename);

        ContentResolver resolver = context.getContentResolver();

        try (
            InputStream input = resolver.openInputStream(imageUri);
            OutputStream output = new FileOutputStream(temporaryFile)
        ) {
            if (input == null) {
                throw new Exception("Could not open selected image.");
            }

            byte[] buffer = new byte[8192];
            int count;

            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }

            output.flush();

        } catch (Exception e) {
            temporaryFile.delete();
            throw e;
        }

        if (!temporaryFile.renameTo(finalFile)) {
            temporaryFile.delete();
            throw new Exception("Could not save cover image.");
        }

        String oldPath = getCoverPath(novelId);

        ContentValues values = new ContentValues();
        values.put("cover_image_path", finalFile.getAbsolutePath());

        int updated = db.update(
            "novels",
            values,
            "id=?",
            new String[]{String.valueOf(novelId)}
        );

        if (updated == 0) {
            finalFile.delete();
            throw new Exception("Could not update novel cover.");
        }

        deleteOldFile(oldPath, finalFile.getAbsolutePath());

        return finalFile.getAbsolutePath();
    }

    /**
     * Returns the saved cover path, or null if no cover is selected.
     */
    public String getCoverPath(long novelId) {

        Cursor cursor = db.query(
            "novels",
            new String[]{"cover_image_path"},
            "id=?",
            new String[]{String.valueOf(novelId)},
            null,
            null,
            null
        );

        try {
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } finally {
            cursor.close();
        }

        return null;
    }

    /**
     * Removes the selected cover from a novel.
     */
    public void removeCover(long novelId) {

        String oldPath = getCoverPath(novelId);

        ContentValues values = new ContentValues();
        values.putNull("cover_image_path");

        db.update(
            "novels",
            values,
            "id=?",
            new String[]{String.valueOf(novelId)}
        );

        deleteOldFile(oldPath, null);
    }

    private boolean novelExists(long novelId) {

        Cursor cursor = db.rawQuery(
            "SELECT 1 FROM novels WHERE id=? LIMIT 1",
            new String[]{String.valueOf(novelId)}
        );

        try {
            return cursor.moveToFirst();
        } finally {
            cursor.close();
        }
    }

    private String getImageExtension(Uri uri) {

        String mimeType = context.getContentResolver().getType(uri);

        if (mimeType != null) {
            String extension = MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(mimeType);

            if (extension != null) {
                return extension.toLowerCase();
            }
        }

        return "img";
    }

    private void deleteOldFile(String oldPath, String keepPath) {

        if (oldPath == null || oldPath.equals(keepPath)) {
            return;
        }

        File oldFile = new File(oldPath);

        // Only delete files inside this app's cover directory.
        File coverDirectory = new File(
            context.getFilesDir(),
            "novel_covers"
        );

        try {
            String directoryPath = coverDirectory.getCanonicalPath()
                + File.separator;

            String oldCanonicalPath = oldFile.getCanonicalPath();

            if (oldCanonicalPath.startsWith(directoryPath)) {
                oldFile.delete();
            }

        } catch (Exception ignored) {
            // A failed cleanup must not interrupt normal app operation.
        }
    }

    public void close() {
        helper.close();
    }
}
