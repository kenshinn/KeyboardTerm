package tw.kenshinn.keyboardTerm;

import tw.kenshinn.keyboardTerm.R;
import tw.kenshinn.keyboardTerm.TerminalActivity.ExtraAction;
import android.R.integer;
import android.R.string;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ViewFlipper;

public class ArrowKeyView extends FrameLayout {

	private SharedPreferences pref;
	final static String KEYHEAD = "settings_arrow_key_keyboards";
	private OnClickListener mClickListener = null;
	ViewGroup mSwitch = null;
	
	public ArrowKeyView(Context context, OnClickListener clickListener) {
		super(context);
		// TODO Auto-generated constructor stub
		mClickListener = clickListener;
		init();
	}
	
	public ArrowKeyView(Context context, AttributeSet attrs, OnClickListener clickListener) {
		super(context, attrs);
		mClickListener = clickListener;
		init();
	}
	
	private void init() {
		pref = PreferenceManager.getDefaultSharedPreferences(this.getContext());
		if(pref == null)
			return;
		
		if(!pref.contains("settings_arrow_key_group_count"))
			PreferenceManager.setDefaultValues(getContext(), R.xml.keyboards, true);
		
		boolean scrollSwitch = pref.getBoolean("settings_use_scrolling_switch", false);
		 
		
		if(scrollSwitch) {
			mSwitch = new KeyboardScrollView(this.getContext());
		} else {
			ViewFlipper flipper = new ViewFlipper(this.getContext());		
			AlphaAnimation inAnimation = new AlphaAnimation(0.0f, 1.0f);
			AlphaAnimation outAnimation = new AlphaAnimation(1.0f, 0.0f);
			inAnimation.setDuration(500);
			outAnimation.setDuration(500);
			flipper.setInAnimation(inAnimation);
			flipper.setOutAnimation(outAnimation);
			mSwitch = flipper;
		}
		ArrowKeyView.this.addView(mSwitch);
		
		if(scrollSwitch) {
			LinearLayout linearLayout = new LinearLayout(this.getContext());
			linearLayout.setOrientation(LinearLayout.VERTICAL);
			mSwitch.addView(linearLayout, ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
			mSwitch = linearLayout;
		}
		
		int keyboardCount = Integer.parseInt(pref.getString("settings_arrow_key_group_count", "1"));
		//int keyboardCount = 1;
		boolean showSwitchButton = !scrollSwitch && keyboardCount > 1;
		for(int i = 1; i <= keyboardCount; i++) {
			LinearLayout keyboardLayout = generateKeyboardLayout(getContext(), pref,showSwitchButton, i, mClickListener, mSwitchClickListener);
			ViewGroup.LayoutParams lParams = new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			mSwitch.addView(keyboardLayout, lParams);
		}
		
	}

	public static LinearLayout generateKeyboardLayout(Context context, SharedPreferences pref, boolean showSwitchButton, int i, OnClickListener clickListener, OnClickListener switchClickListener) {
		LinearLayout keyboardLayout = new LinearLayout(context);
		keyboardLayout.setOrientation(LinearLayout.VERTICAL);
		
		String keyStart = KEYHEAD + "_" + i;
		int keyCount = Integer.parseInt(pref.getString(keyStart + "_count" , "8"));
		for(int j = 1; j <= keyCount; j++) {
			//Log.v("ArrowKeyView", "add button, num: " + j);
			String key = keyStart + "_" + j; 
			String keyValue = pref.getString(key, "NONE");
			if(keyValue.equals("NONE"))
				continue;
			View button = initKeyView(context, keyValue, clickListener);
			if(button != null) {
				LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
				llParams.weight = 1.0f;
				keyboardLayout.addView(button, llParams);
				//Log.v("ArrowKeyView", "add button");
			}
		}
		
		if(showSwitchButton) {
			View switchButton = getSwitchButton(context, switchClickListener);
			LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			llParams.weight = 1.0f;
			keyboardLayout.addView(switchButton, llParams);
		}
		return keyboardLayout;
	}
	
	private static View initKeyView(Context context, String keyValue, OnClickListener clickListener) {
		if(keyValue.contains(",") && !keyValue.startsWith("custom_")){
			LinearLayout buttonLayout = new LinearLayout(context);
			buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
			LinearLayout.LayoutParams lParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			lParams.weight = 1.0f;
			String[] list = keyValue.split(",");
			for(int i = 0; i < list.length; i++) {
				View button = initKeyView(context, list[i], clickListener);
				if(button != null)
					buttonLayout.addView(button, lParams);
			}
			return buttonLayout;
		}
		
		KeyboardButton button = new KeyboardButton(context);
		Object tag = getKeyTag(keyValue);
		if(clickListener != null) {			
			if(tag != null)
				button.setTag(tag);
			else {
				button.setEnabled(false);
				return button;				
			}
			button.setOnClickListener(clickListener);
		}

		
		if(tag instanceof String) {
			button.setText(tag.toString());
			return button;
		}
		
		String value = null;
		
		try {
			int resId = R.string.class.getDeclaredField("key_" + keyValue).getInt(null);
			value = context.getResources().getString(resId);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(value != null) {
			button.setText(value);
			return button;
		}
		
		Drawable drawable = null;
		
		try {
			int resId = R.drawable.class.getDeclaredField("keyboard_" + keyValue.toLowerCase()).getInt(null);
			drawable = context.getResources().getDrawable(resId);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
		if(drawable != null) {
			//Log.v("ArrowKeyView", "set drawable, keyValue: " + keyValue);
			button.setKeyDrawable(drawable);
			return button;
		}			
		
		button.setText(keyValue);
		
		return button;
	}
	
	private static View getSwitchButton(Context context, OnClickListener switchClickListener) {
		KeyboardButton button = new KeyboardButton(context);
		button.setKeyDrawable(context.getResources().getDrawable(R.drawable.keyboard_switch));
		if(switchClickListener != null)
			button.setOnClickListener(switchClickListener);
		return button;
	}
	
	private OnClickListener mSwitchClickListener = new OnClickListener() {
		
		public void onClick(View v) {
			if(mSwitch instanceof ViewFlipper)
				((ViewFlipper)mSwitch).showNext();			
		}
	};
	
	public static Object getKeyTag(String keyValue) {
		Object result = null;
		if(keyValue == null || keyValue.length() == 0 || keyValue.endsWith("NONE"))
			return null;
		else if(keyValue.equals("PAGE_UP")) {
			result = new byte[] { 27, 91, 53, 126 };
		} else if(keyValue.equals("PAGE_DOWN")) {
			result = new byte[] { 27, 91, 54, 126 };
		} else if(keyValue.equals("HOME")) {
			result = new byte[] { 27, '[','1','~'};
		} else if(keyValue.equals("END")) {
			result = new byte[] { 27, '[','4','~'};
		} else if(keyValue.equals("InputHelper")) {
			result = new ExtraAction(ExtraAction.ACTION_SHOW_INPUT_HELPER);			
		} else if(keyValue.startsWith("custom_")) {
			String value = keyValue.replaceAll("^custom_", "");
			result = value;
		} else {
			int KeyCode = -1;
			try {
				KeyCode = KeyEvent.class.getDeclaredField("KEYCODE_" + keyValue).getInt(null);
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if(KeyCode != -1)
				result = new KeyEvent(KeyEvent.ACTION_DOWN, KeyCode);
		}
		return result;
	}

	private static class KeyboardButton extends Button {

		private Drawable mKeyDrawable = null;
		
		public void setKeyDrawable(Drawable drawable) {
			mKeyDrawable = drawable;
			this.invalidate();
		}
		
		public KeyboardButton(Context context) {
			super(context);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void onDraw(Canvas canvas) {
			// TODO Auto-generated method stub
			
			
			
			if(mKeyDrawable != null) {
				mKeyDrawable.setBounds(2, 2,  this.getMeasuredWidth() - 2, this.getMeasuredHeight() - 2);
				mKeyDrawable.draw(canvas);
			}
			else 
				super.onDraw(canvas);
		}
		
		
	}
	
}
