package edu.yu.cs.dataStructures.fall2016.SimpleSQLParser;

/**
 * identifies a a column using the column name and table name
 * @author diament@yu.edu
 *
 */
public class ColumnID
{
    private String columnName;
    private String tableName;
    
    public ColumnID(String columnName, String tableName)
    {
    	this.columnName = columnName;
    	this.tableName = tableName;
    }   
    public String getColumnName()
    {
    	return this.columnName;
    }
    public String getTableName()
    {
    	return this.tableName;
    }
    /**
     * @return the table name, followed by a period, followed by the column name
     */
    public String toString()
    {
    	if(this.tableName != null)
    	{
    		return this.tableName + "." + this.columnName;
    	}
    	return this.columnName;
    }
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((columnName == null) ? 0 : columnName.hashCode());
		result = prime * result + ((tableName == null) ? 0 : tableName.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ColumnID other = (ColumnID) obj;
		if (columnName == null) {
			if (other.columnName != null)
				return false;
		} else if (!columnName.equals(other.columnName))
			return false;
		if (tableName == null) {
			if (other.tableName != null)
				return false;
		} else if (!tableName.equals(other.tableName))
			return false;
		return true;
	}
    
}