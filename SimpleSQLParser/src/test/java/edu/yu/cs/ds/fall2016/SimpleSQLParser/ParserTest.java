package edu.yu.cs.ds.fall2016.SimpleSQLParser;

import org.junit.Test;

import edu.yu.cs.dataStructures.fall2016.SimpleSQLParser.CreateIndexQuery;
import edu.yu.cs.dataStructures.fall2016.SimpleSQLParser.CreateTableQuery;
import edu.yu.cs.dataStructures.fall2016.SimpleSQLParser.DeleteQuery;
import edu.yu.cs.dataStructures.fall2016.SimpleSQLParser.InsertQuery;
import edu.yu.cs.dataStructures.fall2016.SimpleSQLParser.SQLParser;
import edu.yu.cs.dataStructures.fall2016.SimpleSQLParser.SelectQuery;
import edu.yu.cs.dataStructures.fall2016.SimpleSQLParser.SelectQuery.OrderBy;
import edu.yu.cs.dataStructures.fall2016.SimpleSQLParser.UpdateQuery;
import net.sf.jsqlparser.JSQLParserException;

/**
 * Examples of parsing queries
 */
public class ParserTest
{
    @Test
    public void testCreateTable() throws JSQLParserException
    {
	SQLParser parser = new SQLParser();
	String query = "CREATE TABLE YCStudent"
		+ "("
		+ " BannerID int,"
		+ " SSNum int UNIQUE,"
		+ " FirstName varchar(255),"
		+ " LastName varchar(255) NOT NULL,"
		+ " GPA decimal(1,2) DEFAULT 0.00,"
		+ " CurrentStudent boolean DEFAULT true,"
		+ " PRIMARY KEY (BannerID)"
		+ ");";
	CreateTableQuery result = (CreateTableQuery)parser.parse(query);
    }

    @Test
    public void testCreateIndex() throws JSQLParserException
    {
	SQLParser parser = new SQLParser();
	String query = "CREATE INDEX SSNum_Index on YCStudent (SSNum);";
	CreateIndexQuery result = (CreateIndexQuery)parser.parse(query);
    }
    
    @Test
    public void testSelect() throws JSQLParserException
    {
	SQLParser parser = new SQLParser();
	SelectQuery result = (SelectQuery)parser.parse("SELECT first, last, id FROM students,teachers,schools;");
	result = (SelectQuery)parser.parse("SELECT column1, column2 FROM table1 WHERE column3='some value' AND (column4='some value2' OR column4='some other value');");	
	result = (SelectQuery)parser.parse("SELECT * FROM YCStudent, RIESTStudent WHERE YCStudent.BannerID = RIETS.BannerID;");
	result = (SelectQuery)parser.parse("select * from students;");
	result = (SelectQuery)parser.parse("select distinct first, last, id from students;");
	result = (SelectQuery)parser.parse("select first, last, id from students where id=1234;");
	result = (SelectQuery)parser.parse("select first, last, id from students where id=1234 AND first='moshe';");
	result = (SelectQuery)parser.parse("SELECT * FROM YCStudent ORDER BY GPA ASC, Credits DESC;");
	OrderBy[] orderbys =  result.getOrderBys();
	result = (SelectQuery)parser.parse("SELECT AVG(GPA) FROM STUDENTS;");	
	result = (SelectQuery)parser.parse("SELECT COUNT(DISTINCT GPA) FROM STUDENTS;");	
    }

    @Test
    public void testInsert() throws JSQLParserException
    {
	SQLParser parser = new SQLParser();
	InsertQuery result = (InsertQuery)parser.parse("INSERT INTO YCStudent (FirstName, LastName, GPA, Class, BannerID) VALUES ('Ploni','Almoni',4.0, 'Senior',800012345);");
    }    

    @Test
    public void testUpdate() throws JSQLParserException
    {
	SQLParser parser = new SQLParser();
	UpdateQuery result = (UpdateQuery)parser.parse("UPDATE YCStudent SET GPA=3.0,Class='Super Senior' WHERE BannerID=800012345;");
	result = (UpdateQuery)parser.parse("UPDATE YCStudent SET GPA=3.0,Class='Super Senior';");
    }    

    @Test
    public void testDelete() throws JSQLParserException
    {
	SQLParser parser = new SQLParser();
	DeleteQuery result = (DeleteQuery)parser.parse("DELETE FROM YCStudent WHERE Class='Super Senior' AND GPA < 3.0;");
	//NOTE: for some strange reason JSQLParser can't handle the "*" in "DELETE * FROM" - wierd!!
	result = (DeleteQuery)parser.parse("DELETE FROM YCStudent;");
    }    
}