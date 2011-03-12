package tw.kenshinn.keyboardTerm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.xmlpull.v1.XmlSerializer;

import android.R.xml;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.FrameLayout.LayoutParams;

public class ExportKeyboardActivity extends Activity {

	final static String TAG = "ExportKeyboard";
	
	private LinearLayout mKeyboardBoxLayout;
	private SharedPreferences mPref;
	private View mOkButton;
	private File mExportFile;
	
	private boolean mScrollSwitch;
	private int mKeyboard_width;
	private int mKeyboardCount;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_export_keyboard);
		mKeyboardBoxLayout = (LinearLayout)findViewById(R.id.KeyboardBox);
		mOkButton = findViewById(R.id.OkButton);
		mOkButton.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				export();				
			}
		});
		View cancelButton = findViewById(R.id.CancelButton);
		cancelButton.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				finish();				
			}
		});
		mPref = PreferenceManager.getDefaultSharedPreferences(this);
		if(!mPref.contains("settings_arrow_key_group_count"))
			PreferenceManager.setDefaultValues(this, R.xml.keyboards, true);
		generateKeyboardBox();
	}
	
	private void generateKeyboardBox(){
		
		mScrollSwitch = mPref.getBoolean("settings_use_scrolling_switch", false);
		mKeyboardCount = Integer.parseInt(mPref.getString("settings_arrow_key_group_count", "1"));
		boolean showSwitchButton = !mScrollSwitch && mKeyboardCount > 1;		
		mKeyboard_width = Integer.parseInt(mPref.getString("settings_arrow_key_width", "80"));
		Resources r = getResources();
    	
    	float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mKeyboard_width, r.getDisplayMetrics());
		
		int keyboard_height = this.getWindow().getWindowManager().getDefaultDisplay().getWidth();
		
		for(int i = 1; i <= mKeyboardCount; i++) {
			LinearLayout keyboardLayout = ArrowKeyView.generateKeyboardLayout(this, mPref,showSwitchButton, i, null, null);			
			ViewGroup.LayoutParams lParams = new ViewGroup.LayoutParams((int)px, keyboard_height);
			mKeyboardBoxLayout.addView(keyboardLayout, lParams);
		}
	}
	
	private void export() {
		String externalState = Environment.getExternalStorageState();
		if(Environment.MEDIA_MOUNTED.equals(externalState)) {
			// export to sdcard
			File sdCardFile = Environment.getExternalStorageDirectory();
			File keyboardTermPath = new File(sdCardFile.getPath() + File.separator + "keyboardterm");
			Log.v(TAG, "keyboardTermPath: " + keyboardTermPath.getPath());
			if(!keyboardTermPath.exists()) {
				if(!keyboardTermPath.mkdirs()) {
					Toast.makeText(this, 
							getResources().getString(R.string.message_create_folder_failed, keyboardTermPath.getPath()),
							1500).show();
					mOkButton.postDelayed(mEnableOkButtonRunnable, 1500);
					return;
				}
			}
			
			if(!keyboardTermPath.isDirectory()) {
				Toast.makeText(this,
						getResources().getString(R.string.message_not_folder, keyboardTermPath.getPath()),						
						1500).show();
				mOkButton.postDelayed(mEnableOkButtonRunnable, 1500);
				return;
			}
			
			mExportFile = new File(keyboardTermPath.getPath() + File.separator + "keyboard.xml");
			if(mExportFile.exists()) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.message_title_file_exist);
				builder.setMessage(getResources().getString(R.string.message_file_exist, mExportFile.getPath()));
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						runOnUiThread(exportRunnable);
					}
				});

				builder.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								mOkButton.setEnabled(true);
							}
						});

				builder.show();
			} else {
				runOnUiThread(exportRunnable);
			}
		} else {
			// please check sd card state			
			Toast.makeText(this, R.string.message_check_sdcard, 1500).show();
			mOkButton.postDelayed(mEnableOkButtonRunnable, 1500);
		}
	}
	
	private Runnable exportRunnable = new Runnable() {
		@Override
		public void run() {
			try {
				FileOutputStream outputStream = new FileOutputStream(mExportFile);
				XmlSerializer xmlSerializer = Xml.newSerializer();
				xmlSerializer.setOutput(outputStream, "UTF-8");
				xmlSerializer.startDocument("UTF-8", true);
				xmlSerializer.startTag(null, "Keyboards");				
				PackageInfo info = getPackageInfo();
				if(info != null) {
					xmlSerializer.attribute(null, "versionCode", Integer.toString(info.versionCode));
					xmlSerializer.attribute(null, "versionName", info.versionName);					
				}
				
				writeSetting(xmlSerializer, "settings_use_scrolling_switch", Boolean.toString(mScrollSwitch));
				writeSetting(xmlSerializer, "settings_arrow_key_width", Integer.toString(mKeyboard_width));
				writeSetting(xmlSerializer, "settings_arrow_key_group_count", Integer.toString(mKeyboardCount));
				
				for(int i = 1; i <= mKeyboardCount; i++) {
					writeKeyboardSetting(xmlSerializer, i);
				}
				xmlSerializer.endTag(null, "Keyboards");
				xmlSerializer.endDocument();
				xmlSerializer.flush();
				outputStream.close();
				
				mKeyboardBoxLayout.setDrawingCacheEnabled(true);
				String bitmapPath = mExportFile.getParent() + File.separator + "keyboard.png";
				boolean exportBitmap = false;
				Bitmap bitmap = mKeyboardBoxLayout.getDrawingCache();				
				if(bitmap != null) {								
					FileOutputStream bitmapOutputStream = new FileOutputStream(bitmapPath);
					bitmap.compress(CompressFormat.PNG, 100, bitmapOutputStream);				
					bitmapOutputStream.close();
					exportBitmap = true;
				}
				mKeyboardBoxLayout.setDrawingCacheEnabled(false);
				String message = getResources().getString(R.string.message_export_success, mExportFile.getPath());
				if(exportBitmap) {
					message += getResources().getString(R.string.message_export_preview_success, bitmapPath); 
				}
				AlertDialog.Builder builder = new AlertDialog.Builder(ExportKeyboardActivity.this);
				builder.setMessage(message);
				builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						finish();
					}
				});
				builder.setOnCancelListener(new OnCancelListener() {
					
					@Override
					public void onCancel(DialogInterface dialog) {
						finish();						
					}
				});
				builder.show();
