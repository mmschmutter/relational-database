import java.io.*;
import java.util.*;

import edu.yu.cs.dataStructures.fall2016.SimpleSQLParser.*;
import edu.yu.cs.dataStructures.fall2016.SimpleSQLParser.SelectQuery.FunctionInstance;
import net.sf.jsqlparser.JSQLParserException;

class Database implements Serializable {
	protected ReaderWriter logger = new ReaderWriter(this);
	private HashMap<String, Table> tables = new HashMap<String, Table>();
	boolean updating = false;

	Database() {
		if (ReaderWriter.getNewestBackup() != null) {
			Database backup = ReaderWriter.load();
			this.tables = backup.getTables();
		}
	}

	protected void update() {
		updating = true;
		File f = new File("./log.txt");
		Scanner in;
		if (f.exists()) {
			try {
				in = new Scanner(f);
				while (in.hasNextLine()) {
					String line = in.nextLine();
					String[] query = line.split(":");
					Long queryTime = Long.parseLong(query[0]);
					if (ReaderWriter.getNewestBackup() != null) {
						Long backupTime = Long.parseLong(ReaderWriter.getNewestBackup().substring(3,
								ReaderWriter.getNewestBackup().length() - 3));
						if (queryTime > backupTime) {
							this.execute(query[1]);
						}
					} else {
						this.execute(query[1]);
					}
				}
			} catch (IOException | JSQLParserException e) {
				e.printStackTrace();
			}
		}
		updating = false;
	}

	protected Table getTable(String tableName) {
		if (!tables.containsKey(tableName)) {
			throw new IllegalArgumentException("No such table exists.");
		} else {
			return this.tables.get(tableName);
		}
	}

	protected HashMap<String, Table> getTables() {
		return this.tables;
	}

	protected void addTable(Table table) {
		if (table == null) {
			throw new IllegalArgumentException("Entries into database cannot be null.");
		} else {
			this.tables.put(table.getTableName(), table);
		}
	}

