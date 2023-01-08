package com.rise.trading.options;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.studerw.tda.model.account.CashEquivalentInstrument;
import com.studerw.tda.model.account.EquityInstrument;
import com.studerw.tda.model.account.Instrument;
import com.studerw.tda.model.account.OptionInstrument;
import com.studerw.tda.model.account.OptionInstrument.PutCall;
import com.studerw.tda.model.account.Position;

public class GroupedPositions {
	private Map<String, GroupedPosition> groupedPositions = new HashMap<String, GroupedPosition>();

	public Collection<GroupedPosition> getGroupedPositions() {
		return groupedPositions.values();
	}

	public GroupedPositions(List<Position> positions) {
		for (Position p : positions) {
			Instrument i = p.getInstrument();
			String symbol = i.getSymbol();
			if (i instanceof OptionInstrument) {
				symbol = symbol.substring(0, symbol.indexOf('_'));
			}
			GroupedPosition group = groupedPositions.get(symbol);
			if (group == null) {
				group = new GroupedPosition(symbol);
				groupedPositions.put(symbol, group);
			}
			if (i instanceof EquityInstrument) {
				if( p.getShortQuantity().doubleValue() > 0.0d) {
					group.setShortEquity(p);
				}
				else {
					group.setEquity(p);
				}
			}
			if (i instanceof CashEquivalentInstrument) {
				group.setCash(p);
			}
			if (i instanceof OptionInstrument) {
				group.addOption(p);
			}
		}
	}

	public Set<String> getSymbols() {
		return groupedPositions.keySet();
	}

	public String toString() {
		return groupedPositions.values().toString();
	}

	public boolean isEligibleForCoveredCall(String symbol) {
		GroupedPosition gp = groupedPositions.get(symbol);
		if (gp == null) {
			return false;
		}
		int pcc = gp.getNumberOfPotentialCoveredCallContracts();
		if (pcc == 0) {
			return false;
		}
		for (Position p : gp.getOptions()) {
			OptionInstrument oi = (OptionInstrument) p.getInstrument();
			PutCall pc = oi.getPutCall();
			if (pc == PutCall.PUT) {
				continue;
			}
			int quantity = p.getShortQuantity().intValue();
			if (quantity == 0) {
				continue;
			}
			quantity = quantity * -1;
			if (quantity == pcc) {
				System.out.println("Covered Call is already setup");
				return false;
			} else {
				return true;
			}

		}

		return true;
	}

	public GroupedPosition getGroupedPosition(String symbol) {
		// TODO Auto-generated method stub
		return groupedPositions.get(symbol);
	}

}