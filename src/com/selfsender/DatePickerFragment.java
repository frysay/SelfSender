package com.selfsender;

import java.util.GregorianCalendar;
import java.util.Locale;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.DatePicker;
import android.widget.TextView;

public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

	private int year;
	private int month;
	private int day;

	@Override
	public Dialog onCreateDialog (Bundle savedIstanceState) {
		String tmp = ((TextView) getActivity().findViewById(R.id.Date_Picker)).getText().toString();
		int index = tmp.length();
		year = Integer.valueOf(tmp.substring(index-4, index));
		month = Integer.valueOf(tmp.substring(index-7, index-5))-1;
		day = Integer.valueOf(tmp.substring(index-10, index-8));

		return new DatePickerDialog (getActivity(), this, year, month, day);
	}

	@Override
	public void onDateSet (DatePicker view, int setYear, int setMonth, int setDay) {
		String date = String.format(Locale.getDefault(),"%02d/%02d/%04d", setDay, setMonth+1, setYear);
		((TextView) getActivity().findViewById(R.id.Date_Picker)).setText(DateFormat.format("EEEE", new GregorianCalendar(setYear, setMonth, setDay)).toString() + " " + date);

	}
}
