package tw.kenshinn.keyboardTerm;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.NullCipher;

import tw.kenshinn.keyboardTerm.R;

import android.R.integer;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Images;
import android.text.ClipboardManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.FrameLayout;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;

import com.roiding.rterm.bean.FunctionButton;
import com.roiding.rterm.bean.Host;
import com.roiding.rterm.util.DBUtils;
import com.roiding.rterm.util.TerminalManager;
import android.graphics.Bitmap;

public class TerminalActivity extends Activity {

	private static final String TAG = "TerminalActivity";
	protected static final int DIALOG_INPUT_HELP = 0;
	private DBUtils dbUtils;
	private Gallery functionKeyGallery;
	private Map<String, Gesture> gestureDescMap = new HashMap<String, Gesture>();
	
	private List<FunctionButton> functionBtnList;
	protected PowerManager.WakeLock m_wake_lock;
	private FrameLayout terminalFrame;
	private SharedPreferences pref;
	private ClipboardManager cm;
	private String mLogoutString;
	
	public static int termActFlags = 0;
	
	/**
	 * Disable magnifier.
	 */	
	public static final int FLAG_NO_MAGNIFIER = 0x1;
	
	/**
	 * Magnifier show during long press
	 */
	public static final int FLLAG_LONG_PRESS_SHOW = 0x2;
	
	/**
	 * Long press switch Magnifier/Gesture  
	 */
	public static final int FLLAG_LONG_PRESS_MODE_SWITCH = 0x4;
	
	/**
	 * Long press activate/deactivate magnifier
	 */
	public static final int FLLAG_LONG_PRESS_ACTIVATE = 0x8;
	
	/**
	 * Show extract input UI when user is trying to type non ASCII chars.
	 */
	public static final int FLAG_SHOW_EXTRACT_UI = 0x10; 
	
	/**
	 * Use full screen as magnifier display area
	 */
	public static final int FLAG_MAGNIFIER_FULLSCREEN = 0x20;
	
	public AlertDialog.Builder listBuilder;
	
	public boolean mAutoHideFunctionButton = true;
	
	class Gesture {
		public Gesture(String desc, Object action) {
			this.desc = desc;
			this.action = action;
		}

		public String desc;
		public Object action;
		
	}

	private static long currentViewId = -1;

	private RefreshHandler mHandler;

