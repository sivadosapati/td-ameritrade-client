package com.rise.trading.options;

import java.util.ArrayList;
import java.util.List;

import com.studerw.tda.model.account.Position;

public class GroupedPosition {
	private String symbol;
	private Position equity;
	private Position cash;
	private List<Position> options = new ArrayList<Position>();
	private Position shortEquity;
	
	private Double currentStockPrice;
	public void setCurrentStockPrice(Double x) {
		this.currentStockPrice = x;
		
	}
	public Double getCurrentStockPrice() {
		return currentStockPrice;
	}
	
	public Position getCash() {
		return cash;
	}

	public void setCash(Position c) {
		this.cash = c;
	}
	public List<Position> getOptions() {
		return options;
	}

	public GroupedPosition(String symbol) {
		this.symbol = symbol;
	}

	public String toString() {
		return symbol + "\nEquity Positions...\n" + equity + "\nCash Positions...\n" + cash + "\nOption Positions...\n"
				+ options;
	}

	public int getNumberOfPotentialCoveredCallContracts() {
		if (equity == null) {
			return 0;
		}
		int potentialCallContracts = (int) (equity.getLongQuantity().doubleValue() / 100);
		return potentialCallContracts;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setEquity(Position p) {
		this.equity = p;
		
	}
	public void setShortEquity(Position p) {
		this.shortEquity = p;
	}

	public Position getShortEquity() {
		return shortEquity;
	}
	public void addOption(Position p) {
		options.add(p);
		
	}

	public Position getEquity() {
		// TODO Auto-generated method stub
		return equity;
	}

}