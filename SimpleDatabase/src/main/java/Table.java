import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;
import edu.yu.cs.dataStructures.fall2016.SimpleSQLParser.ColumnDescription;
import edu.yu.cs.dataStructures.fall2016.SimpleSQLParser.ColumnValuePair;
import edu.yu.cs.dataStructures.fall2016.SimpleSQLParser.ColumnDescription.DataType;

class Table implements Serializable {
	private String name;
	private int key;
	private LinkedHashMap<String, Column> columns = new LinkedHashMap<String, Column>();
	private LinkedList<Object[]> rows = new LinkedList<Object[]>();
	private int columnNumber = 0;

	Table(String name, ColumnDescription[] columns, String key) {
		this.name = name;
		for (ColumnDescription column : columns) {
			DataType columnType = column.getColumnType();
			String columnName = column.getColumnName();
			boolean notNull = column.isNotNull();
			boolean unique = column.isUnique();
			if (unique) {
				notNull = true;
			}
			boolean hasDefault = column.getHasDefault();
			String defaultValue = null;
			if (column.getColumnName().equals(key)) {
				if (columnType == ColumnDescription.DataType.INT) {
					IntColumn newColumn = new IntColumn(this, columnName, columnNumber, null, true, true);
					newColumn.index();
					this.columns.put(columnName, newColumn);
				} else if (columnType == ColumnDescription.DataType.VARCHAR) {
					int varcharLength = column.getVarCharLength();
					VarcharColumn newColumn = new VarcharColumn(this, columnName, columnNumber, null, true, true,
							varcharLength);
					newColumn.index();
					this.columns.put(columnName, newColumn);
				} else if (columnType == ColumnDescription.DataType.DECIMAL) {
					int wholeNumberLength = column.getWholeNumberLength();
					int fractionalLength = column.getFractionLength();
					DecimalColumn newColumn = new DecimalColumn(this, columnName, columnNumber, null, true, true,
							wholeNumberLength, fractionalLength);
					newColumn.index();
					this.columns.put(columnName, newColumn);
				} else {
					throw new IllegalArgumentException("PRIMARY KEY column must be INT, DECIMAL, or  VARCHAR.");
				}
			} else {
				if (hasDefault == true) {
					defaultValue = column.getDefaultValue();
				}
				if (columnType == ColumnDescription.DataType.INT) {
					IntColumn newColumn = new IntColumn(this, columnName, columnNumber, defaultValue, unique, notNull);
					this.columns.put(columnName, newColumn);
				} else if (columnType == ColumnDescription.DataType.VARCHAR) {
					int varcharLength = column.getVarCharLength();
					VarcharColumn newColumn = new VarcharColumn(this, columnName, columnNumber, defaultValue, unique,
							notNull, varcharLength);
					this.columns.put(columnName, newColumn);
				} else if (columnType == ColumnDescription.DataType.DECIMAL) {
					int wholeNumberLength = column.getWholeNumberLength();
					int fractionalLength = column.getFractionLength();
					DecimalColumn newColumn = new DecimalColumn(this, columnName, columnNumber, defaultValue, unique,
							notNull, wholeNumberLength, fractionalLength);
					this.columns.put(columnName, newColumn);
				} else if (columnType == ColumnDescription.DataType.BOOLEAN) {
					if (hasDefault == true && column.getDefaultValue().equals("true")) {
						BooleanColumn newColumn = new BooleanColumn(this, columnName, columnNumber, "true");
						this.columns.put(columnName, newColumn);
					} else {
						BooleanColumn newColumn = new BooleanColumn(this, columnName, columnNumber, "false");
						this.columns.put(columnName, newColumn);
					}
				}
			}
			columnNumber++;
		}
		this.key = this.columns.get(key).getColumnNum();
	}

	Table(String name) {
		this.name = name;
	}

	public String toString() {
		String result = this.getTableName() + ":\n[";
		for (Column column : this.columns.values()) {
			result += column.toString() + "|";
		}
		result = result.substring(0, result.length() - 1) + "]";
		for (Object[] row : rows) {
			result += "\n[";
			for (Object o : row) {
				result += o + "|";
			}
			result = result.substring(0, result.length() - 1) + "]";
		}
		return result;
	}

	public String getColumnInfo() {
		return this.toString();
	}

	public LinkedList<Object[]> getRows() {
		return this.rows;
	}

	protected void setRows(LinkedList<Object[]> rowSet) {
		this.rows = rowSet;
	}

	protected void addColumn(String columnName, Column column) {
		this.columns.put(columnName, column);
	}

	protected String getTableName() {
		return this.name;
	}

	protected Column getColumn(String columnName) {
		if (!columns.containsKey(columnName)) {
			throw new IllegalArgumentException("No such column exists.");
		} else {
			return this.columns.get(columnName);
		}
	}

