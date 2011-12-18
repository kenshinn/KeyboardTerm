package com.roiding.rterm.bean;

import java.io.Serializable;

import android.content.ContentValues;

import com.roiding.rterm.util.DBUtils;


public class FunctionButton implements Serializable {

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getKeys() {
		return keys;
	}
	public void setKeys(String keys) {
		this.keys = keys;
	}
	
	public boolean getOpenKeyboard() {
		return openKeyboard;
	}
	
	public void setOpenKeyboard(boolean openKeyboard) {
		this.openKeyboard = openKeyboard;
	}
		

	private static final long serialVersionUID = 1L;

	private long id;
	private String name;
	private String keys;
	private long sortNumber;
	private boolean openKeyboard;
	public long getSortNumber() {
		return sortNumber;
	}
	public void setSortNumber(long sortNumber) {
		this.sortNumber = sortNumber;
	}

	public ContentValues getValues() {
		ContentValues values = new ContentValues();

		values.put(DBUtils.FIELD_FUNCBTNS_NAME, name);
		values.put(DBUtils.FIELD_FUNCBTNS_KEYS, keys);
		values.put(DBUtils.FIELD_FUNCBTNS_SORTNUMBER, sortNumber);
		values.put(DBUtils.FIELD_FUNCBTNS_OPEN_KEYBOARD, openKeyboard ? 1 : 0);
		
		return values;
	}
	
}
