package com.roiding.rterm;

import tw.kenshinn.keyboardTerm.FunctionButtonActivity;
import tw.kenshinn.keyboardTerm.KeyboardsSettingsActivity;
import tw.kenshinn.keyboardTerm.R;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;

public class SettingsActivity extends PreferenceActivity {
	private static final String TAG = "RTermSettings";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			addPreferencesFromResource(R.xml.preferences);
		} catch (ClassCastException e) {
			Log.e(TAG, "reset default values");
			PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
			addPreferencesFromResource(R.xml.preferences);
		}

		PreferenceScreen ps = (PreferenceScreen) getPreferenceScreen().findPreference("settings_function_button");
		Intent intent = new Intent();
		intent.setClass(this, FunctionButtonActivity.class);
		ps.setIntent(intent);

		ps = (PreferenceScreen) getPreferenceScreen().findPreference("settings_keyboards");
		Intent keyboardIntent = new Intent();
		keyboardIntent.setClass(this, KeyboardsSettingsActivity.class);
		ps.setIntent(keyboardIntent);
		
		
		/* There is no inversed dependency in Android, so we do it ourself */
		if(!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("settings_magnifier_fullscreen", true)){
			getPreferenceScreen().findPreference("settings_magnifier_focus_width").setEnabled(true);
			getPreferenceScreen().findPreference("settings_magnifier_focus_height").setEnabled(true);
		}
		
//		if(!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("settings_use_arrow_key", false)){
//			getPreferenceScreen().findPreference("settings_arrow_key_type").setEnabled(false);			
//		}
			
		getPreferenceScreen().findPreference("settings_magnifier_fullscreen").setOnPreferenceChangeListener(new OnPreferenceChangeListener(){
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
					boolean depend = ! (Boolean) newValue;
					getPreferenceScreen().findPreference("settings_magnifier_focus_width").setEnabled(depend);
					getPreferenceScreen().findPreference("settings_magnifier_focus_height").setEnabled(depend);
				return true;
			}
		});
		
//		getPreferenceScreen().findPreference("settings_arrow_key_type").setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
//			
//			public boolean onPreferenceChange(Preference preference, Object newValue) {
//				
//				final Object value = newValue;
//				
//			    new AlertDialog.Builder(SettingsActivity.this)
//		        .setIcon(android.R.drawable.ic_dialog_alert)
//		        .setTitle("Confirm?")
//		        .setMessage("This will clear your keyboard settings")
//		        .setPositiveButton("YES", new android.content.DialogInterface.OnClickListener() {
//
//		            public void onClick(android.content.DialogInterface dialog, int which) {
//		                int resId = -1;
//						try {
//							resId = R.xml.class.getDeclaredField("keyboards_" + value).getInt(null);
//						} catch (IllegalArgumentException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						} catch (SecurityException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						} catch (IllegalAccessException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						} catch (NoSuchFieldException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//						if(resId != -1) {
//			                PreferenceManager.setDefaultValues(SettingsActivity.this, resId, true);			                	
//						}
//		            }
//
//		        })
//		        .setNegativeButton("NO", null)
//		        .show();
//				return false;
//			}
//		});
		
//		getPreferenceScreen().findPreference("settings_use_arrow_key").setOnPreferenceChangeListener(new OnPreferenceChangeListener(){
//			public boolean onPreferenceChange(Preference preference,
//					Object newValue) {
//					boolean depend = (Boolean) newValue;
//					getPreferenceScreen().findPreference("settings_arrow_key_type").setEnabled(depend);					
//				return true;
//			}
//		});
	}
}
