package com.selfsender;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.selfsender.arrayadapters.AgendaArrayAdapter;
import com.selfsender.structures.Contact;
import com.selfsender.structures.ContactsDataSource;

public class Dialog extends ListActivity {

	ContactsDataSource datasource;

	List<String> phone_mails;

	Contact contact;
	int address;
	boolean bool;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog);

		datasource = new ContactsDataSource(this);

		Bundle b = getIntent().getExtras();
		phone_mails = new ArrayList<String>();
		phone_mails = b.getStringArrayList(".selfsender.Dialog");			
		contact = b.getParcelable(".selfsender.Contact");
		address = b.getInt(".selfsender.Address");
		bool = b.getBoolean(".selfsender.Bool");

		AgendaArrayAdapter adapter = new AgendaArrayAdapter(this, phone_mails);
		getListView().setAdapter(adapter);

	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		if(bool) {
			datasource.open();
			//NOTA: it's just in case more than one number got modified; in this way you are sure that the update will have the right effect.
			//Without this just the last number will get update cause all the dialog instances are prompt at once
			//and so the contact won't be up to date with the previous number change.
			contact = datasource.findContactFromId(contact.getId());
			ArrayList<String> phone_mail = new ArrayList<String>();
			phone_mail = contact.getPhoneMail();
			phone_mail.set(address, phone_mails.get(position).replace("-", ""));
			contact.setPhoneMail(phone_mail);
			datasource.updateContact(contact);
			finish();
		}else {
			datasource.open();
			//NOTA: it's just in case more than one number got modified; in this way you are sure that the update will have the right effect.
			//Without this just the last number will get update cause all the dialog instances are prompt at once
			//and so the contact won't be up to date with the previous number change.
			contact = datasource.findContactFromId(contact.getId());
			ArrayList<String> phone_mail = new ArrayList<String>();
			phone_mail = contact.getPhoneMail();
			if(isEmailValid(phone_mails.get(position))) {
				phone_mail.set(address, phone_mails.get(position));
				contact.setPhoneMail(phone_mail);
				datasource.updateContact(contact);
				finish();
			}else {
				Toast.makeText(getApplicationContext(), "Error: E-mail not valid. Please choose a valid one", Toast.LENGTH_LONG).show();
				Intent intent = new Intent(this, Dialog.class);
				intent.putStringArrayListExtra(".selfsender.Dialog", (ArrayList<String>) phone_mails);
				intent.putExtra(".selfsender.Contact", contact);
				intent.putExtra(".selfsender.Address", address);
				intent.putExtra(".selfsender.Bool", true);
				startActivity(intent); 
				finish();
			}
		}
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
}
