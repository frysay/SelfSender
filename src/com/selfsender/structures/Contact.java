package com.selfsender.structures;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.widget.Toast;

public class Contact implements Parcelable {

	private long id; // Unique id of the object in the database
	private ArrayList<Long> contact; // list of ids of the contacts in the phone (used for getName, getNumber and getPhoto to always get fresh data)
	private ArrayList<String> phone_mail; // list of the numbers/email addresses at whom we are gonna send the sms/email (same size of ArrayList<Long>contact)
	private String message; // the message we want to send
	private int type; // Value 1: Gps/Date - 2: Date - 3: Gps
	private GregorianCalendar date; // the day and time this should happen or the gps listener should be turned on (based on type), to save the battery 
	private String time; // the time
	private Location location; // where the proximity alert should be triggered
	private boolean entering; // TRUE = entering, FALSE = exiting NOTE: right now we consider just the entering (that's why everywhere we set entering to true)
	private String app; // the app we are gonna use to send the message (just sms or email for the moment)

	public static final GregorianCalendar DEFAULT_DATE = new GregorianCalendar(1970, 0, 1, 00, 00); // default date used to identify Contacts that don't need it (like the type 3 gps)

	public Contact() {
		contact = new ArrayList<Long>();
		phone_mail = new ArrayList<String>();
		location = new Location("");
	}

