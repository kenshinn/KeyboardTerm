package tw.kenshinn.keyboardTerm;

import tw.kenshinn.keyboardTerm.R;
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
	private final static String KEYHEAD = "settings_arrow_key_keyboards";
	private OnClickListener mClickListener = null;
	ViewFlipper mVFlipper = null;
	
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
		
		mVFlipper = new ViewFlipper(this.getContext());
		AlphaAnimation inAnimation = new AlphaAnimation(0.0f, 1.0f);
		AlphaAnimation outAnimation = new AlphaAnimation(1.0f, 0.0f);
		inAnimation.setDuration(500);
		outAnimation.setDuration(500);
		mVFlipper.setInAnimation(inAnimation);
		mVFlipper.setOutAnimation(outAnimation);
		int keyboardCount = Integer.parseInt(pref.getString("settings_arrow_key_group_count", "1"));
		//int keyboardCount = 1;
		for(int i = 1; i <= keyboardCount; i++) {
			LinearLayout keyboardLayout = new LinearLayout(this.getContext());
			ViewGroup.LayoutParams lParams = new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			keyboardLayout.setOrientation(LinearLayout.VERTICAL);						
			mVFlipper.addView(keyboardLayout, lParams);
			
			String keyStart = KEYHEAD + "_" + i;
			int keyCount = Integer.parseInt(pref.getString(keyStart + "_count" , "8"));
			for(int j = 1; j <= keyCount; j++) {
				Log.v("ArrowKeyView", "add button, num: " + j);
				String key = keyStart + "_" + j; 
				String keyValue = pref.getString(key, "");
				View button = initKeyView(keyValue);
				if(button != null) {
					LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
					llParams.weight = 1.0f;
					keyboardLayout.addView(button, llParams);
					Log.v("ArrowKeyView", "add button");
				}
			}
			if(keyboardCount > 1) {
				View switchButton = getSwitchButton();
				LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
				llParams.weight = 1.0f;
				keyboardLayout.addView(switchButton, llParams);
			}
		}
		ArrowKeyView.this.addView(mVFlipper);
	}
	
	private View initKeyView(String keyValue) {
		if(keyValue.contains(",")){
			LinearLayout buttonLayout = new LinearLayout(this.getContext());
			buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
			LinearLayout.LayoutParams lParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			lParams.weight = 1.0f;
			String[] list = keyValue.split(",");
			for(int i = 0; i < list.length; i++) {
				View button = initKeyView(list[i]);
				if(button != null)
					buttonLayout.addView(button, lParams);
			}
			return buttonLayout;
		}
		
		KeyboardButton button = new KeyboardButton(this.getContext());
		Object tag = getKeyTag(keyValue);
		if(tag != null)
			button.setTag(tag);
		
		if(mClickListener != null)
			button.setOnClickListener(mClickListener);
		
		String value = null;
		
		try {
			int resId = R.string.class.getDeclaredField("key_" + keyValue).getInt(null);
			value = getResources().getString(resId);
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
			drawable = getResources().getDrawable(resId);
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
			Log.v("ArrowKeyView", "set drawable, keyValue: " + keyValue);
			button.setKeyDrawable(drawable);
			return button;
		}			
		
		button.setText(keyValue);
		
		return button;
	}
	
	private View getSwitchButton() {
		KeyboardButton button = new KeyboardButton(this.getContext());
		button.setKeyDrawable(getResources().getDrawable(R.drawable.keyboard_switch));
		button.setOnClickListener(mSwitchClickListener);
		return button;
	}
	
	private OnClickListener mSwitchClickListener = new OnClickListener() {
		
		public void onClick(View v) {
			mVFlipper.showNext();
			
		}
	};
	
	private Object getKeyTag(String keyValue) {
		Object result = null;
		if(keyValue.equals("PAGE_UP")) {
			result = new byte[] { 27, 91, 53, 126 };
		} else if(keyValue.equals("PAGE_DOWN")) {
			result = new byte[] { 27, 91, 54, 126 };
		} else if(keyValue.equals("HOME")) {
			result = new byte[] { 27, '[','1','~'};
		} else if(keyValue.equals("END")) {
			result = new byte[] { 27, '[','4','~'};
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

	private class KeyboardButton extends Button {

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
