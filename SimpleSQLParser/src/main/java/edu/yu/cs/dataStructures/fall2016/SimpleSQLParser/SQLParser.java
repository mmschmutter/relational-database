package edu.yu.cs.dataStructures.fall2016.SimpleSQLParser;

import java.util.List;

import edu.yu.cs.dataStructures.fall2016.SimpleSQLParser.SelectQuery.FunctionInstance;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.expression.operators.relational.OldOracleJoinBinaryExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.Index;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.update.Update;

public class SQLParser
{
    /**
     * 
     * @param sqlQuery the query to parse
     * @return an object representing the query
     * @throws JSQLParserException
     */
    public SQLQuery parse(String sqlQuery) throws JSQLParserException
    {
	if(sqlQuery == null || sqlQuery.isEmpty())
	{
	    throw new IllegalArgumentException("sql query can not be empty");
	}
	String queryType = sqlQuery.trim().substring(0, 7).toLowerCase();
	
	if(queryType.startsWith("create"))
	{
	    return parseCreate(sqlQuery);
	}
	else if(queryType.startsWith("select"))
	{
	    return parseSelect(sqlQuery);
	}
	else if(queryType.startsWith("insert"))
	{
	    return parseInsert(sqlQuery);	    
	}
	else if(queryType.startsWith("update"))
	{
	    return parseUpdate(sqlQuery);	    
	}
	else if(queryType.startsWith("delete"))
	{
	    return parseDelete(sqlQuery);	    
	}
	return null;
    }
   
    /**
     * 
     * @param sqlQuery
     * @return
     * @throws JSQLParserException
     */
    private SelectQuery parseSelect(String sqlQuery) throws JSQLParserException
    {
	if(sqlQuery == null || sqlQuery.isEmpty())
	{
	    throw new IllegalArgumentException("Select query can not be empty");
	}
	sqlQuery = sqlQuery.trim();
	if(!sqlQuery.toLowerCase().startsWith("select"))
	{
	    throw new IllegalArgumentException("Select query must start with \"select\" (case insensitive)");	    
	}
	//parse the sql query
	PlainSelect select=(PlainSelect)((Select)CCJSqlParserUtil.parse(sqlQuery)).getSelectBody();
	
	//build up our simple sql object
	SelectQuery queryObject = new SelectQuery(sqlQuery);
	
	//add the columns that are being selected
	this.addColumnsToSelect(select, queryObject);

	//does the query include "distinct"?
	if(select.getDistinct() != null)
	{
	    queryObject.setDistinct(true);
	}
	
	//add the "from" table(s)
	this.addFrom(select, queryObject);
	
	//add with "where" condition if it exists
	Expression where = select.getWhere();
	queryObject.setWhereCondition((Condition)this.getCondition(where));
	
	//add "order by" if it exists
	this.addOrderBys(select, queryObject);
	
	return queryObject;
    }

    /**
     * 
     * @param select
     * @param queryObject
     */
    private void addOrderBys(PlainSelect select, SelectQuery queryObject)
    {
	List<OrderByElement> orderbys = select.getOrderByElements();
	if(orderbys == null)
	{
	    return;
	}
	for(OrderByElement orderby : orderbys)
	{
	    Column col = (Column)orderby.getExpression();
	    queryObject.addOrderBy(new SelectQuery.OrderBy(this.colToColumnID(col), orderby.isAsc()));
	}
    }
    
    /**
     * 
     * @param select
     * @param queryObject
     */
    private void addFrom(PlainSelect select, SelectQuery queryObject)
    {
	Table from =  (Table)select.getFromItem();	
	queryObject.addFromTableName(from.getName());
	List<Join> joins = select.getJoins();
	if(joins == null)
	{
	    return;
	}
	for(Join join : joins)
	{
	    queryObject.addFromTableName(((Table)join.getRightItem()).getName());	    	    
	}
    }

    /**
     * 
     * @param expression
     * @return
     */
    private Object getCondition(Expression expression)
    {
	if(expression == null)
	{
	    return null;
	}
	else if(expression instanceof Parenthesis)
	{
	    return this.getCondition(((Parenthesis)expression).getExpression());
	}
	else if(expression instanceof Column)
	{
	    return this.colToColumnID((Column)expression);
	}
	else if(expression instanceof OldOracleJoinBinaryExpression)
	{
	     return createConditionFromBinaryExpression((OldOracleJoinBinaryExpression)expression);
	}
	else if(expression instanceof AndExpression)
	{
	    Object left = getCondition(((AndExpression)expression).getLeftExpression());
	    Object right = getCondition(((AndExpression)expression).getRightExpression());
	    return new Condition(left, Condition.Operator.AND, right);
	}
	else if(expression instanceof OrExpression)
	{
	    Object left = getCondition(((OrExpression)expression).getLeftExpression());
	    Object right = getCondition(((OrExpression)expression).getRightExpression());
	    return new Condition(left, Condition.Operator.OR, right);	    
	}
	else
	{
	    return expression.toString();
	}
    }
    
