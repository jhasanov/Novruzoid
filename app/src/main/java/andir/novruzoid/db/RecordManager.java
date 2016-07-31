package andir.novruzoid.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import andir.novruzoid.db.contracts.OrganizationContract;

/**
 * Created by itjamal on 5/22/2016.
 */
public class RecordManager {

    /*
    To browse SQLITE3 DB, connect through ADB:
    (http://stackoverflow.com/questions/18370219/how-to-use-adb-in-android-studio-to-view-an-sqlite-db)
    sdk\platform-tools>adb devices
    adb -s emulator-xxxx shell
    cd data/data/<your-package-name>/databases/
    sqlite3 <your-db-name>
    .tables
    */

    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";

    public static String now() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(cal.getTime());
    }

    public static long addOrganization(Context context, ContentValues newOrg) {
        long newRowId = 0;

        SQLiteDatabase db = DBHelper.getInstance(context).getWritableDatabase();
        newRowId = db.insert(OrganizationContract.OrgTable.TABLE_NAME,null,newOrg);
        return newRowId;
    }

    public static String addRecord(Context context,String jsonData) {
        JSONObject json = null;
        String statusResult = "OK";
        try {
            json = new JSONObject(jsonData);
            String objName = json.getString("obj_name");
            String objAddr = json.getString("address");
            String xCoord = json.getString("x_coord");
            String yCoord = json.getString("y_coord");
            String issueDate = json.getString("issue_date");
            String recogDate = json.getString("recog_date");
            String grandTotal = json.getString("grand_total");

            Log.e("Novruzoid", "OBJECT NAME: " + objName);
            // Check if Object doesn't exist, add it
            long objId = getObjectId(context,objName);
            if ( objId == -1) {
                Log.e("Novruzoid", "No Object found. Adding...");
                ContentValues values = new ContentValues();
                values.put(OrganizationContract.OrgTable.ORG_NAME,objName);
                values.put(OrganizationContract.OrgTable.ORG_ADDR,objAddr);
                values.put(OrganizationContract.OrgTable.ENTRY_DATE,now());
                values.put(OrganizationContract.OrgTable.X_COORD,xCoord);
                values.put(OrganizationContract.OrgTable.Y_COORD,yCoord);
                objId = createObject(context, values);
            }

            // Add receipt
            ContentValues values = new ContentValues();
            values.put(OrganizationContract.ReceiptTotalTable.OBJ_ID,objId);
            values.put(OrganizationContract.ReceiptTotalTable.ISSUE_DATE,issueDate);
            values.put(OrganizationContract.ReceiptTotalTable.RECOG_DATE,now());
            values.put(OrganizationContract.ReceiptTotalTable.TOTAL_AMOUNT,grandTotal);
            long recTotalId = createReceipt(context,values);

            // Insert records
            JSONArray jsonArr = json.getJSONArray("records");
            for (int i =0; i<jsonArr.length(); i++ ) {
                String recId = jsonArr.getJSONObject(i).getString("rec_id");
                String recName = jsonArr.getJSONObject(i).getString("rec_name");
                String recType = jsonArr.getJSONObject(i).getString("rec_type");
                Double quan = jsonArr.getJSONObject(i).getDouble("quan");
                Double price = jsonArr.getJSONObject(i).getDouble("price");
                Double total = jsonArr.getJSONObject(i).getDouble("total");
                Log.e("Novruzoid", "REC NAME: " + recName+"; total: "+total);

                long itemId = getItemId(context,recName,recType);
                if (itemId == -1) {
                    values = new ContentValues();
                    values.put(OrganizationContract.ItemsTable.ITEM_TEXT,recName);
                    values.put(OrganizationContract.ItemsTable.ITEM_ADDITIONAL,recType);
                    values.put(OrganizationContract.ItemsTable.ENTRY_DATE,now());
                    values.put(OrganizationContract.ItemsTable.OBJ_ID,objId);
                    itemId = createItem(context,values);
                }

                values = new ContentValues();
                values.put(OrganizationContract.ReceiptsTable.RT_ID,recTotalId);
                values.put(OrganizationContract.ReceiptsTable.RECORD_ID,recId);
                values.put(OrganizationContract.ReceiptsTable.ITEM_ID,itemId);
                values.put(OrganizationContract.ReceiptsTable.QUAN,quan);
                values.put(OrganizationContract.ReceiptsTable.PRICE,price);
                values.put(OrganizationContract.ReceiptsTable.TOTAL,total);

                addReceiptRecord(context,values);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            statusResult = "ERROR : "+e.getMessage();
        } finally {
            return statusResult;
        }
    }

    // suggest low priced offers for the given radius
    public static void getSuggested(Context context,long radiusMeters, HashMap hmItems ) {

    }


    // Get list of all items.
    public static void getItemList() {

    }

    // Get ID of the item.
    private static long getItemId(Context context, String recName, String recType) {
        long itemId = -1;

        SQLiteDatabase db = DBHelper.getInstance(context).getReadableDatabase();
        String [] cols = {OrganizationContract.ItemsTable.ITEM_ID};
        String selection = OrganizationContract.ItemsTable.ITEM_TEXT + " = ? and "+OrganizationContract.ItemsTable.ITEM_ADDITIONAL + " = ?";
        String [] selectionArgs = { recName, recType };
        Cursor c  = db.query(OrganizationContract.ItemsTable.TABLE_NAME,cols,selection,selectionArgs,null,null,null);

        if (c.getCount() == 0) {
            itemId = -1;
        }
        else {
            c.moveToFirst();
            itemId = c.getInt(0);
        }

        c.close();
        return itemId;
    }

    // Create a new item
    private static long createItem(Context context, ContentValues values) {
        long itemId = -1;

        SQLiteDatabase db = DBHelper.getInstance(context).getWritableDatabase();
        itemId = db.insert(OrganizationContract.ItemsTable.TABLE_NAME,null,values);

        return itemId;
    }


    // add new receipt
    private static long createReceipt(Context context, ContentValues values) {
        long receiptId = -1;

        SQLiteDatabase db = DBHelper.getInstance(context).getWritableDatabase();
        receiptId = db.insert(OrganizationContract.ReceiptTotalTable.TABLE_NAME,null,values);

        return receiptId;
    }

    // Insert new record to the receipt data.
    private static void addReceiptRecord(Context context, ContentValues values) {
        SQLiteDatabase db = DBHelper.getInstance(context).getWritableDatabase();
        db.insert(OrganizationContract.ReceiptsTable.TABLE_NAME,null,values);
    }

    // Add new object to the DB
    private static long createObject(Context context,ContentValues values) {
        long objId = -1;

        SQLiteDatabase db = DBHelper.getInstance(context).getWritableDatabase();
        objId = db.insert(OrganizationContract.OrgTable.TABLE_NAME,null,values);

        return objId;
    }

    // get ID of the given Object
    private static long getObjectId(Context context,String objName) {
        long objId = -1;

        SQLiteDatabase db = DBHelper.getInstance(context).getReadableDatabase();
        String [] cols = {OrganizationContract.OrgTable.ORG_ID};
        String selection = OrganizationContract.OrgTable.ORG_NAME + " = ?";
        String [] selectionArgs = { objName };
        Cursor c  = db.query(OrganizationContract.OrgTable.TABLE_NAME,cols,selection,selectionArgs,null,null,null);

        if (c.getCount() == 0) {
            objId = -1;
        }
        else {
            c.moveToFirst();
            objId = c.getInt(0);
        }

        c.close();
        return objId;
    }


}
