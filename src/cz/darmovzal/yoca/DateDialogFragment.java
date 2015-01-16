package cz.darmovzal.yoca;

import java.util.*;
import android.os.*;
import android.app.*;
import android.widget.*;
import android.view.*;
import android.support.v4.app.*;

public class DateDialogFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
	private String id;
	private int titleres;
	private Date date;
	
	public void setArguments(String id, int titleres, Date date){
		Bundle b = new Bundle();
		b.putString("id", id);
		b.putInt("titleres", titleres);
		b.putSerializable("date", date);
		this.setArguments(b);
	}
	
	public void onDateSet(DatePicker dp, int year, int month, int day){
		Calendar c = Calendar.getInstance();
		c.set(Calendar.YEAR, year);
		c.set(Calendar.MONTH, month);
		c.set(Calendar.DAY_OF_MONTH, day);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		Date date = c.getTime();
		((CommonActivity) this.getActivity()).onDateSet(this.id, date);
	}
	
	@Override
	public void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		Bundle b = this.getArguments();
		this.id = b.getString("id");
		this.titleres = b.getInt("titleres");
		this.date = (Date) b.getSerializable("date");
	}
	
	@Override
	public Dialog onCreateDialog(Bundle b){
		Calendar c = Calendar.getInstance();
		c.setTime(this.date);
		DatePickerDialog dpd = new DatePickerDialog(this.getActivity(), this, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
		dpd.setTitle(this.titleres);
		return dpd;
	}
}

