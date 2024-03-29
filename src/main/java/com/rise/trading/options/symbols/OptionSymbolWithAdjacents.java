package com.rise.trading.options.symbols;

public class OptionSymbolWithAdjacents {
	public OptionSymbolWithAdjacents(String symbol, String adjacentLowerSymbol, String adjacentHigherSymbol) {
		super();
		this.symbol = symbol;
		this.adjacentLowerSymbol = adjacentLowerSymbol;
		this.adjacentHigherSymbol = adjacentHigherSymbol;
	}

	public OptionSymbolWithAdjacents() {

	}

	public String toString() {
		return adjacentLowerSymbol + " -> " + symbol + " -> " + adjacentHigherSymbol;
	}
	
	public String toLine() {
		OptionSymbolWithAdjacents x = this;
		return x.symbol + "=" + x.adjacentLowerSymbol + "," + x.adjacentHigherSymbol;
	}
	
	public void setSymbolExists(boolean exists) {
		this.symbolExists = exists;
	}

	public boolean symbolExists = true;
	public String symbol;
	public String adjacentHigherSymbol;
	public String adjacentLowerSymbol;
}
