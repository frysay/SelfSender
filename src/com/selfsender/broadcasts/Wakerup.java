package com.selfsender.broadcasts;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.widget.Toast;

import com.selfsender.R;
import com.selfsender.structures.Contact;
import com.selfsender.structures.ContactsDataSource;

public class Wakerup extends BroadcastReceiver {

	ContactsDataSource datasource;

	//Note: the lock is not actually needed
	@SuppressLint("Wakelock")
	@Override
	public void onReceive(Context context, Intent intent) 
	{   
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PM TAG");

		Bundle bundle = intent.getExtras();
		int id = (int) bundle.getLong("com.selfsender.wakerup.id");

		datasource = new ContactsDataSource(context);
		datasource.open();

		Contact contact = datasource.findContactFromId(id);

		wl.acquire();

		if(contact.getApp().equals("Sms")) {
			showNotification(context, contact.getName(context, 0), contact.getMessage(), id);
			sendMessage(context, contact);
		}else if(contact.getApp().equals("Email")) {
			showNotificationWithIntent(context, contact, id);
		}

		wl.release();

		datasource.removeContact(contact);
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

	public void setAlarm(Context context, Contact contact){
		AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, Wakerup.class);
		intent.putExtra("com.selfsender.wakerup.id", contact.getId());
		PendingIntent pi = PendingIntent.getBroadcast(context, (int) contact.getId(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
		am.set(AlarmManager.RTC_WAKEUP, contact.getDate().getTimeInMillis(), pi);
	}

	public void setAlarmGps(Context context, Contact contact){
		AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, GpsInit.class);
		intent.putExtra(".selfsender.GpsInit", contact.getId());
		PendingIntent pi = PendingIntent.getBroadcast(context, (int) contact.getId(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
		am.set(AlarmManager.RTC_WAKEUP, contact.getDate().getTimeInMillis(), pi);
	}

	public void cancelAlarm(Context context, int id)
	{
		Intent intent = new Intent(context, Wakerup.class);
		PendingIntent sender = PendingIntent.getBroadcast(context, id, intent, 0);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(sender);
	}
}
