package cz.darmovzal.yoca;

import android.os.*;
import android.app.*;
import android.widget.*;
import android.view.*;
import android.support.v4.app.*;
import android.content.*;

public class AlertDialogFragment extends DialogFragment {
	private String title;
	private String message;
	private boolean confirm;
	private String id;
	
	public void setArguments(String title, String message, boolean confirm, String id){
		Bundle b = new Bundle();
		b.putString("title", title);
		b.putString("message", message);
		b.putBoolean("confirm", confirm);
		b.putString("id", id);
		this.setArguments(b);
	}
	
	@Override
	public void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		Bundle b = this.getArguments();
		this.title = b.getString("title");
		this.message = b.getString("message");
		this.confirm = b.getBoolean("confirm");
		this.id = b.getString("id");
	}
	
	@Override
	public Dialog onCreateDialog(Bundle b){
		AlertDialog.Builder builder = (new AlertDialog.Builder(this.getActivity()))
			.setTitle(this.title)
			.setMessage(this.message);
		if(confirm){
			builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface di, int which){
					click(true);
				}
			});
			builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface di, int which){
					click(false);
				}
			});
		} else {
			builder.setPositiveButton(android.R.string.ok, null);
		}
		return builder.create();
	}
	
	private void click(boolean ok){
		((CommonActivity) this.getActivity()).onConfirm(this.id, ok);
	}
}

