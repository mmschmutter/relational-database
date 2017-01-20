import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;

class DecimalColumn implements Column, Serializable{
	private String columnName;
	private boolean unique;
	private boolean notNull;
	private String defaultValue;
	private int wholeNumberLength;
	private int fractionalLength;
	private boolean isIndexed;
	private int columnNum;
	private Table table;
	private BTree<Double, LinkedList<Object[]>> indexedRows;

	DecimalColumn(Table table, String columnName, int columnNum, String defaultValue, boolean unique, boolean notNull,
			int wholeNumberLength, int fractionalLength) {
		this.columnName = columnName;
		this.unique = unique;
		this.notNull = notNull;
		this.defaultValue = defaultValue;
		this.wholeNumberLength = wholeNumberLength;
		this.fractionalLength = fractionalLength;
		this.table = table;
		this.columnNum = columnNum;
	}

	DecimalColumn(Table table, String columnName, int columnNum) {
		this.columnName = columnName;
		this.table = table;
		this.columnNum = columnNum;
	}

	public void index() {
		isIndexed = true;
		indexedRows = new BTree<Double, LinkedList<Object[]>>();
		LinkedList<Object[]> rows = table.getRows();
		for (Object[] row : rows) {
			Double indexedColumn = (Double) row[columnNum];
			LinkedList<Object[]> rowList;
			if (indexedRows.get(indexedColumn) != null) {
				rowList = indexedRows.get(indexedColumn);
				rowList.add(row);
			} else {
				rowList = new LinkedList<Object[]>();
				rowList.add(row);
			}
			indexedRows.put(indexedColumn, rowList);
		}
	}

	public void put(Object[] entry) {
		Double indexedColumn = (Double) entry[columnNum];
		LinkedList<Object[]> rowList = indexedRows.get(indexedColumn);
		rowList.add(entry);
		indexedRows.put(indexedColumn, rowList);
	}

	public String toString() {
		return this.columnNum + ": DECIMAL " + this.columnName;
	}

	public String getColumnName() {
		return this.columnName;
	}

	public String getColumnType() {
		return "DECIMAL";
	}

	public boolean isUnique() {
		return this.unique;
	}

	public boolean isNotNull() {
		return this.notNull;
	}

	public String getDefaultValue() {
		return this.defaultValue;
	}

	public int getWholeNumberLength() {
		return this.wholeNumberLength;
	}

	public int getFractionLength() {
		return this.fractionalLength;
	}

	public boolean isIndexed() {
		return this.isIndexed;
	}

	public int getColumnNum() {
		return this.columnNum;
	}

	public void deleteRow(Object[] row) {
		Double indexedColumn = (Double) row[columnNum];
		LinkedList<Object[]> matches = indexedRows.get(indexedColumn);
		int key = table.getKey();
		for (Object[] match : matches) {
			if (match[key].equals(row[key])) {
				matches.remove(match);
			}
		}
		this.indexedRows.put(indexedColumn, matches);
	}

	public LinkedList<Object[]> getIndexed(Object key) {
		Double columnValue = (Double) key;
		return this.indexedRows.get(columnValue);
	}

	public ArrayList<BTree.Entry> getOrdered() {
		return this.indexedRows.getOrderedEntries();
	}
}