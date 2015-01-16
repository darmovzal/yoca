package cz.darmovzal.yoca.ui;

import cz.darmovzal.yoca.*;
import java.util.*;
import java.text.*;
import android.view.*;
import android.widget.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.content.Context;
import android.util.Log;
import android.os.Bundle;

abstract class UIItem extends LinearLayout {
	protected String id;
	protected TextView title;
	protected GestureDetector gd;
	protected UIBuilder builder;
	protected LinearLayout ll;
	protected ImageView icon;
	
	protected UIItem(Context ctx){
		super(ctx);
		this.setOrientation(LinearLayout.HORIZONTAL);
		
		this.ll = new LinearLayout(ctx);
		this.ll.setOrientation(LinearLayout.VERTICAL);
		// this.ll.setGravity(Gravity.CENTER_VERTICAL);
		this.ll.setPadding(10, 0, 10, 10);
		this.addView(this.ll, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		
		this.title = new TextView(ctx);
		this.title.setPadding(0, 0, 0, 5);
		this.title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
		// this.title.setMaxLines(1);
		this.title.setTextColor(Color.DKGRAY);
		this.ll.addView(this.title, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
	}
	
	public void setIcon(int res){
		if(this.icon == null){
			this.icon = new ImageView(this.getContext());
			this.addView(this.icon, 0, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT));
		}
		this.icon.setImageResource(res);
		this.icon.setPadding(0, 0, 10, 0);
	}
	
	protected abstract void init();
	
	protected void onSingleClick(){}
	
	protected void initGd(){
		this.gd = new GestureDetector(this.getContext(), new GestureDetector.SimpleOnGestureListener(){
			@Override
			public boolean onDown(MotionEvent me){
				return true;
			}
			@Override
			public boolean onSingleTapUp(MotionEvent me){
				onSingleClick();
				return true;
			}
		}){
			@Override
			public boolean onTouchEvent(MotionEvent me){
				switch(me.getActionMasked()){
				case MotionEvent.ACTION_DOWN:
					setPressed(true);
					break;
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
				case MotionEvent.ACTION_OUTSIDE:
					setPressed(false);
					break;
				}
				return super.onTouchEvent(me);
			}
		};
	}
	
	public void setBuilder(UIBuilder builder){
		this.builder = builder;
	}
	
	public void setId(String id){
		this.id = id;
	}
	
	public void setTitleRes(int titleres){
		this.title.setText(titleres);
	}
	
	public void setTitle(String title){
		this.title.setText(title);
	}
	
	public void setHighlight(boolean on){
		this.title.setTextColor(on ? 0xff9d0000 : Color.DKGRAY);
	}
	
	protected void add(View view){
		LinearLayout.LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		this.ll.addView(view, lp);
	}
	
	public void setTitleVisible(boolean on){
		this.title.setVisibility(on ? VISIBLE : GONE);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent me){
		if(this.gd != null){
			return this.gd.onTouchEvent(me);
		} else {
			return super.onTouchEvent(me);
		}
	}
	
	public void focus(){}
	
	public void save(Bundle b){}
	public void restore(Bundle b){}
}

class UISeparator extends View {
	public UISeparator(Context ctx){
		super(ctx);
		this.setBackgroundResource(R.drawable.separator);
	}
}

class UIEdit extends UIItem {
	private EditText edit;
	
	public UIEdit(Context ctx){
		super(ctx);
	}
	
	protected void init(){
		this.edit = new EditText(this.getContext());
		this.edit.setTextSize(18);
		this.edit.setTextColor(Color.BLACK);
		this.edit.setBackgroundResource(R.drawable.rr_whole_shadow);
		this.add(this.edit);
	}
	
	public String getText(){
		return this.edit.getText().toString();
	}
	
	public void setText(String text){
		this.edit.setText(text);
	}
	
	@Override
	public void focus(){
		this.edit.requestFocus();
	}
	
	@Override
	public void save(Bundle b){
		b.putString(this.id, this.getText());
	}
	
