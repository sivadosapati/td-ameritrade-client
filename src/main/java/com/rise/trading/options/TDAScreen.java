package com.rise.trading.options;

import java.awt.Container;
import java.awt.GridLayout;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.rise.trading.options.PassiveIncomeStrategy.Ticker;
import com.rise.trading.options.passive.income.backtest.HistoricalPricesFetcher;
import com.studerw.tda.client.HttpTdaClient;

public class TDAScreen extends JFrame {

	private JButton coveredCallsButton;

	private JButton cancelAllOpenOrders;

	private JButton placeClosingTradesForOpenShortOptions;

	private JButton placeLongCallsAndPutsForShortOptions;

	private JButton rollOptionsIfAnyDuringTheLastHourOfExpiration;

	private JButton orders;
	private JButton positions;
	private JComboBox accounts;

	private HistoricalPricesComponent historicalPricesComponent;
	private PassiveIncomeStrategyComponent passiveIncomeComponent;

	private JTextArea summaryArea;

	private TradeCoveredCalls tcc = new TradeCoveredCalls();
	private OrderHandler orderHandler = new OrderHandler();

	private ByteArrayOutputStream bos = new ByteArrayOutputStream();

	public TDAScreen() {
		create();
		addComponents();
		addListeners();
		setSize(1200, 800);
		setVisible(true);
		pack();
	}

	private void addListeners() {
		coveredCallsButton.addActionListener((l) -> {
			makeCoveredCallsOnAllAccounts();
		});
		cancelAllOpenOrders.addActionListener((l) -> {
			cancelAllOpenOrders();
		});
		placeClosingTradesForOpenShortOptions.addActionListener((l) -> {
			placeClosingTradesForOpenShortOptions();
		});
		placeLongCallsAndPutsForShortOptions.addActionListener((l) -> {
			placeLongCallsAndPutsForShortOptions();
		});
		rollOptionsIfAnyDuringTheLastHourOfExpiration.addActionListener((l) -> {
			rollOptionsIfAnyDuringTheLastHourOfExpiration();
		});

	}

	private void rollOptionsIfAnyDuringTheLastHourOfExpiration() {
		executeOnAccounts((e) -> {
			orderHandler.rollOptonsIfAnyDuringTheLastHourOfExpiration(e);
		});
		System.out.println("Rolled all the necessary options to the next week or after");

	}

	private void placeClosingTradesForOpenShortOptions() {
		System.out.println("Placing All Closing Trades for Short Positions");
		executeOnAccount((e) -> {
			orderHandler.placeClosingTradesOnShortOptions(e);
		});
		System.out.println("Executed All Closing Trades for Short Positions");

	}

	private void placeLongCallsAndPutsForShortOptions() {
		executeOnAccounts((e) -> {
			orderHandler.placeLongCallsAndPutsForShortOptions(e);
		});
		System.out.println("Executed All Closing Trades for Short Positions");

	}

	private void cancelAllOpenOrders() {

		Account a = (Account) accounts.getModel().getSelectedItem();
		String accountId = a.id;
		executeOnAccount((e) -> {
			orderHandler.cancelAllOpenOrders(e);
		}, accountId);
	}

	private void cancelAllOpenOrdersOld() {
		executeOnAccounts((e) -> {
			orderHandler.cancelAllOpenOrders(e);
		});
		System.out.println("Cancelled All Orders");
	}

	private void makeCoveredCallsOnAllAccounts() {
		String[] accounts = Util.getAllAccounts();
		HttpTdaClient client = Util.getHttpTDAClient();
		for (String a : accounts) {
			tcc.tradeCoveredCallsOnAnAccount(client, a);
		}
		System.out.println("Placed covered calls on all accounts");

	}

	void executeOnAccounts(AccountExecutor ae) {
		String[] accounts = Util.getAllAccounts();
		// HttpTdaClient client = Util.getHttpTDAClient();
		for (String a : accounts) {
			ae.execute(a);
		}
		summaryArea.setText(bos.toString());
		summaryArea.updateUI();

	}

	void executeOnAccount(AccountExecutor ae, String accountId) {
		ae.execute(accountId);
	}

