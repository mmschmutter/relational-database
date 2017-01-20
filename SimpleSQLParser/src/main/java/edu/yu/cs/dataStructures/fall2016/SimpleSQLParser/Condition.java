package edu.yu.cs.dataStructures.fall2016.SimpleSQLParser;

/**
 * represents a condition specified in a "WHERE" clause in a sql query
 * @author diament@yu.edu
 *
 */
public class Condition
{
    /**
     * the different operators that can be used in a condition
     * @author diament@yu.edu
     *
     */
    public enum Operator
    {
	EQUALS("="),
	NOT_EQUALS("<>"),
	LESS_THAN("<"),
	lESS_THAN_OR_EQUALS("<="),
	GREATER_THAN(">"),
	GREATER_THAN_OR_EQUALS(">="),
	AND("AND"),
	OR("OR");
	
	private String symbol;
	private Operator(String symbol)
	{
	    this.symbol = symbol;
	}
	
	public String toString()
	{
	    return this.symbol;
	}
    };
    
    private Object leftOperand;
    private Operator operator;
    private Object rightOperand;
    
    public String toString()
    {
	return this.leftOperand.toString() + " " + this.operator.name() + " " + this.rightOperand.toString();
    }
    
    /**
     * 
     * @param leftOperand
     * @param operator
     * @param rightOperand
     */
    Condition(Object leftOperand, Operator operator, Object rightOperand)
    {
	this.leftOperand = this.testOperandValidity(leftOperand);
	this.operator = operator;
	this.rightOperand = this.testOperandValidity(rightOperand);
    }
    /**
     * 
     * @param operand
     * @return
     * @throws IllegalArgumentException
     */
    private Object testOperandValidity(Object operand) throws IllegalArgumentException
    {
	if(operand instanceof Condition || operand instanceof String || operand instanceof ColumnID)
	{
	    return operand;
	}
	throw new IllegalArgumentException("Operands must be a String, Condition, or ColumnID");
    }
    /**
     * @return the left operand of this condition
     */
    public Object getLeftOperand()
    {
	return leftOperand;
    }
    public void setLeftOperand(Object leftOperand)
    {
	this.leftOperand = this.testOperandValidity(leftOperand);
    }
    /**
     * @return the operator
     */
    public Operator getOperator()
    {
	return operator;
    }
    public void setOperator(Operator operator)
    {
	this.operator = operator;
    }
    /**
     * @return the right operand of this condition
     */
    public Object getRightOperand()
    {
	return rightOperand;
    }
    public void setRightOperand(Object rightOperand)
    {
	this.rightOperand = this.testOperandValidity(rightOperand);
    }
}