package cz.darmovzal.yoca;

import java.io.*;
import android.app.Application;
import cz.darmovzal.yoca.guts.Storage;
import cz.darmovzal.yoca.guts.FileHandler;
import android.util.Log;

public class App extends Application {
	private static final String LTAG = "YOCA:App";
	
	private Storage storage;
	
	@Override
	public void onCreate(){
		super.onCreate();
		this.storage = new Storage();
		this.storage.setFileHandler(new FileHandler(){
			public InputStream read(String name) throws IOException {
				return App.this.openFileInput(name);
			}
			public OutputStream write(String name) throws IOException {
				return App.this.openFileOutput(name, MODE_PRIVATE);
			}
			public String[] list(){
				return App.this.fileList();
			}
			public boolean remove(String name){
				return App.this.deleteFile(name);
			}
		});
		this.storage.load();
	}
	
	public Storage storage(){
		return this.storage;
	}
	
	public String getVersion(){
		try {
			return this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName + (this.storage.TRIAL ? "T" : "F");
		} catch (Exception e){
			Log.e(LTAG, "Cannot get version name", e);
			return "???";
		}
	}
}