	class RefreshHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			close((Exception) msg.obj);
		}

		public void dispatch(Exception ex) {
			this.removeMessages(0);
			Message.obtain(this, -1, ex).sendToTarget();
		}
	};

	//private String[] gestureKey;
	//private String[] gestureDesc;
	GestureView mGestureView = null;
	InputMethodManager inputMethodManager = null;
	
	public GestureView getGestureView() {
		return mGestureView;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		inputMethodManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
		
		pref = PreferenceManager
				.getDefaultSharedPreferences(this);

		mLogoutString = pref.getString("settings_logout_keys", "^[[D^[[D^[[D^[[D^[[D^my^m^m");
		
		setStatusBarVisibility(false);
		
		cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		
		setContentView(R.layout.act_terminal);
		terminalFrame = (FrameLayout) findViewById(R.id.terminalFrame);		
		
		final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		this.m_wake_lock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK|PowerManager.ON_AFTER_RELEASE, "rTerm");
		this.m_wake_lock.acquire();
		
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		createGeastureMap();

		mGestureView = (GestureView) findViewById(R.id.gestureView);
		mGestureView.setTerminalActivity(this);
		
		// options
		termActFlags = (!pref.getBoolean("settings_enable_magnifier", true)? FLAG_NO_MAGNIFIER: 0) |
				(pref.getBoolean("settings_auto_extractui", false)? FLAG_SHOW_EXTRACT_UI:0) | 
				//Integer.parseInt(pref.getString("settings_magnifier_behavior", "8")) |
				8 |
				//(pref.getBoolean("settings_magnifier_fullscreen",true)?FLAG_MAGNIFIER_FULLSCREEN:0);
				FLAG_MAGNIFIER_FULLSCREEN;

		//This must be AFTER flags are set.
		mGestureView.setMagnifierParms(
				pref.getInt("settings_magnifier_focus_width", 30),
				pref.getInt("settings_magnifier_focus_height", 15), 
				pref.getInt("settings_magnifier_zoom", 20));
		
		
		mGestureView.setOnGestureListener(new OnGestureListener() {
			//TODO: We should make user define this
			public void onGestureEvent(String gesture) {
				if (gesture == null || gesture.length() == 0)
					return;
				
				if(gestureDescMap.containsKey(gesture)) {
					Gesture g = gestureDescMap.get(gesture);
					Object tag = g.action;
					if(tag != null){
						handleInputKey(tag);
					}
				}

//				if (gesture.equals("U")) {
//					pressKey(KeyEvent.KEYCODE_DPAD_UP);
//				} else if (gesture.equals("D")) {
//					pressKey(KeyEvent.KEYCODE_DPAD_DOWN);
//				} else if (gesture.equals("L")) {
//					pressKey(KeyEvent.KEYCODE_DPAD_LEFT);
//				} else if (gesture.equals("R")) {
//					pressKey(KeyEvent.KEYCODE_DPAD_RIGHT);
//				} else if (gesture.equals("D,L")) {
//					pressKey(KeyEvent.KEYCODE_ENTER);
//				} else if (gesture.equals("D,R,U")) {
//					pressKey(KeyEvent.KEYCODE_SPACE);
//				} else if (gesture.equals("R,U")) {
//					// page up
//					pressKey(new byte[] { 27, 91, 53, 126 });
//				} else if (gesture.equals("R,D")) {
//					// page down
//					pressKey(new byte[] { 27, 91, 54, 126 });
//				} else if (gesture.equals("L,U")) {
//					//HOME
//					pressKey(new byte[] { 27, '[','1','~'});
//				} else if (gesture.equals("L,D")) {
//					//END
//					pressKey(new byte[] { 27, '[','4','~'});					
//				} else if (gesture.equals("R,D,R") || gesture.equals("R,L,R")) {
//					// input helper
//					showInputHelper();
//				}
			}

			public String getGestureText(String gesture) {
				String desc = "Unknown Gesture";
				Gesture r = gestureDescMap.get(gesture);
				if (r != null)
					desc = r.desc;

				StringBuffer t = new StringBuffer();
				t.append("Gesture:").append(gesture).append(" (").append(desc)
						.append(")");

				return t.toString();
			}
		});

		if (dbUtils == null) {
			dbUtils = new DBUtils(this);
		}
		functionBtnList = dbUtils.functionsButtonsDelegate.get();

		functionKeyGallery = (Gallery) findViewById(R.id.functionKeyGallery);

		if (functionBtnList.size() > 0) {
			functionKeyGallery.setAdapter(new FunctionButtonAdapter(this));
		}
		functionKeyGallery.setBackgroundColor(Color.alpha(0));
		functionKeyGallery.setSelection(functionBtnList.size() / 2);
		functionKeyGallery.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if(mAutoHideFunctionButton) {
					changeFunctionKeyGalleryDisplay();
				}
					
				FunctionButton btn = functionBtnList.get(position);				
				pressFunctionButton(btn);
			}
		});
		
		mAutoHideFunctionButton = pref.getBoolean("settings_auto_hide_funtion_button", true);

		mHandler = new RefreshHandler();
		
		listBuilder = new AlertDialog.Builder(this);
		listBuilder.setTitle(this.getResources().getString(R.string.dialog_choose_url));
		
		if(!pref.contains("settings_use_arrow_key")) {
			PreferenceManager.setDefaultValues(this, R.xml.keyboards, true);
		}
		
	    if(pref.getBoolean("settings_use_arrow_key", false)) {
			LinearLayout terminal_root = (LinearLayout)findViewById(R.id.terminal_root_view);
	    	//View v = View.inflate(this, resId, null);
			boolean addFunctionButton = pref.getBoolean("settings_include_function_to_keyboard", false);
			View v = null;
			OnClickListener clickListener = new OnClickListener() {
				public void onClick(View v) {
					Object tag = v.getTag();
					handleInputKey(tag);
				}
			}; 
			if (addFunctionButton) {
				v = new ArrowKeyView(this, clickListener, functionBtnList);
			} else {
				v = new ArrowKeyView(this, clickListener);
			}
				
			int keyboard_height = this.getWindow().getDecorView().getMeasuredHeight();
	    	//Log.v("TerminalActivity", "keyboard_height: " + keyboard_height);
	    	int keyboard_width = Integer.parseInt(pref.getString("settings_arrow_key_width", "80"));
	    	
	    	Resources r = getResources();
	    	
	    	float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, keyboard_width, r.getDisplayMetrics());
	    	//Log.v("TerminalActivity", "arrow_key_width_dip: " + keyboard_width);
	    	//Log.v("TerminalActivity", "arrow_key_width_px: " + px);
	    	ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams((int)px, ViewGroup.LayoutParams.FILL_PARENT);
	    	
	    	int prev_spacing = Integer.parseInt(pref.getString("setting_arrow_key_prev_spacing", "0"));
	    	
	    	if(pref.getBoolean("settings_set_arrow_key_left", false)) {
	    		//Log.v("TerminalActivity", "margin left: " + prev_spacing);	    		
	    		terminal_root.addView(v, 0, lp);
	    		this.getWindow().getDecorView().setPadding(prev_spacing, 0, 0, 0);
	    			    	
	    	} else {
	    		//Log.v("TerminalActivity", "margin right: " + prev_spacing);	    		
	    		terminal_root.addView(v, lp);
	    		this.getWindow().getDecorView().setPadding(0, 0,prev_spacing, 0);
	    	}
	    }
	}
	
	private void sendSpecialKeys(String k) {
		boolean controlPressed = false;
		for (char c : k.toCharArray()) {
			if (c == '^') {
				controlPressed = true;
				pressMetaKey(KeyEvent.KEYCODE_DPAD_CENTER);
			} else {
				if (controlPressed) {
					c = String.valueOf(c).toLowerCase().charAt(0);
					KeyEvent[] events = TerminalView.DEFAULT_KEYMAP
							.getEvents(new char[] { c });

					pressKey(events[0].getKeyCode());
				} else {
					pressKey(c);
				}
				controlPressed = false;
			}
		}
	}	

	private void createGeastureMap() {
		if(!pref.contains("settings_gestures_" + "D_L_U"))
			PreferenceManager.setDefaultValues(this, R.xml.gestures, true);
		
		String[] gestureKey = getResources().getStringArray(R.array.gestures_key);
		String[] gestureDesc = getResources().getStringArray(R.array.gestures_desc);
		ArrayList<String> gestureDefs = new ArrayList<String>(); 
		String[] valueArray = getResources().getStringArray(R.array.gestures_defs);
		for(int i = 0; i < valueArray.length; i++) 
			gestureDefs.add(valueArray[i]);		 

		for (int i = 0; i < gestureKey.length; i++) {
			String key = gestureKey[i];
			String keyValue = pref.getString("settings_gestures_" + key.replace(',', '_'), null);
			Object tag = null;
			if(keyValue != null) {
				tag = ArrowKeyView.getKeyTag(keyValue);
				int index = gestureDefs.indexOf(keyValue);
				Gesture g = new Gesture(gestureDesc[index], tag);
				gestureDescMap.put(key, g);
			}
				
		}
	}
	
