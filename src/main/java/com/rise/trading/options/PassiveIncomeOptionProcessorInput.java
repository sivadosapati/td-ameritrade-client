package com.rise.trading.options;

import com.rise.trading.options.PassiveIncomeStrategy.OptionPositions;
import com.studerw.tda.model.account.Position;

public class PassiveIncomeOptionProcessorInput {
	public Position position;
	public double currentStockPrice;
	public String accountId;
	public String ticker;
	public OptionPositions optionPositions;
	
	private OptionData optionData = null;
	
	
	public static PassiveIncomeOptionProcessorInput make(Position p, String accountId, String ticker, double stockPrice, OptionPositions op) {
		PassiveIncomeOptionProcessorInput xx = new PassiveIncomeOptionProcessorInput();
		xx.position = p;
		xx.accountId = accountId;
		xx.ticker = ticker;
		xx.currentStockPrice = stockPrice;
		xx.optionPositions = op;
		return xx;
	}
	
	public OptionData getOptionData() {
		if( optionData == null) {
			optionData = OptionSymbolParser.parse(position);
		}
		return optionData;
	}
	
	
	
	

}
