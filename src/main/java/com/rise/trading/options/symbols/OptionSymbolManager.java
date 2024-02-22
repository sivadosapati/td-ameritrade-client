package com.rise.trading.options.symbols;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.rise.trading.options.Util;

public class OptionSymbolManager {

	private Map<String, OptionSymbolWithAdjacents> optionSymbolWithAdjacents = new HashMap<String, OptionSymbolWithAdjacents>();

	private String file = "option_symbol_adjacents.txt";

	public static void main(String[] args) {
		OptionSymbolManager manager = new OptionSymbolManager();
		OptionSymbolWithAdjacents x = manager.getAdjacentOptionSymbols("RIOT_030124P14");
		System.out.println(x);
	}

	public OptionSymbolManager() {
		readOptionSymbolsWithAdjacentsFromFile(file);
	}

	private void readOptionSymbolsWithAdjacentsFromFile(String file) {
		try {
			List<String> lines = Files.readAllLines(Paths.get(file));
			for (String line : lines) {
				parseLine(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
			// throw new RuntimeException(e);
		}
	}

	private void parseLine(String line) {
		StringTokenizer st = new StringTokenizer(line, "=,");
		String key = st.nextToken();
		String lower = st.nextToken();
		String higher = st.nextToken();
		OptionSymbolWithAdjacents o = new OptionSymbolWithAdjacents(key, lower, higher);
		optionSymbolWithAdjacents.put(key, o);
	}

	public OptionSymbolWithAdjacents getAdjacentOptionSymbols(String optionSymbol) {
		OptionSymbolWithAdjacents x = optionSymbolWithAdjacents.get(optionSymbol);
		if (x != null) {
			return x;
		}
		x = findOptionSymbolWithAdjacentsFromOptionChain(optionSymbol);
		writeToFile(x);
		return x;
	}

	private void writeToFile(OptionSymbolWithAdjacents x) {
		try {
			PrintWriter writer = new PrintWriter(new FileWriter(file, true), true);
			writer.println(x.symbol + "=" + x.adjacentLowerSymbol + "," + x.adjacentHigherSymbol);
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private OptionSymbolWithAdjacents findOptionSymbolWithAdjacentsFromOptionChain(String optionSymbol) {
		return Util.findOptionSymbolsWithAdjacents(optionSymbol);
	}

}
