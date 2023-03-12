package com.rise.trading.options.passive.income.backtest;

import java.lang.reflect.Field;
import java.text.MessageFormat;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public class DetailedTransactionSummaryForDayScreen extends JFrame {

	private JTable table;
	private DetailedTransactionSummaryForDayModel model;
	private TransactionSummary summary;

	public DetailedTransactionSummaryForDayScreen(DetailedTransactionSummaryForDay day, TransactionSummary summaryForDay) {
		model = new DetailedTransactionSummaryForDayModel(day);
		this.summary = summaryForDay;
		table = new JTable(model);
		table.setAutoCreateRowSorter(true);
		getContentPane().add(new JScrollPane(table));
		getContentPane().add(makeLabel(), "North");
	}

	private JLabel makeLabel() {
		//JLabel label = new JLabel();
		String x = "Open : {0}\nLow : {1}\nHigh : {2}\nClose : {3}\nCallStrike : {4}\nPutStrike : {5}";
		x = MessageFormat.format(x, summary.open, summary.low,summary.high,summary.close,summary.callStrikePrice, summary.putStrikePrice);
		return new JLabel(x);
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
		return fld.length + 4;
	}

	@Override
	public String getColumnName(int columnIndex) {

		Field[] flds = HistoricalPriceForMinute.class.getDeclaredFields();
		if (columnIndex == flds.length) {
			return "StockTransaction[CALL]";
		}
		if (columnIndex == flds.length + 1) {
			return "Stock Price";
		}

		if (columnIndex == flds.length + 2) {
			return "StockTransaction[PUT]";
		}
		if (columnIndex == flds.length + 3) {
			return "Stock Price";
		}

		return flds[columnIndex].getName();

	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		Field flds[] = HistoricalPriceForMinute.class.getDeclaredFields();
		if (columnIndex == flds.length) {
			return String.class;
		}
		if (columnIndex == flds.length + 1) {
			return Double.class;
		}
		if (columnIndex == flds.length + 2) {
			return String.class;
		}
		if (columnIndex == flds.length + 3) {
			return Double.class;
		}

		return flds[columnIndex].getType();
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		try {
			Field[] flds = HistoricalPriceForMinute.class.getDeclaredFields();
			StockTransaction call = getPotentialStockTransactionForCall(rowIndex);
			StockTransaction put = getPotentialStockTransactionForPut(rowIndex);
			if (columnIndex == flds.length) {
				if (call == null) {
					return "";
				} else {
					return call.type.toString();
				}
			}
			if (columnIndex == flds.length + 1) {
				if (call == null) {
					return null;
				} else {
					return call.stockPrice;
				}
			}
			if (columnIndex == flds.length + 2) {
				if (put == null) {
					return "";
				} else {
					return put.type.toString();
				}
			}
			if (columnIndex == flds.length + 3) {
				if (put == null) {
					return null;
				} else {
					return put.stockPrice;
				}
			}

			HistoricalPriceForMinute minute = getMinute(rowIndex);
			Field fld = flds[columnIndex];
			return fld.get(minute);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		throw new RuntimeException("SHOULD NOT COME HERE");
	}

	private HistoricalPriceForMinute getMinute(int row) {
		return getPrices().prices.get(row);
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

	public StockTransaction getPotentialStockTransactionForCall(int row) {
		return data.getStockTransactionForCall(getMinute(row));
	}

	public StockTransaction getPotentialStockTransactionForPut(int row) {
		return data.getStockTransactionForPut(getMinute(row));
	}

}
