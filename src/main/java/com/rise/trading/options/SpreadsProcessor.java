package com.rise.trading.options;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.studerw.tda.model.account.Position;

public class SpreadsProcessor {

	public static void main(String args[]) {
		findSpreads(Util.getAccountId1());
	}

	private static void findSpreads(String accountId) {
		SpreadsProcessor sp = new SpreadsProcessor();
		PositionsHandler ph = new PositionsHandler();
		GroupedPositions gp = ph.getGroupedPositions(accountId);
		for (GroupedPosition x : gp.getGroupedPositions()) {
			Spreads s = sp.makeSpreads(x);
			// System.out.println(Util.toJSON(s));
			s.display();

		}
	}
	
	public Spreads makeSpreads(GroupedPosition x) {
		Spreads spreads = new Spreads();
		List<Position> p = x.getOptions();
		if (p.size() == 0)
			return spreads;
		spreads = makeSpreads(p);
		return spreads;
	}

	public Spreads makeSpreads(List<Position> positions) {
		Spreads s = null;
		Map<String, List<OptionData>> optionPositions = new HashMap<String, List<OptionData>>();
		for (Position p : positions) {
			OptionData od = OptionSymbolParser.parse(p);
			addToMap(od, optionPositions);

		}
		s = makeSpreads(optionPositions);
		return s;
	}

	private Spreads makeSpreads(Map<String, List<OptionData>> optionPositions) {
		Spreads s = new Spreads();
		for (Map.Entry<String, List<OptionData>> entry : optionPositions.entrySet()) {
			Spreads spreads = createSpreads(entry.getKey(), entry.getValue());
			s.addSpreads(spreads);
		}
		return s;

	}

	class CallsAndPuts {
		OptionDataComparator oc = new OptionDataComparator();
		public TreeSet<OptionData> longCalls = new TreeSet<OptionData>(oc);
		public TreeSet<OptionData> shortCalls = new TreeSet<OptionData>(oc);
		public TreeSet<OptionData> longPuts = new TreeSet<OptionData>(oc);
		public TreeSet<OptionData> shortPuts = new TreeSet<OptionData>(oc);

		public Spreads makeSpreads() {
			Spreads callSpreads = makeSpreads(shortCalls, longCalls);

			// Spreads putSpreads = makeSpreads(shortPuts, new
			// TreeSet<OptionData>(longPuts.descendingSet()));
			Spreads putSpreads = makeSpreads(shortPuts, longPuts);

			callSpreads.addSpreads(putSpreads);
			return callSpreads;

		}

		public Spreads makeSpreadsAnother(TreeSet<OptionData> shorts, TreeSet<OptionData> longs) {
			Spreads spreads = new Spreads();
			if (longs.size() == 0 || shorts.size() == 0) {
				return spreads;
			}
			
			OptionData[] sc = (OptionData[]) shorts.toArray(new OptionData[0]);
			OptionData[] lc = (OptionData[]) longs.toArray(new OptionData[0]);
			//System.out.println(sc.length + " -> "+lc.length);
			int shortCounter = 0;
			
			for (; ;) {
				if (shortCounter == sc.length)
					break;
				OptionData sh = sc[shortCounter];
				OptionData ln = getNearestOptionDataForShort(sh, lc);
				Spread spread = makeSpread(sh, ln);
			
				if( sh.getAdjustableQuantity() <=0) {
					shortCounter++;
				}
				//System.out.println(ln.getAdjustableQuantity()+" : "+sh.getAdjustableQuantity());
				spreads.addSpread(spread);

			}
			return spreads;
		}

		public Spreads makeSpreads(TreeSet<OptionData> shorts, TreeSet<OptionData> longs) {
			Spreads spreads = new Spreads();
			if (longs.size() == 0 || shorts.size() == 0) {
				return spreads;
			}

			OptionData[] sc = (OptionData[]) shorts.toArray(new OptionData[0]);
			OptionData[] lc = (OptionData[]) longs.toArray(new OptionData[0]);
			// System.out.println(sc.length + " -> "+lc.length);
			int shortCounter = 0;
			int longCounter = lc.length - 1;
			for (;;) {
				if (longCounter == -1)
					break;
				if (shortCounter == sc.length)
					break;
				OptionData sh = sc[shortCounter];
				OptionData ln = lc[longCounter];
				Spread spread = makeSpread(sh, ln);
				if (ln.getAdjustableQuantity() <= 0) {
					longCounter--;
				}
				if (sh.getAdjustableQuantity() <= 0) {
					shortCounter++;
				}
				// System.out.println(ln.getAdjustableQuantity()+" :
				// "+sh.getAdjustableQuantity());
				spreads.addSpread(spread);

			}
			return spreads;
		}

	}

