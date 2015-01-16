package cz.darmovzal.yoca;

import java.io.File;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.*;
import java.text.MessageFormat;
import android.app.Activity;
import android.view.*;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.os.Bundle;
import android.content.Intent;
import android.content.Context;
import android.text.ClipboardManager;
import cz.darmovzal.yoca.guts.*;
import cz.darmovzal.yoca.ui.*;
import android.os.Environment;
import android.util.Log;
import android.util.DisplayMetrics;

public abstract class CommonActivity extends android.support.v4.app.FragmentActivity {
	protected UIBuilder builder;
	private Bundle restoreBundle;
	
	@Override
	public void onCreate(Bundle b){
		super.onCreate(b);
		// this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		this.processParams();
		DisplayMetrics dm = new DisplayMetrics();
		this.getWindowManager().getDefaultDisplay().getMetrics(dm);
		this.builder = new UIBuilder(this, dm.density);
	}
	
	@Override
	public void onResume(){
		super.onResume();
		this.builder.clear();
		this.createUi(this.builder);
		if(this.restoreBundle != null) this.builder.restore(this.restoreBundle);
		this.restoreBundle = null;
		this.setTitle(this.getTitleString());
		this.setContentView(this.builder.view());
	}
	
	@Override
	public void onSaveInstanceState(Bundle b){
		super.onSaveInstanceState(b);
		Bundle bb = new Bundle();
		this.builder.save(bb);
		b.putBundle("UIBuilder", bb);
	}
	
	@Override
	public void onRestoreInstanceState(Bundle b){
		super.onRestoreInstanceState(b);
		this.restoreBundle = b.getBundle("UIBuilder");
	}
	
	@Override
	public void onPause(){
		super.onPause();
	}
	
