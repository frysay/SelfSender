package com.selfsender;

import java.util.Locale;

import com.selfsender.R;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TextView;
import android.widget.TimePicker;

public class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

	private int hour;
	private int minute;

	public int getHour() {
		return this.hour;
	}

	public int getMinute() {
		return this.minute;
	}

	@Override
	public Dialog onCreateDialog (Bundle savedIstanceState) {
		String tmp = ((TextView) getActivity().findViewById(R.id.Time_Picker)).getText().toString();
		hour = Integer.valueOf(tmp.substring(0, 2));
		minute = Integer.valueOf(tmp.substring(3));

		return new TimePickerDialog(getActivity(), this, hour, minute, DateFormat.is24HourFormat(getActivity()));
	}

	@SuppressLint("DefaultLocale")
	public void onTimeSet(TimePicker view, int setHour, int setMinute) {
		String time = String.format(Locale.getDefault(),"%02d:%02d", setHour, setMinute);
		((TextView) getActivity().findViewById(R.id.Time_Picker)).setText(time);
	}
}