//				Toast.makeText(ExportKeyboardActivity.this, message, 2000).show();
//				finish();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			mOkButton.setEnabled(true);
		}
	};
	
	private void writeKeyboardSetting(XmlSerializer xmlSerializer, int i) {
		String keyStart = ArrowKeyView.KEYHEAD + "_" + i;
		String key = keyStart + "_count";
		int keyCount = Integer.parseInt(mPref.getString(key , "8"));
		
		writeSetting(xmlSerializer, key, Integer.toString(keyCount));
		
		for(int j = 1; j <= keyCount; j++) {
			//Log.v("ArrowKeyView", "add button, num: " + j);
			key = keyStart + "_" + j; 
			String keyValue = mPref.getString(key, "NONE");
			writeSetting(xmlSerializer, key, keyValue);	
		}
		
	}
	
	private void writeSetting(XmlSerializer xmlSerializer, String key, String value) {
		try {
			xmlSerializer.startTag(null, "setting");
			xmlSerializer.attribute(null, "key", key);
			xmlSerializer.attribute(null, "value", value);
			xmlSerializer.endTag(null, "setting");
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private Runnable mEnableOkButtonRunnable = new Runnable() {
		@Override
		public void run() {
			mOkButton.setEnabled(true);
		}
	};
	
	private PackageInfo getPackageInfo() {
		PackageManager packageManager = getPackageManager();
		String packageName = getPackageName();

		try {
			PackageInfo info = packageManager.getPackageInfo(packageName, 0);
			return info;
		} catch (NameNotFoundException e) {
			return null;
		}
	}
}
