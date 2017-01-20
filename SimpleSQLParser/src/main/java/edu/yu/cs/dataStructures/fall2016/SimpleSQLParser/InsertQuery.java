package edu.yu.cs.dataStructures.fall2016.SimpleSQLParser;

import java.util.HashSet;
import java.util.Set;

/**
 * @author diament@yu.edu
 * represents an INSERT query
 */
public class InsertQuery extends SQLQuery
{
    private String tableName;
    private Set<ColumnValuePair> colValPairs;

    /**
     * 
     */
    InsertQuery(String queryString)
    {
	super(queryString);
	this.colValPairs = new HashSet<>();
    }

    /**
     * @return the table name into which to insert data
     */
    public String getTableName()
    {
	return this.tableName;
    }
    void setTableName(String tableName)
    {
	this.tableName = tableName;
    }

    /**
     * @see ColumnValuePair
     * @return the column-value pairs in the order in which they were listed in the INSERT query
     */
    public ColumnValuePair[] getColumnValuePairs()
    {
	return this.colValPairs.toArray(new ColumnValuePair[this.colValPairs.size()]);
    }
    void addColumnValuePair(ColumnID col, String value)
    {
	this.colValPairs.add(new ColumnValuePair(col, value));
    }
}