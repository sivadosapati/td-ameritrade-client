package com.rise.trading.options;

import java.util.List;

import com.studerw.tda.model.account.Order;

public abstract class AbstractPassiveIncomeInput implements PassiveIncomeInput {
	private String accountId;
	private String stockTicker;
	private Integer overrideLongQuantity = null;
	private Integer overrideShortQuantity = null;

	private List<Order> currentWorkingOrders = null;

	public List<Order> getCurrentWorkingOrders() {
		return currentWorkingOrders;
	}

	public void setCurrentWorkingOrders(List<Order> currentWorkingOrders) {
		this.currentWorkingOrders = currentWorkingOrders;
	}

	public Integer getOverrideLongQuantity() {
		return overrideLongQuantity;
	}

	public void setOverrideLongQuantity(Integer overrideLongQuantity) {
		this.overrideLongQuantity = overrideLongQuantity;
	}

	public Integer getOverrideShortQuantity() {
		return overrideShortQuantity;
	}

	public void setOverrideShortQuantity(Integer overrideShortQuantity) {
		this.overrideShortQuantity = overrideShortQuantity;
	}

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public String getStockTicker() {
		return stockTicker;
	}

	public void setStockTicker(String stockTicker) {
		this.stockTicker = stockTicker;
	}
}
