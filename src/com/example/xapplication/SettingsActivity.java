package com.example.xapplication;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Activity to manage application settings.
 */
public class SettingsActivity extends PreferenceActivity {
	
	@SuppressWarnings("deprecation")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.preferences);
    }
	
}