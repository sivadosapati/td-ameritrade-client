package com.rise.trading.options.passive.income.timers;

public class PassiveIncomeInput {
	public String getAccountId() {
		return accountId;
	}
	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}
	public String getTicker() {
		return ticker;
	}
	public void setTicker(String ticker) {
		this.ticker = ticker;
	}
	public int getContracts() {
		return contracts;
	}
	public void setContracts(int contracts) {
		this.contracts = contracts;
	}
	public float getStrikeDistanceForCall() {
		return strikeDistanceForCall;
	}
	public void setStrikeDistanceForCall(float strikeDistanceForCall) {
		this.strikeDistanceForCall = strikeDistanceForCall;
	}
	public float getStrikeDistanceForPut() {
		return strikeDistanceForPut;
	}
	public void setStrikeDistanceForPut(float strikeDistanceForPut) {
		this.strikeDistanceForPut = strikeDistanceForPut;
	}
	private String accountId;
	private String ticker;
	private int contracts;
	private float strikeDistanceForCall;
	private float strikeDistanceForPut;
}
