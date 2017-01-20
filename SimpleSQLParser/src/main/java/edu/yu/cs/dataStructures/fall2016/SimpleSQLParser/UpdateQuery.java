package edu.yu.cs.dataStructures.fall2016.SimpleSQLParser;

import java.util.HashSet;
import java.util.Set;

/**
 * describes a SQL "UPDATE" query
 * @author diament@yu.edu
 *
 */
public class UpdateQuery extends SQLQuery
{
    /**
     * the columns included in the UPDATE query and the values to use for those columns in rows that match the query
     * @see ColumnValuePair
     */
    private Set<ColumnValuePair> colValPairs;
    private String tableName;
    private Condition where;

    UpdateQuery(String queryString)
    {
	super(queryString);
	this.colValPairs = new HashSet<>();
    }

    /**
     * @return the "where" condition which dictates which rows will be updated
     */
    public Condition getWhereCondition()
    {
	return this.where;
    }
    void setWhereCondition(Condition where)
    {
	this.where = where;
    }

    /**
     * @return the name of the table to update
     */
    public String getTableName()
    {
	return tableName;
    }
    void setTableName(String tableName)
    {
	this.tableName = tableName;
    }

    /**
     * 
     * @return the column-value pairs in the order in which they were listed in the query
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