	@Override
	public void restore(Bundle b){
		String s = b.getString(this.id);
		if(s != null) this.setText(s);
	}
}

class UIText extends UIItem {
	private TextView text;
	
	public UIText(Context ctx){
		super(ctx);
	}
	
	protected void init(){
		this.text = new TextView(this.getContext());
		this.text.setTextColor(Color.BLACK);
		this.add(this.text);
	}
	
	public void setTextRes(int textres){
		this.text.setTextSize(18);
		this.text.setText(textres);
	}
	
	public void setText(String text){
		this.text.setTextSize(18);
		this.text.setText(text);
	}
	
	public void setHtmlRes(int htmlres){
		this.text.setTextSize(14);
		String html = this.getContext().getString(htmlres);
		this.text.setText(android.text.Html.fromHtml(html), TextView.BufferType.SPANNABLE);
	}
}

class UIImage extends UIItem {
	public UIImage(Context ctx){
		super(ctx);
	}
	
	protected void init(){}
	
	public void setImageRes(int imageres){
		this.setBackgroundResource(imageres);
	}
}

class UIButton extends UIItem {
	protected Button button;
	
	public UIButton(Context ctx){
		super(ctx);
	}
	
	protected void init(){
		this.button = new Button(this.getContext());
		this.button.setBackgroundResource(R.xml.button);
		this.add(this.button);
	}
	
	public void setTextRes(int textres){
		this.button.setText(textres);
	}
	
	public void setClickListener(final UIBuilder.ClickListener l){
		this.button.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v){
				l.click();
			}
		});
	}
}

class UIPassword extends UIItem {
	private PasswordView password;
	
	public UIPassword(Context ctx){
		super(ctx);
	}
	
	protected void init(){
		this.password = new PasswordView(this.getContext());
		this.add(this.password);
	}
	
	public char[] getPassword(){
		return this.password.getPassword();
	}
	
	public void clear(){
		this.password.clear();
	}
	
	@Override
	public void focus(){
		this.password.focus();
	}
}

class UISButton extends UIItem {
	private SegmentButton sb;
	
	public UISButton(Context ctx){
		super(ctx);
	}
	
	protected void init(){
		this.sb = new SegmentButton(this.getContext());
		this.add(this.sb);
	}
	
	public void setLabelsRes(int labelres1, int labelres2){
		this.sb.setLabels(labelres1, labelres2);
	}
}

class UISlot extends UIItem {
	private LinearLayout ll;
	private UIBuilder.ClickListener listener;
	
	public UISlot(Context ctx){
		super(ctx);
		this.initGd();
	}
	
	protected void init(){
		this.setBackgroundResource(R.xml.link);
		this.ll = new LinearLayout(this.getContext());
		this.ll.setOrientation(LinearLayout.HORIZONTAL);
		this.ll.setGravity(Gravity.RIGHT);
		this.add(this.ll);
	}
	
	public void addIcon(int imageres){
		ImageView iv = new ImageView(this.getContext());
		iv.setImageResource(imageres);
		this.ll.addView(iv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));
	}
	
	public void setClickListener(UIBuilder.ClickListener listener){
		this.listener = listener;
	}
	
	@Override
	protected void onSingleClick(){
		if(this.listener == null) return;
		this.listener.click();
	}
}

class UISelect extends UIItem {
	private boolean exclusive;
	private List<CompoundButton> buttons;
	private LinearLayout ll;
	private UIBuilder.ClickListener l;
	private boolean vertical;
	
	public UISelect(boolean exclusive, boolean vertical, Context ctx){
		super(ctx);
		this.buttons = new ArrayList<CompoundButton>();
		this.exclusive = exclusive;
		this.vertical = vertical;
	}
	
