package com.example.jaimemaps;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Base de datos de marcadores CIFRADA con SQLCipher (AES-256).
 * La contraseña la genera y protege {@link DbKeyManager} con el Android Keystore.
 * Todas las consultas siguen siendo parametrizadas.
 */
public class MarkerDbHelper extends SQLiteOpenHelper {

    /** Nombre del archivo cifrado. La base antigua (sin cifrar) se llamaba "jaimemaps.db". */
    static final String DB_NAME = "jaimemaps_enc.db";
    private static final String LEGACY_DB_NAME = "jaimemaps.db";
    private static final int DB_VERSION = 1;

    static {
        System.loadLibrary("sqlcipher"); // biblioteca nativa de SQLCipher
    }

    private static final String TABLE = "markers";
    private static final String COL_ID = "id";
    private static final String COL_TITLE = "title";
    private static final String COL_LAT = "lat";
    private static final String COL_LNG = "lng";

    public static class SavedMarker {
        public final long id;
        public final String title;
        public final double lat;
        public final double lng;

        SavedMarker(long id, String title, double lat, double lng) {
            this.id = id;
            this.title = title;
            this.lat = lat;
            this.lng = lng;
        }
    }

    public MarkerDbHelper(Context context) {
        super(context, DB_NAME, DbKeyManager.getOrCreatePassphrase(context),
                null, DB_VERSION, 0, null, null, false);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // no incluye ningún dato del usuario.
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_TITLE + " TEXT NOT NULL, "
                + COL_LAT + " REAL NOT NULL, "
                + COL_LNG + " REAL NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }


    public long insertMarker(String title, double lat, double lng) {
        // Defensa adicional: coordenadas válidas y título ya sanitizado
        if (title == null || Double.isNaN(lat) || Double.isNaN(lng)
                || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            return -1;
        }
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, title);
        values.put(COL_LAT, lat);
        values.put(COL_LNG, lng);
        return getWritableDatabase().insert(TABLE, null, values);
    }

    public List<SavedMarker> getAllMarkers() {
        List<SavedMarker> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(TABLE,
                new String[]{COL_ID, COL_TITLE, COL_LAT, COL_LNG},
                null, null, null, null, COL_ID + " ASC")) {
            while (c.moveToNext()) {
                result.add(new SavedMarker(c.getLong(0), c.getString(1),
                        c.getDouble(2), c.getDouble(3)));
            }
        }
        return result;
    }


    public List<SavedMarker> searchByTitle(String text) {
        List<SavedMarker> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(TABLE,
                new String[]{COL_ID, COL_TITLE, COL_LAT, COL_LNG},
                COL_TITLE + " LIKE ? ESCAPE '\\'", new String[]{"%" + escapeLike(text) + "%"},
                null, null, COL_ID + " ASC")) {
            while (c.moveToNext()) {
                result.add(new SavedMarker(c.getLong(0), c.getString(1),
                        c.getDouble(2), c.getDouble(3)));
            }
        }
        return result;
    }

    /** Escapa los comodines de LIKE para que el texto se busque de forma literal. */
    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /**
     * Migra los marcadores de la base antigua SIN cifrar (si existe) a la base cifrada
     * y elimina el archivo antiguo. Es seguro llamarlo en cada inicio.
     */
    public void migrateLegacyDatabase(Context context) {
        java.io.File legacy = context.getDatabasePath(LEGACY_DB_NAME);
        if (!legacy.exists()) return;
        try (android.database.sqlite.SQLiteDatabase old =
                     android.database.sqlite.SQLiteDatabase.openDatabase(
                             legacy.getPath(), null,
                             android.database.sqlite.SQLiteDatabase.OPEN_READONLY);
             Cursor c = old.query(TABLE, new String[]{COL_TITLE, COL_LAT, COL_LNG},
                     null, null, null, null, COL_ID + " ASC")) {
            while (c.moveToNext()) {
                String title = InputValidator.sanitizeTitle(c.getString(0));
                if (title != null) insertMarker(title, c.getDouble(1), c.getDouble(2));
            }
        } catch (RuntimeException ignored) {
            // base antigua ilegible: se descarta igualmente por seguridad
        }
        context.deleteDatabase(LEGACY_DB_NAME); // elimina también -journal/-wal
    }

    public int deleteAllMarkers() {
        return getWritableDatabase().delete(TABLE, null, null);
    }
}
