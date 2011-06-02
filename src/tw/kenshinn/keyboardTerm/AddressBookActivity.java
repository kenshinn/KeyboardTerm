package tw.kenshinn.keyboardTerm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import tw.kenshinn.keyboardTerm.R;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.inputmethod.InputMethodManager;
import 	android.view.KeyEvent;
import android.widget.AdapterView;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;

import com.adwhirl.AdWhirlLayout;
import com.adwhirl.AdWhirlManager;
import com.adwhirl.adapters.AdWhirlAdapter;
import com.roiding.rterm.EditHostActivity;
import com.roiding.rterm.SettingsActivity;
import com.roiding.rterm.bean.FunctionButton;
import com.roiding.rterm.bean.Host;
import com.roiding.rterm.util.DBUtils;
import com.roiding.rterm.util.TerminalManager;
//import com.vpon.adon.android.AdListener;
//import com.vpon.adon.android.AdOnPlatform;
//import com.vpon.adon.android.AdView;

public class AddressBookActivity extends ListActivity {
	private static final String TAG = "AddressBook";
	private static List<Host> hosts;
	private static List<Host> quickConnectHosts = new ArrayList<Host>();
	private DBUtils dbUtils;
	private SharedPreferences prefs;	
		
	private RelativeLayout adonContainerView;
	//private String adOnKey = "ff8080812e04d607012e2293fe2303cd";//change your application AdOn license key
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);

		/*SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(this);
		String languageToLoad = pref.getString(Constants.SETTINGS_LANGUAGE,
				"en");

		Log.i(TAG, languageToLoad);

		String[] localeStr = new String[] { languageToLoad, "" };
		if (languageToLoad.indexOf("_") > 0)
			localeStr = languageToLoad.split("_");
		Locale locale = new Locale(localeStr[0], localeStr[1]);

		Locale.setDefault(locale);
		Configuration config = new Configuration();
		config.locale = locale;
		
		//FIXME: albb0920.100714: Why is this line comment out?
//		getBaseContext().getResources().updateConfiguration(config,getBaseContext().getResources().getDisplayMetrics());*/
				
		setContentView(R.layout.act_addressbook);
		
		((TextView)findViewById(R.id.quickConnect)).setOnEditorActionListener(
				new OnEditorActionListener(){
					public boolean onEditorAction(TextView v, int actionId, KeyEvent event){
						if(event != null)
							quickConnect();							
						return false;
					}					
				});  
		findViewById(R.id.quickConnect_button).setOnClickListener(
				new View.OnClickListener(){
					 public void onClick(View v) {
						 quickConnect();				 
					 }
				});
		
//		AdWhirlAdapter.setGoogleAdSenseAppName("KeyboardTerm"); 
//		AdWhirlAdapter.setGoogleAdSenseCompanyName("Kenshinn.TW"); 
		
		String keyAdWhirl = "c7bce28b019a4e8dbcf33091bce6b542";
		SharedPreferences adWhirlPrefs = this.getSharedPreferences(
				keyAdWhirl, Context.MODE_PRIVATE);
		
		String jsonString = adWhirlPrefs.getString("config", null);		
		
		if(jsonString != null && jsonString.trim().equals("[]")) {			
			SharedPreferences.Editor editor = adWhirlPrefs.edit();
			editor.remove("config");
			editor.commit();
		}
		
		
		AdWhirlLayout adWhirlLayout = new AdWhirlLayout(this, keyAdWhirl);			//1
        adonContainerView = (RelativeLayout)findViewById(R.id.ad);//1
        adonContainerView.addView(adWhirlLayout);	
		
		
//	     	AdView  adView = new AdView(this); 	
//	        adonContainerView = (RelativeLayout)findViewById(R.id.ad);//1
//	        adonContainerView.addView(adView); 								//2
//			boolean autoRefreshAd = true; 									//3
//			adView.setLicenseKey(adOnKey, AdOnPlatform.TW , autoRefreshAd); //4
//			AdListener adListener = new AdListener() { 						//5
//				public void onRecevieAd(AdView adView) {
//					/*
//					 * 廣告抓取成功時,
//					 * 我們建議您可以在這函式中替AdView增加一些簡單的動畫,
//					 * 動畫範例程式碼如下
//					 */
//					//rotationHoriztion(0, 360, adView);
//					Log.v("AdOn", "onRecevieAd");
//				}
//				
//				public void onFailedToRecevieAd(AdView adView) {
//					//廣告抓取失敗
//					Log.i("AdOn", "OnFailesToRecevieAd");
//				}
//			};
//			adView.setAdListener(adListener); 
		