	protected void init(){
		this.ll = new LinearLayout(this.getContext());
		this.ll.setOrientation(this.vertical ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
		this.add(this.ll);
	}
	
	public void setClickListener(UIBuilder.ClickListener l){
		this.l = l;
	}
	
	public void addItem(String title){
		CompoundButton cb;
		if(this.exclusive){
			cb = new RadioButton(this.getContext());
			cb.setButtonDrawable(R.xml.radio);
			cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
				public void onCheckedChanged(CompoundButton cb, boolean isChecked){
					if(!isChecked) return;
					for(CompoundButton _cb : buttons){
						if(_cb != cb) _cb.setChecked(false);
					}
					if(l != null) l.click();
				}
			});
			if(this.buttons.isEmpty()) cb.setChecked(true);
		} else {
			cb = new CheckBox(this.getContext());
			cb.setButtonDrawable(R.xml.check);
		}
		this.buttons.add(cb);
		cb.setTextSize(18);
		cb.setTextColor(Color.BLACK);
		if(this.vertical) cb.setPadding(cb.getPaddingLeft() + 20, 0, 0, 0);
		cb.setText(title);
		this.ll.addView(cb, new LinearLayout.LayoutParams(this.vertical ? LinearLayout.LayoutParams.MATCH_PARENT : LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
	}
	
	public int getChecked(){
		if(!this.exclusive) throw new IllegalStateException("Do not call getChecked() in non-exclusive mode");
		for(int i = 0; i < this.buttons.size(); i++){
			if(this.buttons.get(i).isChecked()) return i;
		}
		throw new IllegalStateException("No radio is checked");
	}
	
	public boolean isChecked(int index){
		return this.buttons.get(index).isChecked();
	}
	
	public void setEnabled(int index, boolean enabled){
		CompoundButton cb = this.buttons.get(index);
		cb.setVisibility(enabled ? View.VISIBLE : View.GONE);
		if(!this.exclusive) return;
		if(!cb.isChecked()) return;
		for(CompoundButton _cb : this.buttons){
			if(_cb.getVisibility() == View.VISIBLE){
				_cb.setChecked(true);
				break;
			}
		}
	}
	
	@Override
	public void save(Bundle b){
		boolean[] checks = new boolean[this.buttons.size()];
		for(int i = 0; i < checks.length; i++)
			checks[i] = this.buttons.get(i).isChecked();
		b.putBooleanArray(this.id, checks);
	}
	
	@Override
	public void restore(Bundle b){
		boolean[] checks = b.getBooleanArray(this.id);
		if(checks == null) return;
		if(checks.length != this.buttons.size()) return;
		for(int i = 0; i < checks.length; i++)
			this.buttons.get(i).setChecked(checks[i]);
	}
}

class UIFile extends UIItem {
	private FileView fv;
	
	public UIFile(Context ctx){
		super(ctx);
	}
	
	public void init(){
		this.fv = new FileView(this.getContext());
		this.add(fv);
	}
	
	public void setPath(String path){
		this.fv.setPath(path);
	}
	
	public String getPath(){
		return this.fv.getPath();
	}
	
	public void setClickListener(final UIBuilder.ClickListener l){
		this.fv.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v){
				l.click();
			}
		});
	}
	
	@Override
	public void focus(){
		this.fv.focus();
	}
	
	@Override
	public void save(Bundle b){
		b.putString(this.id, this.getPath());
	}
	
	@Override
	public void restore(Bundle b){
		String path = b.getString(this.id);
		if(path != null) this.setPath(path);
	}
}

class UIGroup extends UIItem {
	private String name;
	private String text;
	private List<View> children;
	
	public UIGroup(Context ctx, String name, String text){
		super(ctx);
		this.name = name;
		this.text = text;
		this.children = new ArrayList<View>();
		this.initGd();
	}
	
	public void addChild(View child){
		this.children.add(child);
	}
	
	protected void init(){
		if(this.text != null){
			TextView tv = new TextView(this.getContext());
			tv.setTextColor(Color.BLACK);
			tv.setTextSize(18);
			tv.setText(this.text);
			this.add(tv);
		}
		this.setBackgroundResource(R.xml.group_open);
	}
	
