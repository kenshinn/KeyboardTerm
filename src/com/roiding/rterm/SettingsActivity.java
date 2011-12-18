package com.roiding.rterm;

import com.adwhirl.AdWhirlLayout;

import tw.kenshinn.keyboardTerm.FunctionButtonActivity;
import tw.kenshinn.keyboardTerm.GestureSettingsActivity;
import tw.kenshinn.keyboardTerm.ImportKeyboardActivity;
import tw.kenshinn.keyboardTerm.KeyboardsSettingsActivity;
import tw.kenshinn.keyboardTerm.R;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
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
		
		ps = (PreferenceScreen) getPreferenceScreen().findPreference("settings_gestures");
		Intent gestureIntent = new Intent();
		gestureIntent.setClass(this, GestureSettingsActivity.class);
		ps.setIntent(gestureIntent);
		
		ps = (PreferenceScreen) getPreferenceScreen().findPreference("settings_import_default_keyboards");
		Intent importDefaultIntent = new Intent();
		importDefaultIntent.setClass(this, ImportKeyboardActivity.class);
		ps.setIntent(importDefaultIntent);						
		
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		boolean includeFunction = pref.getBoolean("settings_include_function_to_keyboard", false);
		final ListPreference countPreference = (ListPreference)getPreferenceScreen().findPreference("settings_include_to_keyboard_count");
		countPreference.setEnabled(includeFunction);
		countPreference.setSummary(pref.getString("settings_include_to_keyboard_count", "4"));
		
		findPreference("settings_include_function_to_keyboard").setOnPreferenceChangeListener(new OnPreferenceChangeListener() {			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				countPreference.setEnabled(Boolean.parseBoolean(newValue.toString()));
				return true;
			}
		});
		
		countPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				preference.setSummary(newValue.toString());
				return true;
			}
		});
		
		String keyAdWhirl = "c7bce28b019a4e8dbcf33091bce6b542";
		//this.getListView().addFooterView(new com.admob.android.ads.AdView(this));
		this.getListView().addFooterView(new AdWhirlLayout(this, keyAdWhirl));
	}
}
