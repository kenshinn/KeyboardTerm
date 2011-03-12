package tw.kenshinn.keyboardTerm;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.jar.Attributes.Name;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.adwhirl.AdWhirlLayout;

import tw.kenshinn.keyboardTerm.R;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.EditText;
import android.widget.Toast;



public class KeyboardsSettingsActivity extends PreferenceActivity {
	private static final String TAG = "KeyboardsSettings";
	private ArrayList<String> mKeyDefinesList = new ArrayList<String>();
	private ArrayList<String> mKeyValuesList = new ArrayList<String>();
	
	private int mCount;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		MenuItem exportItem = menu.add(R.string.menu_export_preview);
		exportItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Intent intent = new Intent();
				intent.setClass(KeyboardsSettingsActivity.this, ExportKeyboardActivity.class);
				startActivity(intent);
				return true;
			}
		});
		MenuItem importItem = menu.add(R.string.menu_import);
		importItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				
				File sdCardFile = Environment.getExternalStorageDirectory();
				final File importPath = new File(sdCardFile.getPath() + File.separator + "keyboardterm" + File.separator + "keyboard.xml");
				if(importPath.exists()) {
					AlertDialog.Builder builder = new AlertDialog.Builder(KeyboardsSettingsActivity.this);
					builder.setTitle(R.string.menu_import);
					builder.setMessage(getResources().getString(R.string.message_import_confirm, importPath.getPath()));
					builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							runOnUiThread(new Runnable() {
								
								@Override
								public void run() {
									importSetting(importPath);													
								}
							});
						}
					});

					builder.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {									
								}
							});

					builder.show();
										
				} else {
					Toast.makeText(KeyboardsSettingsActivity.this, importPath + getResources().getString(R.string.message_not_found_cant_import), 1000).show();
				}
				
				return true;
			}


		});
		return super.onCreateOptionsMenu(menu);
	}
	
	private void importSetting(File importPath) {
		try {
			Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
			
			FileInputStream inputStream = new FileInputStream(importPath);
			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(inputStream, "UTF-8");
			int eventType = parser.getEventType();
			while(eventType != XmlPullParser.END_DOCUMENT) {
				if(eventType == XmlPullParser.START_TAG) {
					String tagName = parser.getName();
					if(tagName.equals("setting")) {
						String key = parser.getAttributeValue(null, "key");
						String value = parser.getAttributeValue(null, "value");
						Log.v(TAG, "key: " + key + ", value:" + value);
						if(key.equals("settings_use_scrolling_switch"))
							editor.putBoolean(key, Boolean.parseBoolean(value));
//						else if(key.equals("settings_arrow_key_width") || key.equals("settings_arrow_key_group_count"))
//							editor.putInt(key, Integer.parseInt(value));
						else
							editor.putString(key, value);						
					} else if(tagName.equals("Keyboards")) {
//						int versionCode = Integer.parseInt(parser.getAttributeValue(null, "versionCode"));
//						String versionName = parser.getAttributeValue(null, "versionName");
					}
				}
				eventType = parser.next();
			}
			inputStream.close();
			editor.commit();
			getPreferenceScreen().removeAll();
			bindPreference();
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		 String[] definedArray = getResources().getStringArray(R.array.key_defined_name);
		 for(int i = 0; i < definedArray.length; i++) 
			 mKeyDefinesList.add(definedArray[i]);		 
		 mKeyDefinesList.add(getResources().getStringArray(R.array.settings_send_key)[0]);
		 String[] valueArray = getResources().getStringArray(R.array.key_defined_value);
		 for(int i = 0; i < definedArray.length; i++) 
			 mKeyValuesList.add(valueArray[i]);
		 mKeyValuesList.add(getResources().getStringArray(R.array.settings_send_key_values)[0]);
		
		super.onCreate(savedInstanceState);
		bindPreference();
		String keyAdWhirl = "c7bce28b019a4e8dbcf33091bce6b542";
		//this.getListView().addFooterView(new com.admob.android.ads.AdView(this));
		this.getListView().addFooterView(new AdWhirlLayout(this, keyAdWhirl));
	}

	private void bindPreference() {
		try {
			addPreferencesFromResource(R.xml.keyboards);
		} catch (ClassCastException e) {
			Log.e(TAG, "reset default values");
			PreferenceManager.getDefaultSharedPreferences(this).edit().clear().commit();
			PreferenceManager.setDefaultValues(this, R.xml.keyboards, true);
			addPreferencesFromResource(R.xml.keyboards);
		}
		
		mCount = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("settings_arrow_key_group_count","1"));
		turnOnKeyboards(mCount);
		
		getPreferenceScreen().findPreference("settings_use_arrow_key")
				.setOnPreferenceChangeListener(
						new OnPreferenceChangeListener() {
							public boolean onPreferenceChange(
									Preference preference, Object newValue) {
								boolean depend = (Boolean) newValue;
								getPreferenceScreen().findPreference(
										"settings_arrow_key_general").setEnabled(
										depend);
								getPreferenceScreen().findPreference(
								"settings_arrow_key_keyboards").setEnabled(
								depend);
								turnOnKeyboards(mCount);
								return true;
							}
						});
		
		
		getPreferenceScreen().findPreference("settings_arrow_key_group_count")
		.setOnPreferenceChangeListener(
				new OnPreferenceChangeListener() {
					public boolean onPreferenceChange(
							Preference preference, Object newValue) {
						int count = Integer.parseInt(newValue.toString());
						turnOnKeyboards(count);
						return true;
					}
				});

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		String send_key_in_list = pref.getString("settings_send_key_in_list", "ENTER");
		String send_key_in_reading = pref.getString("settings_send_key_in_reading", "SPACE");
		
		Preference pref_in_list = getPreferenceScreen().findPreference("settings_send_key_in_list");
		Preference pref_in_reading = getPreferenceScreen().findPreference("settings_send_key_in_reading");
		
		if(mKeyValuesList.contains(send_key_in_list)) {
			int index = mKeyValuesList.indexOf(send_key_in_list);
			pref_in_list.setSummary(mKeyDefinesList.get(index));			
		}
		
		if(mKeyValuesList.contains(send_key_in_reading)) {
			int index = mKeyValuesList.indexOf(send_key_in_reading);
			pref_in_reading.setSummary(mKeyDefinesList.get(index));			
		}
		
		pref_in_list.setOnPreferenceChangeListener(mButtonChangeListener);
		pref_in_reading.setOnPreferenceChangeListener(mButtonChangeListener);

	}
	
	private void turnOnKeyboards(int count) {
		//int count = mCount;
		PreferenceGroup group = (PreferenceGroup)getPreferenceScreen().findPreference(
		"settings_arrow_key_keyboards");
		
		for (int i = 0; i < group.getPreferenceCount(); i++) {
			group.getPreference(i).setEnabled(false);
		}
		
		for(int i = 0; i < count ; i++) {
			//Log.v("KeyboardsSettingsActivity", "group.getPreference(i): " + group.getPreference(i).getClass().getName());
			group.getPreference(i).setEnabled(true);
			updateKeyboardButtons((PreferenceGroup)(group.getPreference(i)));
		}
	}
	
	private void updateKeyboardButtons(final PreferenceGroup group) {
		//Log.v("KeyboardsSettingsActivity", "updateKeyboardButtons, group: " + group);
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		String keyCountString = pref.getString(group.getPreference(0).getKey(), "8");
		int keyCount = Integer.parseInt(keyCountString);
		
		updateButtonState(group, keyCount);
	}
	
	private void updateButtonState(final PreferenceGroup group, int enableCount) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		
		group.getPreference(0).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				updateButtonState(group, Integer.parseInt(newValue.toString()));
				return true;
			}
		});
		
		group.getPreference(0).setSummary(String.valueOf(enableCount));
		for(int i = 1; i < group.getPreferenceCount(); i++) {
			Preference item = group.getPreference(i);
			String keyValue = pref.getString(item.getKey(), "NONE");
			
			item.setTitle(getResources().getString(R.string.setting_button) + " " + i);
			
			if(keyValue.startsWith("custom_")) {
				item.setSummary(keyValue.replaceAll("^custom_", ""));
				item.setOnPreferenceChangeListener(mButtonChangeListener);
				continue;
			}
			if(mKeyValuesList.contains(keyValue)) {
				int index = mKeyValuesList.indexOf(keyValue);
				//Log.v("KeyboardsSettingsActivity", "set button " + i + " : " + mKeyDefinesList.get(index) + ", keyValue: " + keyValue + ", key: " + item.getKey());				
				item.setSummary(mKeyDefinesList.get(index));
			} else {
				item.setSummary(keyValue);
			}
			
			item.setOnPreferenceChangeListener(mButtonChangeListener);
			
			if(i <= enableCount)
				item.setEnabled(true);
			else 
				item.setEnabled(false);
		}
	}
	
	private OnPreferenceChangeListener mButtonChangeListener = new OnPreferenceChangeListener() {
		
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			final Preference pref = preference;
			if(newValue.toString().equals("custom")) {
				AlertDialog.Builder alert = new AlertDialog.Builder(
						KeyboardsSettingsActivity.this);

				alert.setTitle(R.string.key_Desc_Custom);

				// Set an EditText view to get user input
				final EditText input = new EditText(KeyboardsSettingsActivity.this);
				input.setSingleLine(true);
				alert.setView(input);

				alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString();
						Editor editor = pref.getEditor();
						editor.putString(pref.getKey(), "custom_"+ value);
						editor.commit();
						pref.setSummary(value);						
					}
				});

				alert.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								// Canceled.
							}
						});

				alert.show();
				return false;
			}
			
			
			if(mKeyValuesList.contains(newValue)) {
				int index = mKeyValuesList.indexOf(newValue);							
				preference.setSummary(mKeyDefinesList.get(index));
			}
			return true;
		}
	};


}
