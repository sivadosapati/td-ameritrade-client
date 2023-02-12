package com.rise.trading.options.passive.income.backtest;

import java.lang.reflect.Field;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public class DetailedTransactionSummaryForDayScreen extends JFrame {

	private JTable table;
	private DetailedTransactionSummaryForDayModel model;

	public DetailedTransactionSummaryForDayScreen(DetailedTransactionSummaryForDay day) {
		model = new DetailedTransactionSummaryForDayModel(day);
		table = new JTable(model);
		table.setAutoCreateRowSorter(true);
		getContentPane().add(new JScrollPane(table));
	}

}

class DetailedTransactionSummaryForDayModel implements TableModel {

	private DetailedTransactionSummaryForDay data;

	public DetailedTransactionSummaryForDayModel(DetailedTransactionSummaryForDay data) {
		this.data = data;
	}

	public HistoricalPricesForDay getPrices() {
		return data.day;
	}

	@Override
	public int getRowCount() {
		return data.getMinuteTransactions();
	}

	@Override
	public int getColumnCount() {
		Field fld[] = HistoricalPriceForMinute.class.getDeclaredFields();
		return fld.length;
	}

	@Override
	public String getColumnName(int columnIndex) {
		return HistoricalPriceForMinute.class.getDeclaredFields()[columnIndex].getName();

	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return HistoricalPriceForMinute.class.getDeclaredFields()[columnIndex].getType();
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		try {
			Field fld = HistoricalPriceForMinute.class.getDeclaredFields()[columnIndex];
			HistoricalPriceForMinute minute = getPrices().prices.get(rowIndex);
			return fld.get(minute);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		throw new RuntimeException("SHOULD NOT COME HERE");
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addTableModelListener(TableModelListener l) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeTableModelListener(TableModelListener l) {
		// TODO Auto-generated method stub

	}

}
