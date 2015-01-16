package cz.darmovzal.yoca.ui;

import android.content.Context;
import android.widget.*;
import android.view.View;

public class SegmentButton extends LinearLayout {
	private ToggleButton toggle1, toggle2;
	
	public SegmentButton(Context ctx){
		super(ctx);
		this.setOrientation(HORIZONTAL);
		this.toggle1 = new ToggleButton(ctx);
		this.toggle1.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v){
				updateChecked(true);
			}
		});
		this.addView(this.toggle1, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1));
		this.toggle2 = new ToggleButton(ctx);
		this.toggle2.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v){
				updateChecked(false);
			}
		});
		this.addView(this.toggle2, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1));
		this.updateChecked(true);
	}
	
	public void setLabels(int labelres1, int labelres2){
		String label1 = this.getContext().getString(labelres1);
		String label2 = this.getContext().getString(labelres2);
		this.toggle1.setText(label1);
		this.toggle1.setTextOn(label1);
		this.toggle1.setTextOff(label1);
		this.toggle2.setText(label1);
		this.toggle2.setTextOn(label2);
		this.toggle2.setTextOff(label2);
	}
	
	private void updateChecked(boolean first){
		if(this.toggle1.isChecked() != first) this.toggle1.setChecked(first);
		if(this.toggle2.isChecked() == first) this.toggle2.setChecked(!first);
	}
}


