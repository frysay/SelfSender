package com.selfsender.broadcasts;

import com.selfsender.ProximityService;
import com.selfsender.structures.Contact;
import com.selfsender.structures.ContactsDataSource;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class GpsInit extends BroadcastReceiver {

	ContactsDataSource datasource;
	long id;
	Contact contact;

	@Override
	public void onReceive(Context context, Intent intent) {

		datasource = new ContactsDataSource(context);
		datasource.open();

		Bundle b = intent.getExtras();
		id = b.getLong(".selfsender.GpsInit");
		// we use the id of the contact and not the contact itself to grant that the data is fresh
		contact = datasource.findContactFromId(id);

		Intent i = new Intent (context, ProximityService.class);
		i.putExtra("com.selfsender.proximityservice.serviceCall", true);
		i.putExtra("com.selfsender.proximityservice.addProximityAlert", contact);
		context.startService(i);

	}
}
