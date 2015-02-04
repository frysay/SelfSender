package com.selfsender;

import java.util.Random;

import com.selfsender.broadcasts.Wakerup;
import com.selfsender.structures.Contact;
import com.selfsender.structures.ContactsDataSource;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ViewMessage extends Activity {

	private final static long ONE_HOUR_IN_MILLIS = 3600000;

	public static int CONTACT_DETAIL_ACTIVITY_EDIT = 2;

	Contact contact;

	LinearLayout ll;

	String confirmation;	

	ContactsDataSource datasource;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.view_message);

		ll = (LinearLayout) findViewById(R.id.single_message);		

		datasource = new ContactsDataSource(this);

		Bundle b = getIntent().getExtras();
		contact = b.getParcelable(".selfsender.Contact");        

		refreshDisplay();
	}

	private void refreshDisplay() {	

		ImageView iv = (ImageView) findViewById(R.id.image_photo);
		iv.setImageURI(contact.getPhotoUriFromID(this, 0));

		if(iv.getDrawable() == null)
		{
			iv.setImageDrawable(setRandomIconColor());	
		}

		TextView tv_name = (TextView) findViewById(R.id.contact_text);
		tv_name.setText(contact.getName(this, 0));

		TextView tv_message = (TextView) findViewById(R.id.message_text);
		tv_message.setText(contact.getMessage());

		TextView tv_desc = (TextView) findViewById(R.id.descText);
		tv_desc.setText(contact.toString());	

	}

	private BitmapDrawable setRandomIconColor() {
		Random rnd = new Random(); 
		int color = Color.argb(40, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
		if (color == 0) {
			color = 0xffffffff;
		}

		final Resources res = getResources();
		Drawable maskDrawable = res.getDrawable(R.drawable.ic_contact_picture);
		if (!(maskDrawable instanceof BitmapDrawable)) {
			return null;
		}

		Bitmap maskBitmap = ((BitmapDrawable) maskDrawable).getBitmap();
		final int width = maskBitmap.getWidth();
		final int height = maskBitmap.getHeight();

		Bitmap outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(outBitmap);
		canvas.drawBitmap(maskBitmap, 0, 0, null);

		Paint maskedPaint = new Paint();
		maskedPaint.setColor(color);
		maskedPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));

		canvas.drawRect(0, 0, width, height, maskedPaint);

		BitmapDrawable outDrawable = new BitmapDrawable(res, outBitmap);
		return outDrawable;
	}

	private void startServiceRemove(Context context, Contact contact) {
		Intent i = new Intent (context, ProximityService.class);
		i.putExtra("com.selfsender.proximityservice.serviceCall", false);
		i.putExtra("com.selfsender.proximityservice.removeProximityAlert", contact);
		context.startService(i);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == CONTACT_DETAIL_ACTIVITY_EDIT && resultCode == -2) {
			datasource.open();
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
					startServiceRemove(this, bundleContact);
				}
				break;
			case 2:
				Wakerup wup1 = new Wakerup();
				wup1.cancelAlarm(getApplicationContext(), (int) bundleContact.getId());
				datasource.removeContact(bundleContact);
				break;
			case 3:
				startServiceRemove(this, bundleContact);
				break;

			default:
				break;
			}
			contact = datasource.findContactFromId(bundleContact.getId()+1);
		}
		refreshDisplay();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.ssm_and_vm_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.single_contact_edit:
			Intent intent = new Intent(this, MessageDealer.class);
			intent.putExtra("com.selfsender.showstoredmessages.bool", false);
			intent.putExtra("com.selfsender.showstoredmessages.edit", contact);
			startActivityForResult(intent, CONTACT_DETAIL_ACTIVITY_EDIT);
			return true;

		case R.id.single_contact_delete:
			new AlertDialog.Builder(this)
			.setIcon(android.R.drawable.ic_dialog_alert)
			.setTitle("Delete")
			.setMessage("Are you sure to delete this entity?")
			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (contact.getType()) {
					case 1:
						if(contact.getDate().getTimeInMillis() - System.currentTimeMillis() > ONE_HOUR_IN_MILLIS) {
							Wakerup wup0 = new Wakerup();
							wup0.cancelAlarm(getApplicationContext(), (int) contact.getId());
							datasource.removeContact(contact);
						}else {
							startServiceRemove(getApplicationContext(), contact);
						}
						break;
					case 2:
						Wakerup wup1 = new Wakerup();
						wup1.cancelAlarm(getApplicationContext(), (int) contact.getId());
						datasource.removeContact(contact);
						break;
					case 3:
						startServiceRemove(getApplicationContext(), contact);
						break;

					default:
						break;
					}
					finish();
				}

			})
			.setNegativeButton("No", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();					
				}

			})
			.show();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		datasource.open();    	
	}

	@Override
	protected void onPause() {
		super.onPause();
		datasource.close();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		datasource.close();
	}
}
