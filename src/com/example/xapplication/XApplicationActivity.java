package com.example.xapplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;

import com.github.espiandev.showcaseview.ShowcaseView;

/**
 * Application to set timeout for a user selected application.
 */
public class XApplicationActivity extends Activity implements View.OnClickListener, AdapterView.OnItemClickListener {
	public static final String TAG = "XApplicationTag";
	public static final String RESOLVE_INFO = "ResolveInfo";
	
	private ImageView mAppImageView;
	private Button mSettingsButton;
	private Button mTutorialButton;
	private ListView mListView;
	List<ResolveInfo> mRIList;
	
	private ShowcaseView mSv;
	private int tutorialState = -1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "XApplicationActivity.onCreate");
		setContentView(R.layout.activity_xapplication);
		
		mAppImageView = (ImageView)findViewById(R.id.app_image);
		mSettingsButton = (Button)findViewById(R.id.settings_button);
		mTutorialButton = (Button)findViewById(R.id.tutorial_button);
		mListView = (ListView)findViewById(R.id.list);
		
		mSettingsButton.setOnClickListener(this);
		mTutorialButton.setOnClickListener(this);
		mListView.setOnItemClickListener(this);
		
		try {
			mRIList = getInstalledComponentList();
			List<String> list = getComponentListNames(mRIList);
			
			mListView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, list));
			mListView.setItemsCanFocus(false);
			mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		} catch (Exception e) {
			Log.d(TAG, "XApplicationActivity.onCreate error:" + e);
		}
	}

	@Override
    protected void onDestroy() {
        super.onDestroy();
    }
	
	@Override
    public void onClick(View v) {
		int id = v.getId();
		switch (id) {
	    	case R.id.settings_button:
	    		showSettings();
	    		break;
	    		
	    	case R.id.tutorial_button:
	    		startTutorial();
	    		break;
	    		
	    	case R.id.showcase_button:
	    		updateShowcaseView();
	    		break;
		}
	}
	
	@Override
    public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
        int parentId = parent.getId();
        if (parentId == R.id.list) {
            try {
            	ResolveInfo ri = mRIList.get(pos);
            	Intent intent = new Intent(this, AppStateService.class);
            	intent.putExtra(RESOLVE_INFO, ri);
        		startService(intent);
            } catch (Exception e) {}
        }
    }
	
	/**
	 * Get list of installed applications (ResolveInfo for each app)
	 * 
	 * @return list of ResolveInfo objects
	 */
	private List<ResolveInfo> getInstalledComponentList() {
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> ril = getPackageManager().queryIntentActivities(mainIntent, 0);
        Collections.sort(ril, new ResolveInfo.DisplayNameComparator(getPackageManager()));
        
        return ril;
    }
	
	/**
	 * Get list of installed applications (string names)
	 *  
	 * @param ril list of ResolveInfo objects
	 * @return list of string names
	 * @throws NameNotFoundException
	 */
	private List<String> getComponentListNames(List<ResolveInfo> ril) throws NameNotFoundException {
        List<String> componentList = new ArrayList<String>();
        String name = null;

        for (ResolveInfo ri : ril) {
            if (ri.activityInfo != null) {
                Resources res = getPackageManager().getResourcesForApplication(ri.activityInfo.applicationInfo);
                if (ri.activityInfo.labelRes != 0) {
                    name = res.getString(ri.activityInfo.labelRes);
                } else {
                    name = ri.activityInfo.applicationInfo.loadLabel(getPackageManager()).toString();
                }
                componentList.add(name);
            }
        }
        return componentList;
    }
	
	/**
	 * Show the settings screen.
	 */
	private void showSettings() {
		Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
	}
	
	/**
	 * Start the on screen tutorial. 
	 */
	private void startTutorial() {
		if (tutorialState != -1) {
			return;
		}
		
		// Show case the settings button
		if (mSv == null) {
			ShowcaseView.ConfigOptions co = new ShowcaseView.ConfigOptions();
			mSv = ShowcaseView.insertShowcaseView(mSettingsButton, this, 
					R.string.tutorial_settings_title, R.string.tutorial_settings_desc, co);
			mSv.overrideButtonClick(this);
			mSv.setShowcaseIndicatorScale(0.75f);
		} else {
			mSv.setShowcaseView(mSettingsButton);
			mSv.setText(R.string.tutorial_settings_title, R.string.tutorial_settings_desc);
			mSv.show();
		}
		tutorialState = 1;
	}
	
	/**
	 * Update the showcaseview.
	 */
	private void updateShowcaseView() {
		if (mSv != null) {
			switch (tutorialState) {
				case 1: // Show case the list of apps
					mSv.setShowcaseView(mListView);
					mSv.setText(R.string.tutorial_list_select_title, R.string.tutorial_list_select_desc);
					tutorialState = 2;
					break;
				case 2: // Show case the results of selecting an app
					mSv.setShowcaseView(mAppImageView);
					mSv.setText(R.string.tutorial_timeout_msg_title, R.string.tutorial_timeout_msg_desc);
					tutorialState = 3;
					break;
				case 3:
					mSv.hide();
					mSv.setShowcaseView(mSettingsButton);
					mSv.setText(R.string.tutorial_settings_title, R.string.tutorial_settings_desc);
					tutorialState = -1;
					break;
			}
		}
	}
}