	@Override	
	protected void onSingleClick(){
		this.builder.switchGroup(this.name);
	}
	
	public void setState(boolean open){
		for(View child : this.children)
			child.setVisibility(open ? View.VISIBLE : View.GONE);
		this.setBackgroundResource(open ? R.xml.group_close : R.xml.group_open);
	}
	
	public boolean getState(){
		for(View child : this.children){
			if(child.getVisibility() == View.VISIBLE) return true;
		}
		return false;
	}
	
	public void switchState(){
		this.setState(!this.getState());
	}
	
	public UISeparator removeLastSeparator(){
		if(this.children.size() == 0) return null;
		View last = this.children.get(this.children.size() - 1);
		if(!(last instanceof UISeparator)) return null;
		this.children.remove(last);
		return (UISeparator) last;
	}
	
	@Override
	public void save(Bundle b){
		b.putBoolean(this.name, this.getState());
	}
	
	@Override
	public void restore(Bundle b){
		this.setState(b.getBoolean(this.name, false));
	}
}

class UILink extends UIItem {
	private UIBuilder.ClickListener listener;
	
	public UILink(Context ctx){
		super(ctx);
		this.initGd();
	}
	
	public void init(){
		this.setBackgroundResource(R.xml.link);
	}
	
	public void setClickListener(UIBuilder.ClickListener listener){
		this.listener = listener;
	}
	
	@Override
	protected void onSingleClick(){
		if(this.listener == null) return;
		this.listener.click();
	}
}

class UIDate extends UIButton {
	private Date date;
	
	public UIDate(Context ctx){
		super(ctx);
	}
	
	public void init(){
		super.init();
		this.button.setBackgroundResource(R.drawable.rr_whole_shadow);
	}
	
	public void setDate(Date date){
		DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);
		this.date = date;
		this.button.setText(df.format(date));
	}
	
	public Date getDate(){
		return this.date;
	}
	
	@Override
	public void save(Bundle b){
		if(this.date != null)
			b.putLong(this.id, this.date.getTime());
	}
	
	@Override
	public void restore(Bundle b){
		long d = b.getLong(this.id, 0);
		if(d != 0) this.setDate(new Date(d));
	}
}

public class UIBuilder {
	private static String LTAG = "YOCA:UIBuilder";
	private static final int GROUP_INDENT = 60;
	
	public interface ClickListener {
		public void click();
	}
	
	private Context c;
	private LinearLayout ll;
	private int titleres;
	private String title;
	private int icon;
	private String id;
	private Map<String, UIItem> items;
	private Map<String, UIGroup> groups;
	private UIGroup group;
	private boolean separator;
	private int scrollPosition;
	private ScrollView scroll;
	private float density;
	private boolean highlight;
	
	public UIBuilder(Context c, float density){
		this.c = c;
		this.density = density;
		this.items = new HashMap<String, UIItem>();
		this.groups = new HashMap<String, UIGroup>();
		this.clear();
	}
	
	public UIBuilder separator(){
		if(this.separator == true) return this;
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		if(this.group != null) lp.setMargins((int)(GROUP_INDENT * this.density), 0, 0, 0);
		UISeparator sep = new UISeparator(this.c);
		this.ll.addView(sep, lp);
		this.separator = true;
		if(this.group != null) this.group.addChild(sep);
		return this;
	}
	
	public UIBuilder highlight(boolean on){
		this.highlight = on;
		return this;
	}
	
	public UIBuilder title(int titleres){
		this.titleres = titleres;
		return this;
	}
	
	public UIBuilder title(String title){
		this.title = title;
		return this;
	}
	
	public UIBuilder icon(int res){
		this.icon = res;
		return this;
	}
	
	public UIBuilder text(int textres, boolean html){
		UIText text = new UIText(this.c);
		text.init();
		if(html){
			text.setHtmlRes(textres);
		} else {
			text.setTextRes(textres);
		}
		return this.add(text);
	}
	
