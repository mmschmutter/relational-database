package edu.yu.cs.dataStructures.fall2016.SimpleSQLParser;
/**
 * represents a DELETE query
 * @author diament@yu.edu
 *
 */
public class DeleteQuery extends SQLQuery
{
    private String tableName;
    private Condition where;
    
    DeleteQuery(String queryString)
    {
	super(queryString);
    }

    /**
     * @return the tableName
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
     * @return the "WHERE" condition which dictates which rows will be delete
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
     * not relevant to DELETE queries
     * @throws UnsupportedOperationException
     */    
    public ColumnValuePair[] getColumnValuePairs()
    {
	throw new UnsupportedOperationException();
    }
    /**
     * not relevant to DELETE queries
     * @throws UnsupportedOperationException
     */    
    void addColumnValuePair(ColumnID col, String value)
    {
	throw new UnsupportedOperationException();	
    }    
}