	protected LinkedHashMap<String, Column> getColumns() {
		return this.columns;
	}

	protected int getKey() {
		return this.key;
	}

	public Table copy() throws Exception {
		Table copy = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(bos);
		out.writeObject(this);
		out.flush();
		out.close();
		ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
		copy = (Table) in.readObject();
		return copy;
	}

	protected void addRow(ColumnValuePair[] values) {
		Object[] row = new Object[columns.size()];
		for (ColumnValuePair value : values) {
			String columnName = value.getColumnID().getColumnName();
			Column column = getColumn(columnName);
			int columnNum = column.getColumnNum();
			String columnType = column.getColumnType();
			String valueString = value.getValue();
			if (columnType.equals("VARCHAR")) {
				VarcharColumn varcharColumn = (VarcharColumn) column;
				valueString = valueString.replaceAll("'", "");
				if (valueString.length() > varcharColumn.getVarcharLength()) {
					throw new IllegalArgumentException("Entries into " + varcharColumn.getColumnName()
							+ " cannot be longer than " + varcharColumn.getVarcharLength() + " characters.");
				} else {
					row[columnNum] = valueString;
				}
			} else if (columnType.equals("INT")) {
				Integer entry = Integer.parseInt(valueString);
				row[columnNum] = entry;
			} else if (columnType.equals("DECIMAL")) {
				Double entry = Double.valueOf(valueString);
				String entryString = entry.toString();
				String wholeNumber = entryString.substring(0, entryString.indexOf("."));
				String fractionalNumber = entryString.substring(entryString.indexOf(".") + 1);
				DecimalColumn decimalColumn = (DecimalColumn) column;
				if (wholeNumber.length() > decimalColumn.getWholeNumberLength()
						|| fractionalNumber.length() > decimalColumn.getFractionLength()) {
					throw new IllegalArgumentException("Entries into " + decimalColumn.getColumnName()
							+ " cannot have more than " + decimalColumn.getWholeNumberLength()
							+ " digit(s) before decimal and " + decimalColumn.getFractionLength() + " after decimal.");
				} else {
					row[columnNum] = entry;
				}
			} else if (columnType.equals("BOOLEAN")) {
				if (valueString.equals("true") || valueString.equals("false")) {
					Boolean entry = Boolean.parseBoolean(valueString);
					row[columnNum] = entry;
				} else if (valueString.equals("'true'") || valueString.equals("'false'")) {
					valueString = valueString.replaceAll("'", "");
					Boolean entry = Boolean.parseBoolean(valueString);
					row[columnNum] = entry;
				} else {
					throw new IllegalArgumentException("Entries into " + column.getColumnName() + " must be boolean.");
				}
			}
		}
		for (Column column : columns.values()) {
			int columnNum = column.getColumnNum();
			if (row[columnNum] == null) {
				row[columnNum] = column.getDefaultValue();
			}
			if (column.isNotNull() && row[columnNum] == null) {
				throw new IllegalArgumentException("Entries into " + column.getColumnName() + " cannot be null.");
			}
			if (column.isUnique()) {
				for (Object[] existingRow : rows) {
					if (row[columnNum].equals(existingRow[columnNum])) {
						throw new IllegalArgumentException(
								"Entries into " + column.getColumnName() + " must be be unique.");
					}
				}
			}
			if (column.isIndexed() && row[columnNum] != null) {
				column.put(row);
			}
		}
		rows.add(row);
	}

	protected void deleteRows(LinkedList<Object[]> matches) {
		Iterator<Object[]> iter = this.rows.iterator();
		while (iter.hasNext()) {
			Object[] row = iter.next();
			if (row[key] instanceof String) {
				String rowKey = (String) row[key];
				for (Object[] match : matches) {
					String matchKey = (String) match[key];
					if (rowKey.equals(matchKey)) {
						iter.remove();
						for (Column column : columns.values()) {
							if (column.isIndexed()) {
								column.deleteRow(row);
							}
						}
					}
				}
			} else if (row[key] instanceof Integer) {
				Integer rowKey = (Integer) row[key];
				for (Object[] match : matches) {
					Integer matchKey = (Integer) match[key];
					if (rowKey.equals(matchKey)) {
						iter.remove();
						for (Column column : columns.values()) {
							if (column.isIndexed()) {
								column.deleteRow(row);
							}
						}
					}
				}
			} else if (row[key] instanceof Double) {
				Double rowKey = (Double) row[key];
				for (Object[] match : matches) {
					Double matchKey = (Double) match[key];
					if (rowKey.equals(matchKey)) {
						iter.remove();
						for (Column column : columns.values()) {
							if (column.isIndexed()) {
								column.deleteRow(row);
							}
						}
					}
				}
			}
		}
	}