	@Override
	public void onStop(){
		super.onStop();
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		super.onCreateOptionsMenu(menu);
		menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.settings)
			.setIcon(android.R.drawable.ic_menu_preferences)
			.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener(){
				public boolean onMenuItemClick(MenuItem menuItem){
					start(SettingsActivity.class);
					return true;
				}
			});
		return true;
	}
	
	protected void updateUi(){
		this.builder.clear();
		this.createUi(this.builder);
		this.setContentView(this.builder.view());
	}
	
	protected abstract String getTitleString();
	
	protected Storage storage(){
		return ((App) this.getApplicationContext()).storage();
	}
	
	public abstract void processParams();
	
	public abstract void createUi(UIBuilder b);
	
	protected void start(Class<? extends Activity> activity, Object ... params){
		Intent intent = new Intent(this, activity);
		if(params.length % 2 != 0) throw new RuntimeException("Odd count of params");
		for(int i = 0; i < params.length / 2; i++){
			String name = (String) params[i * 2];
			Serializable value = (Serializable) params[i * 2 + 1];
			intent.putExtra(name, value);
		}
		this.startActivity(intent);
		this.overridePendingTransition(R.anim.r2l_in, R.anim.r2l_out);
	}
	
	public void finish(){
		super.finish();
		this.overridePendingTransition(R.anim.l2r_in, R.anim.l2r_out);
	}
	
	protected String toHex(BigInteger bi, int br){
		return this.toHex(bi.toByteArray(), br, true);
	}
	
	protected String toHex(byte[] bytes, int br, boolean ignoreFirstZero){
		if(bytes == null) throw new NullPointerException("bytes cannot be null");
		char[] digits = "0123456789ABCDEF".toCharArray();
		StringBuilder sb = new StringBuilder();
		int count = 0;
		for(int i = 0; i < bytes.length; i++){
			int b = bytes[i] & 0xff;
			if(ignoreFirstZero && (i == 0) && (b == 0) && (bytes.length > 1)) continue;
			if(sb.length() > 0) sb.append(count % br == 0 ? '\n' : ':');
			sb.append(digits[b / 16]).append(digits[b % 16]);
			count++;
		}
		return sb.toString();
	}
	
	protected byte[] fromHex(String hex){
		hex = hex.replaceAll("\\s", "").replaceAll(":", "").toUpperCase();
		String digits = "0123456789ABCDEF";
		if(hex.length() % 2 != 0) throw new RuntimeException("Odd hex string length");
		byte[] ret = new byte[hex.length() / 2];
		int value = 0;
		for(int i = 0; i < hex.length(); i++){
			char c = hex.charAt(i);
			int digit = digits.indexOf(c);
			if(digit < 0) throw new RuntimeException("Wrong hex character found: " + (int) c + " " + c);
			if(i % 2 == 0){
				value = digit;
			} else {
				ret[i / 2] = (byte)(value * 16 + digit);
			}
		}
		return ret;
	}
	
	protected void showDialog(android.support.v4.app.DialogFragment df, String tag){
		df.show(this.getSupportFragmentManager(), tag);
	}
	
	protected void dismissDialog(String tag){
		((android.support.v4.app.DialogFragment) this.getSupportFragmentManager().findFragmentByTag(tag)).dismiss();
	}
	
	protected void showProgressDialog(int messageres){
		ProgressDialogFragment pdf = new ProgressDialogFragment();
		pdf.setArguments(R.string.please_wait, messageres);
		this.showDialog(pdf, "progress");
	}
	
	protected void dismissProgressDialog(){
		this.dismissDialog("progress");
	}
	
	protected void showErrorDialog(int messageres){
		this.showAlertDialog(R.string.error, messageres, false, null);
	}
	
	protected void showInfoDialog(int messageres){
		this.showAlertDialog(R.string.info, messageres, false, null);
	}
	
	protected void showInfoDialog(String message){
		this.showAlertDialog(this.r(R.string.info), message, false, null);
	}
	
	protected void showConfirmDialog(String id, String message){
		this.showAlertDialog(this.r(R.string.confirmation), message, true, id);
	}
	
	protected void showAlertDialog(int titleres, int messageres, boolean confirm, String id){
		this.showAlertDialog(this.r(titleres), this.r(messageres), confirm, id);
	}
	
	protected void showAlertDialog(String title, String message, boolean confirm, String id){
		AlertDialogFragment adf = new AlertDialogFragment();
		adf.setArguments(title, message, confirm, id);
		this.showDialog(adf, "alert");
	}
	
	protected void showDateDialog(String id, int titleres, Date date){
		DateDialogFragment ddf = new DateDialogFragment();
		ddf.setArguments(id, titleres, date);
		this.showDialog(ddf, "date");
	}
	
	protected void showFileDialog(String id, int titleres, String path, boolean fileOnly, boolean showHidden, String[] allowSuffixes){
		FileDialogFragment fdf = new FileDialogFragment();
		fdf.setArguments(id, titleres, path, fileOnly, showHidden, allowSuffixes);
		this.showDialog(fdf, "file");
	}
	
	protected void dismissFileDialog(){
		this.dismissDialog("file");
	}
	
	public void onDateSet(String id, Date date){
		this.builder.setDateValue(id, date);
	}
	
	public void onFileSelected(String id, String path){
		this.builder.setFileValue(id, path);
		this.dismissFileDialog();
	}
	
	public void onConfirm(String id, boolean ok){}
	
	protected void createNameUi(UIBuilder b, Name name){
		for(int i = 0; i < name.count(); i++){
			String oid = name.oid(i);
			if("CN".equals(oid)){
				b.title(R.string.common_name);
			} else if("O".equals(oid)){
				b.title(R.string.organization);
			} else if("OU".equals(oid)){
				b.title(R.string.organizational_unit);
			} else if("L".equals(oid)){
				b.title(R.string.locality);
			} else if("ST".equals(oid)){
				b.title(R.string.state);
			} else if("C".equals(oid)){
				b.title(R.string.country);
			} else {
				b.title(oid);
			}
			b.text(name.value(i));
		}
	}
	
	protected void createExtensionPresentationUi(final UIBuilder b, Exts exts){
		if(exts == null) return;
		if(exts.size() == 0) return;
		b.title(R.string.extensions).icon(R.drawable.icon_extensions).groupBegin("extensions");
		exts.traverse(new Exts.Listener(){
			private void title(int titleres, boolean critical){
				b.title(r(titleres) + (critical ? " (" + r(R.string.critical) + ")" : ""));
			}
			public void basicConstraints(boolean critical, boolean ca, int pathlen){
				this.title(R.string.basic_constraints, critical);
				b.text(ca ? (r(R.string.is_certificate_authority) + "\n" + r(R.string.path_length) + ": " + pathlen) : r(R.string.is_not_certificate_authority));
			}
			public void keyUsage(boolean critical, Exts.BasicKeyUsage[] values){
				this.title(R.string.key_usage, critical);
				StringBuilder sb = new StringBuilder();
				for(int i = 0; i < values.length; i++){
					if(i > 0) sb.append('\n');
					sb.append(values[i].title());
				}
				b.text(sb.toString());
			}
			public void extKeyUsage(boolean critical, Exts.ExtKeyUsage[] values){
				this.title(R.string.extended_key_usage, critical);
				StringBuilder sb = new StringBuilder();
				for(int i = 0; i < values.length; i++){
					if(i > 0) sb.append('\n');
					sb.append(values[i].title());
				}
				b.text(sb.toString());
			}
			public void subjectAltNames(boolean critical, Exts.SubjectAltName[] names){
				this.title(R.string.subject_alternative_names, critical);
				if((names != null) && (names.length > 0)){
					for(Exts.SubjectAltName name : names)
						b.text(name.tag().title() + ": " + name.name());
				} else {
					b.text(r(R.string.empty));
				}
			}
			public void unknown(boolean critical, String oid){
				this.title(R.string.unknown, critical);
				b.text("OID: " + oid);
			}
		});
		b.groupEnd(true);
	}
	
	protected void createExtensionEditUi(UIBuilder b){
		List<String> subjectAltNameTags = new ArrayList<String>();
		for(Exts.SubjectAltNameTag tag : Exts.SubjectAltNameTag.values()) subjectAltNameTags.add(tag.title());
		b.title(R.string.subject_alternative_name);
		b.id("ext_subj_alt_name").edit("");
		b.id("ext_subj_alt_name_tag").select(true, subjectAltNameTags);
		
		List<String> keyUsages = new ArrayList<String>();
		for(Exts.BasicKeyUsage ku : Exts.BasicKeyUsage.values()) keyUsages.add(ku.title());
		b.id("ext_key_usage").title(R.string.key_usage).select(false, keyUsages);
		
		List<String> extKeyUsages = new ArrayList<String>();
		for(Exts.ExtKeyUsage eku : Exts.ExtKeyUsage.values()) extKeyUsages.add(eku.title());
		b.id("ext_ext_key_usage").title(R.string.extended_key_usage).select(false, extKeyUsages);
	}
	
	protected void processExtensionEditUi(Exts exts){
		String subjectAltName = builder.getEditValue("ext_subj_alt_name");
		int tagindex = builder.getSelectValue("ext_subj_alt_name_tag");
		Exts.SubjectAltNameTag tag = Exts.SubjectAltNameTag.values()[tagindex];
		if(subjectAltName.trim().length() > 0)
			exts.addSubjectAltNames(false, new Exts.SubjectAltName[]{ new Exts.SubjectAltName(tag, subjectAltName) });
		
		List<Exts.BasicKeyUsage> keyUsages = new ArrayList<Exts.BasicKeyUsage>();
		int i = 0;
		for(Exts.BasicKeyUsage ku : Exts.BasicKeyUsage.values()){
			if(builder.isSelectChecked("ext_key_usage", i++)) keyUsages.add(ku);
		}
		if(keyUsages.size() > 0) exts.addKeyUsage(true, keyUsages.toArray(new Exts.BasicKeyUsage[keyUsages.size()]));
		
		List<Exts.ExtKeyUsage> extKeyUsages = new ArrayList<Exts.ExtKeyUsage>();
		i = 0;
		for(Exts.ExtKeyUsage eku : Exts.ExtKeyUsage.values()){
			if(builder.isSelectChecked("ext_ext_key_usage", i++)) extKeyUsages.add(eku);
		}
		if(extKeyUsages.size() > 0) exts.addExtKeyUsage(true, extKeyUsages.toArray(new Exts.ExtKeyUsage[extKeyUsages.size()]));
	}
	
	protected String getExportPath(){
		String state = Environment.getExternalStorageState();
		if(Environment.MEDIA_MOUNTED.equals(state))
			return Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
		return "/";
	}
	
	protected String getImportPath(){
		String state = Environment.getExternalStorageState();
		if(Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
			return Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
		return "/";
	}
	
	protected boolean checkExportPath(String path){
		File file = new File(path);
		if(file.isDirectory()){
			this.showErrorDialog(R.string.please_enter_file_name);
			return false;
		}
		/*
		if(!file.canWrite()){
			this.showErrorDialog(R.string.cannot_write_into_file);
			return false;
		}
		*/
		return true;
	}
	
	protected boolean checkImportPath(String path){
		File file = new File(path);
		if(file.isDirectory()){
			this.showErrorDialog(R.string.please_enter_file_name);
			return false;
		}
		if(!file.canRead()){
			this.showErrorDialog(R.string.cannot_read_from_file);
			return false;
		}
		return true;
	}
	
	protected boolean checkPassphrase(char[] pass){
		if(pass.length == 0){
			this.showErrorDialog(R.string.please_do_not_leave_passphrase_blank);
			return false;
		}
		return true;
	}
	
	protected boolean checkName(String name){
		if(name.trim().length() == 0){
			this.showErrorDialog(R.string.please_enter_name);
			return false;
		}
		return true;
	}
	
	protected boolean checkCommonName(String cn){
		if(cn.trim().length() == 0){
			this.showErrorDialog(R.string.please_do_not_leave_common_name_blank);
			return false;
		}
		return true;
	}
	
	protected boolean checkValidity(Date notBefore, Date notAfter){
		if(!notAfter.after(notBefore)){
			this.showErrorDialog(R.string.validity_end_precedes_validity_begin);
			return false;
		}
		return true;
	}
	
	protected String r(int resid, Object ... args){
		return MessageFormat.format(this.getString(resid), args);
	}
	
	protected String toCertString(Cert cert){
		return cert.subject().toShortString() + " (" + this.toHex(cert.serial(), 16) + ")";
	}
	
	protected void note(int res){
		if(!SettingsActivity.getShowNotes(this)) return;
		this.builder.title(R.string.note).text(res, true);
	}
	
	protected void showToast(String text){
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
	}
	
	private ClipboardManager getClipboardManager(){
		return (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
	}
	
	protected String getClipboardText(){
		CharSequence text = this.getClipboardManager().getText();
		if(text == null) return null;
		String s = text.toString().trim();
		return s.length() == 0 ? null : s;
	}
	
	protected void setClipboardText(String text){
		if(text == null) return;
		this.getClipboardManager().setText(text);
	}
}