//	private void attachClickListen(int id) {
//		View v = findViewById(id);
//		if(v != null)
//			v.setOnClickListener(mArrowClickListener);
//	}
//	
//	  private OnClickListener mArrowClickListener = new OnClickListener() {
//
//			public void onClick(View v) {
//				int keycode = -1;
//				byte[] b = null;
//				switch(v.getId()) {		
//				case R.id.key_up:
//					keycode = KeyEvent.KEYCODE_DPAD_UP;
//					break;
//				case R.id.key_down:
//					keycode = KeyEvent.KEYCODE_DPAD_DOWN;
//					break;
//				case R.id.key_left:
//					keycode = KeyEvent.KEYCODE_DPAD_LEFT;
//					break;
//				case R.id.key_right:
//					keycode = KeyEvent.KEYCODE_DPAD_RIGHT;
//					break;
//				case R.id.key_enter:
//					keycode = KeyEvent.KEYCODE_ENTER;
//					break;
//				case R.id.key_y:
//					keycode = KeyEvent.KEYCODE_Y;
//					break;
//				case R.id.key_n:
//					keycode = KeyEvent.KEYCODE_N;
//					break;
//				case R.id.key_PageUp:
//					b = new byte[] { 27, 91, 53, 126 };					
//					break;
//				case R.id.key_PageDown:
//					b = new byte[] { 27, 91, 54, 126 };
//					break;
//				case R.id.key_foundUp:
//					keycode = KeyEvent.KEYCODE_LEFT_BRACKET;
//					break;
//				case R.id.key_foundDown:
//					keycode = KeyEvent.KEYCODE_RIGHT_BRACKET;
//					break;
//				case R.id.key_space:
//					keycode = KeyEvent.KEYCODE_SPACE;
//					break;
//				case R.id.key_home:
//					keycode = KeyEvent.KEYCODE_HOME;
//					break;
//				case R.id.key_end:
//					keycode = KeyEvent.KEYCODE_ENDCALL;
//					break;					
//				}
//				if(keycode != -1)
//					pressKey(keycode);
//				else
//					pressKey(b);
//			}
//			  
//		  };	

	public class FunctionButtonAdapter extends BaseAdapter {
		public FunctionButtonAdapter(Context c) {
			mContext = c;
		}

		public int getCount() {
			return functionBtnList.size();
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			System.out.println("functionBtnList=" + functionBtnList.size());
			Button btn = new Button(mContext);
			btn.setText(functionBtnList.get(position).getName());
			btn.setClickable(false);
			return btn;
		}

		private Context mContext;
	}

	@Override
	public void onStart() {
		super.onStart();

		Host host = (Host) getIntent().getExtras().getSerializable("host");

		currentViewId = host.getId();
		TerminalView view = TerminalManager.getInstance()
				.getView(currentViewId);
		
		if (view == null) {
			view = new TerminalView(this, null);
			view.terminalActivity = this;
			view.startConnection(host);
			TerminalManager.getInstance().putView(view);
			// checkService();
		}

		view.terminalActivity = this;

		showView(currentViewId);

	}

	public void refreshView() {
		showView(currentViewId);
	}

	public void showView(long id) {
		TerminalView view = TerminalManager.getInstance().getView(id);
		if (view != null) {
			view.terminalActivity = this;			
			//View osd = findViewById(R.id.terminalOSD); remove unuse code
			
			terminalFrame.removeAllViews();			
			terminalFrame.addView(view,LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT);
			//terminalFrame.addView(osd);
			
			view.requestFocus(); // sync lunaterm , for fix switch problem
			currentViewId = id;
			
		}
	}

	public TerminalView getCurrentTerminalView() {
		return TerminalManager.getInstance().getView(currentViewId);
	}
	
	public void showInputHelper(){
		LayoutInflater factory = LayoutInflater.from(TerminalActivity.this);
		final View textEntryView = factory.inflate(R.layout.act_input_helper, null);
		final EditText textEdit = (EditText) textEntryView.findViewById(R.id.text);

		final AlertDialog dialog = new AlertDialog.Builder(TerminalActivity.this)
			.setTitle(R.string.terminal_inputhelper)
			.setView(textEntryView).setPositiveButton(R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,int whichButton) {
							textEdit.onEditorAction(EditorInfo.IME_ACTION_DONE);
						}
					})					
			.setNegativeButton(R.string.cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,int whichButton) {
						}
			}).create();
		textEdit.setOnEditorActionListener(new OnEditorActionListener(){
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event){
				if(actionId == EditorInfo.IME_ACTION_DONE){
					String text = v.getText().toString();
					if (text != null && text.length() > 0){
						sendString(text);				
						/* dismiss dialog*/
						dialog.dismiss();
					}
					return true;
				}
				return false;
			}
		});
		dialog.show();
	}
	
  /* FIXME: This is ridiculous, use vt320.send()? */
  private void sendString(String s){
    /* replace \n with return*/
    s = s.replace('\n', '\r');
    pressKey(s);  
  }
	
	public boolean showUrlDialog(String url) {

		AlertDialog.Builder alert = new AlertDialog.Builder(
				TerminalActivity.this);

		alert.setTitle(R.string.title_openurl);

		// Set an EditText view to get user input
		final EditText input = new EditText(TerminalActivity.this);
		input.setText(url);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString();
				startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, Uri.parse(value)), getString(R.string.title_openurl)));
			}
		});

		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				});

		alert.show();
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		setStatusBarVisibility(false);
		
		switch(item.getItemId()){
		case R.id.terminal_disconnect:
			close(null);
			return true;		
		case R.id.terminal_login_again:
			final TerminalView currentView2 = TerminalManager.getInstance().getView(currentViewId);
			if(currentView2 != null){
				currentView2.loginAgagin();
			}			
			
			return true;
		case R.id.terminal_logout:
			sendSpecialKeys(mLogoutString);
			return true;			
		case R.id.terminal_share_snap:
			final TerminalView currentView = TerminalManager.getInstance().getView(currentViewId);
			if(currentView != null){				
				try{					
					final Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
					shareIntent.setType("image/png");
					
					final String FILENAME = "snapshot.png";
					FileOutputStream out = openFileOutput(FILENAME,MODE_WORLD_READABLE);
					currentView.bitmap.compress( Bitmap.CompressFormat.PNG, 100, out);
					out.close();
					
					Uri url = Uri.fromFile(getFileStreamPath(FILENAME));
					shareIntent.putExtra(Intent.EXTRA_STREAM,url);

					startActivity(Intent.createChooser(shareIntent, getText(R.string.terminal_share_via)));
				}catch(Exception e){
					Log.e(TAG,e.getMessage());	 e.printStackTrace();								
				}
			}
			return true;
		case R.id.terminal_inputhelper:
			showInputHelper();
			return true;
		case R.id.terminal_help:
			Intent helpIntent = new Intent();
			helpIntent.setClass(this,  HelpActivity.class);
			this.startActivity(helpIntent);
			return true;
		case R.id.terminal_switch_ime:
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.showInputMethodPicker();
			return true;
	    case R.id.terminal_paste:
	    	// sync lunaterm
			if (cm.getText() != null)
				sendString(cm.getText().toString());
			else
				Toast.makeText(TerminalActivity.this,
						getString(R.string.terminal_paste_fail_convert),
						Toast.LENGTH_LONG).show();	    	
	    	return true;			
		default: 
			return super.onOptionsItemSelected(item);			
		}
	}
	
	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		setStatusBarVisibility(true);
		return super.onMenuOpened(featureId, menu);
	}

	@Override
	public void onOptionsMenuClosed(Menu menu) {
		setStatusBarVisibility(false);
		super.onOptionsMenuClosed(menu);
	}
	
	 private void setStatusBarVisibility(boolean visible) {
	        int flag = visible ? 0 : WindowManager.LayoutParams.FLAG_FULLSCREEN;
	        getWindow().setFlags(flag, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.terminal_menu, menu);
		
		/* Dynamic add connection button */
		TerminalView[] views = TerminalManager.getInstance().getViews();
		for (final TerminalView view : views) {
			MenuItem item = menu.add(view.host.getName()).setIcon(
					R.drawable.online);
			if (views.length > 1)
				item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					public boolean onMenuItemClick(MenuItem item) {
						showView(view.host.getId());
						return true;
					}
				});
		}


	    /* if no clipboard content, no paste operation */
	    if(!cm.hasText())
	      menu.removeItem(R.id.terminal_paste);
		
		return true;
	}

	public void disconnect(Exception e) {
		mHandler.dispatch(e);
	}

	public void pressKey(String s) {
		Log.i(TAG, "pressKey(byte)");
		TerminalView currentView = TerminalManager.getInstance().getView(
				currentViewId);

		try {
			currentView.write(s.getBytes(currentView.host.getEncoding()));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void pressKey(char c) {
		pressKey(String.valueOf(c));
	}

	public void pressKey(byte[] b) {
		Log.i(TAG, "pressKey(byte)");
		TerminalView currentView = TerminalManager.getInstance().getView(
				currentViewId);

		try {
			currentView.write(b);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void pressMetaKey(int keyCode) {
		Log.i(TAG, "pressMetaKey=" + keyCode);
		TerminalView currentView = TerminalManager.getInstance().getView(
				currentViewId);

		try {
			currentView.processSpecialChar(keyCode, 65);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void pressKey(int keyCode) {
		Log.i(TAG, "pressKey=" + keyCode);
		TerminalView currentView = TerminalManager.getInstance().getView(
				currentViewId);
		
		try {
			currentView.processSpecialChar(keyCode, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void close(Exception e) {

		TerminalView currentView = TerminalManager.getInstance().getView(
				currentViewId);
		if (currentView == null)
			return;

		try {
			currentView.connection.disconnect();
		} catch (Exception _e) {}
		

		if (e != null) {
			String msg = e.getLocalizedMessage();

			Host currentHost = currentView.host;

			if (UnknownHostException.class.isInstance(e)) {
				msg = String
						.format(getText(R.string.terminal_error_unknownhost)
								.toString(), currentHost.getName());
			} else if (ConnectException.class.isInstance(e)) {
				msg = String.format(getText(R.string.terminal_error_connect)
						.toString(), currentHost.getName());
			}

			Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
		}

		TerminalManager.getInstance().removeView(currentViewId);
		// checkService();
		currentViewId = -1;
		finish();
	}

	public void changeFunctionKeyGalleryDisplay() {
		if (functionBtnList.size() == 0) {
			Toast
					.makeText(this, R.string.functionbtn_empty,
							Toast.LENGTH_SHORT).show();
		}

		if (functionKeyGallery.getVisibility() == View.VISIBLE)
			functionKeyGallery.setVisibility(View.GONE);
		else
			functionKeyGallery.setVisibility(View.VISIBLE);
	}

	@Override
	public void onStop() {
		super.onStop();
		
		View view = getCurrentTerminalView();
		if(view != null)
			terminalFrame.removeView(view);

		if (dbUtils != null) {
			dbUtils.close();
			dbUtils = null;
		}

		if (currentViewId == -1) {
			Toast.makeText(this, R.string.terminal_connectclose,
					Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume called");
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    	

	}

	@Override
	protected void onDestroy() {
		this.m_wake_lock.release();
		super.onDestroy();
	}

	private void handleInputKey(Object tag) {
		//Log.v("ArrowKeyView","onKeyClick, tag: " + tag);
		if(tag instanceof KeyEvent) {
			KeyEvent event = (KeyEvent)tag;
			try {
				TerminalManager.getInstance().getView(currentViewId).onKeyDown(event.getKeyCode(), event);
			} catch (Exception e) {
				e.printStackTrace();
			}			
		} else if(tag instanceof byte[]) {
			pressKey((byte[])tag);			
		} else if (tag instanceof ExtraAction) {
			ExtraAction extraAction = (ExtraAction)tag;
			switch (extraAction.actionCode) {
				case ExtraAction.ACTION_SHOW_INPUT_HELPER:
					showInputHelper();
				break;
			}
		} else if (tag instanceof String) {
			pressKey(tag.toString());
		} else if (tag instanceof FunctionButton) {
			pressFunctionButton((FunctionButton)tag);
		}
	}
	
	private void pressFunctionButton(FunctionButton btn) {
		String k = btn.getKeys();
		String v = btn.getName();

		sendSpecialKeys(k);
		if(btn.getOpenKeyboard()) {
			// open keyboard								
			inputMethodManager.toggleSoftInput(
					InputMethodManager.SHOW_FORCED, 0);
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.getRepeatCount() == 0) {
                event.startTracking();
                return true;
            } else {
                return false;
            }	        
	    }

	    return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)  {				
	    if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!event.isTracking() || event.isCanceled())
                return false;	    		       
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_BACK) {
	    	close(null);
	        return true;
	    }
	    return super.onKeyLongPress(keyCode, event);
	}
	
	
	public static class ExtraAction {
		public final static int ACTION_SHOW_INPUT_HELPER = 1;
		
		public int actionCode = 0;
		public ExtraAction() {					
		}
		
		public ExtraAction(int action) {
			actionCode = action;
		}
	}
	
}
