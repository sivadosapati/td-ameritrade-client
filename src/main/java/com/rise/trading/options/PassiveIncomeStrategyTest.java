package com.rise.trading.options;

import java.io.FileOutputStream;
import java.io.PrintStream;

public class PassiveIncomeStrategyTest {

	public static void main(String[] args) throws Exception{
		System.setOut(new PrintStream(new FileOutputStream("passive_income.log",true), true));
		System.setErr(new PrintStream(new FileOutputStream("passive_income.log",true), true));
		// TODO Auto-generated method stub
		Scanner scanner = new Scanner();
		scanner.start();

	}

}

class Scanner extends Thread {
	PassiveIncomeStrategy pis = new PassiveIncomeStrategy();

	public void run() {
		String[] accounts = { Util.getAccountId1(), Util.getAccountId6(), Util.getAccountId7() };
		String[] accountsToBeScannedEveryHour = { Util.getAccountId2(), Util.getAccountId3(), Util.getAccountId4() };
		int counter = 0;
		int minutes = 5;
		while (true) {

			if (!Util.notMarketHours()) {
				System.out.println("Not scanning -> " + counter + " -> " + new java.util.Date());
			} else {
				try {
					System.out.println("CurrentTime -> " + new java.util.Date());
					for (String a : accounts) {
						pis.closeOptionsThatAreInProfitAndPotentiallyOpenNewOnes(a);
					}
					if (counter % (60 / minutes) == 0) {
						for (String a : accountsToBeScannedEveryHour) {
							// pis.closeOptionsThatAreInProfitAndPotentiallyOpenNewOnes(a);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			Util.pauseForSeconds(60 * minutes);
			counter++;
		}
	}
}