	public Table execute(String SQL) throws JSQLParserException, IOException {
		String sqlText = SQL;
		SQLParser parser = new SQLParser();
		SQLQuery sqlQuery = parser.parse(SQL);
		Table resultTable = new Table("Results");
		if (sqlQuery instanceof CreateTableQuery) {
			CreateTableQuery query = (CreateTableQuery) sqlQuery;
			ColumnDescription[] columns = query.getColumnDescriptions();
			String key = query.getPrimaryKeyColumn().getColumnName();
			String name = query.getTableName();
			Table newTable = new Table(name, columns, key);
			this.addTable(newTable);
			if (updating != true) {
				this.logger.log(sqlText);
			}
			resultTable = newTable;
		} else if (sqlQuery instanceof CreateIndexQuery) {
			CreateIndexQuery query = (CreateIndexQuery) sqlQuery;
			this.getTable(query.getTableName()).getColumn(query.getColumnName()).index();
			if (updating != true) {
				this.logger.log(sqlText);
			}
			Column resultColumn = new BooleanColumn(resultTable, "Success", 0);
			resultTable.addColumn("Success", resultColumn);
			LinkedList<Object[]> resultRows = new LinkedList<Object[]>();
			Object[] resultRow = { true };
			resultRows.add(resultRow);
			resultTable.setRows(resultRows);
		} else if (sqlQuery instanceof InsertQuery) {
			InsertQuery query = (InsertQuery) sqlQuery;
			ColumnValuePair[] values = query.getColumnValuePairs();
			Table table = this.getTable(query.getTableName());
			table.addRow(values);
			if (updating != true) {
				this.logger.log(sqlText);
				this.logger.rowChange(1);
			}
			Column resultColumn = new BooleanColumn(resultTable, "Success", 0);
			resultTable.addColumn("Success", resultColumn);
			LinkedList<Object[]> resultRows = new LinkedList<Object[]>();
			Object[] resultRow = { true };
			resultRows.add(resultRow);
			resultTable.setRows(resultRows);
		} else if (sqlQuery instanceof DeleteQuery) {
			DeleteQuery query = (DeleteQuery) sqlQuery;
			Condition condition = query.getWhereCondition();
			Table table = this.getTable(query.getTableName());
			int number = table.getRows().size();
			if (condition == null) {
				table.deleteAll();
				if (updating != true) {
					this.logger.log(sqlText);
					this.logger.rowChange(number);
				}
			} else {
				LinkedList<Object[]> matches = this.parseWhere(condition, table);
				int num = matches.size();
				if (num == 0) {
					Column resultColumn = new BooleanColumn(resultTable, "Success", 0);
					resultTable.addColumn("Success", resultColumn);
					LinkedList<Object[]> resultRows = new LinkedList<Object[]>();
					Object[] resultRow = { false };
					resultRows.add(resultRow);
				} else {
					table.deleteRows(matches);
					if (updating != true) {
						this.logger.log(sqlText);
						this.logger.rowChange(num);
					}
					Column resultColumn = new BooleanColumn(resultTable, "Success", 0);
					resultTable.addColumn("Success", resultColumn);
					LinkedList<Object[]> resultRows = new LinkedList<Object[]>();
					Object[] resultRow = { true };
					resultRows.add(resultRow);
					resultTable.setRows(resultRows);
				}
			}
		} else if (sqlQuery instanceof UpdateQuery) {
			UpdateQuery query = (UpdateQuery) sqlQuery;
			Table table = this.getTable(query.getTableName());
			Condition condition = query.getWhereCondition();
			LinkedList<Object[]> matches;
			if (condition == null) {
				matches = table.getRows();
			} else {
				matches = this.parseWhere(condition, table);
			}
			int number = matches.size();
			if (number == 0) {
				Column resultColumn = new BooleanColumn(resultTable, "Success", 0);
				resultTable.addColumn("Success", resultColumn);
				LinkedList<Object[]> resultRows = new LinkedList<Object[]>();
				Object[] resultRow = { false };
				resultRows.add(resultRow);
			} else {
				ColumnValuePair[] values = query.getColumnValuePairs();
				table.updateRows(matches, values);
				if (updating != true) {
					this.logger.log(sqlText);
					this.logger.rowChange(number);
				}
				Column resultColumn = new BooleanColumn(resultTable, "Success", 0);
				resultTable.addColumn("Success", resultColumn);
				LinkedList<Object[]> resultRows = new LinkedList<Object[]>();
				Object[] resultRow = { true };
				resultRows.add(resultRow);
				resultTable.setRows(resultRows);
			}
		} else if (sqlQuery instanceof SelectQuery) {
			SelectQuery query = (SelectQuery) sqlQuery;
			ColumnID[] columnIDs = query.getSelectedColumnNames();
			String orderColumn = null;
			boolean ascending = false;
			if (query.getOrderBys().length != 0) {
				orderColumn = query.getOrderBys()[0].getColumnID().getColumnName();
				ascending = query.getOrderBys()[0].isAscending();
			}
			Map<ColumnID, FunctionInstance> map = query.getFunctionMap();
			boolean distinct = query.isDistinct();
			Condition condition = query.getWhereCondition();
			String[] tableNames = query.getFromTableNames();
			if (tableNames.length == 1) {
				Table table = this.getTable(tableNames[0]);
				HashMap<String, Column> tableColumns = table.getColumns();
				LinkedList<Object[]> tableMatches;
				if (condition == null) {
					tableMatches = table.getRows();
				} else {
					tableMatches = this.parseWhere(condition, table);
				}
				if (columnIDs[0].getColumnName() != "*") {
					int columnNum = 0;
					for (ColumnID columnID : columnIDs) {
						Column targetColumn = table.getColumn(columnID.getColumnName());
						if (targetColumn instanceof VarcharColumn) {
							VarcharColumn resultColumn = new VarcharColumn(resultTable, targetColumn.getColumnName(),
									columnNum);
							resultTable.addColumn(resultColumn.getColumnName(), resultColumn);
						} else if (targetColumn instanceof DecimalColumn) {
							DecimalColumn resultColumn = new DecimalColumn(resultTable, targetColumn.getColumnName(),
									columnNum);
							resultTable.addColumn(resultColumn.getColumnName(), resultColumn);
						} else if (targetColumn instanceof BooleanColumn) {
							BooleanColumn resultColumn = new BooleanColumn(resultTable, targetColumn.getColumnName(),
									columnNum);
							resultTable.addColumn(resultColumn.getColumnName(), resultColumn);
						} else if (targetColumn instanceof IntColumn) {
							IntColumn resultColumn = new IntColumn(resultTable, targetColumn.getColumnName(),
									columnNum);
							resultTable.addColumn(resultColumn.getColumnName(), resultColumn);
						}
						columnNum++;
					}
					HashMap<String, Column> targetColumns = resultTable.getColumns();
					HashMap<Integer, Integer> columnMap = new HashMap<Integer, Integer>();
					for (String targetColumn : targetColumns.keySet()) {
						int originalNum = tableColumns.get(targetColumn).getColumnNum();
						int newNum = targetColumns.get(targetColumn).getColumnNum();
						columnMap.put(originalNum, newNum);
					}
					ListIterator<Object[]> iter = tableMatches.listIterator();
					while (iter.hasNext()) {
						Object[] tableMatch = iter.next();
						Object[] targetMatch = new Object[targetColumns.size()];
						for (Integer originalNum : columnMap.keySet()) {
							int newNum = columnMap.get(originalNum);
							targetMatch[newNum] = tableMatch[originalNum];
						}
						iter.set(targetMatch);
					}
				} else {
					for (Column targetColumn : table.getColumns().values()) {
						if (targetColumn instanceof VarcharColumn) {
							VarcharColumn resultColumn = new VarcharColumn(resultTable, targetColumn.getColumnName(),
									targetColumn.getColumnNum());
							resultTable.addColumn(resultColumn.getColumnName(), resultColumn);
						} else if (targetColumn instanceof DecimalColumn) {
							DecimalColumn resultColumn = new DecimalColumn(resultTable, targetColumn.getColumnName(),
									targetColumn.getColumnNum());
							resultTable.addColumn(resultColumn.getColumnName(), targetColumn);
						} else if (targetColumn instanceof BooleanColumn) {
							BooleanColumn resultColumn = new BooleanColumn(resultTable, targetColumn.getColumnName(),
									targetColumn.getColumnNum());
							resultTable.addColumn(resultColumn.getColumnName(), resultColumn);
						} else if (targetColumn instanceof IntColumn) {
							IntColumn resultColumn = new IntColumn(resultTable, targetColumn.getColumnName(),
									targetColumn.getColumnNum());
							resultTable.addColumn(resultColumn.getColumnName(), resultColumn);
						}
					}
				}
				if (distinct) {
					LinkedList<Object[]> distinctMatches = new LinkedList<Object[]>();
					HashSet<Object[]> set = new HashSet<Object[]>();
					set.addAll(tableMatches);
					distinctMatches.addAll(set);
					tableMatches = distinctMatches;
				}
				if (!map.isEmpty()) {
					Object[] functionResults = new Object[map.size()];
					for (ColumnID columnID : map.keySet()) {
						String columnName = columnID.getColumnName();
						String function = map.get(columnID).function.name();
						boolean isDistinct = map.get(columnID).isDistinct;
						Column targetColumn = resultTable.getColumn(columnName);
						int targetNum = targetColumn.getColumnNum();
						HashSet<Object> distinctSet = new HashSet<Object>();
						if (isDistinct) {
							for (Object[] tableMatch : tableMatches) {
								distinctSet.add(tableMatch[targetNum]);
							}
						}
						if (function.equals("COUNT")) {
							if (isDistinct) {
								functionResults[targetNum] = distinctSet.size();
							} else {
								functionResults[targetNum] = tableMatches.size();
							}
						} else if (targetColumn instanceof VarcharColumn || targetColumn instanceof BooleanColumn) {
							throw new IllegalArgumentException(
									"MAX, MIN, AVG, and SUM can only be used on number columns.");
						} else {
							if (function.equals("MAX") && targetColumn instanceof DecimalColumn) {
								if (isDistinct) {
									Double max = 0.0;
									for (Object distinctMatch : distinctSet) {
										Double entry = (Double) distinctMatch;
										if (entry > max) {
											max = entry;
										}
									}
									functionResults[targetNum] = max;
								} else {
								}
								Double max = (Double) tableMatches.get(0)[targetNum];
								for (Object[] tableMatch : tableMatches) {
									Double entry = (Double) tableMatch[targetNum];
									if (entry > max) {
										max = entry;
									}
								}
								functionResults[targetNum] = max;
							} else if (function.equals("MAX") && targetColumn instanceof IntColumn) {
								if (isDistinct) {
									Integer max = 0;
									for (Object distinctMatch : distinctSet) {
										Integer entry = (Integer) distinctMatch;
										if (entry > max) {
											max = entry;
										}
									}
									functionResults[targetNum] = max;
								} else {
									Integer max = (Integer) tableMatches.get(0)[targetNum];
									for (Object[] tableMatch : tableMatches) {
										Integer entry = (Integer) tableMatch[targetNum];
										if (entry > max) {
											max = entry;
										}
									}
									functionResults[targetNum] = max;
								}
							} else if (function.equals("MIN") && targetColumn instanceof DecimalColumn) {
								if (isDistinct) {
									Double min = 9999999999.9;
									for (Object distinctMatch : distinctSet) {
										Double entry = (Double) distinctMatch;
										if (entry < min) {
											min = entry;
										}
									}
									functionResults[targetNum] = min;
								} else {
									Double min = (Double) tableMatches.get(0)[targetNum];
									for (Object[] tableMatch : tableMatches) {
										Double entry = (Double) tableMatch[targetNum];
										if (entry < min) {
											min = entry;
										}
									}
									functionResults[targetNum] = min;
								}
							} else if (function.equals("MIN") && targetColumn instanceof IntColumn) {
								if (isDistinct) {
									Double min = 9999999999.9;
									for (Object distinctMatch : distinctSet) {
										Double entry = (Double) distinctMatch;
										if (entry < min) {
											min = entry;
										}
									}
									functionResults[targetNum] = min;
								} else {
									Integer min = (Integer) tableMatches.get(0)[targetNum];
									for (Object[] tableMatch : tableMatches) {
										Integer entry = (Integer) tableMatch[targetNum];
										if (entry < min) {
											min = entry;
										}
									}
									functionResults[targetNum] = min;
								}
							} else if (function.equals("SUM") && targetColumn instanceof DecimalColumn) {
								if (isDistinct) {
									Double sum = 0.0;
									for (Object distinctMatch : distinctSet) {
										Double entry = (Double) distinctMatch;
										sum += entry;
									}
									functionResults[targetNum] = sum;
								} else {
									Double sum = 0.0;
									for (Object[] tableMatch : tableMatches) {
										Double entry = (Double) tableMatch[targetNum];
										sum += entry;
									}
									functionResults[targetNum] = sum;
								}
							} else if (function.equals("SUM") && targetColumn instanceof IntColumn) {
								if (isDistinct) {
									Integer sum = 0;
									for (Object distinctMatch : distinctSet) {
										Integer entry = (Integer) distinctMatch;
										sum += entry;
									}
									functionResults[targetNum] = sum;
								} else {
									Integer sum = 0;
									for (Object[] tableMatch : tableMatches) {
										Integer entry = (Integer) tableMatch[targetNum];
										sum += entry;
									}
									functionResults[targetNum] = sum;
								}
							} else if (function.equals("AVG") && targetColumn instanceof DecimalColumn) {
								if (isDistinct) {
									Double avg = 0.0;
									for (Object distinctMatch : distinctSet) {
										Double entry = (Double) distinctMatch;
										avg += entry;
									}
									avg = avg / distinctSet.size();
									functionResults[targetNum] = avg;
								} else {
									Double avg = 0.0;
									for (Object[] tableMatch : tableMatches) {
										Double entry = (Double) tableMatch[targetNum];
										avg += entry;
									}
									avg = avg / tableMatches.size();
									functionResults[targetNum] = avg;
								}
							} else if (function.equals("AVG") && targetColumn instanceof IntColumn) {
								if (isDistinct) {
									Double avg = 0.0;
									for (Object distinctMatch : distinctSet) {
										Integer entry = (Integer) distinctMatch;
										avg += entry;
									}
									avg = avg / distinctSet.size();
									functionResults[targetNum] = avg;
								} else {
									Double avg = 0.0;
									for (Object[] tableMatch : tableMatches) {
										Integer entry = (Integer) tableMatch[targetNum];
										avg += entry;
									}
									avg = avg / tableMatches.size();
									functionResults[targetNum] = avg;
								}
							}
						}
					}
					tableMatches.clear();
					tableMatches.add(functionResults);
				}
				resultTable.setRows(tableMatches);
				if (orderColumn != null) {
					resultTable.getColumn(orderColumn).index();
					ArrayList<BTree.Entry> orderedEntries = resultTable.getColumn(orderColumn).getOrdered();
					LinkedList<Object[]> orderedResults = new LinkedList<Object[]>();
					if (ascending) {
						for (BTree.Entry orderedEntry : orderedEntries) {
							@SuppressWarnings("unchecked")
							LinkedList<Object[]> orderedRows = (LinkedList<Object[]>) orderedEntry.getValue();
							orderedResults.addAll(orderedRows);
						}
					} else {
						for (ListIterator<BTree.Entry> iter = orderedEntries.listIterator(orderedEntries.size()); iter
								.hasPrevious();) {
							BTree.Entry orderedEntry = iter.previous();
							@SuppressWarnings("unchecked")
							LinkedList<Object[]> orderedRows = (LinkedList<Object[]>) orderedEntry.getValue();
							orderedResults.addAll(orderedRows);
						}
					}
					resultTable.setRows(orderedResults);
				}
			} else {
				int columnNum = 0;
				int tableNum = 0;
				LinkedList<Object[]> resultRows = new LinkedList<Object[]>();
				for (String tableName : tableNames) {
					Table table = this.getTable(tableName);
					HashMap<String, Column> tableColumns = table.getColumns();
					LinkedList<Object[]> tableMatches;
					if (condition == null) {
						tableMatches = table.getRows();
					} else {
						tableMatches = this.parseWhere(condition, table);
					}
					if (columnIDs[0].getColumnName() != "*") {
						for (ColumnID columnID : columnIDs) {
							Column targetColumn = table.getColumn(columnID.getColumnName());
							String resultColumnName = tableName + "." + targetColumn.getColumnName();
							if (targetColumn instanceof VarcharColumn) {
								VarcharColumn resultColumn = new VarcharColumn(resultTable, resultColumnName,
										columnNum);
								resultTable.addColumn(resultColumnName, resultColumn);
							} else if (targetColumn instanceof DecimalColumn) {
								DecimalColumn resultColumn = new DecimalColumn(resultTable, resultColumnName,
										columnNum);
								resultTable.addColumn(resultColumnName, resultColumn);
							} else if (targetColumn instanceof BooleanColumn) {
								BooleanColumn resultColumn = new BooleanColumn(resultTable, resultColumnName,
										columnNum);
								resultTable.addColumn(resultColumnName, resultColumn);
							} else if (targetColumn instanceof IntColumn) {
								IntColumn resultColumn = new IntColumn(resultTable, resultColumnName, columnNum);
								resultTable.addColumn(resultColumnName, resultColumn);
							}
							columnNum++;
						}
						HashMap<String, Column> targetColumns = resultTable.getColumns();
						HashMap<Integer, Integer> columnMap = new HashMap<Integer, Integer>();
						for (String targetColumn : targetColumns.keySet()) {
							int originalNum = tableColumns.get(targetColumn).getColumnNum();
							int newNum = targetColumns.get(targetColumn).getColumnNum();
							columnMap.put(originalNum, newNum);
						}
						ListIterator<Object[]> iter = tableMatches.listIterator();
						while (iter.hasNext()) {
							Object[] tableMatch = iter.next();
							Object[] targetMatch = new Object[targetColumns.size()];
							for (Integer originalNum : columnMap.keySet()) {
								int newNum = columnMap.get(originalNum);
								targetMatch[newNum] = tableMatch[originalNum];
							}
							iter.set(targetMatch);
						}
					} else {
						for (Column targetColumn : table.getColumns().values()) {
							String resultColumnName = tableName + "." + targetColumn.getColumnName();
							if (targetColumn instanceof VarcharColumn) {
								VarcharColumn resultColumn = new VarcharColumn(resultTable, resultColumnName,
										columnNum);
								resultTable.addColumn(resultColumn.getColumnName(), resultColumn);
							} else if (targetColumn instanceof DecimalColumn) {
								DecimalColumn resultColumn = new DecimalColumn(resultTable, resultColumnName,
										columnNum);
								resultTable.addColumn(resultColumn.getColumnName(), targetColumn);
							} else if (targetColumn instanceof BooleanColumn) {
								BooleanColumn resultColumn = new BooleanColumn(resultTable, resultColumnName,
										columnNum);
								resultTable.addColumn(resultColumn.getColumnName(), resultColumn);
							} else if (targetColumn instanceof IntColumn) {
								IntColumn resultColumn = new IntColumn(resultTable, resultColumnName, columnNum);
								resultTable.addColumn(resultColumn.getColumnName(), resultColumn);
							}
							columnNum++;
						}
					}
					if (tableNum == 0) {
						resultRows = tableMatches;
					} else {
						ListIterator<Object[]> iter = resultRows.listIterator();
						int i = 0;
						while (iter.hasNext()) {
							Object[] oldRow = iter.next();
							Object[] newRow = tableMatches.get(i);
							Object[] resultRow = Arrays.copyOf(oldRow, oldRow.length + newRow.length);
							System.arraycopy(newRow, 0, resultRow, oldRow.length, newRow.length);
							iter.set(resultRow);
							i++;
						}
					}
					tableNum++;
				}
				resultTable.setRows(resultRows);
			}
		}
		return resultTable;
	}

