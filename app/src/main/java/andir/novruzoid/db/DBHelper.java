package andir.novruzoid.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import andir.novruzoid.db.contracts.OrganizationContract;

public class DBHelper extends SQLiteOpenHelper {
    private static DBHelper sInstance;
    private static final String DB_NAME = "Novruzoid";
    private static final int DB_VERSION = 3;

    private static final String SQL_CREATE_ORG_ENTRIES =
            "CREATE TABLE " + OrganizationContract.OrgTable.TABLE_NAME + " (" +
                    OrganizationContract.OrgTable.ORG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    OrganizationContract.OrgTable.ORG_NAME + " TEXT, " +
                    OrganizationContract.OrgTable.ENTRY_DATE + " TEXT, " +
                    OrganizationContract.OrgTable.ORG_ADDR + " TEXT, " +
                    OrganizationContract.OrgTable.X_COORD + " TEXT, " +
                    OrganizationContract.OrgTable.Y_COORD + " TEXT " +
            " )";

    private static final String SQL_CREATE_ITEMS_ENTRIES =
            "CREATE TABLE " + OrganizationContract.ItemsTable.TABLE_NAME + " (" +
                    OrganizationContract.ItemsTable.ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    OrganizationContract.ItemsTable.OBJ_ID + " INTEGER NOT NULL, " +
                    OrganizationContract.ItemsTable.ITEM_TEXT + " TEXT, " +
                    OrganizationContract.ItemsTable.ITEM_ADDITIONAL + " TEXT, " +
                    OrganizationContract.ItemsTable.ENTRY_DATE + " TEXT " +
                    " )";

    private static final String SQL_CREATE_REC_TOTAL =
            "CREATE TABLE " + OrganizationContract.ReceiptTotalTable.TABLE_NAME + " (" +
                    OrganizationContract.ReceiptTotalTable.RT_PK + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    OrganizationContract.ReceiptTotalTable.ISSUE_DATE + " TEXT, " +
                    OrganizationContract.ReceiptTotalTable.RECOG_DATE + " TEXT, " +
                    OrganizationContract.ReceiptTotalTable.OBJ_ID + " REAL, " +
                    OrganizationContract.ReceiptTotalTable.TOTAL_AMOUNT + " REAL " +
                    " )";

    private static final String SQL_CREATE_REC_ENTRIES =
            "CREATE TABLE " + OrganizationContract.ReceiptsTable.TABLE_NAME + " (" +
                    OrganizationContract.ReceiptsTable.PK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    OrganizationContract.ReceiptsTable.RT_ID + " INTEGER NOT NULL, " +
                    OrganizationContract.ReceiptsTable.RECORD_ID + " INTEGER NOT NULL, " +
                    OrganizationContract.ReceiptsTable.ITEM_ID + " INTEGER, " +
                    OrganizationContract.ReceiptsTable.QUAN + " REAL, " +
                    OrganizationContract.ReceiptsTable.PRICE + " REAL, " +
                    OrganizationContract.ReceiptsTable.TOTAL + " REAL " +
                    " )";

    private static final String SQL_DELETE_ORG_ENTRIES =
            "DROP TABLE IF EXISTS " + OrganizationContract.OrgTable.TABLE_NAME;
    private static final String SQL_DELETE_ITEMS_ENTRIES =
            "DROP TABLE IF EXISTS " + OrganizationContract.ItemsTable.TABLE_NAME;
    private static final String SQL_DELETE_REC_TOTAL =
            "DROP TABLE IF EXISTS " + OrganizationContract.ReceiptTotalTable.TABLE_NAME;
    private static final String SQL_DELETE_REC_ENTRIES =
            "DROP TABLE IF EXISTS " + OrganizationContract.ReceiptsTable.TABLE_NAME;

    /**
     * Constructor should be private to prevent direct instantiation.
     * make call to static method "getInstance()" instead.
     */
    private DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public static synchronized DBHelper getInstance(Context context) {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new DBHelper(context.getApplicationContext());
        }
        return sInstance;
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ORG_ENTRIES);
        db.execSQL(SQL_CREATE_ITEMS_ENTRIES);
        db.execSQL(SQL_CREATE_REC_TOTAL);
        db.execSQL(SQL_CREATE_REC_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ORG_ENTRIES);
        db.execSQL(SQL_DELETE_ITEMS_ENTRIES);
        db.execSQL(SQL_DELETE_REC_TOTAL);
        db.execSQL(SQL_DELETE_REC_ENTRIES);
        onCreate(db);
    }
}
