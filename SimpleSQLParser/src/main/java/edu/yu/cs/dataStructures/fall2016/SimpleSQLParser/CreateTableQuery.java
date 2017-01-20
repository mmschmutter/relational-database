package edu.yu.cs.dataStructures.fall2016.SimpleSQLParser;

import java.util.HashSet;
import java.util.Set;

/**
 * @author diament@yu.edu
 * represents a CREATE TABLE query
 */
public class CreateTableQuery extends SQLQuery
{
    private Set<ColumnDescription> columnDescriptions;
    private ColumnDescription primaryKeyColumn;
    private String tableName;
    
    /**
     * 
     */
    CreateTableQuery(String queryString)
    {
	super(queryString);
	this.columnDescriptions = new HashSet<ColumnDescription>();
    }

    /**
     * 
     * @return descriptions of all the table columns
     */
    public ColumnDescription[] getColumnDescriptions()
    {
	return this.columnDescriptions.toArray(new ColumnDescription[this.columnDescriptions.size()]);
    }
    void addColumnDescription(ColumnDescription column)
    {
	this.columnDescriptions.add(column);
    }

    /**
     * 
     * @return the description of the column that is the primary key of the table
     */
    public ColumnDescription getPrimaryKeyColumn()
    {
	return this.primaryKeyColumn;
    }    
    void setPrimaryKeyColumn(String columnName)
    {
	ColumnDescription fake = new ColumnDescription();
	fake.setColumnName(columnName);
	for(ColumnDescription real : this.columnDescriptions)
	{
	    if(real.equals(fake))
	    {
		this.primaryKeyColumn = real;
		return;
	    }
	}
	throw new IllegalArgumentException("no column by that name is defined on this table");
    }
    
    /**
     * name of the table to be created
     * @return
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