	protected LinkedList<Object[]> parseWhere(Condition condition, Table table) {
		LinkedList<Object[]> matches = new LinkedList<Object[]>();
		String operator = condition.getOperator().toString();
		if (condition.getLeftOperand() instanceof ColumnID) {
			ColumnID left = (ColumnID) condition.getLeftOperand();
			Table leftTable;
			if (left.getTableName() == null) {
				leftTable = table;
			} else {
				leftTable = getTable(left.getTableName());
			}
			Column leftColumn = leftTable.getColumn(left.getColumnName());
			int leftCol = leftColumn.getColumnNum();
			String leftType = leftColumn.getColumnType();
			if (condition.getRightOperand() instanceof ColumnID) {
				ColumnID right = (ColumnID) condition.getRightOperand();
				Table rightTable;
				if (right.getTableName() == null) {
					rightTable = table;
				} else {
					rightTable = getTable(right.getTableName());
				}
				Column rightColumn = rightTable.getColumn(right.getColumnName());
				int rightCol = rightColumn.getColumnNum();
				String rightType = rightColumn.getColumnType();
				if (!leftType.equals(rightType)) {
					throw new IllegalArgumentException("Cannot compare columns of different types.");
				} else if (leftColumn.isIndexed() && rightColumn.isIndexed()) {
					matches = indexAndIndex(table, leftTable, rightTable, leftColumn, rightColumn, leftType, rightType,
							operator);
				} else {
					matches = columnAndColumn(table, leftTable, rightTable, leftColumn, rightColumn, leftType,
							rightType, operator, leftCol, rightCol);
				}
			} else if (condition.getRightOperand() instanceof String) {
				String right = (String) condition.getRightOperand();
				right = right.replaceAll("'", "");
				if (leftColumn.isIndexed()) {
					matches = indexAndString(table, leftTable, leftColumn, leftType, operator, leftCol, right);
				} else {
					matches = columnAndString(table, leftTable, leftColumn, leftType, operator, leftCol, right);
				}

			} else {
				throw new IllegalArgumentException("Columns must be compared with either a value or another column.");
			}
		} else if (condition.getLeftOperand() instanceof Condition
				&& condition.getRightOperand() instanceof Condition) {
			Condition left = (Condition) condition.getLeftOperand();
			Condition right = (Condition) condition.getRightOperand();
			LinkedList<Object[]> leftRows = parseWhere(left, table);
			LinkedList<Object[]> rightRows = parseWhere(right, table);
			int key = table.getKey();
			if (operator.equals("AND")) {
				for (Object[] leftRow : leftRows) {
					if (leftRow[key] instanceof String) {
						String leftKey = (String) leftRow[key];
						for (Object[] rightRow : rightRows) {
							String rightKey = (String) rightRow[key];
							if (leftKey.equals(rightKey)) {
								matches.add(leftRow);
							}
						}
					} else if (leftRow[key] instanceof Integer) {
						Integer leftKey = (Integer) leftRow[key];
						for (Object[] rightRow : rightRows) {
							Integer rightKey = (Integer) rightRow[key];
							if (leftKey.equals(rightKey)) {
								matches.add(leftRow);
							}
						}
					} else if (leftRow[key] instanceof Double) {
						Double leftKey = (Double) leftRow[key];
						for (Object[] rightRow : rightRows) {
							Double rightKey = (Double) rightRow[key];
							if (leftKey.equals(rightKey)) {
								matches.add(leftRow);
							}
						}
					}
				}
				return matches;
			} else if (operator.equals("OR")) {
				LinkedList<Object> usedKeys = new LinkedList<Object>();
				for (Object[] leftRow : leftRows) {
					Object leftKey = leftRow[key];
					usedKeys.add(leftKey);
					matches.add(leftRow);
				}
				for (Object[] rightRow : rightRows) {
					Object rightKey = rightRow[key];
					if (!usedKeys.contains(rightKey)) {
						matches.add(rightRow);
					}
				}
			}
		}
		return matches;
	}

