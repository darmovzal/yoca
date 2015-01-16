package cz.darmovzal.yoca;

import java.io.File;
import android.preference.*;
import android.os.*;
import android.content.*;
import android.util.Log;
import cz.darmovzal.yoca.guts.Storage;

public class SettingsActivity extends PreferenceActivity {
	private static final String LTAG = "YOCA:SettingsActivity";
	
	@Override
	protected void onCreate(Bundle b){
		super.onCreate(b);
		this.addPreferencesFromResource(R.xml.preferences);
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		
		Preference archiveDir = this.findPreference("archive_dir");
		archiveDir.setDefaultValue(getDefaultArchiveDir());
		archiveDir.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
			public boolean onPreferenceChange(Preference p, Object newValue){
				updateArchiveDir((String) newValue);
				return true;
			}
		});
		
		Preference archivePass = this.findPreference("archive_pass");
		archivePass.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
			public boolean onPreferenceChange(Preference p, Object newValue){
				updateArchivePass((String) newValue);
				return true;
			}
		});
		
		this.registerPrefillListener("pre_organization");
		this.registerPrefillListener("pre_organizational_unit");
		this.registerPrefillListener("pre_locality");
		this.registerPrefillListener("pre_state");
		this.registerPrefillListener("pre_country");
		this.registerPrefillListener("pre_validity");
		
		Preference about = this.findPreference("about");
		about.setSummary("Tomas Darmovzal (C) 2012, v" + ((App) this.getApplicationContext()).getVersion());
		about.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
			public boolean onPreferenceClick(Preference p){
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType("message/rfc822");
				intent.putExtra(Intent.EXTRA_EMAIL, new String[]{ "tomas.darmovzal.android@gmail.com" });
				intent.putExtra(Intent.EXTRA_SUBJECT, Storage.TRIAL ? "YOCA PKI" : "YOCA PKI Full Version");
				try {
					startActivity(intent);
				} catch (Exception e){
					Log.e(LTAG, "Cannot start email intent", e);
				}
				return true;
			}
		});
		
		this.updateAll();
	}
	
	private void registerPrefillListener(final String key){
		Preference p = this.findPreference(key);
		p.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
			public boolean onPreferenceChange(Preference p, Object newValue){
				updatePrefill(key, (String) newValue);
				return true;
			}
		});
	}
	
	private void updateAll(){
		this.updateArchiveDir(getArchiveDir(this));
		this.updateArchivePass(getArchivePass(this));
		this.updatePrefill("pre_organization");
		this.updatePrefill("pre_organizational_unit");
		this.updatePrefill("pre_locality");
		this.updatePrefill("pre_state");
		this.updatePrefill("pre_country");
		this.updatePrefill("pre_validity");
	}
	
	private static String getDefaultArchiveDir(){
		File dir = Environment.getExternalStorageDirectory();
		return dir != null ? new File(dir, "yoca").getAbsolutePath() : "/";
	}
	
	private void updateArchiveDir(String path){
		this.findPreference("archive_dir").setSummary(path);
	}
	
	private void updateArchivePass(String pass){
		this.findPreference("archive_pass").setSummary(pass.length() == 0 ? R.string.passphrase_not_set : R.string.passphrase_is_set);
	}
	
	public static String getArchiveDir(Context ctx){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
		String path = sp.getString("archive_dir", "");
		return ((path == null) || (path.trim().length() == 0)) ? getDefaultArchiveDir() : path.trim();
	}
	
	public static String getArchivePass(Context ctx){
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
		return sp.getString("archive_pass", "");
	}
	
	private void updatePrefill(String key){
		String value = PreferenceManager.getDefaultSharedPreferences(this).getString(key, "");
		this.findPreference(key).setSummary(value);
	}
	
	private void updatePrefill(String key, String value){
		this.findPreference(key).setSummary(value);
	}
	
	private static String getPrefill(Context ctx, String key){
		return PreferenceManager.getDefaultSharedPreferences(ctx).getString(key, "");
	}
	
	public static String getPrefillOrganization(Context ctx){
		return getPrefill(ctx, "pre_organization");
	}
	
	public static String getPrefillOrganizationalUnit(Context ctx){
		return getPrefill(ctx, "pre_organizational_unit");
	}
	
	public static String getPrefillLocality(Context ctx){
		return getPrefill(ctx, "pre_locality");
	}
	
	public static String getPrefillState(Context ctx){
		return getPrefill(ctx, "pre_state");
	}
	
	public static String getPrefillCountry(Context ctx){
		return getPrefill(ctx, "pre_country");
	}
	
	public static int getPrefillValidity(Context ctx){
		try {
			return Integer.parseInt(getPrefill(ctx, "pre_validity"));
		} catch (NumberFormatException e){
			return 365;
		}
	}
	
	public static boolean getShowNotes(Context ctx){
		return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean("show_notes", true);
	}
}

