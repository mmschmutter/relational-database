package edu.yu.cs.dataStructures.fall2016.SimpleSQLParser;

/**
 * represents a CREATE INDEX query
 * @author diament@yu.edu
 *
 */
public class CreateIndexQuery extends SQLQuery
{
    private String tableName;
    private String columnName;
    private String indexName;  
    
    CreateIndexQuery(String queryString)
    {
	super(queryString);
    }

    /**
     * @return the name of the table which contains the column to index
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
     * @return the name of the column on which to create the index
     */
    public String getColumnName()
    {
	return this.columnName;
    }
    void setColumnName(String columnName)
    {
	this.columnName = columnName;
    }

    /**
     * @return the name of the index
     */
    public String getIndexName()
    {
	return this.indexName;
    }
    void setIndexName(String indexName)
    {
	this.indexName = indexName;
    }
    /**
     * not relevant to CREATE queries
     * @throws UnsupportedOperationException
     */        
    public ColumnValuePair[] getColumnValuePairs()
    {
	throw new UnsupportedOperationException();
    }
    /**
     * not relevant to CREATE queries
     * @throws UnsupportedOperationException
     */        
    void addColumnValuePair(ColumnID col, String value)
    {
	throw new UnsupportedOperationException();	
    }    
}