	@SuppressWarnings("unchecked")
	public Contact(Parcel in) {
		id = in.readLong();
		contact = new ArrayList<Long>();
		contact = in.readArrayList(Long.class.getClassLoader());
		phone_mail = new ArrayList<String>();
		phone_mail = in.readArrayList(String.class.getClassLoader());
		message = in.readString();
		type = in.readInt();
		int year = in.readInt();
		int month = in.readInt();
		int day = in.readInt();
		int hour = in.readInt();
		int minute = in.readInt();
		date = new GregorianCalendar(year, month, day, hour, minute);
		time = in.readString();
		location = in.readParcelable(Location.class.getClassLoader());
		entering = in.readByte() == 1;
		app = in.readString();
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public ArrayList<Long> getContact() {
		return contact;
	}

	//the value contact is stored as string like "contactId1%contactId2%..." for methods like toString()
	public String getContactToString() {
		String tmp = "";
		for(int i = 0; i < contact.size(); i++) {
			tmp = tmp + String.valueOf(contact.get(i)) + "%";
		}
		return tmp;
	}

	public void setContact(ArrayList<Long> contact) {
		this.contact = contact;
	}

	public void setContactFromString(String contacts) {
		int tmp = 0;
		for(int i = 0; i < contacts.length(); i++) {
			if (contacts.charAt(i) == '%') {
				contact.add(Long.valueOf(contacts.substring(tmp, i)));
				tmp = i+1;
			}
		}	
	}

	public ArrayList<String> getPhoneMail() {
		return phone_mail;
	}

	//the value phone_mail is stored as string like "phone_mail1%phone_mail2%..." for methods like toString()
	public String getPhoneMailToString() {
		String tmp = "";
		for(int i = 0; i < phone_mail.size(); i++) {
			tmp = tmp + phone_mail.get(i) + "%";
		}
		return tmp;
	}

	public void setPhoneMail(ArrayList<String> phone_mail) {
		this.phone_mail = phone_mail;
	}

	public void setPhoneMailFromString(String addresses) {
		int tmp = 0;
		for(int i = 0; i < addresses.length(); i++) {
			if (addresses.charAt(i) == '%') {
				phone_mail.add(addresses.substring(tmp, i));
				tmp = i+1;
			}
		}		
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	//in order to maintain uniformity among contact objects we store the subject and the text of the mail as a single value ("subject%text")
	public String getMailSubject() {
		int controllMark = message.indexOf("%");
		return message.substring(0, controllMark);
	}

	public String getMailText() {
		int controllMark = message.indexOf("%");
		return message.substring(controllMark+1, message.length());
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public GregorianCalendar getDate() {
		return this.date;
	}

	// we need to normalize the values cause otherwise everything will be considered as a number and instead of having like 03 we will get just 3
	public String getDateToString() {
		return normalize(date.get(GregorianCalendar.DAY_OF_MONTH)) + "/" + normalize(date.get(GregorianCalendar.MONTH)) + "/" + date.get(GregorianCalendar.YEAR) + " - " + normalize(date.get(GregorianCalendar.HOUR_OF_DAY)) + ":" + normalize(date.get(GregorianCalendar.MINUTE));
	}

	public String getDefaultDateToString() {
		if(!date.equals(DEFAULT_DATE)) {
			return normalize(date.get(GregorianCalendar.DAY_OF_MONTH)) + "/" + normalize(date.get(GregorianCalendar.MONTH)+1) + "/" + date.get(GregorianCalendar.YEAR) + " - " + normalize(date.get(GregorianCalendar.HOUR_OF_DAY)) + ":" + normalize(date.get(GregorianCalendar.MINUTE));
		}else {
			return "                 ";
		}
	}

	private String normalize(int notnorm) {
		int length = String.valueOf(notnorm).length()+1;
		return ("0" + notnorm).substring(length-2);
	}

	public void setDate(GregorianCalendar date) {
		this.date = date;
	}

	// this method gives back the date considering the month starting from 0 as in GregorianCalendar or from 1 depending on the value of bln
	public void setDateFromString(String date, boolean bln) {
		int length = date.length();
		if(bln) {
			this.date = new GregorianCalendar(Integer.valueOf(date.substring(length-12, length-8)), Integer.valueOf(date.substring(length-15, length-13))-1, Integer.valueOf(date.substring(length-18, length-16)), Integer.valueOf(date.substring(length-5, length-3)), Integer.valueOf(date.substring(length-2, length)));	
		}else {
			this.date = new GregorianCalendar(Integer.valueOf(date.substring(length-12, length-8)), Integer.valueOf(date.substring(length-15, length-13)), Integer.valueOf(date.substring(length-18, length-16)), Integer.valueOf(date.substring(length-5, length-3)), Integer.valueOf(date.substring(length-2, length)));	
		}
	}

	private int getYear(){
		return this.date.get(GregorianCalendar.YEAR);
	}

	private int getMonth(){
		return this.date.get(GregorianCalendar.MONTH);
	}

	private int getDay(){
		return this.date.get(GregorianCalendar.DAY_OF_MONTH);
	}

	private int getHour(){
		return this.date.get(GregorianCalendar.HOUR_OF_DAY);
	}

	private int getMinute(){
		return this.date.get(GregorianCalendar.MINUTE);
	}

	public String getTime() {
		return time;
	}

	// if the date of this object is equal to DEFAULT_DATE it means the type is equal to 3 and we give back and empty string
	// the length of the string is just adapted for the method toString in ContactDataSource
	// this could have been done just considering the type without the need for DEFAULT_DATE, but it seemed to be the best call
	// considering further modification and implementation of other options
	public String getDefaultTime() {
		if(!date.equals(DEFAULT_DATE)) {
			return time;
		}else {
			return "     ";
		}

	}

	public void setTime(String time) {
		this.time = time;
	}

	public Location getLocation() {
		return location;
	}

	public String getLocationToString() {
		return location.getLatitude() + "-" + location.getLongitude();
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public void setLocationFromString(String loc) {
		int controllMark = loc.indexOf("-");
		this.location.setLatitude(Double.parseDouble(loc.substring(0, controllMark)));
		this.location.setLongitude(Double.parseDouble(loc.substring(controllMark+1, loc.length())));
	}

	public boolean getEntering() {
		return entering;
	}

	public void setEntering(boolean entering) {
		this.entering = entering;
	}

	public String getApp() {
		return app;
	}

	public void setApp(String app) {
		this.app = app;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(id);
		dest.writeList(contact);
		dest.writeList(phone_mail);
		dest.writeString(message);
		dest.writeInt(type);
		dest.writeInt(getYear());
		dest.writeInt(getMonth());
		dest.writeInt(getDay());
		dest.writeInt(getHour());
		dest.writeInt(getMinute());
		dest.writeString(time);
		dest.writeParcelable(location, flags);
		dest.writeByte((byte) (entering ? 1 : 0));
		dest.writeString(app);
	}

	public static final Parcelable.Creator<Contact> CREATOR = new Parcelable.Creator<Contact>() {

		@Override
		public Contact createFromParcel(Parcel source) {
			return new Contact(source);
		}

		@Override
		public Contact[] newArray(int size) {
			return new Contact[size];
		}
	};

	public String getName(Context context, int i) {
		Cursor cursor = context.getContentResolver().query(Phone.CONTENT_URI, null, Phone.CONTACT_ID + "=?", new String[] { String.valueOf(this.contact.get(i)) }, null);
		if (cursor == null) {
			return null;
		}
		try {
			if (cursor.moveToFirst()) {
				String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME_PRIMARY));
				if (name != null) {
					return name;
				}
			}
		} finally {
			cursor.close();
		}
		return null;
	}

	public String getNumber(Context context, int i) {
		Cursor cursor = context.getContentResolver().query(Phone.CONTENT_URI, null, Phone.CONTACT_ID + "=?", new String[] { String.valueOf(this.contact.get(i)) }, null);
		if (cursor == null) {
			return null;
		}
		try {
			if (cursor.moveToFirst()) {
				String cellNumber = "";
				int index = cursor.getColumnIndex(Phone.DATA);
				while (!phone_mail.get(i).equals(cellNumber) && cursor.isAfterLast() == false) {
					cellNumber = cursor.getString(index).replace("-", "");
					cursor.moveToNext();
				}	     

				if (cursor.isAfterLast() && !phone_mail.get(i).equals(cellNumber)) {
					cursor.moveToFirst();
					return cursor.getString(index).replace("-", "");
				}
			}
		} finally {
			cursor.close();
		}

		return phone_mail.get(i);
	}

	public String[] getMail(Context context) {
		String[] tmp = new String[phone_mail.size()];
		for(int i = 0; i < tmp.length; i++) {
			tmp[i] = getMail(context, i);
		}
		return tmp;
	}

	private String getMail(Context context, int i) {
		Cursor cursor = context.getContentResolver().query(Email.CONTENT_URI, null, Email.CONTACT_ID + "=?", new String[] { String.valueOf(this.contact.get(i)) }, null);
		if (cursor == null) {
			return null;
		}
		try {
			if (cursor.moveToFirst()) {
				String cellNumber = "";
				int firstValidMailPosition = -1;
				int index = cursor.getColumnIndex(Email.DATA);
				while (!phone_mail.get(i).equals(cellNumber) && cursor.isAfterLast() == false) {
					if(isEmailValid(cursor.getString(index))) {
						cellNumber = cursor.getString(index);
						if(firstValidMailPosition == -1) {
							firstValidMailPosition = cursor.getPosition();
						}
					}
					cursor.moveToNext();
				}	     

				if (cursor.isAfterLast() && !phone_mail.get(i).equals(cellNumber)) {
					if(cursor.moveToPosition(firstValidMailPosition)) {
						return cursor.getString(index);
					}else {
						Toast.makeText(context, "No valid email has been found", Toast.LENGTH_LONG).show();
						return null;
					}
				}
			}
		} finally {
			cursor.close();
		}

		return phone_mail.get(i);
	}

	// this method just checks that the email is something like username@example.com
	private static boolean isEmailValid(String email) {
		boolean isValid = false;

		String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
		CharSequence inputStr = email;

		Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(inputStr);
		if (matcher.matches()) {
			isValid = true;
		}
		return isValid;
	}

	public Uri getPhotoUriFromID(Context context, int i) {

		Cursor cur = context.getContentResolver()
				.query(ContactsContract.Data.CONTENT_URI,
						null,
						ContactsContract.Data.CONTACT_ID
						+ "="
						+ String.valueOf(getContact().get(i))
						+ " AND "
						+ ContactsContract.Data.MIMETYPE
						+ "='"
						+ ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
						+ "'", null, null);
		try {
			if (cur != null) {
				if (!cur.moveToFirst()) {
					return null; // no photo was found
				}
			} else {
				return null; // error in cursor process
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			cur.close();
		}
		Uri person = ContentUris.withAppendedId(
				ContactsContract.Contacts.CONTENT_URI, this.contact.get(i));
		return Uri.withAppendedPath(person,
				ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
	}

	// just used for testing purposes
	public String toString() {
		return "1 - Id: " + id + "\n2 - Contact: " + getContactToString() + "\n3 - Phone/Mail: " + getPhoneMailToString() + "\n4 - Message: " + message + "\n5 - Type: " + type + "\n6 - Date: " + this.getDefaultDateToString() + "\n7 - Time: " + this.getDefaultTime() + "\n8 - Location: " + this.getLocationToString() + "\n9 - Entering: " + entering + "\n10 - App: " + app + "\n11 - Fine!";
	}

}
