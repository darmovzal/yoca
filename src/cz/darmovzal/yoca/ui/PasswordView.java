package cz.darmovzal.yoca.ui;

import cz.darmovzal.yoca.R;
import android.content.Context;
import android.widget.*;
import android.view.View;

public class PasswordView extends LinearLayout {
	private EditText edit;
	private ToggleButton toggle;
	
	public PasswordView(Context ctx){
		super(ctx);
		this.setOrientation(HORIZONTAL);
		LayoutParams lp;
		
		this.edit = new EditText(ctx);
		// this.edit.setBackgroundResource(R.drawable.edit_left);
this.edit.setBackgroundResource(R.drawable.rr_left_shadow_gray);
		lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1);
		this.addView(this.edit, lp);
		
		this.toggle = new ToggleButton(ctx);
		this.toggle.setTextOn("");
		this.toggle.setTextOff("");
		this.toggle.setChecked(false);
		// this.toggle.setBackgroundResource(R.drawable.button_right);
this.toggle.setBackgroundResource(R.xml.eye_button);
		this.toggle.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v){
				setPasswordVisible(toggle.isChecked());
			}
		});
		lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, 0);
		this.addView(this.toggle, lp);
		
		this.setPasswordVisible(false);
	}
	
	private void setPasswordVisible(boolean visible){
		this.edit.setTransformationMethod(visible ? null : new android.text.method.PasswordTransformationMethod());
		this.edit.setSelection(this.edit.getText().length());
	}
	
	public char[] getPassword(){
		CharSequence seq = this.edit.getText();
		char[] pass = new char[seq.length()];
		for(int i = 0; i < seq.length(); i++) pass[i] = seq.charAt(i);
		return pass;
	}
	
	public void clear(){
		this.edit.setText("");
	}
	
	public void focus(){
		this.edit.requestFocus();
	}
}