	private OptionData getNearestOptionDataForShort(OptionData sh, OptionData[] longs) {
		double strikePrice = sh.getPrice().doubleValue();
		OptionData returnableOptionData = null;
		double returnablePrice = strikePrice;
		for (OptionData x : longs) {
			if (x.getAdjustableQuantity() == 0) {
				continue;
			}
			if (returnableOptionData == null) {
				returnableOptionData = x;
			}
			double lsp = x.getPrice().doubleValue();
			if (sh.isPut()) {
				if (lsp < returnablePrice) {
					returnablePrice = lsp;
					returnableOptionData = x;
				}
			}
			if (sh.isCall()) {
				if (lsp > returnablePrice) {
					returnablePrice = lsp;
					returnableOptionData = x;
				}
			}
		}

		return returnableOptionData;
	}

	private Spread makeSpread(OptionData sh, OptionData ln) {
		int sq = sh.getAdjustableQuantity();
		int lq = ln.getAdjustableQuantity();
		// Spread spread = new Spread();
		if (sq == lq) {
			Spread spread = new Spread(ln);
			spread.setShortPosition(sh);
			ln.setAdjustableQuantity(0);
			sh.setAdjustableQuantity(0);
			return spread;
		}
		if (sq < lq) {
			Spread spread = new Spread(sh);
			spread.setLongPosition(ln);
			ln.setAdjustableQuantity(ln.getAdjustableQuantity() - sq);
			sh.setAdjustableQuantity(0);
			return spread;
		}
		if (sq > lq) {
			Spread spread = new Spread(ln);
			spread.setShortPosition(sh);
			sh.setAdjustableQuantity(sh.getAdjustableQuantity() - lq);
			ln.setAdjustableQuantity(0);
			return spread;
		}
		return null;
	}

	class OptionDataComparator implements Comparator {

		@Override
		public int compare(Object x, Object y) {
			OptionData o1 = (OptionData) x;
			OptionData o2 = (OptionData) y;
			int priceComparison = o1.getPrice().compareTo(o2.getPrice());
			// 2. If prices are equal, sort by quantity in descending order
			if (priceComparison == 0) {
				// Reverse order for quantity to achieve descending order
				return -Double.compare(o1.getQuantity(), o2.getQuantity());
			}
			// 3. Sort positive quantities before negative quantities
			// Positive quantity OptionData should come first
			if (o1.getQuantity() >= 0 && o2.getQuantity() < 0) {
				return -1;
			} else if (o1.getQuantity() < 0 && o2.getQuantity() >= 0) {
				return 1;
			}
			return priceComparison; // Return price comparison result if quantities are of the same sign

		}

	}

	private CallsAndPuts findCallsAndPuts(List<OptionData> od) {
		CallsAndPuts cp = new CallsAndPuts();
		for (OptionData x : od) {
			if (x.isCall()) {
				if (x.getQuantity() > 0)
					cp.longCalls.add(x);
				else
					cp.shortCalls.add(x);
			} else {
				if (x.getQuantity() > 0)
					cp.longPuts.add(x);
				else
					cp.shortPuts.add(x);
			}
		}
		return cp;
	}

	private Spreads createSpreads(String key, List<OptionData> list) {
		CallsAndPuts callsAndPuts = findCallsAndPuts(list);
		// System.out.println(key + " -> " + Util.toJSON(callsAndPuts));
		Spreads s = callsAndPuts.makeSpreads();
		return s;
		// makeSpreadsOld(list);
	}

	private void makeSpreadsOld(List<OptionData> list) {
		Spreads spreads = new Spreads();
		for (OptionData od : list) {
			Spread spread = spreads.getPossibleSpread(od);
			if (spread == null) {
				spreads.addSpread(makeSpread(od));
				continue;
			}
			spread.setOptionData(od);
		}
	}

	private Spread makeSpread(OptionData od) {
		Spread spread = new Spread(od);
		return spread;
	}

	private void addToMap(OptionData od, Map<String, List<OptionData>> optionPositions) {
		String key = od.getTickerAndExpiryDateKey();

		List<OptionData> list = optionPositions.get(key);
		if (list == null) {
			list = new ArrayList<OptionData>();
			optionPositions.put(key, list);
		}
		list.add(od);

	}

}
