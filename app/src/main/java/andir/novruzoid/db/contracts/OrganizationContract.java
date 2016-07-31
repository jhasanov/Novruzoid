/**
 * Created by itjamal on 5/22/2016.
 */

package andir.novruzoid.db.contracts;

import android.provider.BaseColumns;

public final class OrganizationContract {

    public OrganizationContract() {}

    public static abstract class OrgTable implements BaseColumns {
        public static final String TABLE_NAME = "organizations";
        public static final String ORG_ID = "_id";
        public static final String ORG_NAME = "org_name";
        public static final String ENTRY_DATE = "entry_date";
        public static final String ORG_ADDR = "address";
        public static final String X_COORD = "x_coord";
        public static final String Y_COORD = "y_coord";
    }

    public static abstract class ItemsTable implements BaseColumns {
        public static final String TABLE_NAME = "items";
        public static final String ITEM_ID = "item_id";
        public static final String OBJ_ID = "obj_id";
        public static final String ITEM_TEXT = "item_text";
        public static final String ITEM_ADDITIONAL = "item_additional";
        public static final String ENTRY_DATE = "entry_date";
    }

    public static abstract class ReceiptTotalTable implements BaseColumns {
        public static final String TABLE_NAME = "receipts_total";
        public static final String RT_PK = "rt_pk";
        public static final String ISSUE_DATE = "issue_date";
        public static final String RECOG_DATE = "recog_date";
        public static final String OBJ_ID = "obj_id";
        public static final String TOTAL_AMOUNT = "total_amount";
    }

    public static abstract class ReceiptsTable implements BaseColumns {
        public static final String TABLE_NAME = "receipts";
        public static final String PK_ID = "pk_id";
        public static final String RT_ID = "rt_id";
        public static final String RECORD_ID = "record_id";
        public static final String ITEM_ID = "item_id";
        public static final String QUAN = "quan";
        public static final String PRICE = "price";
        public static final String TOTAL = "total";
    }

}
