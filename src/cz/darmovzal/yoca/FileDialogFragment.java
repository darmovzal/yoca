package cz.darmovzal.yoca;

import java.io.*;
import java.util.*;
import android.os.*;
import android.app.*;
import android.widget.*;
import android.view.*;
import android.support.v4.app.*;
import android.content.*;
import android.graphics.Color;

class FileAdapter extends BaseAdapter {
	private Context c;
	private File dir, file;
	private List<File> files;
	private boolean showHidden;
	private String[] allowSuffixes;
	
	public FileAdapter(Context c, File file, boolean showHidden, String[] allowSuffixes){
		this.c = c;
		this.files = new ArrayList<File>();
		if(file.isDirectory()){
			this.dir = file;
			this.file = null;
		} else {
			this.dir = file.getParentFile();
			this.file = file;
		}
		this.showHidden = showHidden;
		this.allowSuffixes = allowSuffixes;
		this.populate();
	}
	
	public View getView(int pos, View v, ViewGroup parent){
		TextView tv;
		if(v instanceof TextView){
			tv = (TextView) v;
		} else {
			tv = new TextView(this.c);
		}
		tv.setText(this.getName(pos));
		tv.setPadding(15, 15, 15, 15);
		tv.setBackgroundColor(Color.WHITE);
		tv.setTextColor(Color.BLACK);
		return tv;
	}
	
	public Object getItem(int pos){
		return null;
	}
	
	public long getItemId(int pos){
		return pos;
	}
	
	public int getCount(){
		return (this.files == null ? 0 : this.files.size()) + (this.isRoot() ? 0 : 1);
	}
	
	public boolean select(int pos){
		File f;
		if(this.isRoot()){
			f = this.files.get(pos);
		} else {
			if(pos == 0){
				f = this.dir.getParentFile();
			} else {
				f = this.files.get(pos - 1);
			}
		}
		if(!f.isDirectory()){
			this.file = f;
			return true;
		}
		this.dir = f;
		this.file = null;
		this.populate();
		this.notifyDataSetChanged();
		return false;
	}
	
	private void populate(){
		File[] files = this.dir.listFiles();
		if(files == null) return;
		this.files.clear();
		for(File file : files){
			String name = file.getName();
			boolean hidden = name.startsWith(".");
			if(!this.showHidden && hidden) continue;
			if((this.allowSuffixes != null) && !file.isDirectory()){
				boolean allow = false;
				for(String suffix : this.allowSuffixes){
					if(name.endsWith("." + suffix)) allow = true;
				}
				if(!allow) continue;
			}
			this.files.add(file);
		}
		Collections.sort(this.files, new Comparator<File>(){
			public int compare(File a, File b){
				if(a.isDirectory() && !b.isDirectory()){
					return -1;
				} else if(!a.isDirectory() && b.isDirectory()){
					return 1;
				} else {
					return a.getName().toLowerCase().compareTo(b.getName().toLowerCase());
				}
			}
		});
	}
	
	private String getName(int pos){
		if(!this.isRoot() && (pos == 0)) return "..";
		File f = this.files.get(pos - (this.isRoot() ? 0 : 1));
		return f.isDirectory() ? "[" + f.getName() + "]" : f.getName();
	}
	
	private boolean isRoot(){
		return this.dir.getParentFile() == null;
	}
	
	public File getDir(){
		return this.dir;
	}
	
	public File getFile(){
		return this.file;
	}
}

public class FileDialogFragment extends DialogFragment {
	private String id;
	private int titleres;
	private String path;
	private FileAdapter fa;
	private boolean fileOnly;
	
	public void setArguments(String id, int titleres, String path, boolean fileOnly, boolean showHidden, String[] allowSuffixes){
		Bundle b = new Bundle();
		b.putString("id", id);
		b.putInt("titleres", titleres);
		b.putString("path", path);
		b.putBoolean("showHidden", showHidden);
		b.putStringArray("allowSuffixes", allowSuffixes);
		b.putBoolean("fileOnly", fileOnly);
		this.setArguments(b);
	}
	
	private void onFileSelected(String path){
		((CommonActivity) this.getActivity()).onFileSelected(this.id, path);
	}
	
	@Override
	public void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		Bundle b = this.getArguments();
		this.id = b.getString("id");
		this.titleres = b.getInt("titleres");
		this.path = b.getString("path");
		boolean showHidden = b.getBoolean("showHidden");
		String[] allowSuffixes = b.getStringArray("allowSuffixes");
		this.fileOnly = b.getBoolean("fileOnly");
		this.fa = new FileAdapter(this.getActivity(), new File(this.path), showHidden, allowSuffixes);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle b){
		AlertDialog.Builder adb = new AlertDialog.Builder(this.getActivity());
		adb.setTitle(this.titleres).setAdapter(this.fa, null);
		if(this.fileOnly){
			adb.setNegativeButton(android.R.string.cancel, null);
		} else {
			adb.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
				public void onClick(DialogInterface di, int which){
					onFileSelected(fa.getDir().getAbsolutePath());
				}
			});
		}
		AlertDialog ad = adb.create();
		ad.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener(){
			public void onItemClick(AdapterView parent, View view, int position, long id){
				if(fa.select(position))
					onFileSelected(fa.getFile().getAbsolutePath());
			}
		});
		return ad;
	}
}

