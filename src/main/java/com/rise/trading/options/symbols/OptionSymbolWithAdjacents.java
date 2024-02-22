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

	public String symbol;
	public String adjacentHigherSymbol;
	public String adjacentLowerSymbol;
}
