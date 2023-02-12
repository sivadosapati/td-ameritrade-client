package com.rise.trading.options.passive.income.backtest;

import java.util.Date;

public class HistoricalPriceForMinute {
	public Long timestamp;
	public Date d;
	public Double open, low, high, close, volume;

	public String toString() {
		return timestamp + "," + d + "," + open + "," + low + "," + high + "," + close + "," + volume;
	}

}