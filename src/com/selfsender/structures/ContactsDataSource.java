package com.selfsender.structures;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ContactsDataSource {

	SQLiteOpenHelper dbhelper;
	SQLiteDatabase database;

	private static final String[] allcolumns = {
		DictionaryOpenHelper.COLUMN_ID,
		DictionaryOpenHelper.COLUMN_CONTACT,
		DictionaryOpenHelper.COLUMN_PHONE_MAIL,
		DictionaryOpenHelper.COLUMN_MESSAGE,
		DictionaryOpenHelper.COLUMN_TYPE,
		DictionaryOpenHelper.COLUMN_DATE,
		DictionaryOpenHelper.COLUMN_TIME,
		DictionaryOpenHelper.COLUMN_LOCATION,
		DictionaryOpenHelper.COLUMN_ENTERING,
		DictionaryOpenHelper.COLUMN_APP};

	public ContactsDataSource(Context context) {		
		dbhelper = new DictionaryOpenHelper(context);	
	}

	public void open() {	
		database = dbhelper.getWritableDatabase();
	}	

	public void close() {	
		dbhelper.close();		
	}

	public Contact create(Contact contact) {
		ContentValues values = new ContentValues();
		values.put(DictionaryOpenHelper.COLUMN_CONTACT, contact.getContactToString());
		values.put(DictionaryOpenHelper.COLUMN_PHONE_MAIL, contact.getPhoneMailToString());
		values.put(DictionaryOpenHelper.COLUMN_MESSAGE, contact.getMessage());
		values.put(DictionaryOpenHelper.COLUMN_TYPE, contact.getType());
		values.put(DictionaryOpenHelper.COLUMN_DATE, contact.getDateToString());
		values.put(DictionaryOpenHelper.COLUMN_TIME, contact.getTime());
		values.put(DictionaryOpenHelper.COLUMN_LOCATION, contact.getLocationToString());
		values.put(DictionaryOpenHelper.COLUMN_ENTERING, contact.getEntering());
		values.put(DictionaryOpenHelper.COLUMN_APP, contact.getApp());
		long insertid = database.insert(DictionaryOpenHelper.CONTACTS_TABLE, null, values);
		contact.setId(insertid);
		return contact;	
	}

	public boolean removeContact(Contact contact) {
		String where = DictionaryOpenHelper.COLUMN_ID + "=" + contact.getId();
		int result = database.delete(DictionaryOpenHelper.CONTACTS_TABLE, where, null);
		return (result == 1);
	}

	public List<Contact> findAll() {		
		Cursor cursor = database.query(DictionaryOpenHelper.CONTACTS_TABLE, allcolumns, null, null, null, null, null);
		return cursorToList(cursor);
	}

	public List<Contact> findFiltered(String selection, String orderBy) {		
		Cursor cursor = database.query(DictionaryOpenHelper.CONTACTS_TABLE, allcolumns, selection, null, null, null, orderBy);
		return cursorToList(cursor);
	}

	public Contact findContactFromId(long contactId) {
		Cursor cursor = database.query(DictionaryOpenHelper.CONTACTS_TABLE, allcolumns, DictionaryOpenHelper.COLUMN_ID + "=?", new String[] { String.valueOf(contactId) }, null, null, null, null);
		if (cursor != null) {
			cursor.moveToFirst();
		}

		Contact contact = new Contact();
		contact.setId(cursor.getLong(cursor.getColumnIndex(DictionaryOpenHelper.COLUMN_ID)));
		contact.setContactFromString(cursor.getString(cursor.getColumnIndex(DictionaryOpenHelper.COLUMN_CONTACT)));
		contact.setPhoneMailFromString(cursor.getString(cursor.getColumnIndex(DictionaryOpenHelper.COLUMN_PHONE_MAIL)));
		contact.setMessage(cursor.getString(cursor.getColumnIndex(DictionaryOpenHelper.COLUMN_MESSAGE)));
		contact.setType(cursor.getInt(cursor.getColumnIndex(DictionaryOpenHelper.COLUMN_TYPE)));
		contact.setDateFromString(cursor.getString(cursor.getColumnIndex(DictionaryOpenHelper.COLUMN_DATE)), false);
		contact.setTime(cursor.getString(cursor.getColumnIndex(DictionaryOpenHelper.COLUMN_TIME)));
		contact.setLocationFromString(cursor.getString(cursor.getColumnIndex(DictionaryOpenHelper.COLUMN_LOCATION)));
		contact.setEntering(cursor.getInt(cursor.getColumnIndex(DictionaryOpenHelper.COLUMN_ENTERING))>0);
		contact.setApp(cursor.getString(cursor.getColumnIndex(DictionaryOpenHelper.COLUMN_APP)));

		return contact;
	}

	private List<Contact> cursorToList(Cursor cursor) {
		List<Contact> contacts = new ArrayList<Contact>();
		if(cursor.getCount() > 0) {
			while (cursor.moveToNext()) {
				Contact contact = new Contact();
				contact.setId(cursor.getLong(cursor.getColumnIndex(DictionaryOpenHelper.COLUMN_ID)));
				contact.setContactFromString(cursor.getString(cursor.getColumnIndex(DictionaryOpenHelper.COLUMN_CONTACT)));
				contact.setPhoneMailFromString(cursor.getString(cursor.getColumnIndex(DictionaryOpenHelper.COLUMN_PHONE_MAIL)));
				contact.setMessage(cursor.getString(cursor.getColumnIndex(DictionaryOpenHelper.COLUMN_MESSAGE)));
				contact.setType(cursor.getInt(cursor.getColumnIndex(DictionaryOpenHelper.COLUMN_TYPE)));
				contact.setDateFromString(cursor.getString(cursor.getColumnIndex(DictionaryOpenHelper.COLUMN_DATE)), false);
				contact.setTime(cursor.getString(cursor.getColumnIndex(DictionaryOpenHelper.COLUMN_TIME)));
				contact.setLocationFromString(cursor.getString(cursor.getColumnIndex(DictionaryOpenHelper.COLUMN_LOCATION)));
				contact.setEntering(cursor.getInt(cursor.getColumnIndex(DictionaryOpenHelper.COLUMN_ENTERING))>0);
				contact.setApp(cursor.getString(cursor.getColumnIndex(DictionaryOpenHelper.COLUMN_APP)));
				contacts.add(contact);
			}	
		}
		return contacts;
	}

	public int updateContact(Contact contact) {
		ContentValues values = new ContentValues();
		values.put(DictionaryOpenHelper.COLUMN_CONTACT, contact.getContactToString());
		values.put(DictionaryOpenHelper.COLUMN_PHONE_MAIL, contact.getPhoneMailToString());
		values.put(DictionaryOpenHelper.COLUMN_MESSAGE, contact.getMessage());
		values.put(DictionaryOpenHelper.COLUMN_TYPE, contact.getType());
		values.put(DictionaryOpenHelper.COLUMN_DATE, contact.getDateToString());
		values.put(DictionaryOpenHelper.COLUMN_TIME, contact.getTime());
		values.put(DictionaryOpenHelper.COLUMN_LOCATION, contact.getLocationToString());
		values.put(DictionaryOpenHelper.COLUMN_ENTERING, contact.getEntering());
		values.put(DictionaryOpenHelper.COLUMN_APP, contact.getApp());
		return database.update(DictionaryOpenHelper.CONTACTS_TABLE, values, DictionaryOpenHelper.COLUMN_ID + " = ?",
				new String[] { String.valueOf(contact.getId()) });
	}

	// never actually used
	public String toString() {
		List<Contact> contacts = this.findAll();
		String print = 	"|  ID  | CONTACT |      PHONE_MAIL      |        MESSAGE        | TYPE |         DATE         |   TIME   |        LOCATION       | ENTERING |    APP    |";
		print = print + "\n|-----------------------------------------------------------------------------------------------------------------------------------------------|";
		// Lunga 4
		String id = "    ";
		// Lunga 7
		String contact = "       ";
		// Lunga 20
		String phone_mail = "                    ";
		// Lunga 21
		String message = "                     ";
		// Lunga 4
		String type = "    ";
		// Lunga 20
		String date = "                    ";
		// Lunga 8
		String time = "        ";
		// Lunga 10
		String location = "                     ";
		// Lunga 8
		String entering = "        ";
		// Lunga 9
		String  app = "         ";

		for (int i = 0; i < contacts.size(); i++)
		{
			String temp = String.valueOf(contacts.get(i).getId());
			id = id.replaceFirst(id.substring(0, temp.length()), temp);

			temp = String.valueOf(contacts.get(i).getContact());
			contact = contact.replaceFirst(contact.substring(0, temp.length()), temp);

			temp = contacts.get(i).getPhoneMailToString();
			phone_mail = phone_mail.replaceFirst(phone_mail.substring(0, temp.length()), temp);

			temp = contacts.get(i).getMessage();
			message = message.replaceFirst(message.substring(0, temp.length()), temp);

			temp = String.valueOf(contacts.get(i).getType());
			type = type.replaceFirst(type.substring(0, temp.length()), temp);

			temp = contacts.get(i).getDefaultDateToString();
			date = date.replaceFirst(date.substring(0, temp.length()), temp);

			temp = contacts.get(i).getDefaultTime();
			time = time.replaceFirst(time.substring(0, temp.length()), temp);

			temp = contacts.get(i).getLocationToString();
			location = location.replaceFirst(location.substring(0, temp.length()), temp);

			temp = contacts.get(i).getEntering() ? "1" : "0";
			entering = entering.replaceFirst(entering.substring(0, temp.length()), temp);

			temp = contacts.get(i).getApp();
			app = app.replaceFirst(app.substring(0, temp.length()), temp);

			print = print + "\n| " + id + " | " + contact + " | " + phone_mail + " | " + message + " | " + type + " | " + date + " | " + time + " | " + location + " | " + entering + " | " + app + " |";//" | " + processed + " |";
		}
		return print;
	}

}
