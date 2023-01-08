package com.rise.trading.options;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Accounts implements Serializable {
	public List<Account> accounts = new ArrayList<Account>();

	public void addAccount(String id, String name) {
		accounts.add(new Account(id, name));
	}
	public Account[] getAccounts() {
		return (Account[])accounts.toArray(new Account[accounts.size()]);
	}
}
