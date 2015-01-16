package cz.darmovzal.yoca.ui;

import cz.darmovzal.yoca.R;
import android.content.Context;
import android.widget.*;
import android.view.View;

public class FileView extends LinearLayout {
	private EditText edit;
	private ImageButton button;
	
	public FileView(Context ctx){
		super(ctx);
		this.setOrientation(HORIZONTAL);
		LayoutParams lp;
		
		this.edit = new EditText(ctx);
		this.edit.setBackgroundResource(R.drawable.rr_left_shadow_gray);
		lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1);
		this.addView(this.edit, lp);
		
		this.button = new ImageButton(ctx);
		this.button.setBackgroundResource(R.xml.file_button);
		
		lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, 0);
		this.addView(this.button, lp);
	}
	
	public void setOnClickListener(View.OnClickListener l){
		this.button.setOnClickListener(l);
	}
	
	public String getPath(){
		return this.edit.getText().toString();
	}
	
	public void setPath(String path){
		this.edit.setText(path);
		this.edit.setSelection(path.length());
	}
	
	public void focus(){
		this.edit.requestFocus();
	}
}

