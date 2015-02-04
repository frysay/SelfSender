package com.selfsender.arrayadapters;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.selfsender.R;

public class AgendaArrayAdapter extends BaseAdapter {  

	LayoutInflater inflater;
	List<String> phone_mails;
	Context context;

	public AgendaArrayAdapter(Context context, List<String> phone_mails) {  
		super();

		this.context = context;
		this.phone_mails = phone_mails;
		this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override  
	public int getCount() {    
		return phone_mails.size();  
	}  

	@Override  
	public String getItem(int position) {  
		return phone_mails.get(position); 
	}  

	@Override  
	public long getItemId(int position) {
		return position;  
	}

	@Override  
	public View getView(final int position, View convertView, ViewGroup parent) {    

		String phone_mail = phone_mails.get(position);

		View vi = convertView;

		if(convertView == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			vi = inflater.inflate(R.layout.agenda_array_adapter_inflate, parent, false);
		}

		TextView tv = (TextView) vi.findViewById(R.id.phone_mail);

		tv.setText(phone_mail);  

		return vi;  
	}
}