	@SuppressWarnings("unchecked")
	private LinkedList<Object[]> indexAndIndex(Table table, Table leftTable, Table rightTable, Column leftColumn,
			Column rightColumn, String leftType, String rightType, String operator) {
		LinkedList<Object[]> matches = new LinkedList<Object[]>();
		ArrayList<BTree.Entry> leftIdxs = leftColumn.getOrdered();
		ArrayList<BTree.Entry> rightIdxs = rightColumn.getOrdered();
		for (BTree.Entry leftIdx : leftIdxs) {
			Object leftVal = leftIdx.getKey();
			for (BTree.Entry rightIdx : rightIdxs) {
				Object rightVal = rightIdx.getKey();
				if (leftType.equals("VARCHAR")) {
					String leftValue = (String) leftVal;
					String rightValue = (String) rightVal;
					if (operator.equals("=") && leftValue.equals(rightValue)) {
						if (table.equals(leftTable)) {
							matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
						} else if (table.equals(rightTable)) {
							matches.addAll((LinkedList<Object[]>) rightIdx.getValue());
						}
					} else if (operator.equals("<>") && !leftVal.equals(rightVal)) {
						if (table.equals(leftTable)) {
							matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
						} else if (table.equals(rightTable)) {
							matches.addAll((LinkedList<Object[]>) rightIdx.getValue());
						}
					}
				} else if (leftType.equals("BOOLEAN")) {
					Boolean leftValue = (Boolean) leftVal;
					Boolean rightValue = (Boolean) rightVal;
					if (operator.equals("=") && leftValue.equals(rightValue)) {
						if (table.equals(leftTable)) {
							matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
						} else if (table.equals(rightTable)) {
							matches.addAll((LinkedList<Object[]>) rightIdx.getValue());
						}
					} else if (operator.equals("<>") && !leftVal.equals(rightVal)) {
						if (table.equals(leftTable)) {
							matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
						} else if (table.equals(rightTable)) {
							matches.addAll((LinkedList<Object[]>) rightIdx.getValue());
						}
					}
				} else if (leftType.equals("DECIMAL")) {
					Double leftNum = (Double) leftVal;
					Double rightNum = (Double) rightVal;
					if (operator.equals("=") && leftNum == rightNum) {
						if (table.equals(leftTable)) {
							matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
						} else if (table.equals(rightTable)) {
							matches.addAll((LinkedList<Object[]>) rightIdx.getValue());
						}
					} else if (operator.equals("<>") && leftNum != rightNum) {
						if (table.equals(leftTable)) {
							matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
						} else if (table.equals(rightTable)) {
							matches.addAll((LinkedList<Object[]>) rightIdx.getValue());
						}
					} else if (operator.equals("<") && leftNum < rightNum) {
						if (table.equals(leftTable)) {
							matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
						} else if (table.equals(rightTable)) {
							matches.addAll((LinkedList<Object[]>) rightIdx.getValue());
						}
					} else if (operator.equals("<=") && leftNum <= rightNum) {
						if (table.equals(leftTable)) {
							matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
						} else if (table.equals(rightTable)) {
							matches.addAll((LinkedList<Object[]>) rightIdx.getValue());
						}
					} else if (operator.equals(">") && leftNum > rightNum) {
						if (table.equals(leftTable)) {
							matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
						} else if (table.equals(rightTable)) {
							matches.addAll((LinkedList<Object[]>) rightIdx.getValue());
						}
					} else if (operator.equals(">=") && leftNum >= rightNum) {
						if (table.equals(leftTable)) {
							matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
						} else if (table.equals(rightTable)) {
							matches.addAll((LinkedList<Object[]>) rightIdx.getValue());
						}
					}
				} else if (leftType.equals("INT")) {
					Integer leftNum = (Integer) leftVal;
					Integer rightNum = (Integer) rightVal;
					if (operator.equals("=") && leftNum == rightNum) {
						if (table.equals(leftTable)) {
							matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
						} else if (table.equals(rightTable)) {
							matches.addAll((LinkedList<Object[]>) rightIdx.getValue());
						}
					} else if (operator.equals("<>") && leftNum != rightNum) {
						if (table.equals(leftTable)) {
							matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
						} else if (table.equals(rightTable)) {
							matches.addAll((LinkedList<Object[]>) rightIdx.getValue());
						}
					} else if (operator.equals("<") && leftNum < rightNum) {
						if (table.equals(leftTable)) {
							matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
						} else if (table.equals(rightTable)) {
							matches.addAll((LinkedList<Object[]>) rightIdx.getValue());
						}
					} else if (operator.equals("<=") && leftNum <= rightNum) {
						if (table.equals(leftTable)) {
							matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
						} else if (table.equals(rightTable)) {
							matches.addAll((LinkedList<Object[]>) rightIdx.getValue());
						}
					} else if (operator.equals(">") && leftNum > rightNum) {
						if (table.equals(leftTable)) {
							matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
						} else if (table.equals(rightTable)) {
							matches.addAll((LinkedList<Object[]>) rightIdx.getValue());
						}
					} else if (operator.equals(">=") && leftNum >= rightNum) {
						if (table.equals(leftTable)) {
							matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
						} else if (table.equals(rightTable)) {
							matches.addAll((LinkedList<Object[]>) rightIdx.getValue());
						}
					}
				}
			}
		}
		return matches;
	}

