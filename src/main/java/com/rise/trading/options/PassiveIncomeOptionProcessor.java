package com.rise.trading.options;

public interface PassiveIncomeOptionProcessor {
	void closeOptionIfInProfitAndPotentiallyOpenNewOne(PassiveIncomeOptionProcessorInput input);
	boolean processSpreads(ProcessSpreadsInput input);
}
