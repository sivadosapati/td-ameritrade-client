package com.rise.trading.options;

import java.io.Serializable;

public class Account implements Serializable{
	public String id;
	public String name;
	public String[] skippedSymbols = new String[] {};

	public Account() {
	}

	public Account(String id, String name) {
		this.id = id;
		this.name = name;
	}
	public String toString() {
		return id+" : "+name;
	}
}