	private LinkedList<Object[]> columnAndColumn(Table table, Table leftTable, Table rightTable, Column leftColumn,
			Column rightColumn, String leftType, String rightType, String operator, int leftCol, int rightCol) {
		LinkedList<Object[]> matches = new LinkedList<Object[]>();
		LinkedList<Object[]> leftRows = leftTable.getRows();
		LinkedList<Object[]> rightRows = rightTable.getRows();
		for (Object[] leftRow : leftRows) {
			for (Object[] rightRow : rightRows) {
				if (leftType.equals("VARCHAR")) {
					String leftStr = (String) leftRow[leftCol];
					String rightStr = (String) rightRow[rightCol];
					if (operator.equals("=") && leftStr.equals(rightStr)) {
						if (table.equals(leftTable)) {
							matches.add(leftRow);
						} else if (table.equals(rightTable)) {
							matches.add(rightRow);
						}
					} else if (operator.equals("<>") && !leftStr.equals(rightStr)) {
						if (table.equals(leftTable)) {
							matches.add(leftRow);
						} else if (table.equals(rightTable)) {
							matches.add(rightRow);
						}
					} else {
						throw new IllegalArgumentException(
								"Only INT and DECIMAL columns can be evaluated as greater or lesser.");
					}
				} else if (leftType.equals("BOOLEAN")) {
					Boolean leftStr = (Boolean) leftRow[leftCol];
					Boolean rightStr = (Boolean) rightRow[rightCol];
					if (operator.equals("=") && leftStr.equals(rightStr)) {
						if (table.equals(leftTable)) {
							matches.add(leftRow);
						} else if (table.equals(rightTable)) {
							matches.add(rightRow);
						}
					} else if (operator.equals("<>") && !leftStr.equals(rightStr)) {
						if (table.equals(leftTable)) {
							matches.add(leftRow);
						} else if (table.equals(rightTable)) {
							matches.add(rightRow);
						}
					} else {
						throw new IllegalArgumentException(
								"Only INT and DECIMAL columns can be evaluated as greater or lesser.");
					}
				} else if (leftType.equals("DECIMAL")) {
					Double leftNum = (Double) leftRow[leftCol];
					Double rightNum = (Double) rightRow[rightCol];
					if (operator.equals("=") && leftNum == rightNum) {
						if (table.equals(leftTable)) {
							matches.add(leftRow);
						} else if (table.equals(rightTable)) {
							matches.add(rightRow);
						}
					} else if (operator.equals("<>") && leftNum != rightNum) {
						if (table.equals(leftTable)) {
							matches.add(leftRow);
						} else if (table.equals(rightTable)) {
							matches.add(rightRow);
						}
					} else if (operator.equals("<") && leftNum < rightNum) {
						if (table.equals(leftTable)) {
							matches.add(leftRow);
						} else if (table.equals(rightTable)) {
							matches.add(rightRow);
						}
					} else if (operator.equals("<=") && leftNum <= rightNum) {
						if (table.equals(leftTable)) {
							matches.add(leftRow);
						} else if (table.equals(rightTable)) {
							matches.add(rightRow);
						}
					} else if (operator.equals(">") && leftNum > rightNum) {
						if (table.equals(leftTable)) {
							matches.add(leftRow);
						} else if (table.equals(rightTable)) {
							matches.add(rightRow);
						}
					} else if (operator.equals(">=") && leftNum >= rightNum) {
						if (table.equals(leftTable)) {
							matches.add(leftRow);
						} else if (table.equals(rightTable)) {
							matches.add(rightRow);
						}
					}
				} else if (leftType.equals("INT")) {
					Integer leftNum = (Integer) leftRow[leftCol];
					Integer rightNum = (Integer) rightRow[rightCol];
					if (operator.equals("=") && leftNum == rightNum) {
						if (table.equals(leftTable)) {
							matches.add(leftRow);
						} else if (table.equals(rightTable)) {
							matches.add(rightRow);
						}
					} else if (operator.equals("<>") && leftNum != rightNum) {
						if (table.equals(leftTable)) {
							matches.add(leftRow);
						} else if (table.equals(rightTable)) {
							matches.add(rightRow);
						}
					} else if (operator.equals("<") && leftNum < rightNum) {
						if (table.equals(leftTable)) {
							matches.add(leftRow);
						} else if (table.equals(rightTable)) {
							matches.add(rightRow);
						}
					} else if (operator.equals("<=") && leftNum <= rightNum) {
						if (table.equals(leftTable)) {
							matches.add(leftRow);
						} else if (table.equals(rightTable)) {
							matches.add(rightRow);
						}
					} else if (operator.equals(">") && leftNum > rightNum) {
						if (table.equals(leftTable)) {
							matches.add(leftRow);
						} else if (table.equals(rightTable)) {
							matches.add(rightRow);
						}
					} else if (operator.equals(">=") && leftNum >= rightNum) {
						if (table.equals(leftTable)) {
							matches.add(leftRow);
						} else if (table.equals(rightTable)) {
							matches.add(rightRow);
						}
					}
				}
			}
		}
		return matches;
	}

