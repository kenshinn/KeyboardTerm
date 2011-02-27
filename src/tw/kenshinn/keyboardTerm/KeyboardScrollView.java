package tw.kenshinn.keyboardTerm;

import tw.kenshinn.keyboardTerm.R;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class KeyboardScrollView extends ScrollView {

	public KeyboardScrollView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	public KeyboardScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}
	
	public KeyboardScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		// TODO Auto-generated method stub
		super.onSizeChanged(w, h, oldw, oldh);
		//Log.v("KeyboardScrollView", "onSizeChanged, oldH: " + oldh + ", newH: " + h);
    	LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, h);
    	ViewGroup group = (ViewGroup)(KeyboardScrollView.this.getChildAt(0));
    	for(int i = 0; i < group.getChildCount(); i++) {
    		View v = group.getChildAt(i);
    		if(v.getVisibility() == View.VISIBLE) {
    			v.setLayoutParams(llp);
    			v.forceLayout();
    		}
    			
    	}
    	//findViewById(R.id.mainKeyboard).setLayoutParams(llp);
    	//findViewById(R.id.extraKeyboard).setLayoutParams(llp);

	}

	
}