	void executeOnAccount(AccountExecutor ae) {
		Account a = (Account) accounts.getModel().getSelectedItem();
		String accountId = a.id;
		executeOnAccount(ae, accountId);
	}

	interface AccountExecutor {
		void execute(String account);
	}

	private void addComponents() {
		Container con = new JPanel();
		con.setLayout(new GridLayout(8, 1, 5, 5));
		JPanel panel = new JPanel();
		panel.add(new JLabel("Select Account : "));
		panel.add(accounts);
		con.add(panel);
		addToContainerWithPanel(con, coveredCallsButton);
		addToContainerWithPanel(con, cancelAllOpenOrders);
		addToContainerWithPanel(con, placeClosingTradesForOpenShortOptions);
		addToContainerWithPanel(con, placeLongCallsAndPutsForShortOptions);
		addToContainerWithPanel(con, rollOptionsIfAnyDuringTheLastHourOfExpiration);
		con.add(historicalPricesComponent.getDisplayableComponent());
		con.add(passiveIncomeComponent.getDisplayPanel());

		Container parent = getContentPane();
		// parent.setLayout(new FlowLayout());
		parent.add(con);
		// parent.add(con, "North");

		// parent.add(new JScrollPane(), "Center");
	}

	private void addToContainerWithPanel(Container con, JButton b) {
		JPanel panel = new JPanel();
		panel.add(b);
		con.add(panel);

	}