//	    AdManager.setTestDevices( new String[] {
//	    	        "55147E086EFCE3CC36FF34AE118206C1" 
//	    	    } );
	}
	private void quickConnect(){
		String hostname = ((TextView)findViewById(R.id.quickConnect)).getText().toString();
		int port = 23,pos;
		/* ignore protocol */
		if((pos = hostname.indexOf("://")) !=-1)
			hostname = hostname.substring(pos+3);			
		
		if((pos = hostname.indexOf(":"))!=-1){ //extract port number
			try{
				port = Integer.parseInt(hostname.substring(pos+1));
				hostname = hostname.substring(0, pos);
			}catch (Exception e){
				Toast.makeText(this, R.string.addressbook_quick_connect_failed_parse, 1000);
			}
		}
		if(hostname.indexOf(".")==-1) //If it's not a domain name, assume it's twbbs prefix
			hostname += ".twbbs.org"; 
		//Start Connection
		Host host = new Host();
		host.setHost(hostname);
		host.setPort(port);

	    String lang = Locale.getDefault().getCountry();
	    
	    if("CN".equals(lang)){
	      host.setEncoding("GBK");
	    }
	    else{
	      host.setEncoding("Big5");
	    }
		
		
		host.setProtocal("Telnet");
		host.setName("("+hostname+":"+port+")");
		host.setId(-1*(quickConnectHosts.size()+2)); // use negative id to cheat DBtools, it's below -2 because -1 is magic number 

		quickConnectHosts.add(host);
		connect(host);
	}

	private void initFunctionBtns() {
		String[] functionBtnKey = getResources().getStringArray(
				R.array.function_buttons_key);
		String[] functionBtnDesc = getResources().getStringArray(
				R.array.function_buttons_desc);

		for (int i = 0; i < functionBtnKey.length; i++) {
			FunctionButton btn = new FunctionButton();
			btn.setName(functionBtnDesc[i]);
			btn.setKeys(functionBtnKey[i]);
			btn.setSortNumber(i);
			dbUtils.functionsButtonsDelegate.insert(btn);
		}

	}
	
	private void initHost() {
		String lang = Locale.getDefault().getCountry();
		
	    if("CN".equals(lang)){
	    	initZhCnHost();		      
	    }
	    else if ("TW".equals(lang)){
	    	initZhTwHost();	      
	    } else {
	    	initZhCnHost();
	    	initZhTwHost();
	    }
	}

	/*
	 * init China BBS sites
	 */
	private void initZhCnHost() {
		Host h1 = new Host();
		h1.setName(getText(R.string.addressbook_site_lilacbbs).toString());
		h1.setProtocal("Telnet");
		h1.setEncoding("GBK");
		h1.setHost("lilacbbs.com");
		h1.setPort(23);
		dbUtils.hostDelegate.insert(h1);

		Host h2 = new Host();
		h2.setName(getText(R.string.addressbook_site_newsmth).toString());
		h2.setProtocal("Telnet");
		h2.setEncoding("GBK");
		h2.setHost("newsmth.net");
		h2.setPort(23);
		dbUtils.hostDelegate.insert(h2);

		Host h3 = new Host();
		h3.setName(getText(R.string.addressbook_site_lqqm).toString());
		h3.setProtocal("Telnet");
		h3.setEncoding("GBK");
		h3.setHost("lqqm.net");
		h3.setPort(23);
		dbUtils.hostDelegate.insert(h3);

	}

	/*
	 * init Taiwan BBS sites
	 */
	private void initZhTwHost() {
		Host h4 = new Host();
		h4.setName(getText(R.string.addressbook_site_ptt).toString());
		h4.setProtocal("Telnet");
		h4.setEncoding("Big5");
		h4.setHost("ptt.cc");
		h4.setPort(23);
		dbUtils.hostDelegate.insert(h4);

		Host h5 = new Host();
		h5.setName(getText(R.string.addressbook_site_ptt2).toString());
		h5.setProtocal("Telnet");
		h5.setEncoding("Big5");
		h5.setHost("ptt2.twbbs.org");
		h5.setPort(23);
		dbUtils.hostDelegate.insert(h5);
	}

	/**
	 * This is a temporary workaround to make quick connected host appear in the list,
	 * without messing up host variable. 
	 * 
	 * @param position position in list
	 * @return the host object of the position
	 */
	private Host getRealHost(int position){
		//TODO: This is just so ugly, rewrite quick connection hosts someday. 
		if(position < quickConnectHosts.size())
			return quickConnectHosts.get(position);
		else	
			return hosts.get(position-quickConnectHosts.size());		
	}
	
	@Override
	public void onResume() {
		super.onResume();

		if (dbUtils == null)
			dbUtils = new DBUtils(this);
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (!prefs.getBoolean("INITIALIZED", false)) {
			initHost();
			initFunctionBtns();
			Editor editor = prefs.edit();
			editor.putBoolean("INITIALIZED", true);
			editor.commit();
			AlertDialog.Builder builder = new AlertDialog.Builder(AddressBookActivity.this);
			builder.setMessage(R.string.initial_message);
			builder.setPositiveButton("OK", null);
			builder.show();
		}

		this.getListView().setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {				
				Host host = getRealHost(position);
				Log.i(TAG, host.getHost());
				connect(host);
			}

		});
		this.registerForContextMenu(this.getListView());

		update();

	}

	private void connect(Host host) {
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(((TextView)findViewById(R.id.quickConnect)).getWindowToken(), 0);
		
		Intent intent = new Intent();
		intent.setClass(AddressBookActivity.this, TerminalActivity.class);
		intent.putExtra("host", host);
		Log.v(TAG,""+host.getId());
		Toast.makeText(AddressBookActivity.this, host.getName(),
				Toast.LENGTH_SHORT).show();

		AddressBookActivity.this.startActivity(intent);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i(TAG, "onActivityResult");
		update();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		MenuItem add = menu.add(R.string.addressbook_add_host).setIcon(
				android.R.drawable.ic_menu_add);

		add.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				Intent intent = new Intent();
				intent.setClass(AddressBookActivity.this,
						EditHostActivity.class);
				AddressBookActivity.this.startActivityForResult(intent, 0);
				return true;
			}
		});

		MenuItem settings = menu.add(R.string.addressbook_settings).setIcon(
				android.R.drawable.ic_menu_preferences);

		settings.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				Intent intent = new Intent();
				intent.setClass(AddressBookActivity.this,
						SettingsActivity.class);
				AddressBookActivity.this.startActivityForResult(intent, 0);
				return true;
			}
		});

		MenuItem help = menu.add(R.string.addressbook_help).setIcon(
				android.R.drawable.ic_menu_help);

		help.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				Intent intent = new Intent();
				intent.setClass(AddressBookActivity.this, HelpActivity.class);
				AddressBookActivity.this.startActivityForResult(intent, 0);
				return true;
			}
		});
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		final Host host = getRealHost(info.position);

		menu.setHeaderTitle(host.getName());

		MenuItem connect = menu.add(R.string.addressbook_connect_host);
		connect.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				connect(host);
				return true;
			}
		});

		if(hosts.indexOf(host)!=-1){ // not implemented for quick connect
			MenuItem edit = menu.add(R.string.addressbook_edit_host);
			edit.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					Intent intent = new Intent();
					intent.setClass(AddressBookActivity.this,
							EditHostActivity.class);
					intent.putExtra("host", host);
					AddressBookActivity.this.startActivityForResult(intent, 0);
					return true;
				}
			});
	
			MenuItem delete = menu.add(R.string.addressbook_delete_host);
			delete.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					dbUtils.hostDelegate.delete(host);
					AddressBookActivity.this.update();
					return true;
				}
			});
		}
	}

	protected void update() {
		if (dbUtils == null) {
			dbUtils = new DBUtils(this);
		}

		hosts = dbUtils.hostDelegate.get();

		SimpleAdapter adapter = new SimpleAdapter(this, getList(hosts),
				R.layout.item_addressbook_list, new String[] { "name", "uri",
						"icon" }, new int[] { android.R.id.text1,
						android.R.id.text2, android.R.id.icon });

		this.setListAdapter(adapter);
	}

	private List<Map<String, String>> getList(List<Host> list) {
		ArrayList<Host> all = new ArrayList<Host>(quickConnectHosts);
		all.addAll(list);				
		ArrayList<Map<String, String>> hostList = new ArrayList<Map<String, String>>();
		
		for (Host h : all) {
			Map<String, String> map = new HashMap<String, String>();
			map.put("name", h.getName());
			String uri = h.getProtocal() + "://" + h.getHost();
			if (h.getPort() != 23)
				uri = uri + ":" + h.getPort();
			map.put("uri", uri);

			if (TerminalManager.getInstance().getView(h.getId()) != null)
				map.put("icon", String.valueOf(R.drawable.online));
			else if(list.indexOf(h)!=-1)
				map.put("icon", String.valueOf(R.drawable.offline));
			else{
				Log.i(TAG,"Quick connect: "+h.getName()+" removed from list");
				quickConnectHosts.remove(h);
				continue;
			}
			
			hostList.add(map);
		}
		return hostList;
	}

	@Override
	public void onStop() {
		super.onStop();

		if (dbUtils != null) {
			dbUtils.close();
			dbUtils = null;
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
	    if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() > 0) {	        
	        return true;
	    }

	    return super.onKeyDown(keyCode, event);
	}

}

