package edu.yu.cs.dataStructures.fall2016.SimpleSQLParser;
/**
 * represents the ID of a column and the value to set it to in an INSERT or UPDATE query
 * @author diament@yu.edu
 *
 */
public class ColumnValuePair
{
    private ColumnID col;
    private String value;

    ColumnValuePair(ColumnID col, String value)
    {
	this.col = col;
	this.value = value;
    }

    public ColumnID getColumnID()
    {
	return this.col;
    }

    public String getValue()
    {
	return this.value;
    }
}