	public UIBuilder text(String text){
		UIText _text = new UIText(this.c);
		_text.init();
		_text.setText(text);
		return this.add(_text);
	}
	
	public UIBuilder button(int textres, ClickListener l){
		UIButton button = new UIButton(this.c);
		button.init();
		button.setTextRes(textres);
		button.setClickListener(l);
		return this.add(button);
	}
	
	public UIBuilder image(int imageres){
		UIImage image = new UIImage(this.c);
		image.init();
		image.setImageRes(imageres);
		image.setPadding(20, 20, 20, 20);
		return this.add(image);
	}
	
	public UIBuilder edit(String text){
		UIEdit edit = new UIEdit(this.c);
		edit.init();
		if(text != null) edit.setText(text);
		return this.add(edit);
	}
	
	public UIBuilder password(){
		UIPassword password = new UIPassword(this.c);
		password.init();
		return this.add(password);
	}
	
	public UIBuilder sbutton(int labelres1, int labelres2){
		UISButton sb = new UISButton(this.c);
		sb.init();
		sb.setLabelsRes(labelres1, labelres2);
		return this.add(sb);
	}
	
	public UIBuilder slot(int certs, int csrs, boolean priv, ClickListener l){
		UISlot slot = new UISlot(this.c);
		slot.init();
		if(certs > 2){
			slot.addIcon(R.drawable.certs);
		} else {
			for(int i = 0; i < certs; i++) slot.addIcon(R.drawable.cert);
		}
		if(csrs > 2){
			slot.addIcon(R.drawable.csrs);
		} else {
			for(int i = 0; i < csrs; i++) slot.addIcon(R.drawable.csr);
		}
		if(priv) slot.addIcon(R.drawable.privkey);
		slot.addIcon(R.drawable.pubkey);
		slot.setClickListener(l);
		return this.separator().add(slot).separator();
	}
	
	public UIBuilder select(boolean exclusive, String ... items){
		return this.select(exclusive, items, null);
	}
	
	public UIBuilder select(boolean exclusive, Collection<String> items){
		return this.select(exclusive, items.toArray(new String[items.size()]));
	}
	
	public UIBuilder select(boolean exclusive, String[] items, ClickListener l){
		UISelect select = new UISelect(exclusive, true, this.c);
		select.init();
		for(String item : items) select.addItem(item);
		select.setClickListener(l);
		return this.add(select);
	}
	
	public UIBuilder file(String path, ClickListener l){
		UIFile file = new UIFile(this.c);
		file.init();
		file.setPath(path);
		file.setClickListener(l);
		return this.add(file);
	}
	
	public UIBuilder link(ClickListener l){
		UILink link = new UILink(this.c);
		link.init();
		//link.setText(text);
		link.setClickListener(l);
		return this.separator().add(link).separator();
	}
	
	public UIBuilder date(Date date, ClickListener l){
		UIDate d = new UIDate(this.c);
		d.init();
		d.setDate(date);
		d.setClickListener(l);
		return this.add(d);
	}
	
	private UIBuilder add(UIItem item){
		if(this.id != null){
			item.setId(this.id);
			this.items.put(this.id, item);
		}
		this.id = null;
		if(this.title != null){
			item.setTitle(this.title);
		} else if(this.titleres != 0){
			item.setTitleRes(this.titleres);
		} else {
			item.setTitleVisible(false);
		}
		item.setHighlight(this.highlight);
		this.highlight = false;
		if(this.icon != 0)
			item.setIcon(this.icon);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		if(this.group != null) lp.setMargins((int)(GROUP_INDENT * this.density), 0, 0, 0);
		this.ll.addView(item, lp);
		this.separator = false;
		this.title = null;
		this.titleres = 0;
		this.icon = 0;
		if(this.group != null) this.group.addChild(item);
		item.setBuilder(this);
		return this;
	}
	
	public UIBuilder groupBegin(String name){
		return this.groupBegin(name, null);
	}
	
