package tw.kenshinn.keyboardTerm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.Identity;
import java.util.HashMap;
import java.util.Iterator;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.Toast;

public class ImportKeyboardActivity extends Activity {
	 
	private HashMap<String, String> mSettingMap;
	private LinearLayout mKeyboardBoxLayout;
	private View mOkButton;
	private View mPrevButton;
	private View mNextButton;
	final static String TAG = "ImportKeyboardActivity";
	private boolean mScrollSwitch;
	private int mKeyboard_width;
	private int mKeyboardCount;
	private String[] mKeyboardList;
	private AssetManager mAssetManager;
	private int mCurrentIndex;	
	public final static String KEY_INTENT_IMPORT = "IMPORT_PATH";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAssetManager = getResources().getAssets();
		setContentView(R.layout.act_export_keyboard);
		mKeyboardBoxLayout = (LinearLayout)findViewById(R.id.KeyboardBox);
		mOkButton = findViewById(R.id.OkButton);
		mOkButton.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				importSetting();
				setResult(1);
				finish();
								
			}
		});
		View cancelButton = findViewById(R.id.CancelButton);
		cancelButton.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				setResult(-1);
				finish();				
			}
		});
		

		
		mSettingMap = new HashMap<String, String>();
		InputStream inputStream = null;
		String fileName = "";
		
		
		
		if(getIntent().hasExtra(KEY_INTENT_IMPORT)) {
			String filePath = getIntent().getStringExtra(KEY_INTENT_IMPORT);
			fileName = new File(filePath).getName();
			try {
				inputStream = new FileInputStream(filePath);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			try {				
				mKeyboardList = mAssetManager.list("keyboards");
				if(mKeyboardList != null && mKeyboardList.length > 0) {
					mCurrentIndex = 0;					
				}
				fileName = mKeyboardList[mCurrentIndex];
				inputStream  = mAssetManager.open("keyboards/" + fileName);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
			mPrevButton = findViewById(R.id.PrevButton);
			mPrevButton.setOnClickListener(mNavButtonOnClickListener);
			mNextButton = findViewById(R.id.NextButton);
			mNextButton.setOnClickListener(mNavButtonOnClickListener);		
			
		}
		
		loadInputStream(inputStream, fileName);
	}
	
	private OnClickListener mNavButtonOnClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Log.v(TAG, "mNavButtonOnClickListener, v.getId(): " + v.getId());
			if(v.getId() == R.id.NextButton)
				mCurrentIndex++;
			else if(v.getId() == R.id.PrevButton)
				mCurrentIndex--;
			String fileName = mKeyboardList[mCurrentIndex];
			InputStream inputStream = null;
			try {
				inputStream = mAssetManager.open("keyboards/" + fileName);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			loadInputStream(inputStream, fileName);						
		}
	};

	private void loadInputStream(InputStream inputStream, String fileName) {
		if(inputStream != null) {
			loadXmlToHashMap(inputStream);
			generateKeyboardBox();
			try {
				inputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
			
		setTitle(getResources().getString(R.string.menu_import) + " - " + fileName);
		updateButtonStatus();
	}
	
	private void updateButtonStatus() {
		if(mKeyboardList != null) {
			mPrevButton.setVisibility(mCurrentIndex != 0 ? View.VISIBLE : View.GONE);
			mNextButton.setVisibility(mCurrentIndex != (mKeyboardList.length - 1)  ? View.VISIBLE : View.GONE);
		}
	}

	private void importSetting() {		
		Iterator it = mSettingMap.keySet().iterator();//这是取得键对象
		Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		while(it.hasNext()) 
		{ 
			String key = it.next().toString();
			String value = mSettingMap.get(key);
			Log.v(TAG, "key: " + key + ", value:" + value);
			if(key.equals("settings_use_scrolling_switch"))
				editor.putBoolean(key, Boolean.parseBoolean(value));
//			else if(key.equals("settings_arrow_key_width") || key.equals("settings_arrow_key_group_count"))
//				editor.putInt(key, Integer.parseInt(value));
			else
				editor.putString(key, value);			
		}
			editor.commit();
			
			Toast.makeText(this, getResources().getString(R.string.message_import_success), 500).show();
			
	}


	private void generateKeyboardBox() {
		mKeyboardBoxLayout.removeAllViews();
		mScrollSwitch = Boolean.parseBoolean(mSettingMap.get("settings_use_scrolling_switch"));
		mKeyboardCount = Integer.parseInt(mSettingMap.get("settings_arrow_key_group_count"));
		boolean showSwitchButton = !mScrollSwitch && mKeyboardCount > 1;		
		mKeyboard_width = Integer.parseInt(mSettingMap.get("settings_arrow_key_width"));
		Resources r = getResources();
    	
    	float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mKeyboard_width, r.getDisplayMetrics());
		
		int keyboard_height = this.getWindow().getWindowManager().getDefaultDisplay().getWidth();
		
		for(int i = 1; i <= mKeyboardCount; i++) {
			LinearLayout keyboardLayout = ArrowKeyView.generateKeyboardLayout(this, mSettingMap,showSwitchButton, i);			
			ViewGroup.LayoutParams lParams = new ViewGroup.LayoutParams((int)px, keyboard_height);
			mKeyboardBoxLayout.addView(keyboardLayout, lParams);
		}
		
	}

	private void loadXmlToHashMap(InputStream inputStream) {
		mSettingMap.clear();		
		try {
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
						mSettingMap.put(key, value);
					} else if(tagName.equals("Keyboards")) {
//						int versionCode = Integer.parseInt(parser.getAttributeValue(null, "versionCode"));
//						String versionName = parser.getAttributeValue(null, "versionName");
					}
				}
				eventType = parser.next();
			}
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