	private void create() {
		coveredCallsButton = new JButton("Place Covered Calls");
		cancelAllOpenOrders = new JButton("Cancel All Open Orders");
		placeClosingTradesForOpenShortOptions = new JButton("Place closing trades for short options");
		placeLongCallsAndPutsForShortOptions = new JButton("Place Long Calls and Puts for short options");
		rollOptionsIfAnyDuringTheLastHourOfExpiration = new JButton("Roll options during the last hour of expiration");
		orders = new JButton("Get Current Orders");
		positions = new JButton("Get Current positions");
		summaryArea = new JTextArea(40, 40);
		accounts = new JComboBox<Account>(new DefaultComboBoxModel<Account>(Util.getAccounts().getAccounts()));
		passiveIncomeComponent = new PassiveIncomeStrategyComponent();
		historicalPricesComponent = new HistoricalPricesComponent();
		try {
			// System.setOut(new PrintStream(bos, true));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		new TDAScreen();

	}

	class PassiveIncomeStrategyComponent {
		private JTextField stockTicker = new JTextField(5);
		private JTextField numberOfContracts = new JTextField(3);
		private JTextField callDistance = new JTextField(3);
		private JTextField putDistance = new JTextField(3);
		private JPanel displayPanel;
		private JButton placeOrder;
		private JButton placeNextDayOrder;
		private JButton placeWeeklyOrder;
		private JButton placeClosingOrdersForOptions;
		private JButton placeClosingOrdersForEquities;
		private JButton placeCloseShortEquitiesWhenTheyAreInExpectedProfitOrLoss;
		private JButton placeCloseLongEquitiesWhenTheyAreInExpectedProfitOrLoss;

		private JButton adjustEquityPrice;
		private PassiveIncomeStrategy strategy;

		public PassiveIncomeStrategyComponent() {
			strategy = new PassiveIncomeStrategy();
			stockTicker.setText("QQQ");
			numberOfContracts.setText("1");
			callDistance.setText("2");
			putDistance.setText("2");
			placeOrder = new JButton("Passive Income");
			placeNextDayOrder = new JButton("Passive Income For Next Day");
			placeWeeklyOrder = new JButton("Passive Income For Weekly Options");
			placeClosingOrdersForOptions = new JButton("Place Closing Orders for Options");
			placeClosingOrdersForEquities = new JButton("Place Closing Orders for Equities");
			adjustEquityPrice = new JButton("Adjust Equity Price For Working Orders");
			placeCloseShortEquitiesWhenTheyAreInExpectedProfitOrLoss = new JButton(
					"Close Short Equities when they are in expected profit or loss");
			placeCloseLongEquitiesWhenTheyAreInExpectedProfitOrLoss = new JButton(
					"Close Long Equities when they are in expected profit or loss");
			displayPanel = new JPanel(new GridLayout(3, 1));
			JPanel panel = new JPanel();
			panel.add(new JLabel("Stock Ticker : "));
			panel.add(stockTicker);
			panel.add(new JLabel("Number of Option Contracts : "));
			panel.add(numberOfContracts);
			panel.add(new JLabel("Call Distance from stock price : "));
			panel.add(callDistance);
			panel.add(new JLabel("Put Distance from stock price : "));
			panel.add(putDistance);
			displayPanel.add(panel);
			panel = new JPanel();
			panel.add(placeOrder);
			panel.add(placeNextDayOrder);
			panel.add(placeWeeklyOrder);
			panel.add(placeClosingOrdersForOptions);
			panel.add(placeClosingOrdersForEquities);
			panel.add(adjustEquityPrice);
			displayPanel.add(panel);
			panel = new JPanel();
			panel.add(placeCloseShortEquitiesWhenTheyAreInExpectedProfitOrLoss);
			panel.add(placeCloseLongEquitiesWhenTheyAreInExpectedProfitOrLoss);
			displayPanel.add(panel);
			placeOrder.addActionListener((e) -> placePassiveIncomeOrders());
			placeWeeklyOrder.addActionListener((e) -> placeWeeklyIncomeOrders());
			placeNextDayOrder.addActionListener((e) -> placePassiveIncomeForNextDayOrders());
			placeClosingOrdersForEquities.addActionListener((e) -> placeClosingOrdersForEquities());
			placeClosingOrdersForOptions.addActionListener((e) -> placeClosingOrdersForOptions());
			adjustEquityPrice.addActionListener((e) -> adjustEquityPrices());
			placeCloseShortEquitiesWhenTheyAreInExpectedProfitOrLoss
					.addActionListener((e) -> placeCloseShortEquitiesWhenTheyAreExpectedToBeInProfitOrLoss());
			placeCloseLongEquitiesWhenTheyAreInExpectedProfitOrLoss
					.addActionListener((e) -> placeCloseLongEquitiesWhenTheyAreExpectedToBeInProfitOrLoss());
		}

		private void placeWeeklyIncomeOrders() {
			String accountId = ((Account) accounts.getSelectedItem()).id;
			String stockCode = stockTicker.getText();
			int contracts = getInteger(numberOfContracts);
			int call = getInteger(callDistance);
			int put = getInteger(putDistance);
			strategy.placeWeeklyTradeForPassiveIncome(accountId, Ticker.make(stockCode), call, put, contracts);
		}

		private void adjustEquityPrices() {
			String accountId = ((Account) accounts.getSelectedItem()).id;
			String stockCode = stockTicker.getText();
			strategy.adjustWorkingEquityOrdersByMovingTheClosingPrice(accountId, stockCode, 0.10d);
		}

		private void placePassiveIncomeForNextDayOrders() {
			String accountId = ((Account) accounts.getSelectedItem()).id;
			String stockCode = stockTicker.getText();
			int contracts = getInteger(numberOfContracts);
			int call = getInteger(callDistance);
			int put = getInteger(putDistance);
			strategy.placeNextDayTradeForPassiveIncome(accountId, Ticker.make(stockCode), (float)call, (float)put, contracts);
		}

		private void placeClosingOrdersForOptions() {
			String accountId = ((Account) accounts.getSelectedItem()).id;
			String stockCode = stockTicker.getText();
			strategy.placeClosingTradesForOptionsOnDailyExpiringOptions(accountId, stockCode);
		}

		private void placeClosingOrdersForEquities() {
			String accountId = ((Account) accounts.getSelectedItem()).id;
			String stockCode = stockTicker.getText();
			strategy.placeClosingTradesForEquityOnDailyExpiringOptions(accountId, stockCode);
		}

		private void placeCloseShortEquitiesWhenTheyAreExpectedToBeInProfitOrLoss() {
			String accountId = ((Account) accounts.getSelectedItem()).id;
			String stockCode = stockTicker.getText();
			strategy.closeShortEquitiesIfTheyAreInProfitAndPlaceAnotherOrderIfThereAreSellOptions(accountId, stockCode,
					1.0d, 0.25d);
		}

		private void placeCloseLongEquitiesWhenTheyAreExpectedToBeInProfitOrLoss() {
			String accountId = ((Account) accounts.getSelectedItem()).id;
			String stockCode = stockTicker.getText();
			strategy.closeLongEquitiesIfTheyAreInProfitAndPlaceAnotherOrderIfThereAreSellOptions(accountId, stockCode,
					1.0d, 0.25d);
		}

		private void placePassiveIncomeOrders() {
			String accountId = ((Account) accounts.getSelectedItem()).id;
			String stockCode = stockTicker.getText();
			int contracts = getInteger(numberOfContracts);
			int call = getInteger(callDistance);
			int put = getInteger(putDistance);
			strategy.placeDailyTradeForPassiveIncome(accountId, Ticker.make(stockCode), (float)call, (float)put, contracts);

		}

		private int getInteger(JTextField textField) {
			return Integer.parseInt(textField.getText());
		}

		public JPanel getDisplayPanel() {
			return displayPanel;
		}

	}

}

class HistoricalPricesComponent {
	private JTextField ticker;
	private JTextField startDate;
	private JTextField endDate;
	private JButton fetchHistoricalPrices;
	private JButton fetchHistoricalPricesFromEOD;

