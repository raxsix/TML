package eu.raxsix.tml.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static eu.raxsix.tml.database.TmlContract.RoomEntry;
import static eu.raxsix.tml.database.TmlContract.UserEntry;

/**
 * Created by Ragnar on 2/27/2016.
 */
public class TmlDbHelper extends SQLiteOpenHelper {


    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;

    public static final String DATABASE_NAME = "tml.db";

    public static final String SQL_CREATE_ROOM_TABLE = "CREATE TABLE " + RoomEntry.TABLE_NAME + " (" +

            RoomEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            RoomEntry.COLUMN_NAME + " TEXT NULL, " +
            RoomEntry.COLUMN_COUNT + " INTEGER NULL) ";

    public static final String SQL_CREATE_USER_TABLE = "CREATE TABLE " + UserEntry.TABLE_NAME + " (" +

            UserEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            UserEntry.COLUMN_USER_NAME + " TEXT NULL, " +
            UserEntry.COLUMN_DISTANCE + " TEXT  NULL) ";
    //UserEntry.COLUMN_ROOM_KEY + " INTEGER NOT NULL, " +
    // Set up the location column as a foreign key to location table.
    //" FOREIGN KEY (" + UserEntry.COLUMN_ROOM_KEY + ") REFERENCES " +
    //RoomEntry.TABLE_NAME + " (" + RoomEntry._ID + "))";

    public TmlDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }



    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(SQL_CREATE_ROOM_TABLE);
        db.execSQL(SQL_CREATE_USER_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + RoomEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + UserEntry.TABLE_NAME);
        onCreate(db);

    }
}
