package com.rise.trading.options.passive.income.backtest;

import java.awt.Container;
import java.awt.Desktop;
import java.awt.GridLayout;
import java.awt.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import com.rise.trading.options.ObjectEditingComponent;
import com.rise.trading.options.Util;

public class TransactionSummaryScreen extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTextField ticker;
	private JButton fetchTransactions, fetchWeeklyTransactions;
	private JButton fetchTransactionsForDurationWeekly, fetchTransactionsForDuration;
	private JButton fetchTransactionsForParticularDay;

	private JTable results;
	private TransactionSummaryTableModel model;
	private PassiveIncomeBackwardTestStrategy strategy;

	private JTextField startDate;
	private JTextField endDate;
	private JButton exportToExcel;

	private ObjectEditingComponent component;

	public TransactionSummaryScreen() {
		create();
		addComponents();
		setSize(1400, 900);
		setVisible(true);
	}

	private void addComponents() {
		JPanel northPanel = new JPanel(new GridLayout(2, 1));

		JPanel panel = new JPanel();
		panel.add(new JLabel("Enter Ticker : "));

		panel.add(ticker);

		panel.add(fetchTransactions);
		panel.add(fetchWeeklyTransactions);
		panel.add(exportToExcel);
		northPanel.add(panel);

		panel = new JPanel();
		panel.add(new JLabel("Enter Start Date: "));

		panel.add(startDate);
		panel.add(new JLabel("Enter End Date: "));
		panel.add(endDate);
		panel.add(fetchTransactionsForDuration);
		panel.add(fetchTransactionsForDurationWeekly);
		panel.add(fetchTransactionsForParticularDay);
		northPanel.add(panel);
		Container con = getContentPane();
		con.add(northPanel, "North");
		con.add(new JScrollPane(results));
		con.add(component, "South");
	}

	private void fetch() {

		fetchThroughFetcher((ticker) -> strategy.getTransactionSummaries(ticker));
	}

	private void fetchForDuration() {
		String a = startDate.getText();
		String b = endDate.getText();
		List<String> dates = findDaysInBetween(a, b);
		fetchThroughFetcher((ticker) -> strategy.getTransactionSummaries(ticker, dates));
	}

	private List<String> findDaysInBetween(String a, String b) {
		List<String> daysInBetween = new ArrayList<String>();
		DateTimeFormatter df = DateTimeFormatter.ofPattern("d-MM-yyyy");
		LocalDate x = LocalDate.parse(a, df);
		LocalDate y = LocalDate.parse(b, df);
		while (x.isBefore(y)) {
			String s = x.format(df);
			daysInBetween.add(s);
			x = x.plusDays(1);
		}

		return daysInBetween;
	}

	private void fetchThroughFetcher(TransactionSummaryFetcher fetcher) {
		PassiveIncomeInput object = (PassiveIncomeInput) component.getObject();
		System.out.println(Util.toJSON(object));
		strategy.setPassiveIncomeInput(object);
		List<TransactionSummary> summaries = fetcher.get(ticker.getText());
		String summary = getSummary(summaries);
		model.setTransactionSummaries(summaries);
		model.fireTableDataChanged();
		results.updateUI();
		JOptionPane.showInternalMessageDialog(null, summary, "Summary", JOptionPane.INFORMATION_MESSAGE);

	}

	private void fetchWeekly() {
		fetchThroughFetcher((e) -> strategy.getTransactionSummariesGroupedByBusinessWeek(e));
	}

	interface TransactionSummaryFetcher {
		List<TransactionSummary> get(String ticker);
	}

	private String getSummary(List<TransactionSummary> summaries) {
		StringBuffer result = new StringBuffer("");
		double totalGain = 0.0d;
		double callGain = 0;
		double putGain = 0;
		double potentialCallOptionGain = 0;
		double potentialPutOptionGain = 0;
		double totalLongStockTransactions = 0;
		double totalShortStockTransactions = 0;
		int count = 0;

		for (TransactionSummary ts : summaries) {
			totalGain += ts.totalGainOrLoss;
			callGain += ts.callGainOrLossFromStocks;
			putGain += ts.putGainOrLossFromStocks;
			potentialCallOptionGain += ts.potentialCallOptionGainOrLoss;
			potentialPutOptionGain += ts.potentialPutOptionGainOrLoss;
			totalLongStockTransactions += ts.callTransactions;
			totalShortStockTransactions += ts.putTransactions;
			count++;
		}

		result.append("Total days : " + count + "\n");
		result.append("Total Gain or Loss : " + totalGain + "\n");
		result.append("Call Gain : " + callGain + "\n");
		result.append("Put Gain : " + putGain + "\n");
		result.append("Potential Call Option Gain : " + potentialCallOptionGain + "\n");
		result.append("Potential Put Option Gain : " + potentialPutOptionGain + "\n");
		result.append("Long Stock Transactions : " + totalLongStockTransactions + "\n");
		result.append("Short Stock Transacgtions : " + totalShortStockTransactions + "\n");

		return result.toString();
	}

	private void create() {
		ticker = new JTextField(10);
		startDate = new JTextField(10);
		startDate.setText("01-01-2023");

		endDate = new JTextField(10);
		endDate.setText(getDate(0));
		ticker.setText("SPY");
		fetchTransactions = new JButton("Backtest");
		fetchWeeklyTransactions = new JButton("Backtest weekly");
		exportToExcel = new JButton("Export to Excel");
		fetchTransactionsForDuration = new JButton("Backtest for the duration");
		fetchTransactionsForDurationWeekly = new JButton("Backtest for the duration (weekly)");
		fetchTransactionsForParticularDay = new JButton("Fetch Transactions of particular day");

		exportToExcel.addActionListener((e) -> export());
		fetchTransactions.addActionListener((e) -> fetch());
		fetchWeeklyTransactions.addActionListener((e) -> fetchWeekly());
		fetchTransactionsForDurationWeekly.addActionListener((e) -> fetchForDurationWeekly());
		fetchTransactionsForDuration.addActionListener((e) -> fetchForDuration());
		fetchTransactionsForParticularDay.addActionListener((e) -> fetchTransactionsForParticularDay());

		model = new TransactionSummaryTableModel();
		results = new JTable(model);
        results.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		strategy = new PassiveIncomeBackwardTestStrategy();

		results.setAutoCreateRowSorter(true);

		component = new ObjectEditingComponent(new PassiveIncomeInput());

	}
	
	private String getDate(int distanceFromToday) {
		// "2023-01-31" , yyyy-mm-dd
		LocalDate start = LocalDate.now();
		LocalDate end = start.plusDays(distanceFromToday);

		return toDateString(end);
	}

	private String toDateString(LocalDate ld) {

		DateTimeFormatter df = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		String s = ld.format(df);
		return s;
	}


	private void fetchTransactionsForParticularDay() {
		// int selectedRow = results.getSelectedRow();
		ListSelectionModel selection = results.getSelectionModel();
		int selectedRow = selection.getMinSelectionIndex();
		//selectedRow = selection.get
		int row = results.convertRowIndexToView(selectedRow);
		JOptionPane.showMessageDialog(null, "[selectedRow,row] = ["+selectedRow+","+row+"]");
		TransactionSummary summary = model.getTransactionSummary(row);

		String ticker = summary.ticker;
		LocalDate day = summary.date;
		DetailedTransactionSummaryForDay txn = strategy.processDetailedTransactions(ticker, day);
		DetailedTransactionSummaryForDayScreen screen = new DetailedTransactionSummaryForDayScreen(txn, summary);
		screen.setLocation(new Point(200, 100));
		screen.setSize(800, 700);
		screen.setVisible(true);
	}

	private void fetchForDurationWeekly() {
		String a = startDate.getText();
		String b = endDate.getText();
		List<String> dates = findDaysInBetween(a, b);
		fetchThroughFetcher((ticker) -> strategy.getTransactionSummariesGroupedByBusinessWeek(ticker, dates));
	}

	private void export() {
		try {
			JFileChooser fileChooser = new JFileChooser();
			int retval = fileChooser.showSaveDialog(exportToExcel);

			if (retval == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				if (file != null) {
					if (!file.getName().toLowerCase().endsWith(".xls")) {
						file = new File(file.getParentFile(), file.getName() + ".xls");

					}

					try {
						ExcelExporter exp = new ExcelExporter();
						exp.exportTable(results, file);

						Desktop.getDesktop().open(file);
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();

					} catch (FileNotFoundException e) {
						e.printStackTrace();
						System.out.println("not found");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new TransactionSummaryScreen();

	}

}

class TransactionSummaryTableModel extends AbstractTableModel {

	private List<TransactionSummary> summaries = new ArrayList<TransactionSummary>();

	public TransactionSummaryTableModel() {

	}

	public TransactionSummary getTransactionSummary(int selectedRow) {
		return summaries.get(selectedRow);
	}

	public void setTransactionSummaries(List<TransactionSummary> summaries) {
		this.summaries = summaries;
	}

	@Override
	public int getRowCount() {
		return summaries.size();
	}

	@Override
	public int getColumnCount() {
		return TransactionSummary.class.getDeclaredFields().length;
	}

	public String getColumnName(int column) {

		Field f = TransactionSummary.class.getDeclaredFields()[column];
		return f.getName();
	}

	public Class getColumnClass(int column) {
		Field f = TransactionSummary.class.getDeclaredFields()[column];
		Class x = f.getDeclaringClass();
		Class y = f.getType();
		return y;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		try {
			TransactionSummary x = summaries.get(rowIndex);
			Field f = x.getClass().getDeclaredFields()[columnIndex];
			return f.get(x);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

}