	private JPanel panel;

	public HistoricalPricesComponent() {
		create();
		addComponents();
	}

	private void create() {
		ticker = new JTextField(10);
		ticker.setText("QQQ");
		startDate = new JTextField(10);
		startDate.setText(getDate(-45));
		endDate = new JTextField(10);
		endDate.setText(getDate(0));
		fetchHistoricalPrices = new JButton("Fetch Historical Prices by Minute");
		fetchHistoricalPricesFromEOD = new JButton("Fetch HP from EOD");
		panel = new JPanel();
		Fetcher hf = new Fetcher() {

			@Override
			public void fetch(HistoricalPricesFetcher fetcher, String ticker, LocalDate start, LocalDate end)
					throws Exception {
				fetcher.fetchAndPotentialStoreHistoricalPrices(ticker, start);

			}

		};
		fetchHistoricalPrices.addActionListener((e) -> fetchHistoricalPrices(hf));
		Fetcher eod = new Fetcher() {

			@Override
			public void fetch(HistoricalPricesFetcher fetcher, String ticker, LocalDate start, LocalDate end)
					throws Exception {
				fetcher.fetchAndPotentialStoreHistoricalPricesEOD(ticker, start, end);

			}

		};
		fetchHistoricalPricesFromEOD.addActionListener((e) -> fetchHistoricalPrices(eod));
	}

	private String getDate(int distanceFromToday) {
		// "2023-01-31" , yyyy-mm-dd
		LocalDate start = LocalDate.now();
		LocalDate end = start.plusDays(distanceFromToday);

		return toDateString(end);
	}

	private String toDateString(LocalDate ld) {

		DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String s = ld.format(df);
		return s;
	}

	private void addComponents() {
		addToPanel("Enter Ticker : ", ticker);
		addToPanel("Enter Start Date : ", startDate);
		addToPanel("Enter End Date : ", endDate);
		panel.add(fetchHistoricalPrices);
		panel.add(fetchHistoricalPricesFromEOD);
	}

	private void addToPanel(String string, JComponent component) {
		panel.add(new JLabel(string));
		panel.add(component);

	}

	public JPanel getDisplayableComponent() {
		return panel;
	}

	private void fetchHistoricalPrices(Fetcher f) {
		String ticker = this.ticker.getText();
		LocalDate start = makeDate(this.startDate.getText());
		LocalDate end = makeDate(this.endDate.getText());
		System.out.println(start);
		System.out.println(end);
		HistoricalPricesFetcher hp = new HistoricalPricesFetcher();
		try {
			f.fetch(hp, ticker, start, end);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	interface Fetcher {
		void fetch(HistoricalPricesFetcher fetcher, String ticker, LocalDate start, LocalDate end) throws Exception;
	}

	private LocalDate makeDate(String text) {
		return LocalDate.parse(text);
	}

}
