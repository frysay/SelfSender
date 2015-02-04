package com.selfsender;

import java.util.ArrayList;
import java.util.List;

import com.selfsender.arrayadapters.ContactArrayAdapter;
import com.selfsender.broadcasts.Wakerup;
import com.selfsender.structures.Contact;
import com.selfsender.structures.ContactsDataSource;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ShowStoredMessages extends ListActivity {

	public static ShowStoredMessages instance; 

	LinearLayout ll;

	TextView contactPhoto;

	List<Contact> contacts;

	ContactsDataSource datasource;

	String temp;

	Button tmp;

	static boolean resumeActivity;

	public static int CONTACT_DETAIL_ACTIVITY_EDIT = 2;

	private final static long ONE_HOUR_IN_MILLIS = 3600000;

	static Messenger messenger;
	static boolean serviceBind;

	private static boolean activityVisible;

	private static ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder binder) {
			messenger = new Messenger(binder);
			serviceBind = true;
		}

		public void onServiceDisconnected(ComponentName className) {
			messenger = null;
			serviceBind = false;
		}
	};

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_stored_messages);

		instance = this;	
		resumeActivity = false;
		activityVisible = true;

		registerForContextMenu(getListView());

		datasource = new ContactsDataSource(this);
		datasource.open();

		contacts = datasource.findAll();
		refreshDisplay();
	}

	// it's a method to be used to check if this activity is in foreground
	// (if an alert is triggered and this activity is in foreground, refreshDisplay() should be called, otherwise we have no display update)
	public static boolean isActivityVisible() {
		return activityVisible;
	}  

	private static void activityResumed() {
		activityVisible = true;
	}

	private static void activityPaused() {
		activityVisible = false;
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		Contact contact = contacts.get(position);

		Intent intent = new Intent(this, ViewMessage.class);
		intent.putExtra(".selfsender.Contact", contact);
		startActivity(intent);

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		resumeActivity();
		if (requestCode == CONTACT_DETAIL_ACTIVITY_EDIT && resultCode == -2) {
			Bundle b = data.getExtras();
			b.setClassLoader(getClassLoader());
			Contact bundleContact = b.getParcelable("com.selfsender.messagedealer.bundleContact");
			switch (bundleContact.getType()) {
			case 1:
				if(bundleContact.getDate().getTimeInMillis() - System.currentTimeMillis() > ONE_HOUR_IN_MILLIS) {
					Wakerup wup0 = new Wakerup();
					wup0.cancelAlarm(getApplicationContext(), (int) bundleContact.getId());
					datasource.removeContact(bundleContact);
				}else {
					removeProximityAlert(bundleContact);
				}
				break;
			case 2:
				Wakerup wup1 = new Wakerup();
				wup1.cancelAlarm(getApplicationContext(), (int) bundleContact.getId());
				datasource.removeContact(bundleContact);
				break;
			case 3:
				removeProximityAlert(bundleContact);
				break;

			default:
				break;
			}
		}
	}

	public void update(View view) {
		contacts = datasource.findAll();
		for(int i=0; i < contacts.size(); i++)
		{
			if(contacts.get(i).getApp().equals("Sms")) {
				for(int j = 0; j < contacts.get(i).getPhoneMail().size(); j++) {
					if(!contacts.get(i).getPhoneMail().get(j).equals(contacts.get(i).getNumber(this, j))) {
						updateNumber(contacts.get(i), j);
					}
				}
			}else if(contacts.get(i).getApp().equals("Email")) {
				for(int j = 0; j < contacts.get(i).getPhoneMail().size(); j++) {
					if(!contacts.get(i).getPhoneMail().get(j).equals((contacts.get(i).getMail(this))[j])) {
						updateMail(contacts.get(i), j);
					}
				}
			}else {
				//For future alternatives to sms and email
			}
		}
	}

	private void updateNumber(Contact contact, int address) {
		Cursor cursor = getContentResolver().query(Phone.CONTENT_URI, null, Phone.CONTACT_ID + "=?", new String[] { String.valueOf(contact.getContact().get(address)) }, null);
		if (cursor == null) {
			// Cursor error
		}
		List<String> phone_mails = new ArrayList<String>();
		try {
			if (cursor.moveToFirst()) {
				int index = cursor.getColumnIndex(Phone.DATA);
				while (cursor.isAfterLast() == false) {
					phone_mails.add(cursor.getString(index));
					cursor.moveToNext();
				}
			} else {
				//no results actions
			} 
		} catch (Exception e) {  
			Toast.makeText(getApplicationContext(), "Failed to get contact data", Toast.LENGTH_LONG).show();
		} finally {  
			if (cursor != null) {
				cursor.close();
			} 
			Intent intent = new Intent(this, Dialog.class);
			intent.putStringArrayListExtra(".selfsender.Dialog", (ArrayList<String>) phone_mails);
			intent.putExtra(".selfsender.Contact", contact);
			intent.putExtra(".selfsender.Address", address);
			intent.putExtra(".selfsender.Bool", true);
			startActivity(intent); 
		}
	}

	private void updateMail(Contact contact, int address) {
		Cursor cursor = getContentResolver().query(Email.CONTENT_URI, null, Email.CONTACT_ID + "=?", new String[] { String.valueOf(contact.getContact().get(address)) }, null);
		if (cursor == null) {
			// Cursor error
		}
		List<String> phone_mails = new ArrayList<String>();
		try {
			if (cursor.moveToFirst()) {
				int index = cursor.getColumnIndex(Email.DATA);
				while (cursor.isAfterLast() == false) {
					phone_mails.add(cursor.getString(index));
					cursor.moveToNext();
				}
			} else {
				//no results actions
			} 
		} catch (Exception e) {  
			Toast.makeText(getApplicationContext(), "Failed to get contact data", Toast.LENGTH_LONG).show();
		} finally {  
			if (cursor != null) {
				cursor.close();
			} 
			Intent intent = new Intent(this, Dialog.class);
			intent.putStringArrayListExtra(".selfsender.Dialog", (ArrayList<String>) phone_mails);
			intent.putExtra(".selfsender.Contact", contact);
			intent.putExtra(".selfsender.Address", address);
			intent.putExtra(".selfsender.Bool", false);
			startActivity(intent); 
		}
	}

	public void newMessage(View view) {

		Intent intent = new Intent(this, MessageDealer.class);
		intent.putExtra("com.selfsender.showstoredmessages.bool", true);
		startActivity(intent);
	}

	private void removeProximityAlert(Contact contact) {
		Message msg = Message.obtain(null, ProximityService.REMOVE_PROXIMITY_ALERT);
		msg.replyTo = new Messenger(new ResponseHandler());

		Bundle b = new Bundle();
		b.putParcelable("data", contact);

		msg.setData(b);

		try {
			messenger.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	protected static class ResponseHandler extends Handler {

		Boolean result;

		@Override
		public void handleMessage(Message msg) {
			int respCode = msg.what;

			switch(respCode) {
			case ProximityService.REMOVE_PROXIMITY_ALERT_RESPONSE:
				result = msg.getData().getBoolean("respData");
				if(result) {
					ShowStoredMessages ssm = ShowStoredMessages.instance;
					ssm.contacts = ssm.datasource.findAll();
					ssm.refreshDisplay();
				}
				break;
			}
		}
	}

	public static class MyBroadcastReceiver extends BroadcastReceiver { 

		ContactsDataSource datasource;

		@Override
		public void onReceive(Context context, Intent intent) {

			String key = LocationManager.KEY_PROXIMITY_ENTERING;

			// There is a known Android bug about the entering and the not primitive objects
			// (http://stackoverflow.com/questions/6507718/locationmanager-key-proximity-entering-unavailable-for-proximity-alerts)
			Boolean entering = intent.getBooleanExtra(key, true);

			Bundle bundle = intent.getExtras();

			int id = (int) bundle.getLong("com.selfsender.MyBroadcastReceiver.contactID");

			datasource = new ContactsDataSource(context);
			datasource.open();

			Contact contact = datasource.findContactFromId(id);

			if (entering) {
				if(contact.getApp().equals("Sms")) {
					showNotification(context, contact.getName(context, 0), contact.getMessage(), id);
					sendMessage(context, contact);
				}else if(contact.getApp().equals("Email")) {
					showNotificationWithIntent(context, contact, id);
				}
				// the exiting part not yet implemented
				//			}else {
				//				if(contact.getApp().equals("Sms")) {
				//					showNotification(context, contact.getName(context, 0), contact.getMessage(), id);
				//					sendMessage(context, contact);
				//				}else if(contact.getApp().equals("Email")) {
				//					showNotificationWithIntent(context, contact, id);
				//				}
			}

			datasource.close();   
			startService(context, contact);
		}

		private void startService(Context context, Contact contact) {
			Intent i = new Intent (context, ProximityService.class);
			i.putExtra("com.selfsender.proximityservice.serviceCall", false);
			i.putExtra("com.selfsender.proximityservice.removeProximityAlert", contact);
			context.startService(i);
		}

		private void sendMessage(Context context, Contact contact) {
			try {
				SmsManager smsManager = SmsManager.getDefault();
				ArrayList<String> parts = smsManager.divideMessage(contact.getMessage());
				for(int i = 0; i < contact.getContact().size(); i++) {
					smsManager.sendMultipartTextMessage(contact.getNumber(context, i), null, parts, null, null);
					Toast.makeText(context, "Message sent to " + contact.getName(context, i), Toast.LENGTH_LONG).show();
				}
			} catch (Exception e) {
				Toast.makeText(context, "Sms failed. Please try again.", Toast.LENGTH_LONG).show();
			}
			try {
				for(int i = 0; i < contact.getContact().size(); i++) {
					ContentValues values = new ContentValues();         
					values.put("address", contact.getNumber(context, i));                   
					values.put("body", contact.getMessage());                   
					context.getContentResolver().insert(Uri.parse("content://sms/sent"), values);
				}
			} catch (Exception e) {
				Toast.makeText(context, "Failed to store the message", Toast.LENGTH_LONG).show();
			}
		}	

		private void showNotificationWithIntent(Context context, Contact contact, int i) {
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("message/rfc822");
			intent.putExtra(Intent.EXTRA_EMAIL  , contact.getMail(context));
			intent.putExtra(Intent.EXTRA_SUBJECT, contact.getMailSubject());
			intent.putExtra(Intent.EXTRA_TEXT   , contact.getMailText());
			PendingIntent pendingIntent = PendingIntent.getActivity(context, i, Intent.createChooser(intent, "Send mail..."), PendingIntent.FLAG_UPDATE_CURRENT);

			NotificationCompat.Builder mBuilder =
					new NotificationCompat.Builder(context)
			.setSmallIcon(R.drawable.ic_launcher)
			.setContentTitle(contact.getMailSubject())
			.setContentText(contact.getMailText())
			.setAutoCancel(true)
			.setContentIntent(pendingIntent); //Required on Gingerbread and below
			NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.notify(i, mBuilder.build());
		}

		private void showNotification(Context context, String name, String text, int i) {
			final Intent emptyIntent = new Intent();
			PendingIntent pendingIntent = PendingIntent.getActivity(context, i, emptyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

			NotificationCompat.Builder mBuilder =
					new NotificationCompat.Builder(context)
			.setSmallIcon(R.drawable.ic_launcher)
			.setContentTitle("Message sent to " + name)
			.setContentText(text)
			.setAutoCancel(true)
			.setContentIntent(pendingIntent); //Required on Gingerbread and below
			NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.notify(i, mBuilder.build());
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.show_stored_messages_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.show_messages_all:
			contacts = datasource.findAll();
			refreshDisplay();
			return true;

		case R.id.show_messages_gps_date:
			contacts = datasource.findFiltered("type == 1", "time ASC");
			refreshDisplay();
			return true;

		case R.id.show_messages_date:
			contacts = datasource.findFiltered("type == 2", "time ASC");
			refreshDisplay();
			return true;

		case R.id.show_messages_gps:
			contacts = datasource.findFiltered("type == 3", null);
			refreshDisplay();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.ssm_and_vm_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.single_contact_edit:
			AdapterContextMenuInfo infoEdit = (AdapterContextMenuInfo) item.getMenuInfo();
			int positionEdit = infoEdit.position;
			Contact contact = contacts.get(positionEdit);
			Intent intent = new Intent(this, MessageDealer.class);
			intent.putExtra("com.selfsender.showstoredmessages.bool", false);
			intent.putExtra("com.selfsender.showstoredmessages.edit", contact);
			startActivityForResult(intent, CONTACT_DETAIL_ACTIVITY_EDIT);
			break;
		case R.id.single_contact_delete:
			AdapterContextMenuInfo infoDelete = (AdapterContextMenuInfo) item.getMenuInfo();
			final int positionDelete = infoDelete.position;
			new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle("Delete")
			.setMessage("Are you sure to delete this entity?")
			.setNegativeButton("No", null)
			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {	
					// this if is needed in case I have at least an item in my database and my app gets killed
					// in that situation I want to resume the service connection before removing the item
					// otherwise if I try to delete that item I get a messenger NPE cause the service has not yet connected
					// (the connection isn't made in onCreate, because I don't want to immediately connect to the service if I don't need to use it) 
					if(!resumeActivity) {
						resumeActivity();
					}
					Contact contact = contacts.get(positionDelete);
					switch (contact.getType()) {
					case 1:
						if(contact.getDate().getTimeInMillis() - System.currentTimeMillis() > ONE_HOUR_IN_MILLIS) {
							Wakerup wup0 = new Wakerup();
							wup0.cancelAlarm(getApplicationContext(), (int) contact.getId());
							datasource.removeContact(contact);
						}else {
							removeProximityAlert(contact);
						}
						break;
					case 2:
						Wakerup wup1 = new Wakerup();
						wup1.cancelAlarm(getApplicationContext(), (int) contact.getId());
						datasource.removeContact(contact);
						break;
					case 3:
						removeProximityAlert(contact);
						break;

					default:
						break;
					}

					contacts.remove(positionDelete);
					refreshDisplay();
				}

			})
			.show();
			break;
		}
		return (super.onOptionsItemSelected(item));
	}

	private void resumeActivity() {
		activityResumed();
		datasource.open(); 
		Intent intent= new Intent(this, ProximityService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		contacts = datasource.findAll();
		refreshDisplay(); 
		resumeActivity = true;
	}

	// public and not private cause of the purpose of isActivityVisible()
	public void refreshDisplay() {
		ContactArrayAdapter adapter = new ContactArrayAdapter(this, contacts);
		getListView().setAdapter(adapter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (!resumeActivity) {
			resumeActivity();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		activityPaused();
		resumeActivity = false;
		datasource.close();
		if(serviceBind) {
			serviceBind = false;
			unbindService(mConnection);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		datasource.close();    	
		if(serviceBind) {
			serviceBind = false;
			unbindService(mConnection);
		}
	}
}