    /**
     * deals with =, <>, >, >=, <, <=
     * @param expression
     * @param queryObject
     */
    private Condition createConditionFromBinaryExpression(OldOracleJoinBinaryExpression expression)
    {
	Condition.Operator op = null;
	
	if(expression instanceof EqualsTo)
	{
	    op = Condition.Operator.EQUALS;
	}
	else if(expression instanceof NotEqualsTo)
	{
	    op = Condition.Operator.NOT_EQUALS;
	}
	else if(expression instanceof GreaterThan)
	{
	    op = Condition.Operator.GREATER_THAN;	    
	}
	else if(expression instanceof GreaterThanEquals)
	{
	    op = Condition.Operator.GREATER_THAN_OR_EQUALS;
	}
	else if(expression instanceof MinorThan)
	{
	    op = Condition.Operator.LESS_THAN;	    
	}
	else if(expression instanceof MinorThanEquals)
	{
	    op = Condition.Operator.lESS_THAN_OR_EQUALS;
	}
	Object left = getCondition(expression.getLeftExpression());
	Object right = getCondition(expression.getRightExpression());
	return new Condition(left, op, right);
    }
    
    /**
     * 
     * @param select
     * @param queryObject
     */
    private void addColumnsToSelect(PlainSelect select, SelectQuery queryObject)
    {
	List<SelectItem> selectItems = select.getSelectItems();
	
	//deal with select *
	if(selectItems.size() == 1 &&  selectItems.get(0) instanceof AllColumns)
	{
	    queryObject.addSelectedColumnName(new ColumnID("*", null));
	    return;
	}

	//all other cases
	for(int i = 0; i < selectItems.size(); i++)
	{
	    Expression expression = ((SelectExpressionItem)selectItems.get(i)).getExpression();
	    if(expression instanceof Column)
	    {
		queryObject.addSelectedColumnName(colToColumnID((Column)expression));
	    }
	    else if(expression instanceof Function)
	    {	
		Function func = (Function)expression;
		//create the function instance
		FunctionInstance funcInst = new FunctionInstance();
		funcInst.function = SelectQuery.FunctionName.valueOf(func.getName());		
		//set if the function is distinct, e.g. "COUNT(DISTINCT column_name)"
		funcInst.isDistinct = func.isDistinct();
		//set the column that the function is acting on
		Column funcTarget = (Column)func.getParameters().getExpressions().get(0);
		ColumnID colid = colToColumnID(funcTarget);
		queryObject.addFunction(colid, funcInst);
		//add the column to the list of columns relevant to this query
		queryObject.addSelectedColumnName(colid);		
	    }
	}		
    }
    
    /**
     * 
     * @param col
     * @return
     */
    private ColumnID colToColumnID(Column col)
    {
	String table = null;
	if(col.getTable() != null)
	{
	    table = col.getTable().getName();
	}
	return new ColumnID(col.getColumnName(),table);	
    }        
    /**
     * 
     * @param sqlQuery
     * @return
     * @throws JSQLParserException
     */
    private InsertQuery parseInsert(String sqlQuery) throws JSQLParserException
    {
	if(sqlQuery == null || sqlQuery.isEmpty())
	{
	    throw new IllegalArgumentException("Insert query can not be empty");
	}
	sqlQuery = sqlQuery.trim();
	if(!sqlQuery.toLowerCase().startsWith("insert into"))
	{
	    throw new IllegalArgumentException("Insert query must start with \"Insert Into\" (case insensitive)");	    
	}
	//parse the sql query
	Insert insert = (Insert)CCJSqlParserUtil.parse(sqlQuery);
	
	//build up our simple sql object
	InsertQuery queryObject = new InsertQuery(sqlQuery);
	queryObject.setTableName(insert.getTable().getName());
	
	//add columns and values
	this.addColumnValuePairsToInsert(insert, queryObject);
	return queryObject;
    }
    
