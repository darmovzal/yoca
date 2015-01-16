package cz.darmovzal.yoca;

import android.os.AsyncTask;
import android.util.Log;

public abstract class Task {
	private static final String LTAG = "YOCA:Task";
	
	protected Throwable t;
	
	public Task(){}
	
	public abstract void pre() throws Exception;
	public abstract Object work(Object ... args) throws Exception;
	public abstract void success(Object result) throws Exception;
	public abstract void fail(Throwable t) throws Exception;
	
	public void execute(final Object ... args){
		this.t = null;
		try {
			this.pre();
		} catch (Exception e){
			Log.e(LTAG, "Error in Task.pre()", e);
			return;
		}
		(new AsyncTask<Void, Void, Object>(){
			protected Object doInBackground(Void ... params){
				try {
					return work(args);
				} catch (Throwable t){
					Task.this.t = t;
					Log.e(LTAG, "Error in Task.work()", t);
				}
				return null;
			}
			
			protected void onPostExecute(Object result){
				if(t == null){
					try {
						success(result);
					} catch (Exception e){
						Log.e(LTAG, "Error in Task.success()", e);
					}
				} else {
					try {
						fail(t);
					} catch (Exception e){
						Log.e(LTAG, "Error in Task.fail()", e);
					}
				}
			}
		}).execute();
	}
}

