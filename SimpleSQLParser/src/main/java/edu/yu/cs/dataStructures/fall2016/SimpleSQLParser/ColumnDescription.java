package edu.yu.cs.dataStructures.fall2016.SimpleSQLParser;
/**
 * describes a column specified in a CREATE TABLE query
 * @author diament@yu.edu
 *
 */
public class ColumnDescription implements Comparable<ColumnDescription>
{
    public enum DataType{INT,VARCHAR,DECIMAL,BOOLEAN};
    
    private DataType columnType;
    private String columnName;
    private boolean unique = false;
    private boolean notNull = false;
    private boolean hasDefault = false;
    private String defaultValue;
    private int wholeNumberLength;
    private int fractionalLength;
    private int varcharLength;
    
    ColumnDescription()
    {	
    }
    
    public String toString()
    {
	return this.columnType.name() + " " + this.columnName;
    }  

    public ColumnDescription.DataType getColumnType()
    {
	return this.columnType;
    }
    void setColumnType(DataType columnType)
    {
	this.columnType = columnType;
    }
    
    public String getColumnName()
    {
	return this.columnName;
    }
    void setColumnName(String name)
    {
	this.columnName = name;
    }
    
    public boolean isUnique()
    {
	return this.unique;
    }
    void setUnique(boolean unique)
    {
	this.unique = unique;
    }
    
    public boolean isNotNull()
    {
	return this.notNull;
    }
    void setNotNull(boolean notNull)
    {
	this.notNull = notNull;
    }
    
    public boolean getHasDefault()
    {
	return this.hasDefault;
    }
    void setHasDefault(boolean hasDefault)
    {
	this.hasDefault = hasDefault;
    }
    
    /**
     * 
     * @return default value represented as a string, if there is one
     */
    public String getDefaultValue()
    {
	return this.defaultValue;
    }
    void setDefaultValue(String defaultValue)
    {
	this.defaultValue = defaultValue;
    }
    
    /**
     * 
     * @return for a decimal column, the number of digits in the whole number, i.e. before the decimal point
     */
    public int getWholeNumberLength()
    {
	return this.wholeNumberLength;
    }
    void setWholeNumberLength(int length)
    {
	this.wholeNumberLength = length;
    }
    
    /**
     * 
     * @return for a decimal column, the number of digits in the fraction, i.e. after the decimal point
     */
    public int getFractionLength()
    {
	return this.fractionalLength;
    }
    void setFractionalLength(int length)
    {
	this.fractionalLength = length;
    }
        
    /**
     * for a varchar column, how many characters long is it
     * @return
     */
    public int getVarCharLength()
    {
	return this.varcharLength;
    }
    void setVarcharLength(int varcharLength)
    {
	this.varcharLength = varcharLength;
    }

    @Override
    public int compareTo(ColumnDescription other)
    {
	if(this.columnName.equalsIgnoreCase(other.columnName))
	{
	    return 0;
	}
	return this.columnName.compareTo(other.columnName);
    }
    @Override
    public int hashCode()
    {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((columnName == null) ? 0 : columnName.hashCode());
	return result;
    }

    @Override
    public boolean equals(Object obj)
    {
	if (this == obj)
	{
	    return true;
	}
	if (obj == null)
	{
	    return false;
	}
	if (getClass() != obj.getClass())
	{
	    return false;
	}
	ColumnDescription other = (ColumnDescription) obj;
	if (columnName == null && other.columnName != null)
	{
	    return false;
	}
	return columnName.equals(other.columnName);
    }    
}