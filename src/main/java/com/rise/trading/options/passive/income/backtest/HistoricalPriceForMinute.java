package com.rise.trading.options.passive.income.backtest;

import java.util.Date;

public class HistoricalPriceForMinute {
	public Long timestamp;
	public String time;
	public Date d;
	public Double open, low, high, close, volume;
	
	void setTime() {
		this.time = new java.sql.Time(timestamp).toString();
	}

	public String toString() {
		return timestamp + "," + d + "," + open + "," + low + "," + high + "," + close + "," + volume;
	}

}