    private void addColumnValuePairsToInsert(Insert insert, InsertQuery queryObject)
    {
	List<Column> columns = insert.getColumns();
	List<Expression> values = ((ExpressionList)insert.getItemsList()).getExpressions();	
	addColumnValuePairs(queryObject,columns,values,queryObject.getTableName());
    }

    private void addColumnValuePairs(SQLQuery queryObject, List<Column> columns, List<Expression> values, String tablename)
    {
	if(columns.size() != values.size())
	{
	    throw new IllegalArgumentException("must have same number of columns and values");
	}
	for(int i = 0; i < columns.size(); i++)
	{
	    ColumnID cid = new ColumnID(columns.get(i).getColumnName(), tablename);
	    String value = values.get(i).toString();
	    queryObject.addColumnValuePair(cid, value);
	}
    }
    
    private UpdateQuery parseUpdate(String sqlQuery) throws JSQLParserException
    {
	if(sqlQuery == null || sqlQuery.isEmpty())
	{
	    throw new IllegalArgumentException("update query can not be empty");
	}
	sqlQuery = sqlQuery.trim();
	if(!sqlQuery.toLowerCase().startsWith("update"))
	{
	    throw new IllegalArgumentException("update query must start with \"Update\" (case insensitive)");	    
	}
	//parse the sql query
	Update update = (Update)CCJSqlParserUtil.parse(sqlQuery);
	
	//build up our simple sql object
	UpdateQuery queryObject = new UpdateQuery(sqlQuery);
	queryObject.setTableName(update.getTables().get(0).getName()); //assuming only one table
	
	//add columns and values
	this.addColumnValuePairsToUpdate(update, queryObject);
	
	//add where conditions
	Expression where = update.getWhere();
	queryObject.setWhereCondition((Condition)this.getCondition(where));	
	
	return queryObject;
    }

    private void addColumnValuePairsToUpdate(Update update, UpdateQuery queryObject)
    {
	List<Column> columns = update.getColumns();
	List<Expression> values = update.getExpressions();
	addColumnValuePairs(queryObject,columns,values,queryObject.getTableName());
    }    
    
    private DeleteQuery parseDelete(String sqlQuery) throws JSQLParserException
    {
	if(sqlQuery == null || sqlQuery.isEmpty())
	{
	    throw new IllegalArgumentException("delete query can not be empty");
	}
	sqlQuery = sqlQuery.trim();
	if(!sqlQuery.toLowerCase().startsWith("delete"))
	{
	    throw new IllegalArgumentException("delete query must start with \"Delete\" (case insensitive)");	    
	}
	//parse the sql query
	Delete delete = (Delete)CCJSqlParserUtil.parse(sqlQuery);
	
	//build up our simple sql object
	DeleteQuery queryObject = new DeleteQuery(sqlQuery);
	queryObject.setTableName(delete.getTable().getName());
	
	//add where conditions
	Expression where = delete.getWhere();
	queryObject.setWhereCondition((Condition)this.getCondition(where));	
	
	return queryObject;
    }
    
    /**
     * 
     * @param sqlQuery
     * @return
     * @throws JSQLParserException
     */
    private SQLQuery parseCreate(String sqlQuery) throws JSQLParserException
    {
	if(sqlQuery == null || sqlQuery.isEmpty())
	{
	    throw new IllegalArgumentException("create query can not be empty");
	}
	sqlQuery = sqlQuery.trim();
	String lowerCaseQuery = sqlQuery.toLowerCase(); 
	if(!lowerCaseQuery.startsWith("create"))
	{
	    throw new IllegalArgumentException("create query must start with \"create\" (case insensitive)");	    
	}
	if(lowerCaseQuery.startsWith("create table"))
	{
	    return parseCreateTable(sqlQuery);
	}
	else if(lowerCaseQuery.startsWith("create index"))
	{
	    return parseCreateIndex(sqlQuery);
	}
	return null;
    }

    private CreateIndexQuery parseCreateIndex(String sqlQuery) throws JSQLParserException
    {
	//parse the sql query
	CreateIndex createIndex = (CreateIndex)CCJSqlParserUtil.parse(sqlQuery);
	
	//build up our simple sql object
	CreateIndexQuery queryObject = new CreateIndexQuery(sqlQuery);
	queryObject.setTableName(createIndex.getTable().getName());
	
	Index index = createIndex.getIndex();
	queryObject.setIndexName(index.getName());
	queryObject.setColumnName(index.getColumnsNames().get(0));
	
	return queryObject;	
    }

