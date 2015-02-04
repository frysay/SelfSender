package com.selfsender.arrayadapters;

import java.util.List;
import java.util.Random;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.selfsender.R;
import com.selfsender.structures.Contact;

public class ContactArrayAdapter extends BaseAdapter {  

	LayoutInflater inflater;
	List<Contact> contacts;
	Context context;

	public ContactArrayAdapter(Context context, List<Contact> contacts) {  
		super();

		this.context = context;
		this.contacts = contacts;
		this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override  
	public int getCount() {    
		return contacts.size();  
	}  

	@Override  
	public Contact getItem(int position) {  
		return contacts.get(position); 
	}  

	@Override  
	public long getItemId(int position) {
		return position;  
	}

	@Override  
	public View getView(final int position, View convertView, ViewGroup parent) {    

		Contact contact = contacts.get(position);

		View vi = convertView;

		if(convertView == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			vi = inflater.inflate(R.layout.contact_array_adapter_inflate, parent, false);
		}

		ImageView iv = (ImageView) vi.findViewById(R.id.message_icon);
		TextView tv_name = (TextView) vi.findViewById(R.id.message_contact);
		TextView tv_message = (TextView) vi.findViewById(R.id.message_message);

		//Note: this call consider just the picture of the first contact stored in the contact list if it has one otherwise it uses the default one
		iv.setImageURI(contact.getPhotoUriFromID(context, 0));

		if(iv.getDrawable() == null)
		{
			iv.setImageDrawable(setRandomIconColor());	
		}


		//NOTA: MODIFICARE QUESTA PARTE


		//Note: this call consider just the name of the first contact stored in the contact list
		tv_name.setText(contact.getName(context, 0));

		tv_message.setText(contact.getMessage());  

		return vi;  
	}

	private BitmapDrawable setRandomIconColor() {
		Random rnd = new Random(); 
		int color = Color.argb(40, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
		if (color == 0) {
			color = 0xffffffff;
		}

		final Resources res = context.getResources();
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

}