	public UIBuilder groupBegin(String name, String text){
		if(this.group != null) throw new IllegalStateException("Group already bagun");
		if(this.groups.containsKey(name)) throw new IllegalArgumentException("Group name \"" + name + "\" already defined");
		this.separator();
		UIGroup g = new UIGroup(this.c, name, text);
		g.init();
		this.add(g);
		this.groups.put(name, g);
		this.group = g;
		return this;
	}
	
	public UIBuilder groupEnd(){
		return this.groupEnd(false);
	}
	
	public UIBuilder groupEnd(boolean state){
		if(this.group == null) throw new IllegalStateException("No group begun");
		this.group.setState(state);
		UISeparator lastSep = this.group.removeLastSeparator();
		if(lastSep != null) this.ll.removeView(lastSep);
		this.separator = false;
		this.group = null;
		this.separator();
		return this;
	}
	
	public void switchGroup(String name){
		UIGroup group = this.groups.get(name);
		if(group == null) throw new IllegalArgumentException("Group \"" + name + "\" not found");
		group.switchState();
	}
	
	public void clear(){
		this.items.clear();
		this.groups.clear();
		this.group = null;
		this.id = null;
		this.title = null;
		this.titleres = 0;
		this.ll = new LinearLayout(this.c);
		this.ll.setOrientation(LinearLayout.VERTICAL);
		this.scroll = new ScrollView(this.c){
			public void onScrollChanged(int l, int t, int oldl, int oldt){
				super.onScrollChanged(l, t, oldl, oldt);
				scrollPosition = t;
			}
		};
		this.scroll.setBackgroundColor(Color.WHITE);
		this.scroll.addView(this.ll);
	}
	
	public UIBuilder id(String id){
		if(this.items.containsKey(id))
			throw new IllegalArgumentException("Item with id \"" + id + "\" already exists in builder");
		this.id = id;
		return this;
	}
	
	private UIItem get(String id){
		UIItem item = this.items.get(id);
		if(item == null) throw new IllegalArgumentException("Cannot find item by id \"" + id + "\"");
		return item;
	}
	
	public View view(){
		return this.scroll;
	}
	
	public void focus(String id){
		this.get(id).focus();
	}
	
	public int getSelectValue(String id){
		return ((UISelect) this.get(id)).getChecked();
	}
	
	public boolean isSelectChecked(String id, int index){
		return ((UISelect) this.get(id)).isChecked(index);
	}
	
	public char[] getPasswordValue(String id){
		return ((UIPassword) this.get(id)).getPassword();
	}
	
	public String getEditValue(String id){
		return ((UIEdit) this.get(id)).getText();
	}
	
	public void setEditValue(String id, String text){
		((UIEdit) this.get(id)).setText(text);
	}
	
	public Date getDateValue(String id){
		return ((UIDate) this.get(id)).getDate();
	}
	
	public void setDateValue(String id, Date date){
		((UIDate) this.get(id)).setDate(date);
	}
	
	public void setTextValue(String id, String text){
		((UIText) this.get(id)).setText(text);
	}
	
	public void clearPassword(String id){
		((UIPassword) this.get(id)).clear();
	}
	
	public String getFileValue(String id){
		return ((UIFile) this.get(id)).getPath();
	}
	
	public void setFileValue(String id, String path){
		((UIFile) this.get(id)).setPath(path);
	}
	
	public void setSelectEnabled(String id, int index, boolean enabled){
		((UISelect) this.get(id)).setEnabled(index, enabled);
	}
	
	public void save(Bundle b){
		for(UIItem item : this.items.values()) item.save(b);
		for(UIGroup group : this.groups.values()) group.save(b);
		b.putInt("_scroll", this.scrollPosition);
	}
	
	public void restore(Bundle b){
		for(UIItem item : this.items.values()) item.restore(b);
		for(UIGroup group : this.groups.values()) group.restore(b);
		final int _scroll = b.getInt("_scroll", 0);
		this.scroll.post(new Runnable() {
			public void run() {
				scroll.scrollTo(0, _scroll);
			} 
		});
	}
}

