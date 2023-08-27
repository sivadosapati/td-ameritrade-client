package com.rise.trading.options.passive.income.backtest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HistoricalPricesForMultipleDays {
	public String ticker;
	public Map<String, HistoricalPricesForDay> historicalPricesMap = new LinkedHashMap<String, HistoricalPricesForDay>();

	public void addHistoricalPricesForDay(String date, HistoricalPricesForDay hpd) {
		historicalPricesMap.put(date, hpd);

	}

	public Map<String, HistoricalPricesForDay> getHistoricalPricesMap() {
		return historicalPricesMap;
	}

	public HistoricalPricesForDay mergeAllHistoricalPricesBySortingTheDates() {
		ArrayList<HistoricalPricesForDay> values = new ArrayList<HistoricalPricesForDay>(historicalPricesMap.values());
		Collections.sort(values, HistoricalPricesForDay.dayComparator);
		HistoricalPricesForDay result = new HistoricalPricesForDay();
		StringBuffer dates = new StringBuffer("[");
		for (HistoricalPricesForDay x : values) {
			if (result.ticker == null) {
				result.ticker = x.ticker;
			}
			if (result.day == null) {
				result.day = x.day;
				dates.append(x.day + "={");
			}
			dates.append(x.day + ",");
			Collection<HistoricalPriceForMinute> hpm = result.prices;
			hpm.addAll(x.getPricesForOnlyHoursWhereOptionsAreTraded());
			dates.append("(" + hpm.size() + ")");

		}
		dates.append("}]");
		// System.out.println(dates);
		return result;
	}

	public List<HistoricalPricesForDay> sortHistoricalPricesByDay() {
		ArrayList<HistoricalPricesForDay> values = new ArrayList<HistoricalPricesForDay>(historicalPricesMap.values());
		Collections.sort(values, HistoricalPricesForDay.dayComparator);
		return values;
	}

	public Map<String, HistoricalPricesForDay> getPricesGroupedByBusinessWeeks() {

		Map<String, HistoricalPricesForMultipleDays> results = new LinkedHashMap<String, HistoricalPricesForMultipleDays>();

		for (String key : historicalPricesMap.keySet()) {
			String weeklyKey = getWeeklyKey(key);
			HistoricalPricesForMultipleDays days = results.get(weeklyKey);
			if (days == null) {
				days = new HistoricalPricesForMultipleDays();
				results.put(weeklyKey, days);
			}
			days.addHistoricalPricesForDay(key, historicalPricesMap.get(key));
		}

		Map<String, HistoricalPricesForDay> output = new LinkedHashMap<String, HistoricalPricesForDay>();
		for (String key : results.keySet()) {
			HistoricalPricesForMultipleDays x = (HistoricalPricesForMultipleDays) results.get(key);
			output.put(key, x.mergeAllHistoricalPricesBySortingTheDates());
		}
		return output;

	}

	private String getWeeklyKey(String key) {
		try {
			DateTimeFormatter df = DateTimeFormatter.ofPattern("d-M-yyyy");
			LocalDate ld = LocalDate.parse(key, df);

			if (ld.getDayOfWeek() == DayOfWeek.MONDAY) {
				return ld.minusDays(0).format(df);
			}
			if (ld.getDayOfWeek() == DayOfWeek.TUESDAY) {
				return ld.minusDays(1).format(df);
			}
			if (ld.getDayOfWeek() == DayOfWeek.WEDNESDAY) {
				return ld.minusDays(2).format(df);
			}
			if (ld.getDayOfWeek() == DayOfWeek.THURSDAY) {
				return ld.minusDays(3).format(df);
			}
			if (ld.getDayOfWeek() == DayOfWeek.FRIDAY) {
				return ld.minusDays(4).format(df);
			}
			if (ld.getDayOfWeek() == DayOfWeek.SATURDAY) {
				return ld.minusDays(5).format(df);
			}
			if (ld.getDayOfWeek() == DayOfWeek.SUNDAY) {
				return ld.minusDays(6).format(df);
			}
			throw new RuntimeException("Should not reach this line");
			// return key;
		} catch (Exception e) {
			System.out.println("Key[" + key + "]");
			// TODO Auto-generated catch block
			e.printStackTrace();
			return key;
		}
	}

}
