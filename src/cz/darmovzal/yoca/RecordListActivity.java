package cz.darmovzal.yoca;

import cz.darmovzal.yoca.guts.*;
import cz.darmovzal.yoca.ui.*;

public class RecordListActivity extends CommonActivity {
	private boolean ca;
	
	public void processParams(){
		this.ca = this.getIntent().getBooleanExtra("ca", false);
	}
	
	protected String getTitleString(){
		return r(R.string.main) + " > " + r(ca ? R.string.authorities : R.string.records);
	}
	
	public void createUi(UIBuilder b){
		int i = 1;
		for(final Slot slot : this.storage().slots()){
			if(slot.isCa() != this.ca) continue;
			b.title(i++ + ". " + slot.getName())
				.slot(slot.certs().size(), slot.csrs().size(), slot.keys().hasPrivate(),
				new UIBuilder.ClickListener(){
					public void click(){
						start(RecordActivity.class, "slot", slot.id());
					}
				});
		}
		if(i == 1)
			b.text(r(R.string.no_records));
	}
}

