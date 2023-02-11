package com.rise.trading.options.passive.income.backtest;

import java.awt.Container;
import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
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
import javax.swing.table.AbstractTableModel;

public class TransactionSummaryScreen extends JFrame {

	private JTextField ticker;
	private JButton fetchTransactions, fetchWeeklyTransactions;

	private JTable results;
	private TransactionSummaryTableModel model;
	private PassiveIncomeBackwardTestStrategy strategy;
	
	private JButton exportToExcel;

	public TransactionSummaryScreen() {
		create();
		addComponents();
		setSize(600, 600);
		setVisible(true);
	}

	private void addComponents() {
		JPanel panel = new JPanel();
		panel.add(new JLabel("Enter Ticker : "));

		panel.add(ticker);

		panel.add(fetchTransactions);
		panel.add(fetchWeeklyTransactions);
		panel.add(exportToExcel);
		Container con = getContentPane();
		con.add(panel, "North");
		con.add(new JScrollPane(results));
		fetchTransactions.addActionListener((e) -> fetch());
		fetchWeeklyTransactions.addActionListener((e) -> fetchWeekly());
	}

	
	private void fetch() {
		fetchThroughFetcher((e) -> strategy.getTransactionSummaries(e));
	}

	private void fetchThroughFetcher(TransactionSummaryFetcher fetcher) {
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
		ticker.setText("QQQ");
		fetchTransactions = new JButton("Backtest");
		fetchWeeklyTransactions = new JButton("Backtest weekly");
		exportToExcel = new JButton("Export to Excel");
		model = new TransactionSummaryTableModel();
		results = new JTable(model);
		strategy = new PassiveIncomeBackwardTestStrategy();
	
		results.setAutoCreateRowSorter(true);
		exportToExcel.addActionListener((e) -> export());

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
		return f.getDeclaringClass();
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
