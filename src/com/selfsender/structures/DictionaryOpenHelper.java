package com.selfsender.structures;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DictionaryOpenHelper extends SQLiteOpenHelper{

	private static final String DATABASE_NAME = "data.db";
	private static final int DATABASE_VERSION = 1;

	public static final String CONTACTS_TABLE = "Contacts";
	public static final String COLUMN_ID = "contactId";
	public static final String COLUMN_CONTACT = "contact";
	public static final String COLUMN_PHONE_MAIL = "phone_mail";
	public static final String COLUMN_MESSAGE = "message";
	public static final String COLUMN_TYPE = "type";
	public static final String COLUMN_DATE = "date";
	public static final String COLUMN_TIME = "time";
	public static final String COLUMN_LOCATION = "location";
	public static final String COLUMN_ENTERING = "entering";
	public static final String COLUMN_APP = "app";

	private static final String TABLE_CREATE = 
			"CREATE TABLE " + CONTACTS_TABLE + " (" + 
					COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
					COLUMN_CONTACT + " TEXT, " +
					COLUMN_PHONE_MAIL + " TEXT, " +
					COLUMN_MESSAGE + " TEXT, " +
					COLUMN_TYPE + " NUMERIC, " +
					COLUMN_DATE + " TEXT, " +
					COLUMN_TIME + " TEXT, " +
					COLUMN_LOCATION + " TEXT, " +
					COLUMN_ENTERING + " TEXT, " +
					COLUMN_APP + " TEXT " +
					")";	

	DictionaryOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int arg1, int arg2) {
		db.execSQL("DROP TABLE IF EXISTS " + CONTACTS_TABLE);
		onCreate(db);

	}

}
