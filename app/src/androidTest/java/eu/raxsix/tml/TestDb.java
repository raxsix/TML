package eu.raxsix.tml;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import android.util.Log;

import java.util.HashSet;

import eu.raxsix.tml.database.TmlContract;
import eu.raxsix.tml.database.TmlDbHelper;

/**
 * Created by Ragnar on 2/27/2016.
 */
public class TestDb extends AndroidTestCase {


    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testCreateDb() throws Throwable {

        final HashSet<String> tableNameHashSet = new HashSet<>();
        tableNameHashSet.add(TmlContract.RoomEntry.TABLE_NAME);
        tableNameHashSet.add(TmlContract.UserEntry.TABLE_NAME);


        mContext.deleteDatabase(TmlDbHelper.DATABASE_NAME);
        SQLiteDatabase db = new TmlDbHelper(
                this.mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());

        // have we created the tables we want?
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);

        assertTrue("Error: This means that the database has not been created correctly",
                c.moveToFirst());


        // verify that the tables have been created
        do {
            tableNameHashSet.remove(c.getString(0));
        } while (c.moveToNext());

        // if this fails, it means that your database doesn't contain both the location entry
        // and weather entry tables
        assertTrue("Error: Your database was created without tables",
                tableNameHashSet.isEmpty());

        // now, do our tables contain the correct columns?
        c = db.rawQuery("PRAGMA table_info(" + TmlContract.RoomEntry.TABLE_NAME + ")",
                null);

        assertTrue("Error: This means that we were unable to query the database for table information.",
                c.moveToFirst());


        // Build a HashSet of all of the column names we want to look for
        final HashSet<String> roomColumnHashSet = new HashSet<>();
        roomColumnHashSet.add(TmlContract.RoomEntry._ID);
        roomColumnHashSet.add(TmlContract.RoomEntry.COLUMN_NAME);
        roomColumnHashSet.add(TmlContract.RoomEntry.COLUMN_COUNT);

        int columnNameIndex = c.getColumnIndex("name");
        do {
            String columnName = c.getString(columnNameIndex);
            Log.d("DB", columnName);
            roomColumnHashSet.remove(columnName);
        } while (c.moveToNext());
        Log.d("DB", "------------------------------------------------------------------------");
        // if this fails, it means that your database doesn't contain all of the required room
        // entry columns
        assertTrue("Error: The database doesn't contain all of the required room entry columns",
                roomColumnHashSet.isEmpty());


        // now, do our tables contain the correct columns?
        c = db.rawQuery("PRAGMA table_info(" + TmlContract.UserEntry.TABLE_NAME + ")",
                null);

        assertTrue("Error: This means that we were unable to query the database for table information.",
                c.moveToFirst());

        // Build a HashSet of all of the column names we want to look for
        final HashSet<String> userColumnHashSet = new HashSet<>();
        userColumnHashSet.add(TmlContract.UserEntry._ID);
        //userColumnHashSet.add(TmlContract.UserEntry.COLUMN_ROOM_KEY);
        userColumnHashSet.add(TmlContract.UserEntry.COLUMN_USER_NAME);
        userColumnHashSet.add(TmlContract.UserEntry.COLUMN_DISTANCE);

        columnNameIndex = c.getColumnIndex("name");
        do {
            String columnName = c.getString(columnNameIndex);
            Log.d("DB", columnName);
            userColumnHashSet.remove(columnName);
        } while (c.moveToNext());
        Log.d("DB", "------------------------------------------------------------------------");
        // if this fails, it means that your database doesn't contain all of the required user
        // entry columns
        assertTrue("Error: The database doesn't contain all of the required user entry columns",
                userColumnHashSet.isEmpty());

        db.close();
    }

}