	private LinkedList<Object[]> columnAndString(Table table, Table leftTable, Column leftColumn, String leftType,
			String operator, int leftCol, String right) {
		LinkedList<Object[]> matches = new LinkedList<Object[]>();
		LinkedList<Object[]> leftRows = leftTable.getRows();
		for (Object[] leftRow : leftRows) {
			if (leftType.equals("VARCHAR")) {
				String leftStr = (String) leftRow[leftCol];
				if (operator.equals("=") && leftStr.equals(right)) {
					matches.add(leftRow);
				} else if (operator.equals("<>") && !leftStr.equals(right)) {
					matches.add(leftRow);
				}
			} else if (leftType.equals("BOOLEAN")) {
				Boolean leftStr = (Boolean) leftRow[leftCol];
				if (operator.equals("=") && leftStr.equals(Boolean.parseBoolean(right))) {
					matches.add(leftRow);
				} else if (operator.equals("<>") && !leftStr.equals(Boolean.parseBoolean(right))) {
					matches.add(leftRow);
				}
			} else if (leftType.equals("DECIMAL")) {
				Double leftNum = (Double) leftRow[leftCol];
				Double rightNum = Double.parseDouble(right);
				if (operator.equals("=") && leftNum == rightNum) {
					matches.add(leftRow);
				} else if (operator.equals("<>") && leftNum != rightNum) {
					matches.add(leftRow);
				} else if (operator.equals("<") && leftNum < rightNum) {
					matches.add(leftRow);
				} else if (operator.equals("<=") && leftNum <= rightNum) {
					matches.add(leftRow);
				} else if (operator.equals(">") && leftNum > rightNum) {
					matches.add(leftRow);
				} else if (operator.equals(">=") && leftNum >= rightNum) {
					matches.add(leftRow);
				}
			} else if (leftType.equals("INT")) {
				Integer leftNum = (Integer) leftRow[leftCol];
				Integer rightNum = Integer.parseInt(right);
				if (operator.equals("=") && leftNum == rightNum) {
					matches.add(leftRow);
				} else if (operator.equals("<>") && leftNum != rightNum) {
					matches.add(leftRow);
				} else if (operator.equals("<") && leftNum < rightNum) {
					matches.add(leftRow);
				} else if (operator.equals("<=") && leftNum <= rightNum) {
					matches.add(leftRow);
				} else if (operator.equals(">") && leftNum > rightNum) {
					matches.add(leftRow);
				} else if (operator.equals(">=") && leftNum >= rightNum) {
					matches.add(leftRow);
				}
			} else {
				throw new IllegalArgumentException(
						"Only INT and DECIMAL columns can be evaluated as greater or lesser.");
			}
		}
		return matches;
	}