	protected void deleteAll() {
		LinkedList<Object[]> newList = new LinkedList<Object[]>();
		this.rows = newList;
	}

	protected void updateRows(LinkedList<Object[]> rows, ColumnValuePair[] values) {
		LinkedList<Object[]> updatedRows = new LinkedList<Object[]>();
		for (Object[] row : rows) {
			for (ColumnValuePair value : values) {
				Column column = getColumn(value.getColumnID().getColumnName());
				int columnNum = column.getColumnNum();
				String columnType = column.getColumnType();
				String valueString = value.getValue();
				if (columnType.equals("VARCHAR")) {
					VarcharColumn varcharColumn = (VarcharColumn) column;
					valueString = valueString.replaceAll("'", "");
					if (valueString.length() > varcharColumn.getVarcharLength()) {
						throw new IllegalArgumentException("Entries into " + varcharColumn.getColumnName()
								+ " cannot be longer than " + varcharColumn.getVarcharLength() + " characters.");
					} else {
						row[columnNum] = valueString;
					}
				} else if (columnType.equals("INT")) {
					Integer entry = Integer.parseInt(valueString);
					row[columnNum] = entry;
				} else if (columnType.equals("DECIMAL")) {
					Double entry = Double.valueOf(valueString);
					String entryString = entry.toString();
					String wholeNumber = entryString.substring(0, entryString.indexOf("."));
					String fractionalNumber = entryString.substring(entryString.indexOf(".") + 1);
					DecimalColumn decimalColumn = (DecimalColumn) column;
					if (wholeNumber.length() > decimalColumn.getWholeNumberLength()
							|| fractionalNumber.length() > decimalColumn.getFractionLength()) {
						throw new IllegalArgumentException(
								"Entries into " + decimalColumn.getColumnName() + " cannot have more than "
										+ decimalColumn.getWholeNumberLength() + " digit(s) before decimal and "
										+ decimalColumn.getFractionLength() + " after decimal.");
					} else {
						row[columnNum] = entry;
					}
				} else if (columnType.equals("BOOLEAN")) {
					if (valueString.equals("true") || valueString.equals("false")) {
						Boolean entry = Boolean.parseBoolean(valueString);
						row[columnNum] = entry;
					} else if (valueString.equals("'true'") || valueString.equals("'false'")) {
						valueString = valueString.replaceAll("'", "");
						Boolean entry = Boolean.parseBoolean(valueString);
						row[columnNum] = entry;
					} else {
						throw new IllegalArgumentException(
								"Entries into " + column.getColumnName() + " must be boolean.");
					}
				}
			}
			for (Column column : columns.values()) {
				int columnNum = column.getColumnNum();
				if (row[columnNum] == null) {
					row[columnNum] = column.getDefaultValue();
				}
				if (column.isNotNull() && row[columnNum] == null) {
					throw new IllegalArgumentException("Entries into " + column.getColumnName() + " cannot be null.");
				}
				if (column.isUnique()) {
					for (Object[] existingRow : this.rows) {
						if (row[columnNum].equals(existingRow[columnNum]) && !row[key].equals(existingRow[key])) {
							throw new IllegalArgumentException(
									"Entries into " + column.getColumnName() + " must be be unique.");
						}
					}
				}
			}
			updatedRows.add(row);
		}
		for (Object[] row : rows) {
			if (row[key] instanceof String) {
				String rowKey = (String) row[key];
				for (Object[] updatedRow : updatedRows) {
					String updatedRowKey = (String) updatedRow[key];
					if (rowKey.equals(updatedRowKey)) {
						row = updatedRow;
						for (Column column : columns.values()) {
							if (column.isIndexed()) {
								column.deleteRow(row);
								if (updatedRow[column.getColumnNum()] != null) {
									column.put(row);
								}
							}
						}
					}
				}
			} else if (row[key] instanceof Integer) {
				Integer rowKey = (Integer) row[key];
				for (Object[] updatedRow : updatedRows) {
					Integer updatedRowKey = (Integer) updatedRow[key];
					if (rowKey.equals(updatedRowKey)) {
						row = updatedRow;
						for (Column column : columns.values()) {
							if (column.isIndexed()) {
								column.deleteRow(row);
								if (updatedRow[column.getColumnNum()] != null) {
									column.put(row);
								}
							}
						}
					}
				}
			} else if (row[key] instanceof Double) {
				Double rowKey = (Double) row[key];
				for (Object[] updatedRow : updatedRows) {
					Double updatedRowKey = (Double) updatedRow[key];
					if (rowKey.equals(updatedRowKey)) {
						row = updatedRow;
						for (Column column : columns.values()) {
							if (column.isIndexed()) {
								column.deleteRow(row);
								if (updatedRow[column.getColumnNum()] != null) {
									column.put(row);
								}
							}
						}
					}
				}
			}
		}
	}
}