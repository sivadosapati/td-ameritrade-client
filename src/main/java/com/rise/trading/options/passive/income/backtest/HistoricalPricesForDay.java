package com.rise.trading.options.passive.income.backtest;

import java.io.PrintStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class HistoricalPricesForDay {
	public String ticker;
	public List<HistoricalPriceForMinute> prices = new ArrayList<HistoricalPriceForMinute>();
	public LocalDate day;

	public void addHistoricalPriceForMinute(HistoricalPriceForMinute minute) {
		prices.add(minute);
	}

	Collection<HistoricalPriceForMinute> getPricesForOnlyHoursWhereOptionsAreTraded() {
		Collection<HistoricalPriceForMinute> prices = new ArrayList<HistoricalPriceForMinute>();
		for (HistoricalPriceForMinute x : this.prices) {
			Date d = x.d;
			if (d.getHours() >= 6 && d.getHours() < 13) {
				if (d.getHours() == 6) {
					if (d.getMinutes() >= 30) {
						prices.add(x);
						continue;
					} else {
						continue;
					}
				}
				prices.add(x);
				continue;
			}
		}

		return prices;
	}

	public static Comparator<HistoricalPricesForDay> dayComparator = new Comparator<HistoricalPricesForDay>() {
		@Override
		public int compare(HistoricalPricesForDay jc1, HistoricalPricesForDay jc2) {
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("d-M-yyyy");
			//LocalDate x = LocalDate.parse(jc1.day, dtf);
			//LocalDate y = LocalDate.parse(jc2.day, dtf);
			LocalDate x = jc1.day;
			LocalDate y = jc2.day;
			return x.compareTo(y);
		}
	};

	public void printToStream(PrintStream out) {
		for (HistoricalPriceForMinute m : prices) {
			out.println(m.toString());
		}
	}

}
