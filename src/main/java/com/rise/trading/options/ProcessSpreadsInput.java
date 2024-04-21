package com.rise.trading.options;

public class ProcessSpreadsInput extends AbstractPassiveIncomeInput implements PassiveIncomeInput {
	private GroupedPosition groupedPostion;

	public GroupedPosition getGroupedPostion() {
		return groupedPostion;
	}

	public void setGroupedPostion(GroupedPosition groupedPostion) {
		this.groupedPostion = groupedPostion;
	}
	
}
