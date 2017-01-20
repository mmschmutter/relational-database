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

	protected void update() throws Exception {
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
		} else if (this.tables.containsKey(table.getTableName())) {
			throw new IllegalArgumentException("A table with that name already exists.");
		} else {
			this.tables.put(table.getTableName(), table);
		}
	}

	public Table execute(String SQL) throws Exception {
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
				Column resultColumn = new BooleanColumn(resultTable, "Success", 0);
				resultTable.addColumn("Success", resultColumn);
				LinkedList<Object[]> resultRows = new LinkedList<Object[]>();
				Object[] resultRow = { true };
				resultRows.add(resultRow);
				resultTable.setRows(resultRows);
			} else {
				LinkedList<Object[]> matches = this.parseWhere(condition, table);
				int num = matches.size();
				if (num == 0) {
					Column resultColumn = new BooleanColumn(resultTable, "Success", 0);
					resultTable.addColumn("Success", resultColumn);
					LinkedList<Object[]> resultRows = new LinkedList<Object[]>();
					Object[] resultRow = { false };
					resultRows.add(resultRow);
					resultTable.setRows(resultRows);
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
				resultTable.setRows(resultRows);
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
				Table table = this.getTable(tableNames[0]).copy();
				LinkedHashMap<String, Column> tableColumns = table.getColumns();
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
					LinkedHashMap<String, Column> targetColumns = resultTable.getColumns();
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
							int i = 0;
							if (isDistinct) {
								for (Object distinctMatch : distinctSet) {
									if (distinctMatch != null) {
										i++;
									}
								}
							} else {
								for (Object tableMatch : tableMatches) {
									if (tableMatch != null) {
										i++;
									}
								}
							}
							functionResults[targetNum] = i;
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
			} else if (tableNames.length > 1) {
				LinkedList<Object[]> resultRows = new LinkedList<Object[]>();
				Table table1 = this.getTable(tableNames[0]).copy();
				Table table2 = this.getTable(tableNames[1]).copy();
				Table table = new Table(table1.getTableName() + "." + table2.getTableName());
				int colNum = 0;
				for (Column t1Column : table1.getColumns().values()) {
					String colName = table1.getTableName() + "." + t1Column.getColumnName();
					if (t1Column instanceof VarcharColumn) {
						VarcharColumn col = new VarcharColumn(table, colName, colNum);
						table.addColumn(colName, col);
					} else if (t1Column instanceof DecimalColumn) {
						DecimalColumn col = new DecimalColumn(table, colName, colNum);
						table.addColumn(colName, col);
					} else if (t1Column instanceof BooleanColumn) {
						BooleanColumn col = new BooleanColumn(table, colName, colNum);
						table.addColumn(colName, col);
					} else if (t1Column instanceof IntColumn) {
						IntColumn col = new IntColumn(table, colName, colNum);
						table.addColumn(colName, col);
					}
					colNum++;
				}
				for (Column t2Column : table2.getColumns().values()) {
					String colName = table2.getTableName() + "." + t2Column.getColumnName();
					if (t2Column instanceof VarcharColumn) {
						VarcharColumn col = new VarcharColumn(table, colName, colNum);
						table.addColumn(colName, col);
					} else if (t2Column instanceof DecimalColumn) {
						DecimalColumn col = new DecimalColumn(table, colName, colNum);
						table.addColumn(colName, col);
					} else if (t2Column instanceof BooleanColumn) {
						BooleanColumn col = new BooleanColumn(table, colName, colNum);
						table.addColumn(colName, col);
					} else if (t2Column instanceof IntColumn) {
						IntColumn col = new IntColumn(table, colName, colNum);
						table.addColumn(colName, col);
					}
					colNum++;
				}
				LinkedList<Object[]> mergedRows = new LinkedList<Object[]>();
				LinkedList<Object[]> t1Rows = table1.getRows();
				LinkedList<Object[]> t2Rows = table2.getRows();
				for (int j = 0; j < t1Rows.size(); j++) {
					Object[] t1Row = t1Rows.get(j);
					for (int k = 0; k < t2Rows.size(); k++) {
						Object[] t2Row = table2.getRows().get(k);
						Object[] mergedRow = Arrays.copyOf(t1Row, t1Row.length + t2Row.length);
						System.arraycopy(t2Row, 0, mergedRow, t1Row.length, t2Row.length);
						mergedRows.add(mergedRow);
					}
				}
				table.setRows(mergedRows);
				if (condition == null) {
					resultRows = table.getRows();
				} else {
					resultRows = this.parseWhere(condition, table);
				}
				if (columnIDs[0].getColumnName() != "*") {
					int columnNum = 0;
					for (ColumnID columnID : columnIDs) {
						Column targetColumn = table.getColumn(columnID.getTableName() + "." + columnID.getColumnName());
						String resultColumnName = targetColumn.getColumnName();
						if (targetColumn instanceof VarcharColumn) {
							VarcharColumn resultColumn = new VarcharColumn(resultTable, resultColumnName, columnNum);
							resultTable.addColumn(resultColumnName, resultColumn);
						} else if (targetColumn instanceof DecimalColumn) {
							DecimalColumn resultColumn = new DecimalColumn(resultTable, resultColumnName, columnNum);
							resultTable.addColumn(resultColumnName, resultColumn);
						} else if (targetColumn instanceof BooleanColumn) {
							BooleanColumn resultColumn = new BooleanColumn(resultTable, resultColumnName, columnNum);
							resultTable.addColumn(resultColumnName, resultColumn);
						} else if (targetColumn instanceof IntColumn) {
							IntColumn resultColumn = new IntColumn(resultTable, resultColumnName, columnNum);
							resultTable.addColumn(resultColumnName, resultColumn);
						}
						columnNum++;
					}
					LinkedHashMap<String, Column> targetColumns = resultTable.getColumns();
					HashMap<Integer, Integer> columnMap = new HashMap<Integer, Integer>();
					for (String targetColumn : targetColumns.keySet()) {
						int originalNum = table.getColumns().get(targetColumn).getColumnNum();
						int newNum = targetColumns.get(targetColumn).getColumnNum();
						columnMap.put(originalNum, newNum);
					}
					ListIterator<Object[]> iter = resultRows.listIterator();
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
					int columnNum = 0;
					for (Column targetColumn : table.getColumns().values()) {
						String resultColumnName = targetColumn.getColumnName();
						if (targetColumn instanceof VarcharColumn) {
							VarcharColumn resultColumn = new VarcharColumn(resultTable, resultColumnName, columnNum);
							resultTable.addColumn(resultColumn.getColumnName(), resultColumn);
						} else if (targetColumn instanceof DecimalColumn) {
							DecimalColumn resultColumn = new DecimalColumn(resultTable, resultColumnName, columnNum);
							resultTable.addColumn(resultColumn.getColumnName(), targetColumn);
						} else if (targetColumn instanceof BooleanColumn) {
							BooleanColumn resultColumn = new BooleanColumn(resultTable, resultColumnName, columnNum);
							resultTable.addColumn(resultColumn.getColumnName(), resultColumn);
						} else if (targetColumn instanceof IntColumn) {
							IntColumn resultColumn = new IntColumn(resultTable, resultColumnName, columnNum);
							resultTable.addColumn(resultColumn.getColumnName(), resultColumn);
						}
						columnNum++;
					}
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
			String leftName;
			if (left.getTableName() != null) {
				leftName = left.getTableName() + "." + left.getColumnName();
			} else {
				leftName = left.getColumnName();
			}
			Column leftColumn = table.getColumn(leftName);
			int leftCol = leftColumn.getColumnNum();
			String leftType = leftColumn.getColumnType();
			if (condition.getRightOperand() instanceof ColumnID) {
				ColumnID right = (ColumnID) condition.getRightOperand();
				String rightName;
				if (right.getTableName() != null) {
					rightName = right.getTableName() + "." + right.getColumnName();
				} else {
					rightName = right.getColumnName();
				}
				Column rightColumn = table.getColumn(rightName);
				int rightCol = rightColumn.getColumnNum();
				String rightType = rightColumn.getColumnType();
				if (!leftType.equals(rightType)) {
					throw new IllegalArgumentException("Cannot compare columns of different types.");
				} else if (leftColumn.isIndexed() && rightColumn.isIndexed()) {
					matches = indexAndIndex(table, leftColumn, rightColumn, leftType, rightType, operator);
				} else {
					matches = columnAndColumn(table, leftColumn, rightColumn, leftType, rightType, operator, leftCol,
							rightCol);
				}
			} else if (condition.getRightOperand() instanceof String) {
				String right = (String) condition.getRightOperand();
				right = right.replaceAll("'", "");
				if (leftColumn.isIndexed()) {
					matches = indexAndString(table, leftColumn, leftType, operator, leftCol, right);
				} else {
					matches = columnAndString(table, leftColumn, leftType, operator, leftCol, right);
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
			if (operator.equals("AND")) {
				for (Object[] leftRow : leftRows) {
					for (Object[] rightRow : rightRows) {
						if (leftRow.equals(rightRow)) {
							matches.add(leftRow);
						}
					}
				}
				return matches;
			} else if (operator.equals("OR")) {
				HashSet<Object[]> matchSet = new HashSet<Object[]>();
				for (Object[] leftRow : leftRows) {
					matchSet.add(leftRow);
				}
				for (Object[] rightRow : rightRows) {
					matchSet.add(rightRow);
				}
				matches.addAll(matchSet);
			}
		}
		return matches;
	}

	@SuppressWarnings("unchecked")
	private LinkedList<Object[]> indexAndIndex(Table table, Column leftColumn, Column rightColumn, String leftType,
			String rightType, String operator) {
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
						matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
					} else if (operator.equals("<>") && !leftVal.equals(rightVal)) {
						matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
					}
				} else if (leftType.equals("BOOLEAN")) {
					Boolean leftValue = (Boolean) leftVal;
					Boolean rightValue = (Boolean) rightVal;
					if (operator.equals("=") && leftValue.equals(rightValue)) {
						matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
					} else if (operator.equals("<>") && !leftVal.equals(rightVal)) {
						matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
					}
				} else if (leftType.equals("DECIMAL")) {
					Double leftNum = (Double) leftVal;
					Double rightNum = (Double) rightVal;
					if (operator.equals("=") && leftNum.equals(rightNum)) {
						matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
					} else if (operator.equals("<>") && !leftNum.equals(rightNum)) {
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
					Integer leftNum = (Integer) leftVal;
					Integer rightNum = (Integer) rightVal;
					if (operator.equals("=") && leftNum.equals(rightNum)) {
						matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
					} else if (operator.equals("<>") && !leftNum.equals(rightNum)) {
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
				}
			}
		}
		return matches;
	}

	private LinkedList<Object[]> columnAndColumn(Table table, Column leftColumn, Column rightColumn, String leftType,
			String rightType, String operator, int leftCol, int rightCol) {
		LinkedList<Object[]> matches = new LinkedList<Object[]>();
		LinkedList<Object[]> rows = table.getRows();
		for (Object[] row : rows) {
			if (leftType.equals("VARCHAR")) {
				String leftStr = (String) row[leftCol];
				String rightStr = (String) row[rightCol];
				if (operator.equals("=") && leftStr.equals(rightStr)) {
					matches.add(row);
				} else if (operator.equals("<>") && !leftStr.equals(rightStr)) {
					matches.add(row);
				} else {
					throw new IllegalArgumentException(
							"Only INT and DECIMAL columns can be evaluated as greater or lesser.");
				}
			} else if (leftType.equals("BOOLEAN")) {
				Boolean leftStr = (Boolean) row[leftCol];
				Boolean rightStr = (Boolean) row[rightCol];
				if (operator.equals("=") && leftStr.equals(rightStr)) {
					matches.add(row);
				} else if (operator.equals("<>") && !leftStr.equals(rightStr)) {
					matches.add(row);
				} else {
					throw new IllegalArgumentException(
							"Only INT and DECIMAL columns can be evaluated as greater or lesser.");
				}
			} else if (leftType.equals("DECIMAL")) {
				Double leftNum = (Double) row[leftCol];
				Double rightNum = (Double) row[rightCol];
				if (operator.equals("=") && leftNum.equals(rightNum)) {
					matches.add(row);
				} else if (operator.equals("<>") && !leftNum.equals(rightNum)) {
					matches.add(row);
				} else if (operator.equals("<") && leftNum < rightNum) {
					matches.add(row);
				} else if (operator.equals("<=") && leftNum <= rightNum) {
					matches.add(row);
				} else if (operator.equals(">") && leftNum > rightNum) {
					matches.add(row);
				} else if (operator.equals(">=") && leftNum >= rightNum) {
					matches.add(row);
				}
			} else if (leftType.equals("INT")) {
				Integer leftNum = (Integer) row[leftCol];
				Integer rightNum = (Integer) row[rightCol];
				if (operator.equals("=") && leftNum.equals(rightNum)) {
					matches.add(row);
				} else if (operator.equals("<>") && !leftNum.equals(rightNum)) {
					matches.add(row);
				} else if (operator.equals("<") && leftNum < rightNum) {
					matches.add(row);
				} else if (operator.equals("<=") && leftNum <= rightNum) {
					matches.add(row);
				} else if (operator.equals(">") && leftNum > rightNum) {
					matches.add(row);
				} else if (operator.equals(">=") && leftNum >= rightNum) {
					matches.add(row);
				}
			}
		}
		return matches;
	}

	private LinkedList<Object[]> columnAndString(Table table, Column leftColumn, String leftType, String operator,
			int leftCol, String right) {
		LinkedList<Object[]> matches = new LinkedList<Object[]>();
		LinkedList<Object[]> rows = table.getRows();
		for (Object[] row : rows) {
			if (leftType.equals("VARCHAR")) {
				String leftStr = (String) row[leftCol];
				if (operator.equals("=") && leftStr.equals(right)) {
					matches.add(row);
				} else if (operator.equals("<>") && !leftStr.equals(right)) {
					matches.add(row);
				}
			} else if (leftType.equals("BOOLEAN")) {
				Boolean leftStr = (Boolean) row[leftCol];
				if (operator.equals("=") && leftStr.equals(Boolean.parseBoolean(right))) {
					matches.add(row);
				} else if (operator.equals("<>") && !leftStr.equals(Boolean.parseBoolean(right))) {
					matches.add(row);
				}
			} else if (leftType.equals("DECIMAL")) {
				Double leftNum = (Double) row[leftCol];
				Double rightNum = Double.parseDouble(right);
				if (operator.equals("=") && leftNum.equals(rightNum)) {
					matches.add(row);
				} else if (operator.equals("<>") && !leftNum.equals(rightNum)) {
					matches.add(row);
				} else if (operator.equals("<") && leftNum < rightNum) {
					matches.add(row);
				} else if (operator.equals("<=") && leftNum <= rightNum) {
					matches.add(row);
				} else if (operator.equals(">") && leftNum > rightNum) {
					matches.add(row);
				} else if (operator.equals(">=") && leftNum >= rightNum) {
					matches.add(row);
				}
			} else if (leftType.equals("INT")) {
				Integer leftNum = (Integer) row[leftCol];
				Integer rightNum = Integer.parseInt(right);
				if (operator.equals("=") && leftNum.equals(rightNum)) {
					matches.add(row);
				} else if (operator.equals("<>") && !leftNum.equals(rightNum)) {
					matches.add(row);
				} else if (operator.equals("<") && leftNum < rightNum) {
					matches.add(row);
				} else if (operator.equals("<=") && leftNum <= rightNum) {
					matches.add(row);
				} else if (operator.equals(">") && leftNum > rightNum) {
					matches.add(row);
				} else if (operator.equals(">=") && leftNum >= rightNum) {
					matches.add(row);
				}
			} else {
				throw new IllegalArgumentException(
						"Only INT and DECIMAL columns can be evaluated as greater or lesser.");
			}
		}
		return matches;
	}

	@SuppressWarnings("unchecked")
	private LinkedList<Object[]> indexAndString(Table table, Column leftColumn, String leftType, String operator,
			int leftCol, String right) {
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
				if (operator.equals("=") && leftNum.equals(rightNum)) {
					matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
				} else if (operator.equals("<>") && !leftNum.equals(rightNum)) {
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
				if (operator.equals("=") && leftNum.equals(rightNum)) {
					matches.addAll((LinkedList<Object[]>) leftIdx.getValue());
				} else if (operator.equals("<>") && !leftNum.equals(rightNum)) {
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