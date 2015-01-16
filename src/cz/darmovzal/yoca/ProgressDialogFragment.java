package cz.darmovzal.yoca;

import android.os.*;
import android.app.*;
import android.widget.*;
import android.view.*;
import android.support.v4.app.*;

public class ProgressDialogFragment extends DialogFragment {
	private int titleres, messageres;
	
	public void setArguments(int titleres, int messageres){
		Bundle b = new Bundle();
		b.putInt("titleres", titleres);
		b.putInt("messageres", messageres);
		this.setArguments(b);
	}
	
	@Override
	public void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		Bundle b = this.getArguments();
		this.titleres = b.getInt("titleres");
		this.messageres = b.getInt("messageres");
		this.setCancelable(false);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle b){
		ProgressDialog pd = new ProgressDialog(this.getActivity());
		pd.setIndeterminate(true);
		pd.setTitle(this.getActivity().getString(this.titleres));
		pd.setMessage(this.getActivity().getString(this.messageres));
		pd.setCancelable(false);
		return pd;
	}
}

