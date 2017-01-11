import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;

class VarcharColumn implements Column, Serializable {
	private String columnName;
	private boolean unique;
	private boolean notNull;
	private String defaultValue;
	private int varcharLength;
	private boolean isIndexed;
	private int columnNum;
	private Table table;
	private BTree<String, LinkedList<Object[]>> indexedRows;

	VarcharColumn(Table table, String columnName, int columnNum, String defaultValue, boolean unique, boolean notNull,
			int varcharLength) {
		this.columnName = columnName;
		this.unique = unique;
		this.notNull = notNull;
		this.defaultValue = defaultValue;
		this.varcharLength = varcharLength;
		this.table = table;
		this.columnNum = columnNum;
	}

	VarcharColumn(Table table, String columnName, int columnNum) {
		this.columnName = columnName;
		this.table = table;
		this.columnNum = columnNum;
	}

	public void index() {
		isIndexed = true;
		indexedRows = new BTree<String, LinkedList<Object[]>>();
		LinkedList<Object[]> rows = table.getRows();
		for (Object[] row : rows) {
			String indexedColumn = (String) row[columnNum];
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
		String indexedColumn = (String) entry[columnNum];
		LinkedList<Object[]> rowList = indexedRows.get(indexedColumn);
		rowList.add(entry);
		indexedRows.put(indexedColumn, rowList);
	}

	public String toString() {
		return this.columnNum + ": VARCHAR " + this.columnName;
	}

	public String getColumnType() {
		return "VARCHAR";
	}

	public String getColumnName() {
		return this.columnName;
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

	public int getVarcharLength() {
		return this.varcharLength;
	}

	public boolean isIndexed() {
		return this.isIndexed;
	}

	public int getColumnNum() {
		return columnNum;
	}

	public void deleteRow(Object[] row) {
		String indexedColumn = (String) row[columnNum];
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
		String columnValue = (String) key;
		return this.indexedRows.get(columnValue);
	}

	public ArrayList<BTree.Entry> getOrdered() {
		return this.indexedRows.getOrderedEntries();
	}
}