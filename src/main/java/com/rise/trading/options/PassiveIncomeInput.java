package com.rise.trading.options;

import java.util.List;

import com.studerw.tda.model.account.Order;

public interface PassiveIncomeInput {
	String getAccountId();

	String getStockTicker();

	Integer getOverrideLongQuantity();

	Integer getOverrideShortQuantity();

	List<Order> getCurrentWorkingOrders();

}
