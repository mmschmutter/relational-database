import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;

class BooleanColumn implements Column, Serializable {
	private String columnName;
	private String defaultValue;
	private boolean isIndexed;
	private int columnNum;
	private Table table;
	private BTree<Boolean, LinkedList<Object[]>> indexedRows;

	BooleanColumn(Table table, String columnName, int columnNum, String defaultValue) {
		this.columnName = columnName;
		this.defaultValue = defaultValue;
		this.table = table;
		this.columnNum = columnNum;
	}

	BooleanColumn(Table table, String columnName, int columnNum) {
		this.columnName = columnName;
		this.table = table;
		this.columnNum = columnNum;
	}

	public void index() {
		isIndexed = true;
		indexedRows = new BTree<Boolean, LinkedList<Object[]>>();
		LinkedList<Object[]> rows = table.getRows();
		for (Object[] row : rows) {
			Boolean indexedColumn = (Boolean) row[columnNum];
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
		Boolean indexedColumn = (Boolean) entry[columnNum];
		LinkedList<Object[]> rowList = indexedRows.get(indexedColumn);
		rowList.add(entry);
		indexedRows.put(indexedColumn, rowList);
	}

	public String toString() {
		return this.columnNum + ": BOOLEAN " + this.columnName;
	}

	public String getColumnName() {
		return this.columnName;
	}

	public String getColumnType() {
		return "BOOLEAN";
	}

	public String getDefaultValue() {
		return this.defaultValue;
	}

	public boolean isIndexed() {
		return this.isIndexed;
	}

	public int getColumnNum() {
		return columnNum;
	}

	public boolean isUnique() {
		return false;
	}

	public boolean isNotNull() {
		return true;
	}

	public void deleteRow(Object[] row) {
		Boolean indexedColumn = (Boolean) row[columnNum];
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
		Boolean columnValue = (Boolean) key;
		return this.indexedRows.get(columnValue);
	}

	public ArrayList<BTree.Entry> getOrdered() {
		return this.indexedRows.getOrderedEntries();
	}
}