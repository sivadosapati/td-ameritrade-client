package com.rise.trading.options;

import java.util.ArrayList;
import java.util.List;

public class Spreads {
	public List<Spread> spreads = new ArrayList<Spread>();

	void addSpread(Spread spread) {
		if (spread == null)
			return;
		spreads.add(spread);
	}

	public Spread getPossibleSpread(OptionData od) {
		for (Spread s : spreads) {
			if (isCandidate(s, od)) {
				return s;
			}
		}
		return null;
	}

	private boolean isCandidate(Spread s, OptionData od) {
		if (s.isMatched()) {
			return false;
		}
		if (s.getPutOrCall() == od.putOrCall && s.getDate() == od.date && s.getTicker() == od.stockTicker) {
			if (s.getLongPosition() == null) {
				if (!od.isLong()) {
					return true;
				}
			}
			if (s.getShortPosition() == null) {
				if (od.isLong()) {
					return true;
				}
			}
		}
		return false;

	}

	public void addSpreads(Spreads ss) {
		if (ss == null)
			return;
		for (Spread s : ss.spreads) {
			this.addSpread(s);
		}

	}

	public void display() {
		for (Spread s : spreads) {
			System.out.println(s.toSpreadString());
		}

	}

	public boolean isEmpty() {
		return spreads.size() == 0;
	}

	public List<Spread> getSpreads() {
		return spreads;
	}

	public Spread getAlternateSpreadIfExisting(Spread spread) {
		for (Spread s : spreads) {
			if (s == spread) {
				continue;
			}
			if( s.matchesAlternateSpread(spread)) {
				return s;
			}
		}
		
		return null;
	}

}
