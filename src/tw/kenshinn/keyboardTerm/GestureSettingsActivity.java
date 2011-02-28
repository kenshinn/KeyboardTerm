package tw.kenshinn.keyboardTerm;

import java.util.ArrayList;

import com.adwhirl.AdWhirlLayout;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;

public class GestureSettingsActivity extends PreferenceActivity {
	private static final String TAG = "GestureSettings";
	private ArrayList<String> mKeyDefinesList = new ArrayList<String>();
	private ArrayList<String> mKeyValuesList = new ArrayList<String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		 String[] definedArray = getResources().getStringArray(R.array.gestures_desc);
		 for(int i = 0; i < definedArray.length; i++) 
			 mKeyDefinesList.add(definedArray[i]);		 
		 String[] valueArray = getResources().getStringArray(R.array.gestures_defs);
		 for(int i = 0; i < definedArray.length; i++) 
			 mKeyValuesList.add(valueArray[i]);
		
		super.onCreate(savedInstanceState);
		try {
			addPreferencesFromResource(R.xml.gestures);
		} catch (ClassCastException e) {
			Log.e(TAG, "reset default values");
			PreferenceManager.setDefaultValues(this, R.xml.gestures, true);
			addPreferencesFromResource(R.xml.gestures);
		}

		updateGesturesState(getPreferenceScreen());
		
		String keyAdWhirl = "c7bce28b019a4e8dbcf33091bce6b542";
		//this.getListView().addFooterView(new com.admob.android.ads.AdView(this));
		this.getListView().addFooterView(new AdWhirlLayout(this, keyAdWhirl));
	}

	private void updateGesturesState(final PreferenceGroup group) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
				
		for(int i = 0; i < group.getPreferenceCount(); i++) {
			Preference item = group.getPreference(i);
			String key = item.getKey();
			String keyValue = pref.getString(key, "NONE");
			
			
			item.setTitle(getArrowString(key.replace("settings_gestures_", "").replace('_', ',')));
			if(mKeyValuesList.contains(keyValue)) {
				int index = mKeyValuesList.indexOf(keyValue);
				//Log.v("KeyboardsSettingsActivity", "set button " + i + " : " + mKeyDefinesList.get(index) + ", keyValue: " + keyValue + ", key: " + item.getKey());				
				item.setSummary(mKeyDefinesList.get(index));
			}
			item.setOnPreferenceChangeListener(mButtonChangeListener);
			
		}
	}
	
	private OnPreferenceChangeListener mButtonChangeListener = new OnPreferenceChangeListener() {
		
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			if(mKeyValuesList.contains(newValue)) {
				int index = mKeyValuesList.indexOf(newValue);							
				preference.setSummary(mKeyDefinesList.get(index));
			}
			return true;
		}
	};

	private String getArrowString(String src) {
		String result = "";
		if(src != null) {
			result = src.replace('U', '↑').replace('D', '↓').replace('L', '←').replace('R', '→');
		}
		return result;
	}
	
}
