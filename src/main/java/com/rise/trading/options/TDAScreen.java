package com.rise.trading.options;

import java.awt.Container;
import java.awt.GridLayout;
import java.io.ByteArrayOutputStream;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

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
		executeOnAccounts((e) -> {
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

	interface AccountExecutor {
		void execute(String account);
	}

	private void addComponents() {
		Container con = new JPanel();
		con.setLayout(new GridLayout(7, 1, 5, 5));
		JPanel panel = new JPanel();
		panel.add(new JLabel("Select Account : "));
		panel.add(accounts);
		con.add(panel);
		con.add(coveredCallsButton);
		con.add(cancelAllOpenOrders);
		con.add(placeClosingTradesForOpenShortOptions);
		con.add(placeLongCallsAndPutsForShortOptions);
		con.add(rollOptionsIfAnyDuringTheLastHourOfExpiration);
		con.add(passiveIncomeComponent.getDisplayPanel());

		Container parent = getContentPane();
		parent.add(con, "North");
		parent.add(new JScrollPane(), "Center");
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
		private JButton placeClosingOrdersForOptions;
		private JButton placeClosingOrdersForEquities;
		private PassiveIncomeStrategy strategy;

		public PassiveIncomeStrategyComponent() {
			strategy = new PassiveIncomeStrategy();
			stockTicker.setText("SPY");
			numberOfContracts.setText("1");
			callDistance.setText("2");
			putDistance.setText("2");
			placeOrder = new JButton("Passive Income");
			placeClosingOrdersForOptions = new JButton("Place Closing Orders for Options");
			placeClosingOrdersForEquities = new JButton("Place Closing Orders for Equities");
			displayPanel = new JPanel(new GridLayout(2,1));
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
			panel.add(placeClosingOrdersForOptions);
			panel.add(placeClosingOrdersForEquities);
			displayPanel.add(panel);
			placeOrder.addActionListener((e) -> placePassiveIncomeOrders());
			placeClosingOrdersForEquities.addActionListener((e) -> placeClosingOrdersForEquities());
			placeClosingOrdersForOptions.addActionListener((e) -> placeClosingOrdersForOptions());

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

		private void placePassiveIncomeOrders() {
			String accountId = ((Account) accounts.getSelectedItem()).id;
			String stockCode = stockTicker.getText();
			int contracts = getInteger(numberOfContracts);
			int call = getInteger(callDistance);
			int put = getInteger(putDistance);
			strategy.placeDailyTradeForPassiveIncome(accountId, stockCode, call, put, contracts);

		}

		private int getInteger(JTextField textField) {
			return Integer.parseInt(textField.getText());
		}

		public JPanel getDisplayPanel() {
			return displayPanel;
		}

	}

}
