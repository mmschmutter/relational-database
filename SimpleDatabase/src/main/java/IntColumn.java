import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;

class IntColumn implements Column, Serializable {
	private String columnName;
	private boolean unique;
	private boolean notNull;
	private String defaultValue;
	private boolean isIndexed;
	private int columnNum;
	private Table table;
	private BTree<Integer, LinkedList<Object[]>> indexedRows;

	IntColumn(Table table, String columnName, int columnNum, String defaultValue, boolean unique, boolean notNull) {
		this.columnName = columnName;
		this.defaultValue = defaultValue;
		this.unique = unique;
		this.notNull = notNull;
		this.table = table;
		this.columnNum = columnNum;
	}

	IntColumn(Table table, String columnName, int columnNum) {
		this.columnName = columnName;
		this.table = table;
		this.columnNum = columnNum;
	}

	public void index() {
		isIndexed = true;
		indexedRows = new BTree<Integer, LinkedList<Object[]>>();
		LinkedList<Object[]> rows = table.getRows();
		for (Object[] row : rows) {
			Integer indexedColumn = (Integer) row[columnNum];
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
		Integer indexedColumn = (Integer) entry[columnNum];
		LinkedList<Object[]> rowList;
		if (indexedRows.get(indexedColumn) != null) {
			rowList = indexedRows.get(indexedColumn);
			rowList.add(entry);
		} else {
			rowList = new LinkedList<Object[]>();
			rowList.add(entry);
		}
		indexedRows.put(indexedColumn, rowList);
	}

	public String toString() {
		return this.columnNum + ": INT " + this.columnName;
	}

	public String getColumnName() {
		return this.columnName;
	}

	public String getColumnType() {
		return "INT";
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

	public boolean isIndexed() {
		return this.isIndexed;
	}

	public int getColumnNum() {
		return columnNum;
	}

	public void deleteRow(Object[] row) {
		Integer indexedColumn = (Integer) row[columnNum];
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
		Integer columnValue = (Integer) key;
		return this.indexedRows.get(columnValue);
	}

	public ArrayList<BTree.Entry> getOrdered() {
		return this.indexedRows.getOrderedEntries();
	}
}
