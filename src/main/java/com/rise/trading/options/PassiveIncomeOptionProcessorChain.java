package com.rise.trading.options;

import java.util.HashMap;
import java.util.Map;

public class PassiveIncomeOptionProcessorChain extends BaseHandler implements PassiveIncomeOptionProcessor {

	private Map<String, PassiveIncomeOptionProcessor> processors = new HashMap<String, PassiveIncomeOptionProcessor>();

	private PassiveIncomeOptionProcessor COMMON = new PassiveIncomeStrategy();

	public PassiveIncomeOptionProcessorChain() {
		//addPassiveIncomeOptionProcessor("TQQQ", new PassiveIncomeLongStrategy());
	}

	@Override
	public void closeOptionIfInProfitAndPotentiallyOpenNewOne(PassiveIncomeOptionProcessorInput input) {
		PassiveIncomeOptionProcessor x = processors.get(getKey(input.ticker, input.accountId));
		if (x == null) {
			x = processors.get(input.ticker);
			if (x == null) {
				x = COMMON;
			}
		}
		x.closeOptionIfInProfitAndPotentiallyOpenNewOne(input);

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

}