    private CreateTableQuery parseCreateTable(String sqlQuery) throws JSQLParserException
    {
	//parse the sql query
	CreateTable createTable = (CreateTable)CCJSqlParserUtil.parse(sqlQuery);
	
	//build up our simple sql object
	CreateTableQuery queryObject = new CreateTableQuery(sqlQuery);
	queryObject.setTableName(createTable.getTable().getName());
	
	//add the column descriptions
	this.addColumnDescriptions(createTable,queryObject);
	
	//set the primary key
	setPrimaryKey(createTable,queryObject);
	return queryObject;	
    }
    
    private void setPrimaryKey(CreateTable createTable,CreateTableQuery queryObject)
    {
	List<Index> indeces = createTable.getIndexes();
	if(indeces == null || indeces.size() == 0)
	{
	    throw new IllegalArgumentException("No primary key defined on table " + queryObject.getTableName());
	}
	for(Index index : indeces)
	{
	    if(index.getType().equalsIgnoreCase("PRIMARY KEY"))
	    {
		queryObject.setPrimaryKeyColumn(index.getColumnsNames().get(0));
		return;
	    }
	}
	throw new IllegalArgumentException("No primary key defined on table " + queryObject.getTableName());
    }
    
    private void addColumnDescriptions(CreateTable createTable,CreateTableQuery queryObject)
    {
	List<ColumnDefinition> coldefs = createTable.getColumnDefinitions();
	if(coldefs == null)
	{
	    return;
	}
	for(ColumnDefinition def : coldefs)
	{
	    ColumnDescription desc = new ColumnDescription();
	    ColDataType type = def.getColDataType();
	    desc.setColumnName(def.getColumnName());
	    setDataType(desc,type);
	    List<String> specStrings = def.getColumnSpecStrings();
	    if(specStrings != null)
	    {
		this.setUnique(desc,specStrings);
		this.setNotNull(desc,specStrings);	
		this.setDefaultValue(desc,specStrings);
	    }
	    List<String> args = type.getArgumentsStringList();
	    if(args != null)
	    {
		setArgs(desc,args);
	    }
	    queryObject.addColumnDescription(desc);
	}
    }
    
    private void setArgs(ColumnDescription desc,List<String> args)
    {
	if(desc.getColumnType() == ColumnDescription.DataType.VARCHAR)
	{
	    desc.setVarcharLength(Integer.parseInt(args.get(0)));
	}
	else if(desc.getColumnType() == ColumnDescription.DataType.DECIMAL)
	{
	    desc.setWholeNumberLength(Integer.parseInt(args.get(0)));
	    desc.setFractionalLength(Integer.parseInt(args.get(1)));
	}
    }
    
    private void setDefaultValue(ColumnDescription desc,List<String> specStrings)
    {
	int index = -1;
	if(specStrings.contains("default"))
	{
	    index = specStrings.indexOf("default");
	}
	else if(specStrings.contains("DEFAULT"))
	{
	    index = specStrings.indexOf("DEFAULT");
	}
	if(index != -1)
	{
	    desc.setHasDefault(true);
	    desc.setDefaultValue(specStrings.get(index+1));
	}
    }
    
    private void setUnique(ColumnDescription desc,List<String> specStrings)
    {
	if(specStrings.contains("unique") || specStrings.contains("UNIQUE"))
	{
	    desc.setUnique(true);
	}
    }
    private void setNotNull(ColumnDescription desc,List<String> specStrings)
    {
	if(specStrings.contains("not") && specStrings.contains("null"))
	{
	    desc.setNotNull(true);
	}
	else if(specStrings.contains("NOT") && specStrings.contains("NULL"))
	{
	    desc.setNotNull(true);
	}
    }
    
    private void setDataType(ColumnDescription desc, ColDataType type)
    {
	if(type.getDataType().equalsIgnoreCase("int"))
	{
	    desc.setColumnType(ColumnDescription.DataType.INT);
	}
	else if(type.getDataType().equalsIgnoreCase("boolean"))
	{
	    desc.setColumnType(ColumnDescription.DataType.BOOLEAN);
	}
	else if(type.getDataType().equalsIgnoreCase("decimal"))
	{
	    desc.setColumnType(ColumnDescription.DataType.DECIMAL);
	}
	else if(type.getDataType().equalsIgnoreCase("varchar"))
	{
	    desc.setColumnType(ColumnDescription.DataType.VARCHAR);
	}
    }    
}