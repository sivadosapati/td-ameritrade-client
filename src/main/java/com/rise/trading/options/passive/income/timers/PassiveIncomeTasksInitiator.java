package com.rise.trading.options.passive.income.timers;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.rise.trading.options.OrderTests;
import com.rise.trading.options.Util;

public class PassiveIncomeTasksInitiator {

	public static void main(String[] args) {
		PassiveIncomeTaskImplementation implementation = new PassiveIncomeTaskImplementation();
		Timer timer = new Timer();
		PassiveIncomeInput open = makeInputForOpen();
		PassiveIncomeInput close = makeInputForClose();

		TimerTask tt = new TimerTask() {

			final String accountId = Util.getAccountId6();

			class Execution {
				boolean open;
				boolean close;
			}

			Map<String, Execution> executions = new HashMap<String, Execution>();

			private void dailySellCalls() {
				// OrderTests.dailySellCalls(Util.getAccountId1(), "QQQ", 2, 3, 0.25);
				OrderTests.dailySellCalls(accountId, "SPY", 1, 3, 0.25);

			}

			private void dailySellPuts() {
				// OrderTests.dailySellPuts(Util.getAccountId1(), "QQQ", 2, 3, 0.25);
				OrderTests.dailySellPuts(accountId, "SPY", 1, 3, 0.25);
			}

			private void open() {
				dailySellCalls();
				dailySellPuts();
			}

			private void close() {

			}

			@Override
			public void run() {
				System.out.println("Starting execution for open and close orders");
				while (true) {
					try {
						Thread.sleep(1000);
						Date d = new Date();
						int m = d.getMinutes();
						int s = d.getSeconds();
						int h = d.getHours();
						int day = d.getDay();
						int month = d.getMonth();
						int year = d.getYear();
						if (day == 1 || day == 2 || day == 3 || day == 4 || day == 5) {
							if (h == 6 && m == 30 && s <= 15) {
								System.out.println(day + " -> " + h + " -> " + m + " -> " + s);
								Execution e = getExecution(d);
								if (e.open == false) {
									System.out.println("Executing opening orders -> " + getKey(d) + " -> " + day
											+ " -> " + h + " -> " + m + " -> " + s);
									// implementation.placeOrderAtMarketOpen(open);
									open();
									e.open = true;
									System.out.println("Done opening orders -> " + getKey(d) + " -> " + day + " -> " + h
											+ " -> " + m + " -> " + new Date().getSeconds());
								}
							}
							if (h == 12 && m == 59 && s <= 15) {
								System.out.println(day + " -> " + h + " -> " + m + " -> " + s);
								Execution e = getExecution(d);
								if (e.close == false) {
									System.out.println("Executing close order -> " + getKey(d) + " -> " + day + " -> "
											+ h + " -> " + m + " -> " + s);
									close();
									e.close = true;
									System.out.println("Done close order -> " + getKey(d) + " -> " + day + " -> " + h
											+ " -> " + m + " -> " + new Date().getSeconds());

								}
							}
							// System.out.println(day + " -> " + h + " -> " + m + " -> " + s);
						}
					} catch (Exception e) {
						e.printStackTrace();
						break;
					}
				}
				System.out.println("Done execution for open and close orders");
			}

			private Execution getExecution(Date d) {
				String k = getKey(d);
				Execution e = executions.get(k);
				if (e == null) {
					e = new Execution();
					executions.put(k, e);
				}
				return e;
			}

			private String getKey(Date d) {
				return d.getYear() + "-" + d.getMonth() + "-" + d.getDate();
			}

		};
		timer.schedule(tt, new Date());

	}

	private static PassiveIncomeInput makeInputForOpen() {
		PassiveIncomeInput input = new PassiveIncomeInput();
		input.setAccountId(Util.getAccountId1());
		input.setTicker("QQQ");
		input.setContracts(1);
		input.setStrikeDistanceForCall(2);
		input.setStrikeDistanceForPut(2);
		return input;
	}

	private static PassiveIncomeInput makeInputForClose() {
		PassiveIncomeInput input = new PassiveIncomeInput();
		input.setAccountId(Util.getAccountId1());
		input.setTicker("QQQ");
		input.setContracts(1);
		input.setStrikeDistanceForCall(2);
		input.setStrikeDistanceForPut(2);
		return input;
	}

}
