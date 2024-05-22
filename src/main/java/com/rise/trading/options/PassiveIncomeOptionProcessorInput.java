package com.rise.trading.options;

import java.math.BigDecimal;

import com.rise.trading.options.PassiveIncomeStrategy.OptionPositions;
import com.studerw.tda.model.account.OptionInstrument;
import com.studerw.tda.model.account.Position;

public class PassiveIncomeOptionProcessorInput extends AbstractPassiveIncomeInput implements PassiveIncomeInput {
	public Position position;
	public double currentStockPrice;
	public OptionPositions optionPositions;

	private OptionData optionData = null;

	private String printableMessage = null;

	public void setPrintableMessage(String printableMessage) {
		this.printableMessage = printableMessage;
	}

	public String getPrintableMessage() {
		return printableMessage;
	}

	public boolean isShort() {
		if (optionData != null)
			return !optionData.isLong();
		return false;
	}

	public String getPrintableString() {
		return getAccountId() + "," + getStockTicker() + "," + currentStockPrice + "," + new java.util.Date() + ","
				+ Util.toJSON(position);
	}

	public OptionInstrument.PutCall getPutCall() {
		OptionInstrument oi = (OptionInstrument) position.getInstrument();
		return oi.getPutCall();
	}

	public static PassiveIncomeOptionProcessorInput make(Position p, String accountId, String ticker, double stockPrice,
			OptionPositions op, GroupedPosition gp, GroupedPositions gps) {
		PassiveIncomeOptionProcessorInput xx = new PassiveIncomeOptionProcessorInput();
		xx.position = p;
		xx.setAccountId(accountId);
		xx.setStockTicker(ticker);
		xx.currentStockPrice = stockPrice;
		xx.optionPositions = op;
		xx.setCurrentWorkingOrders(gps.getCurrentWorkingOrders());

		return xx;
	}

	public void setOptionData(OptionData od) {
		this.optionData = od;
	}

	public OptionData getOptionData() {
		if (optionData == null) {
			optionData = OptionSymbolParser.parse(position);
		}
		return optionData;
	}

	public BigDecimal getPositionStrikePrice() {
		return getOptionData().getPrice();
	}

}
