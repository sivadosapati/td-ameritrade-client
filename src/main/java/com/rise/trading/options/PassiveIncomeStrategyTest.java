package com.rise.trading.options;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public class PassiveIncomeStrategyTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Scanner scanner = new Scanner();
		scanner.start();

	}

}

class Scanner extends Thread {
	PassiveIncomeStrategy pis = new PassiveIncomeStrategy();

	public void run() {
		while (true) {

			if (!notMarketHours()) {
				System.out.println("Not scanning -> " + new java.util.Date());
			} else {
				try {
					System.out.println("\nCurrentTime -> " + new java.util.Date());
					pis.closeOptionsThatAreInProfitAndPotentiallyOpenNewOnes(Util.getAccountId1());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			Util.pauseForSeconds(300);
		}
	}

	private boolean notMarketHours() {
		LocalDate date = LocalDate.now();
		if (date.getDayOfWeek() == DayOfWeek.SATURDAY) {
			return false;

		}
		if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
			return false;
		}
		LocalTime time = LocalTime.now();

		// System.out.println(time.getHour());
		// After 12 PM - pick the next day
		if (time.getHour() >= 14) {
			return false;
		}
		if (time.getHour() < 6) {
			return false;
		}
		if (time.getHour() == 6) {
			if (time.getMinute() >= 30) {
				return true;
			} else {
				return false;
			}
		}
		if (time.getHour() == 13) {
			if (time.getMinute() < 15) {
				return true;
			} else {
				return false;
			}
		}

		return true;
	}
}