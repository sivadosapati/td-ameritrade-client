package com.rise.trading.options;

import java.util.HashMap;
import java.util.Map;

public class PassiveIncomeOptionProcessorChain extends BaseHandler implements PassiveIncomeOptionProcessor {

	private Map<String, PassiveIncomeOptionProcessor> processors = new HashMap<String, PassiveIncomeOptionProcessor>();

	private PassiveIncomeOptionProcessor COMMON = new PassiveIncomeStrategy();

	public PassiveIncomeOptionProcessorChain() {
		// addPassiveIncomeOptionProcessor("TQQQ", new PassiveIncomeLongStrategy());
		PassiveIncomeSpreadProcessorStrategy spreads = new PassiveIncomeSpreadProcessorStrategy();
		addPassiveIncomeOptionProcessor("AMD", spreads);
		addPassiveIncomeOptionProcessor("QQQ", spreads);
	}

	@Override
	public void closeOptionIfInProfitAndPotentiallyOpenNewOne(PassiveIncomeOptionProcessorInput input) {
		PassiveIncomeOptionProcessor x = getPassiveIncomeOptionProcessor(input);
		x.closeOptionIfInProfitAndPotentiallyOpenNewOne(input);

	}

	private PassiveIncomeOptionProcessor getPassiveIncomeOptionProcessor(PassiveIncomeInput input) {
		PassiveIncomeOptionProcessor x = processors.get(getKey(input.getStockTicker(), input.getAccountId()));
		if (x == null) {
			x = processors.get(input.getStockTicker());
			if (x == null) {
				x = COMMON;
			}
		}
		return x;
	}

	public void addPassiveIncomeOptionProcessor(String ticker, String accountId, PassiveIncomeOptionProcessor xx) {
		String key = getKey(ticker, accountId);
		processors.put(key, xx);
	}

	public void addPassiveIncomeOptionProcessor(String ticker, PassiveIncomeOptionProcessor xx) {
		processors.put(ticker, xx);
	}

	private String getKey(String ticker, String accountId) {
		return ticker + " -> " + accountId;
	}

	@Override
	public boolean processSpreads(ProcessSpreadsInput input) {
		PassiveIncomeOptionProcessor x = getPassiveIncomeOptionProcessor(input);
		return x.processSpreads(input);
	}

}
