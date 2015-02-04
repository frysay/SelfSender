package com.selfsender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.selfsender.broadcasts.Wakerup;
import com.selfsender.structures.Contact;
import com.selfsender.structures.ContactsDataSource;

public class MessageDealer extends Activity {

	RelativeLayout rl;

	int view_check = 0;
	int menu_check = 0;

	int idCounter = 123;

	View viewApp;
	View viewMenu;

	boolean bool;

	ArrayList<String> selectedAddresses;
	String name;

	private static final int PICK_CONTACT = 0;

	private final static long ONE_HOUR_IN_MILLIS = 3600000;

	public static MessageDealer instance; 

	Contact contact;
	Contact contactEdit;

	ArrayList<Long> listContactId;

	List<Contact> contacts;

	ContactsDataSource datasource;

	private GoogleMap map;
	private Location tmpLoc;

	Button date_picker;
	Button time_picker;

	EditText locationName;

	static Messenger messenger;
	static boolean serviceBind;

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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.message_dealer);

		instance = this;		
		registerReceiver(androidBug, new IntentFilter("error.bug.android"));

		rl = (RelativeLayout) findViewById(R.id.Rl_dealer);

		Bundle b = getIntent().getExtras();
		bool = b.getBoolean("com.selfsender.showstoredmessages.bool");		

		datasource = new ContactsDataSource(this);
		datasource.open();
		contacts = datasource.findAll();
		listContactId = new ArrayList<Long>();
		selectedAddresses = new ArrayList<String>();
		tmpLoc = new Location("");

		if(bool) {
			DateGpsLayout();
			setResult(-1);
		}else {
			contactEdit = b.getParcelable("com.selfsender.showstoredmessages.edit");
			switch (contactEdit.getType()) {
			case 1:
				DateGpsLayout();
				getLatLong(rl, contactEdit.getLocation().getLatitude(), contactEdit.getLocation().getLongitude());
				setCurrentDateOnView(contactEdit.getDate());
				setCurrentTimeOnView(contactEdit.getDate());
				break;
			case 2:
				DateLayout();
				setCurrentDateOnView(contactEdit.getDate());
				setCurrentTimeOnView(contactEdit.getDate());
				break;
			case 3:
				GpsLayout();
				getLatLong(rl, contactEdit.getLocation().getLatitude(), contactEdit.getLocation().getLongitude());
				break;

			default:
				break;
			}
			if(contactEdit.getApp().equals("Sms")) {
				smsLayout();
				((Button) findViewById(R.id.dealer_button_app)).setText("Sms");
				((Button) findViewById(R.id.app_layout_button)).setText("Add another contact");
				Button btn = new Button(this);
				RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
				lp.addRule(RelativeLayout.ALIGN_LEFT);
				lp.addRule(RelativeLayout.BELOW, R.id.app_layout_button);
				btn.setLayoutParams(lp);
				for(int i = 0; i < contactEdit.getContact().size(); i++) {
					listContactId.add(contactEdit.getContact().get(i));
					selectedAddresses.add(contactEdit.getPhoneMail().get(i));
					btn.setId(idCounter);
					((RelativeLayout) findViewById(R.id.sms_layout)).addView(btn, 1, lp);
					btn.setText(contactEdit.getName(this, i));
					moveSmsView();
				}
				((EditText) findViewById(R.id.app_layout_message)).setText(contactEdit.getMessage());
			}else if(contactEdit.getApp().equals("Email")) {
				emailLayout();
				((Button) findViewById(R.id.dealer_button_app)).setText("E-mail");
				((Button) findViewById(R.id.app_layout_button)).setText("Add another contact");
				Button btn = new Button(this);
				RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
				lp.addRule(RelativeLayout.ALIGN_LEFT);
				lp.addRule(RelativeLayout.BELOW, R.id.app_layout_button);
				btn.setLayoutParams(lp);
				for(int i = 0; i < contactEdit.getContact().size(); i++) {
					listContactId.add(contactEdit.getContact().get(i));
					selectedAddresses.add(contactEdit.getPhoneMail().get(i));
					btn.setId(idCounter);
					((RelativeLayout) findViewById(R.id.mail_layout)).addView(btn, 1, lp);
					btn.setText(contactEdit.getName(this, i));
					moveMailView();
				}
				((EditText) findViewById(R.id.app_layout_subject)).setText(contactEdit.getMailSubject());
				((EditText) findViewById(R.id.app_layout_message)).setText(contactEdit.getMailText());
			}
		}

	}

	protected static class ResponseHandler extends Handler {

		Boolean result;

		@Override
		public void handleMessage(Message msg) {
			int respCode = msg.what;
			switch (respCode) {
			case ProximityService.ADD_PROXIMITY_ALERT_RESPONSE:
				result = msg.getData().getBoolean("respData");
				break;

			default:
				break;
			}
		}
	}

	private void startService() {
		Intent i = new Intent (this, ProximityService.class);
		startService(i);
		bindService(i, mConnection, Context.BIND_AUTO_CREATE);
	}

	// zoom the map view to the point we searched for and put a mark on it
	private void gotoLocation(String name, double lat, double lng, float zoom) {
		LatLng ll = new LatLng (lat, lng);
		CameraUpdate camUpdate = CameraUpdateFactory.newLatLngZoom(ll, zoom);

		MarkerOptions marker = new MarkerOptions().position(new LatLng(lat, lng)).title(name);
		map.addMarker(marker);
		map.moveCamera(camUpdate);
	}

	// it's called when we press the search icon on the keyboard. it's used to find latitude and longitude of the address we were searching for
	private void getLatLong(View view) {

		InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

		String geolocation = locationName.getText().toString();

		Geocoder geocoder = new Geocoder(this);
		List<Address> list;
		try {
			list = geocoder.getFromLocationName(geolocation, 1);
			Address item = list.get(0);
			String locality = item.getLocality();
			Toast.makeText(this, locality, Toast.LENGTH_LONG).show();

			double lat = item.getLatitude();
			double lng = item.getLongitude();

			gotoLocation(locality, lat, lng, 17);

			tmpLoc.setLatitude(lat);
			tmpLoc.setLongitude(lng);

		} catch (IOException e) {
			Toast.makeText(this, "Error finding the location", Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}

	}

	// same method as the one before but used when editing an already existing contact 
	private void getLatLong(View view, double lat, double lng) {

		InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

		Geocoder geocoder = new Geocoder(this);
		List<Address> list;

		try {
			list = geocoder.getFromLocation(lat, lng, 1);
			Address item = list.get(0);
			String locality = item.getLocality();
			Toast.makeText(this, locality, Toast.LENGTH_LONG).show();

			gotoLocation(locality, lat, lng, 17);

			tmpLoc.setLatitude(lat);
			tmpLoc.setLongitude(lng);

		} catch (IOException e) {
			Toast.makeText(this, "Error finding the location", Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}

	}

	public void appChooser(View view) {

		final CharSequence[] items = {"Sms", "E-mail"};

		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle("List of Apps");
		alertDialogBuilder
		.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				switch (item) {
				case 0:
					chkClnApp();
					((Button) findViewById(R.id.dealer_button_app)).setText("Sms");
					smsLayout();
					break;

				case 1:
					chkClnApp();
					((Button) findViewById(R.id.dealer_button_app)).setText("E-mail");
					emailLayout();
					break;

				default:
					break;
				}
			}
		});
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}

	private void setCurrentDateOnView(GregorianCalendar c) {

		int yy = c.get(GregorianCalendar.YEAR);
		int mm = c.get(GregorianCalendar.MONTH);
		int dd = c.get(GregorianCalendar.DAY_OF_MONTH);
		String date = String.format(Locale.getDefault(),"%02d/%02d/%04d", dd, mm+1, yy);
		date_picker.setText(DateFormat.format("EEEE", new GregorianCalendar(yy, mm, dd)).toString() + " " + date);
	}

	private void setCurrentTimeOnView(GregorianCalendar c) {

		int hr = c.get(GregorianCalendar.HOUR_OF_DAY);
		int mn= c.get(GregorianCalendar.MINUTE);
		time_picker.setText(normalize(hr) + ":" + normalize(mn));
	}

	public void showDatePickerDialog(View v) {
		DialogFragment newFragment = new DatePickerFragment();
		newFragment.show(getFragmentManager(), "datePicker");	
	}

	public void showTimePickerDialog(View v) {
		TimePickerFragment newFragment = new TimePickerFragment();
		newFragment.show(getFragmentManager(), "timePicker");
	}

	private void chkClnApp() {
		if (view_check!=0)
		{
			((ViewGroup) viewApp.getParent()).removeView(viewApp);
			idCounter = 123;
			listContactId.clear();
			selectedAddresses.clear();
		}
	}

	private void chkClnMenu() {
		switch (menu_check) {
		case 1:
			FragmentTransaction ft1 = getFragmentManager().beginTransaction();
			ft1.remove(getFragmentManager().findFragmentById(R.id.map_date_gps)).commit();
			((ViewGroup) viewMenu.getParent()).removeView(viewMenu);
			break;
		case 2:
			((ViewGroup) viewMenu.getParent()).removeView(viewMenu);
			break;
		case 3:
			FragmentTransaction ft3 = getFragmentManager().beginTransaction();
			ft3.remove(getFragmentManager().findFragmentById(R.id.map_gps)).commit();
			((ViewGroup) viewMenu.getParent()).removeView(viewMenu);
			break;

		default:
			break;
		}
	}

	private void smsLayout() {
		Button accept;
		switch (menu_check) {
		case 1:
			RelativeLayout rl_inflate1 = (RelativeLayout) findViewById(R.id.Rl_date_gps);
			LayoutInflater inflater1 = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			viewApp = inflater1.inflate(R.layout.sms_layout, null);
			RelativeLayout.LayoutParams rlp_child1 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
			rlp_child1.addRule(RelativeLayout.ALIGN_LEFT);
			rlp_child1.addRule(RelativeLayout.BELOW, R.id.dealer_button_app);
			viewApp.setLayoutParams(rlp_child1);
			rl_inflate1.addView(viewApp);
			accept = (Button) findViewById(R.id.app_layout_accept);
			accept.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					acceptGpsDate();
				}
			});
			break;
		case 2:
			RelativeLayout rl_inflate2 = (RelativeLayout) findViewById(R.id.Rl_date);
			LayoutInflater inflater2 = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			viewApp = inflater2.inflate(R.layout.sms_layout, null);
			RelativeLayout.LayoutParams rlp_child2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
			rlp_child2.addRule(RelativeLayout.ALIGN_LEFT);
			rlp_child2.addRule(RelativeLayout.BELOW, R.id.dealer_button_app);
			viewApp.setLayoutParams(rlp_child2);
			rl_inflate2.addView(viewApp);
			accept = (Button) findViewById(R.id.app_layout_accept);
			accept.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					acceptDate();
				}
			});
			break;
		case 3:
			RelativeLayout rl_inflate3 = (RelativeLayout) findViewById(R.id.Rl_gps);
			LayoutInflater inflater3 = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			viewApp = inflater3.inflate(R.layout.sms_layout, null);
			RelativeLayout.LayoutParams rlp_child3 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
			rlp_child3.addRule(RelativeLayout.ALIGN_LEFT);
			rlp_child3.addRule(RelativeLayout.BELOW, R.id.dealer_button_app);
			viewApp.setLayoutParams(rlp_child3);
			rl_inflate3.addView(viewApp);
			accept = (Button) findViewById(R.id.app_layout_accept);
			accept.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					acceptGps();
				}
			});
			break;

		default:
			break;
		}

		RelativeLayout.LayoutParams etparams = (LayoutParams) ((EditText) findViewById(R.id.app_layout_message)).getLayoutParams();
		etparams.addRule(RelativeLayout.BELOW, R.id.app_layout_button);
		((EditText) findViewById(R.id.app_layout_message)).setLayoutParams(etparams);
		RelativeLayout.LayoutParams btnparams = (LayoutParams) ((Button) findViewById(R.id.app_layout_accept)).getLayoutParams();
		btnparams.addRule(RelativeLayout.BELOW, R.id.app_layout_message);
		((Button) findViewById(R.id.app_layout_accept)).setLayoutParams(btnparams);
		viewApp.requestFocus();

		view_check=1;
	}

	private void DateGpsLayout() {
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		viewMenu = inflater.inflate(R.layout.prox_alert_date_gps, null);
		RelativeLayout.LayoutParams rlp_child = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.WRAP_CONTENT);

		viewMenu.setLayoutParams(rlp_child);
		map = ((MapFragment)getFragmentManager().findFragmentById(R.id.map_date_gps)).getMap();
		rl.addView(viewMenu);	

		locationName = (EditText) findViewById(R.id.location_name);
		locationName.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				boolean handled = false;
				if (actionId == EditorInfo.IME_ACTION_SEARCH) {
					getLatLong(v);
					handled = true;
				}
				return handled;
			}
		});

		date_picker = (Button) findViewById(R.id.Date_Picker);
		time_picker = (Button) findViewById(R.id.Time_Picker);
		GregorianCalendar c = new GregorianCalendar();
		setCurrentDateOnView(c);
		time_picker.setText("00:00");

		date_picker.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showDatePickerDialog(v);

			}
		});

		time_picker.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showTimePickerDialog(v);				
			}
		});

		menu_check=1;

	}

	private void DateLayout() {		
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		viewMenu = inflater.inflate(R.layout.prox_alert_date, null);
		RelativeLayout.LayoutParams rlp_child = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
		viewMenu.setLayoutParams(rlp_child);

		rl.addView(viewMenu);

		date_picker = (Button) findViewById(R.id.Date_Picker);
		time_picker = (Button) findViewById(R.id.Time_Picker);
		GregorianCalendar c = new GregorianCalendar();
		setCurrentDateOnView(c);
		setCurrentTimeOnView(c);

		date_picker.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showDatePickerDialog(v);

			}
		});

		time_picker.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showTimePickerDialog(v);				
			}
		});

		menu_check = 2;
	}

	private void GpsLayout() {	
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		viewMenu = inflater.inflate(R.layout.prox_alert_gps, null);
		RelativeLayout.LayoutParams rlp_child = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
		viewMenu.setLayoutParams(rlp_child);

		map = ((MapFragment)getFragmentManager().findFragmentById(R.id.map_gps)).getMap();
		rl.addView(viewMenu);

		locationName = (EditText) findViewById(R.id.location_name);
		locationName.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				boolean handled = false;
				if (actionId == EditorInfo.IME_ACTION_SEARCH) {
					getLatLong(v);
					handled = true;
				}
				return handled;
			}
		});

		menu_check=3;
	}

	public void appMessage(View view) {
		Intent intent = new Intent(Intent.ACTION_PICK);
		intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
		startActivityForResult(intent, PICK_CONTACT);
	}

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

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
		if (resultCode == RESULT_OK) {  
			switch (requestCode) {  
			case PICK_CONTACT :
				if(view_check == 1) {
					setPhone(data.getData());
				}else if(view_check == 2) {
					setMail(data.getData());
				}
				break;  
			}  
		} else {
			// there's been an activity result error
		}  
	}

	private void setPhone(Uri data) {
		Cursor cursor = null;  
		String cellNumber = "";
		String name = "";
		List<String> allNumbers = new ArrayList<String>();
		int phoneIdx = 0;
		long contactId = -1;
		try {  
			String id = data.getLastPathSegment();  
			cursor = getContentResolver().query(Phone.CONTENT_URI, null, Phone.CONTACT_ID + "=?", new String[] { id }, null);  
			phoneIdx = cursor.getColumnIndex(Phone.DATA);
			if (cursor.moveToFirst()) {
				while (cursor.isAfterLast() == false) {
					cellNumber = cursor.getString(phoneIdx);
					allNumbers.add(cellNumber);
					contactId = Long.parseLong(cursor.getString(cursor.getColumnIndex(Phone.CONTACT_ID)));
					name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME_PRIMARY));
					cursor.moveToNext();
				}
				listContactId.add(contactId);
			} else {
				//no results actions
			} 
		} catch (Exception e) {  
			Toast.makeText(this, "Failed to get contact data", Toast.LENGTH_LONG).show();
		} finally {  
			final String contactName = name;
			if (cursor != null) {
				cursor.close();
			} 
			final Button cnt = new Button(this);
			final RelativeLayout.LayoutParams lpNumbers = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
			lpNumbers.addRule(RelativeLayout.ALIGN_LEFT);
			lpNumbers.addRule(RelativeLayout.BELOW, R.id.app_layout_button);
			cnt.setLayoutParams(lpNumbers);
			final CharSequence[] items = allNumbers.toArray(new String[allNumbers.size()]);
			AlertDialog.Builder builder = new AlertDialog.Builder(MessageDealer.this);
			builder.setTitle("Choose a number");
			builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					selectedAddresses.add(items[item].toString().replace("-", ""));
					cnt.setId(idCounter);
					((Button) findViewById(R.id.app_layout_button)).setText("Add another contact");
					((RelativeLayout) findViewById(R.id.sms_layout)).addView(cnt, 1, lpNumbers);
					cnt.setText(contactName);
					moveSmsView();
				}
			});
			AlertDialog alert = builder.create();
			if(allNumbers.size() > 1) {
				alert.show();
			} else if (allNumbers.size() == 0) {  
				Toast.makeText(this, "No phone number found", Toast.LENGTH_LONG).show();
			} else {
				selectedAddresses.add(items[0].toString().replace("-", ""));
				cnt.setId(idCounter);
				((Button) findViewById(R.id.app_layout_button)).setText("Add another contact");
				((RelativeLayout) findViewById(R.id.sms_layout)).addView(cnt, 1, lpNumbers);
				cnt.setText(name);
				moveSmsView();
			}
		} 
	}

	private void setMail(Uri data) {
		Cursor cursor = null;  
		String cellNumber = "";
		String name = "";
		List<String> allNumbers = new ArrayList<String>();
		int phoneIdx = 0;
		long contactId = -1;
		try {  
			String id = data.getLastPathSegment();  
			cursor = getContentResolver().query(Email.CONTENT_URI, null, Email.CONTACT_ID + "=?", new String[] { id }, null);  
			phoneIdx = cursor.getColumnIndex(Email.DATA);
			if (cursor.moveToFirst()) {
				while (cursor.isAfterLast() == false) {
					cellNumber = cursor.getString(phoneIdx);
					allNumbers.add(cellNumber);
					contactId = Long.parseLong(cursor.getString(cursor.getColumnIndex(Email.CONTACT_ID)));
					name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME_PRIMARY));
					cursor.moveToNext();
				}
				listContactId.add(contactId);
			} else {
				//no results actions
			} 
		} catch (Exception e) {  
			Toast.makeText(this, "Failed to get contact data", Toast.LENGTH_LONG).show();
		} finally {  
			final String contactName = name;
			if (cursor != null) {
				cursor.close();
			} 
			final Button cnt = new Button(this);
			final RelativeLayout.LayoutParams lpNumbers = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
			lpNumbers.addRule(RelativeLayout.ALIGN_LEFT);
			lpNumbers.addRule(RelativeLayout.BELOW, R.id.app_layout_button);
			cnt.setLayoutParams(lpNumbers);
			final CharSequence[] items = allNumbers.toArray(new String[allNumbers.size()]);
			AlertDialog.Builder builder = new AlertDialog.Builder(MessageDealer.this);
			builder.setTitle("Choose a number");
			builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					if(isEmailValid(items[item].toString())) {
						selectedAddresses.add(items[item].toString());					
						cnt.setId(idCounter);
						((Button) findViewById(R.id.app_layout_button)).setText("Add another contact");
						((RelativeLayout) findViewById(R.id.mail_layout)).addView(cnt, 1, lpNumbers);
						cnt.setText(contactName);
						moveMailView();
					}else {
						Toast.makeText(getApplicationContext(), items[item].toString() + " is not a valid email address", Toast.LENGTH_LONG).show();
						listContactId.remove(listContactId.size()-1);
					}
				}
			});
			AlertDialog alert = builder.create();
			if(allNumbers.size() > 1) {
				alert.show();
			} else if (allNumbers.size() == 0) {  
				Toast.makeText(this, "No email address found", Toast.LENGTH_LONG).show();
			} else {
				if(isEmailValid(items[0].toString())) {
					selectedAddresses.add(items[0].toString());
					cnt.setId(idCounter);
					((Button) findViewById(R.id.app_layout_button)).setText("Add another contact");
					((RelativeLayout) findViewById(R.id.mail_layout)).addView(cnt, 1, lpNumbers);
					cnt.setText(name);
					moveMailView();
				}else {
					Toast.makeText(getApplicationContext(), items[0].toString() + " is not a valid email address", Toast.LENGTH_LONG).show();
					listContactId.remove(listContactId.size()-1);
				}
			}
		}  
	}

	private void moveSmsView() {
		if(idCounter == 123) {
			RelativeLayout.LayoutParams etparams = (LayoutParams) ((EditText) findViewById(R.id.app_layout_message)).getLayoutParams();
			etparams.addRule(RelativeLayout.BELOW, idCounter++);
			((EditText) findViewById(R.id.app_layout_message)).setLayoutParams(etparams);
			RelativeLayout.LayoutParams btnparams = (LayoutParams) ((Button) findViewById(R.id.app_layout_accept)).getLayoutParams();
			btnparams.addRule(RelativeLayout.BELOW, R.id.app_layout_message);
			((Button) findViewById(R.id.app_layout_accept)).setLayoutParams(btnparams);
		}else {
			RelativeLayout.LayoutParams cntparams = (LayoutParams) ((Button) findViewById(idCounter)).getLayoutParams();
			cntparams.addRule(RelativeLayout.BELOW, idCounter-1);
			((Button) findViewById(idCounter)).setLayoutParams(cntparams);
			RelativeLayout.LayoutParams etparams = (LayoutParams) ((EditText) findViewById(R.id.app_layout_message)).getLayoutParams();
			etparams.addRule(RelativeLayout.BELOW, idCounter++);
			((EditText) findViewById(R.id.app_layout_message)).setLayoutParams(etparams);
			RelativeLayout.LayoutParams btnparams = (LayoutParams) ((Button) findViewById(R.id.app_layout_accept)).getLayoutParams();
			btnparams.addRule(RelativeLayout.BELOW, R.id.app_layout_message);
			((Button) findViewById(R.id.app_layout_accept)).setLayoutParams(btnparams);
		}
	}

	private void moveMailView() {
		if(idCounter == 123) {
			RelativeLayout.LayoutParams et1params = (LayoutParams) ((EditText) findViewById(R.id.app_layout_subject)).getLayoutParams();
			et1params.addRule(RelativeLayout.BELOW, idCounter++);
			((EditText) findViewById(R.id.app_layout_subject)).setLayoutParams(et1params);
			RelativeLayout.LayoutParams et2params = (LayoutParams) ((EditText) findViewById(R.id.app_layout_message)).getLayoutParams();
			et2params.addRule(RelativeLayout.BELOW, R.id.app_layout_subject);
			((EditText) findViewById(R.id.app_layout_message)).setLayoutParams(et2params);
			RelativeLayout.LayoutParams btnparams = (LayoutParams) ((Button) findViewById(R.id.app_layout_accept)).getLayoutParams();
			btnparams.addRule(RelativeLayout.BELOW, R.id.app_layout_message);
			((Button) findViewById(R.id.app_layout_accept)).setLayoutParams(btnparams);
		}else {
			RelativeLayout.LayoutParams cntparams = (LayoutParams) ((Button) findViewById(idCounter)).getLayoutParams();
			cntparams.addRule(RelativeLayout.BELOW, idCounter-1);
			((Button) findViewById(idCounter)).setLayoutParams(cntparams);
			RelativeLayout.LayoutParams et1params = (LayoutParams) ((EditText) findViewById(R.id.app_layout_subject)).getLayoutParams();
			et1params.addRule(RelativeLayout.BELOW, idCounter++);
			((EditText) findViewById(R.id.app_layout_subject)).setLayoutParams(et1params);
			RelativeLayout.LayoutParams et2params = (LayoutParams) ((EditText) findViewById(R.id.app_layout_message)).getLayoutParams();
			et2params.addRule(RelativeLayout.BELOW, R.id.app_layout_subject);
			((EditText) findViewById(R.id.app_layout_message)).setLayoutParams(et2params);
			RelativeLayout.LayoutParams btnparams = (LayoutParams) ((Button) findViewById(R.id.app_layout_accept)).getLayoutParams();
			btnparams.addRule(RelativeLayout.BELOW, R.id.app_layout_message);
			((Button) findViewById(R.id.app_layout_accept)).setLayoutParams(btnparams);
		}
	}

	private void addNewProximityAlert(Contact contact) {
		Message msg = Message.obtain(null, ProximityService.ADD_PROXIMITY_ALERT);
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

	private void chkServiceOn() {
		if(!ProximityService.isRunning()) {
			startService();
		}
	}

	private boolean checkFields() {
		// Note: all the other fields are initialized for sure
		// Note: listContactId empty means that selectedAddresses is empty too and the other way around (I put the or just for clarity but it's useless)
		if(menu_check != 2) {
			if(tmpLoc.distanceTo(new Location("")) == 0) {
				Toast.makeText(this, "Attention: no location has been selected", Toast.LENGTH_LONG).show();
				return false;
			}
		}
		if(listContactId.isEmpty() || selectedAddresses.isEmpty()) {
			Toast.makeText(this, "Attention: no contact has been selected", Toast.LENGTH_LONG).show();
			return false;
		}
		if(view_check == 1) {
			if(((EditText) findViewById(R.id.app_layout_message)).getText().toString().isEmpty()) {
				Toast.makeText(this, "Attention: no message has been written", Toast.LENGTH_LONG).show();
				return false;
			}
		}else if(view_check == 2) {
			if(((EditText) findViewById(R.id.app_layout_subject)).getText().toString().isEmpty()) {
				Toast.makeText(this, "Attention: no subject has been written", Toast.LENGTH_LONG).show();
				return false;
			}else if (((EditText) findViewById(R.id.app_layout_message)).getText().toString().isEmpty()) {
				Toast.makeText(this, "Attention: no message has been written", Toast.LENGTH_LONG).show();
				return false;
			}
		}
		return true;
	}

	private void acceptGpsDate() {
		if(checkFields()) {
			contact = new Contact();
			contact.setContact(listContactId);
			contact.setPhoneMail(selectedAddresses);
			if(view_check == 1) {
				contact.setMessage(((EditText) findViewById(R.id.app_layout_message)).getText().toString());
			}else if(view_check == 2) {
				contact.setMessage(((EditText) findViewById(R.id.app_layout_subject)).getText().toString() + "%" + ((EditText) findViewById(R.id.app_layout_message)).getText().toString());
			}else{
				Toast.makeText(this, "Error setting the message", Toast.LENGTH_LONG).show();
			}
			contact.setType(menu_check);
			contact.setDateFromString(date_picker.getText().toString() + " - "  + time_picker.getText().toString(), true);
			contact.setTime(time_picker.getText().toString());
			contact.setLocation(tmpLoc);
			contact.setEntering(true);
			contact.setApp(getAppToString());

			datasource.create(contact);
			contacts = datasource.findAll();

			if(contact.getDate().getTimeInMillis() - System.currentTimeMillis() > ONE_HOUR_IN_MILLIS) {
				Wakerup wup0 = new Wakerup();
				Toast.makeText(this, "" + (contact.getDate().getTimeInMillis() - System.currentTimeMillis()), Toast.LENGTH_LONG).show();
				wup0.setAlarmGps(this, contact);
			}else {
				chkServiceOn();
				addNewProximityAlert(contact);
			}

			if(!bool) {
				Intent intent = new Intent();
				intent.putExtra("com.selfsender.messagedealer.bundleContact", contactEdit);
				setResult(-2, intent);
			}
			finish();
		}
	}

	private void acceptDate() {
		if(checkFields()) {
			contact = new Contact();
			contact.setContact(listContactId);
			contact.setPhoneMail(selectedAddresses);
			if(view_check == 1) {
				contact.setMessage(((EditText) findViewById(R.id.app_layout_message)).getText().toString());
			}else if(view_check == 2) {
				contact.setMessage(((EditText) findViewById(R.id.app_layout_subject)).getText().toString() + "%" + ((EditText) findViewById(R.id.app_layout_message)).getText().toString());
			}else{
				Toast.makeText(this, "Error setting the message", Toast.LENGTH_LONG).show();
			}
			contact.setType(menu_check);
			contact.setDateFromString(date_picker.getText().toString() + " - "  + time_picker.getText().toString(), true);
			contact.setTime(time_picker.getText().toString());
			contact.setLocation(tmpLoc); // 0.0 - 0.0
			contact.setEntering(true);
			contact.setApp(getAppToString());

			datasource.create(contact);
			contacts = datasource.findAll();

			Wakerup wup1 = new Wakerup();
			Toast.makeText(this, "" + (contact.getDate().getTimeInMillis() - System.currentTimeMillis()), Toast.LENGTH_LONG).show();
			wup1.setAlarm(this, contact);

			if(!bool) {
				Intent intent = new Intent();
				intent.putExtra("com.selfsender.messagedealer.bundleContact", contactEdit);
				setResult(-2, intent);
			}
			finish();
		}
	}

	private void acceptGps() {
		if(checkFields()) {
			contact = new Contact();
			contact.setContact(listContactId);
			contact.setPhoneMail(selectedAddresses);
			if(view_check == 1) {
				contact.setMessage(((EditText) findViewById(R.id.app_layout_message)).getText().toString());
			}else if(view_check == 2) {
				contact.setMessage(((EditText) findViewById(R.id.app_layout_subject)).getText().toString() + "%" + ((EditText) findViewById(R.id.app_layout_message)).getText().toString());
			}else{
				Toast.makeText(this, "Error setting the message", Toast.LENGTH_LONG).show();
			}
			contact.setType(menu_check);
			contact.setDate(Contact.DEFAULT_DATE);
			contact.setTime(normalize(Contact.DEFAULT_DATE.get(Calendar.HOUR_OF_DAY)) + ":" + normalize(Contact.DEFAULT_DATE.get(Calendar.MINUTE)));
			contact.setLocation(tmpLoc);
			contact.setEntering(true);
			contact.setApp(getAppToString());

			datasource.create(contact);
			contacts = datasource.findAll();

			chkServiceOn();

			addNewProximityAlert(contact);

			if(!bool) {
				Intent intent = new Intent();
				intent.putExtra("com.selfsender.messagedealer.bundleContact", contactEdit);
				setResult(-2, intent);
			}
			finish();
		}
	}

	private String normalize(int notnorm) {
		int length = String.valueOf(notnorm).length()+1;
		return ("0" + notnorm).substring(length-2);
	}

	private String getAppToString() {
		switch (view_check) {
		case 1:
			return "Sms";

		case 2:
			return "Email";

		default:
			return null;
		}
	}

	private void emailLayout() {

		Button accept;
		switch (menu_check) {
		case 1:
			RelativeLayout rl_inflate1 = (RelativeLayout) findViewById(R.id.Rl_date_gps);
			LayoutInflater inflater1 = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			viewApp = inflater1.inflate(R.layout.mail_layout, null);
			RelativeLayout.LayoutParams rlp_child1 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
			rlp_child1.addRule(RelativeLayout.ALIGN_LEFT);
			rlp_child1.addRule(RelativeLayout.BELOW, R.id.dealer_button_app);
			viewApp.setLayoutParams(rlp_child1);
			rl_inflate1.addView(viewApp);
			accept = (Button) findViewById(R.id.app_layout_accept);
			accept.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					acceptGpsDate();
				}
			});
			break;
		case 2:
			RelativeLayout rl_inflate2 = (RelativeLayout) findViewById(R.id.Rl_date);
			LayoutInflater inflater2 = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			viewApp = inflater2.inflate(R.layout.mail_layout, null);
			RelativeLayout.LayoutParams rlp_child2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
			rlp_child2.addRule(RelativeLayout.ALIGN_LEFT);
			rlp_child2.addRule(RelativeLayout.BELOW, R.id.dealer_button_app);
			viewApp.setLayoutParams(rlp_child2);
			rl_inflate2.addView(viewApp);
			accept = (Button) findViewById(R.id.app_layout_accept);
			accept.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					acceptDate();
				}
			});
			break;
		case 3:
			RelativeLayout rl_inflate3 = (RelativeLayout) findViewById(R.id.Rl_gps);
			LayoutInflater inflater3 = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			viewApp = inflater3.inflate(R.layout.mail_layout, null);
			RelativeLayout.LayoutParams rlp_child3 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
			rlp_child3.addRule(RelativeLayout.ALIGN_LEFT);
			rlp_child3.addRule(RelativeLayout.BELOW, R.id.dealer_button_app);
			viewApp.setLayoutParams(rlp_child3);
			rl_inflate3.addView(viewApp);
			accept = (Button) findViewById(R.id.app_layout_accept);
			accept.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					acceptGps();
				}
			});
			break;

		default:
			break;
		}

		RelativeLayout.LayoutParams et1params = (LayoutParams) ((EditText) findViewById(R.id.app_layout_subject)).getLayoutParams();
		et1params.addRule(RelativeLayout.BELOW, R.id.app_layout_button);
		((EditText) findViewById(R.id.app_layout_subject)).setLayoutParams(et1params);
		RelativeLayout.LayoutParams et2params = (LayoutParams) ((EditText) findViewById(R.id.app_layout_message)).getLayoutParams();
		et2params.addRule(RelativeLayout.BELOW, R.id.app_layout_subject);
		((EditText) findViewById(R.id.app_layout_message)).setLayoutParams(et2params);
		RelativeLayout.LayoutParams btnparams = (LayoutParams) ((Button) findViewById(R.id.app_layout_accept)).getLayoutParams();
		btnparams.addRule(RelativeLayout.BELOW, R.id.app_layout_message);
		((Button) findViewById(R.id.app_layout_accept)).setLayoutParams(btnparams);
		viewApp.requestFocus();

		view_check=2;
	}

	private void displayDialog() {
		new AlertDialog.Builder(this)
		.setIcon(android.R.drawable.ic_dialog_alert)
		.setTitle("Android Bug")
		.setMessage("In order to solve a known Android bug that just occurred, please disable and re-enable the \"Access to my location\" and \"Use wireless networks\" option in Location services settings. Click the \"Ok\" button to prompt those settings.")
		.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);  
				startActivity(gpsOptionsIntent);
				finish();                                   
			}
		})
		.show();
	}

	private final BroadcastReceiver androidBug = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			displayDialog();			
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.show_date_gps_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.show_date_gps:
			if(menu_check!=1) {
				chkClnMenu();
				DateGpsLayout();
			}
			return true;

		case R.id.show_date:
			if(menu_check!=2) {
				chkClnMenu();
				DateLayout();
			}
			return true;

		case R.id.show_gps:
			if(menu_check!=3) {
				chkClnMenu();
				GpsLayout();
			}
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		datasource.open();    
		Intent intent= new Intent(this, ProximityService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		contacts = datasource.findAll();
	}

	@Override
	protected void onPause() {
		super.onPause();
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
		unregisterReceiver(androidBug);
		if(serviceBind) {
			serviceBind = false;
			unbindService(mConnection);
		}
	}
}
