package com.rise.trading.options.passive.income.timers;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.rise.trading.options.Util;

public class PassiveIncomeTasksInitiator {

	public static void main(String[] args) {
		PassiveIncomeTaskImplementation implementation = new PassiveIncomeTaskImplementation();
		Timer timer = new Timer();
		PassiveIncomeInput open = makeInputForOpen();
		PassiveIncomeInput close = makeInputForClose();

		TimerTask tt = new TimerTask() {

			class Execution {
				boolean open;
				boolean close;
			}

			Map<String, Execution> executions = new HashMap<String, Execution>();

			@Override
			public void run() {
				System.out.println("Starting execution for open and close orders for QQQ");
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
							if (h == 6 && m == 30 && s <= 30) {
								Execution e = getExecution(d);
								if (e.open == false) {
									System.out.println("Executing open order -> " + getKey(d) + " -> " + day + " -> "
											+ h + " -> " + m + " -> " + s);
									implementation.placeOrderAtMarketOpen(open);
									e.open = true;
									System.out.println("Done open order -> " + getKey(d) + " -> " + day + " -> " + h
											+ " -> " + m + " -> " + new Date().getSeconds());
								}
							}
							if (h == 12 && m == 59 && s <= 30) {
								Execution e = getExecution(d);
								if (e.close == false) {
									System.out.println("Executing close order -> " + getKey(d) + " -> " + day + " -> "
											+ h + " -> " + m + " -> " + s);
									implementation.placeOrderAtMarketClose(close);
									e.close = true;
									System.out.println("Done close order -> " + getKey(d) + " -> " + day + " -> " + h
											+ " -> " + m + " -> " + new Date().getSeconds());

								}
							}
							System.out.println(day + " -> " + h + " -> " + m + " -> " + s);
						}
					} catch (Exception e) {
						e.printStackTrace();
						break;
					}
				}
				System.out.println("Done execution for open and close orders for QQQ");
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
