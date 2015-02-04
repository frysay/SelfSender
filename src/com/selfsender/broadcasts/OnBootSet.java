package com.selfsender.broadcasts;

import java.util.List;

import com.selfsender.ProximityService;
import com.selfsender.structures.Contact;
import com.selfsender.structures.ContactsDataSource;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OnBootSet extends BroadcastReceiver {

	private final static long ONE_HOUR_IN_MILLIS = 3600000;

	List<Contact> contacts;

	ContactsDataSource datasource;

	@Override
	public void onReceive(Context context, Intent intent)
	{   

		if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
		{
			datasource = new ContactsDataSource(context);
			datasource.open();
			contacts = datasource.findAll();
			for(int i = 0; i < contacts.size(); i++) {
				switch (contacts.get(i).getType()) {
				case 1:
					if(contacts.get(i).getDate().getTimeInMillis() - System.currentTimeMillis() > ONE_HOUR_IN_MILLIS) {
						Wakerup wup0 = new Wakerup();
						wup0.setAlarmGps(context, contacts.get(i));
					}else {
						addProximityAlert(context, contacts.get(i));
					}
					break;
				case 2:
					Wakerup wup1 = new Wakerup();
					wup1.setAlarm(context, contacts.get(i));
					break;
				case 3:
					addProximityAlert(context, contacts.get(i));
					break;

				default:
					break;
				}
			}
		}
	}

	private void addProximityAlert(Context context, Contact contact) {
		Intent i = new Intent (context, ProximityService.class);
		i.putExtra("com.selfsender.proximityservice.serviceCall", true);
		i.putExtra("com.selfsender.proximityservice.addProximityAlert", contact);
		context.startService(i);
	}
}