	@SuppressWarnings("unchecked")
	private LinkedList<Object[]> indexAndString(Table table, Table leftTable, Column leftColumn, String leftType,
			String operator, int leftCol, String right) {
		LinkedList<Object[]> matches = new LinkedList<Object[]>();
		ArrayList<BTree.Entry> leftIdxs = leftColumn.getOrdered();
		for (BTree.Entry leftIdx : leftIdxs) {
			if (leftType.equals("VARCHAR")) {
				String leftStr = (String) leftIdx.getKey();
				if (operator.equals("=") && leftStr.equals(right)) {
					matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
				} else if (operator.equals("<>") && !leftStr.equals(right)) {
					matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
				}
			} else if (leftType.equals("BOOLEAN")) {
				Boolean leftStr = (Boolean) leftIdx.getKey();
				if (operator.equals("=") && leftStr.equals(Boolean.parseBoolean(right))) {
					matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
				} else if (operator.equals("<>") && !leftStr.equals(Boolean.parseBoolean(right))) {
					matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
				}
			} else if (leftType.equals("DECIMAL")) {
				Double leftNum = (Double) leftIdx.getKey();
				Double rightNum = Double.parseDouble(right);
				if (operator.equals("=") && leftNum == rightNum) {
					matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
				} else if (operator.equals("<>") && leftNum != rightNum) {
					matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
				} else if (operator.equals("<") && leftNum < rightNum) {
					matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
				} else if (operator.equals("<=") && leftNum <= rightNum) {
					matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
				} else if (operator.equals(">") && leftNum > rightNum) {
					matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
				} else if (operator.equals(">=") && leftNum >= rightNum) {
					matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
				}
			} else if (leftType.equals("INT")) {
				Integer leftNum = (Integer) leftIdx.getKey();
				Integer rightNum = Integer.parseInt(right);
				if (operator.equals("=") && leftNum == rightNum) {
					matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
				} else if (operator.equals("<>") && leftNum != rightNum) {
					matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
				} else if (operator.equals("<") && leftNum < rightNum) {
					matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
				} else if (operator.equals("<=") && leftNum <= rightNum) {
					matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
				} else if (operator.equals(">") && leftNum > rightNum) {
					matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
				} else if (operator.equals(">=") && leftNum >= rightNum) {
					matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
				}
			} else {
				throw new IllegalArgumentException(
						"Only INT and DECIMAL columns can be evaluated as greater or lesser.");
			}
		}
		return matches;
	}
}