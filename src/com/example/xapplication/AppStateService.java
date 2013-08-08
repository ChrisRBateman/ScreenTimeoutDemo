package com.example.xapplication;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

/**
 * Class monitors foreground/background state of user selected app and
 * sets screen timeout accordingly.
 */
public class AppStateService extends Service {
	public static final String TAG = XApplicationActivity.TAG;
	public static final String RESOLVE_INFO = XApplicationActivity.RESOLVE_INFO;
	
	private static final int MAX_RECENT_TASKS = 20;
	private static Timer timer = new Timer(); 
	private boolean mForegroundTimeoutIsSet = false;
	private boolean mBackgroundTimeoutIsSet = false;
	private ResolveInfo mCurrentRI = null;
	private final Object mLock = new Object();
	private int defTimeOut = 0;
	private Map<String, TimeoutInfo> TIMEOUT_MAP = null;
    private static final String DEFAULT_TIMEOUT_KEY = "7";
    
	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "AppStateService.onCreate");
		TIMEOUT_MAP = createTimeoutMap();
		defTimeOut = Settings.System.getInt(getContentResolver(), 
				Settings.System.SCREEN_OFF_TIMEOUT, 
				TIMEOUT_MAP.get(DEFAULT_TIMEOUT_KEY).getValue());
        startService();
	}
	
	private void startService() {
        timer.scheduleAtFixedRate(new mainTask(), 0, 5000);
    }

	/**
	 * Task that checks if selected app is in foreground/background and sets screen timeout.
	 */
    private class mainTask extends TimerTask {
        public void run() {
        	final ActivityManager tasksManager = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
            final List<ActivityManager.RunningTaskInfo> runningTasks = tasksManager.getRunningTasks(MAX_RECENT_TASKS);
            final int count = runningTasks.size();
            
        	synchronized (mLock) {
	        	if (mCurrentRI != null) {
	        		String pName = "";
	        		try {
	        			pName = mCurrentRI.activityInfo.applicationInfo.packageName;
	        		} catch (Exception e) {}
	        		
	        		boolean found = false;
	        		int index = -1;
		            for (int i = 0; i < count; i++) {
		            	ComponentName cn = runningTasks.get(i).baseActivity;
		            	
		            	if ((cn != null) && cn.getPackageName().equals(pName)) {
		            		found = true;
		            		index = i;
		            		break;
		    	        }
		            }
		            
		            if (found) {
		            	if (index == 0) { // app running in foreground
	            			if (!mForegroundTimeoutIsSet) {
	            				Settings.System.putInt(getContentResolver(), 
	            						Settings.System.SCREEN_OFF_TIMEOUT, getTimeoutValueFromSettings());
	            				mForegroundTimeoutIsSet = true;
	            				mBackgroundTimeoutIsSet = false;
	            				
	            				Message msg = toastHandler.obtainMessage(0, getTimeoutLabelFromSettings());
	            				toastHandler.sendMessage(msg);
	            			}
	            		} else { // app running in background
	            			if (!mBackgroundTimeoutIsSet) {
	            				Settings.System.putInt(getContentResolver(), 
	            						Settings.System.SCREEN_OFF_TIMEOUT, defTimeOut);
	            				mBackgroundTimeoutIsSet = true;
	            				mForegroundTimeoutIsSet = false;
	            				
	            				toastHandler.sendEmptyMessage(1);
	            			}
	            		}
		            } else {
		            	mBackgroundTimeoutIsSet = false;
        				mForegroundTimeoutIsSet = false;
        				Settings.System.putInt(getContentResolver(), 
        						Settings.System.SCREEN_OFF_TIMEOUT, defTimeOut);
		            }
	        	}
        	}
        }
    }    

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		ResolveInfo ri = (ResolveInfo)intent.getParcelableExtra(RESOLVE_INFO);
		String pn1 = ri.activityInfo.applicationInfo.packageName;
		
		synchronized (mLock) {
			if (mCurrentRI != null) {
				String pn2 = mCurrentRI.activityInfo.applicationInfo.packageName;
				
				if ((pn1 != null) && (pn2 != null) && !pn1.equals(pn2)) {
					mCurrentRI = ri;
					mForegroundTimeoutIsSet = false;
					mBackgroundTimeoutIsSet = false;
				}
				
			} else {
				if (ri != null) {
					mCurrentRI = ri;
					mForegroundTimeoutIsSet = false;
					mBackgroundTimeoutIsSet = false;
				}
			}
		}
		
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	  
	@Override
	public void onDestroy() {
		timer.cancel();
		Settings.System.putInt(getContentResolver(), 
				Settings.System.SCREEN_OFF_TIMEOUT, defTimeOut);
	}
	
	private final Handler toastHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	Resources res = getApplicationContext().getResources();
        	switch (msg.what) {
	        	case 0: {
	        		String s = (String)msg.obj;
	        		Toast.makeText(getApplicationContext(),	s, Toast.LENGTH_SHORT).show();
	        		break;
	        	}
	        	case 1: {
	        		String s = res.getString(R.string.restore_screen_timeout);
	        		Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
	        		break;
	        	}
        	}
        }
    };
    
    /**
     * Returns screen timeout value from application settings
     * 
     * @return screen timeout value
     */
    public int getTimeoutValueFromSettings() {
    	int timeout = TIMEOUT_MAP.get(DEFAULT_TIMEOUT_KEY).getValue();
        try {
            SharedPreferences sharedPrefs = 
            		PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String key = sharedPrefs.getString("timeoutPref", DEFAULT_TIMEOUT_KEY);
            timeout = TIMEOUT_MAP.get(key).getValue();
        } catch (Exception e) {
        }
        return timeout;
    }
    
    /**
     * Returns screen timeout label from application settings
     * 
     * @return screen timeout label
     */
    public String getTimeoutLabelFromSettings() {
    	Resources res = getApplicationContext().getResources();
    	String extra = "";
    	String label = TIMEOUT_MAP.get(DEFAULT_TIMEOUT_KEY).getLabel();
        try {
        	extra = res.getString(R.string.screen_timeout);
            SharedPreferences sharedPrefs = 
            		PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String key = sharedPrefs.getString("timeoutPref", DEFAULT_TIMEOUT_KEY);
            label = TIMEOUT_MAP.get(key).getLabel();
        } catch (Exception e) {
        }
        return label + " " + extra;
    }
    
    /**
     * Create a new timeout info map.
     * 
     * @return timeout info map
     */
    public Map<String, TimeoutInfo> createTimeoutMap() {
    	Resources res = getApplicationContext().getResources();
    	
    	Map<String, TimeoutInfo> aMap = new HashMap<String, TimeoutInfo>();
        aMap.put("1", new TimeoutInfo(15 * 1000, res.getString(R.string._15_seconds))); 
        aMap.put("2", new TimeoutInfo(30 * 1000, res.getString(R.string._30_seconds))); 
        aMap.put("3", new TimeoutInfo(1 * 60 * 1000, res.getString(R.string._1_minute))); 
        aMap.put("4", new TimeoutInfo(2 * 60 * 1000, res.getString(R.string._2_minutes))); 
        aMap.put("5", new TimeoutInfo(3 * 60 * 1000, res.getString(R.string._3_minutes))); 
        aMap.put("6", new TimeoutInfo(4 * 60 * 1000, res.getString(R.string._4_minutes))); 
        aMap.put("7", new TimeoutInfo(5 * 60 * 1000, res.getString(R.string._5_minutes))); 
        aMap.put("8", new TimeoutInfo(10 * 60 * 1000, res.getString(R.string._10_minutes))); 
        aMap.put("9", new TimeoutInfo(15 * 60 * 1000, res.getString(R.string._15_minutes))); 
        
        return Collections.unmodifiableMap(aMap); 
    }
}
