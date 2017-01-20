package edu.yu.cs.dataStructures.fall2016.SimpleSQLParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * represents a SELECT query
 * @author diament@yu.edu
 *
 */
public class SelectQuery extends SQLQuery
{
    /**
     * the names of functions supported within SELECT queries in this project
     * @author diament@yu.edu
     */
    public enum FunctionName
    {
	AVG("AVG"),
	COUNT("COUNT"),
	MAX("MAX"),
	MIN("MIN"),
	SUM("SUM");
	
	private String name;
	private FunctionName(String name)
	{
	    this.name = name;
	}
	
	public String toString()
	{
	    return this.name;
	}
    };
    
    public static class FunctionInstance
    {
	public FunctionName function;
	public boolean isDistinct;	
    }
    
    /**
     * holds the name of a column to order by, as well as a flag indicating if the result set should be ordered in ascending or descending order
     * @author diament@yu.edu
     *
     */
    public static class OrderBy
    {
	private boolean ascending;
	private ColumnID column;
	public OrderBy(ColumnID col, boolean ascending)
	{
	    this.column = col;
	    this.ascending = ascending;
	}
	public ColumnID getColumnID()
	{
	    return this.column;
	}
	public boolean isAscending()
	{
	    return this.ascending;
	}
	public boolean isDescending()
	{
	    return !this.ascending;
	}
    };
    
    private ArrayList<ColumnID> columnNames;
    private ArrayList<String> tableNames;
    private boolean distinct;
    private Condition where;
    private Map<ColumnID,FunctionInstance> functionMap;
    private ArrayList<OrderBy> orderBys;
    
    SelectQuery(String queryString)
    {
	super(queryString);
	this.columnNames = new ArrayList<>(); 
	this.tableNames = new ArrayList<>();
	this.orderBys = new ArrayList<>();
	this.functionMap = new HashMap<>();
    }

    /**
     * 
     * @return the column names in the order in which they were listed in the query
     */
    public OrderBy[] getOrderBys()
    {
	return this.orderBys.toArray(new OrderBy[this.orderBys.size()]);
    }
    void addOrderBy(OrderBy name)
    {
	this.orderBys.add(name);
    }
    
    /**
     * 
     * @return the column names selected by this query, in the order in which they were listed in the query
     */
    public ColumnID[] getSelectedColumnNames()
    {
	return this.columnNames.toArray(new ColumnID[this.columnNames.size()]);
    }
    void addSelectedColumnName(ColumnID name)
    {
	this.columnNames.add(name);
    }

    /**
     * 
     * @return the names of the tables to select data from, in the order in which they were listed in the query
     */
    public String[] getFromTableNames()
    {
	return this.tableNames.toArray(new String[this.tableNames.size()]);
    }
    void addFromTableName(String name)
    {
	this.tableNames.add(name);
    }    
    
    /**
     * @return indicates if the query included "DISTINCT", i.e. that no values be repeated
     */
    public boolean isDistinct()
    {
	return this.distinct;
    }
    void setDistinct(boolean distinct)
    {
	this.distinct = distinct;
    }

    /**
     * @return the "WHERE" condition of this query, if one exists
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
     *  
     * @return a map that indicates which function is being applied to a given column. If there is no function on a given column, then map.get(columnID) will return null
     */
    public Map<ColumnID,FunctionInstance> getFunctionMap()
    {
	return this.functionMap;
    }
    void addFunction(ColumnID column,FunctionInstance func)
    {
	this.functionMap.put(column, func);
    }
    /**
     * not relevant to SELECT queries
     * @throws UnsupportedOperationException
     */
    public ColumnValuePair[] getColumnValuePairs()
    {
	throw new UnsupportedOperationException();
    }
    /**
     * not relevant to SELECT queries
     * @throws UnsupportedOperationException
     */
    void addColumnValuePair(ColumnID col, String value)
    {
	throw new UnsupportedOperationException();	
    }    
}