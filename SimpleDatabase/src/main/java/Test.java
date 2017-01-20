public class Test {
	public static void main(String[] args) {
		Database database = new Database();
		try {
			System.out.println(
					"CREATE TABLE table1 (Int int, Doub decimal(1,1), Str varchar(255), Bool boolean, Int2 int, PRIMARY KEY (Int))");
			System.out.println(database.execute(
					"CREATE TABLE table1 (Int int, Doub decimal(1,1), Str varchar(255), Bool boolean, Int2 int, PRIMARY KEY (Int))"));
			System.out.println(
					"CREATE TABLE table2 (Num int, Dec decimal(1,1), Word varchar(255), Truth boolean, Num2 int, PRIMARY KEY (Num))");
			System.out.println(database.execute(
					"CREATE TABLE table2 (Num int, Dec decimal(1,1), Word varchar(255), Truth boolean, Num2 int, PRIMARY KEY (Num))"));
			System.out.println("INSERT INTO table1 (Int, Str, Bool, Doub, Int2) VALUES (5, 'test', 'true', 2.3, 6)");
			System.out.println(database
					.execute("INSERT INTO table1 (Int, Str, Bool, Doub, Int2) VALUES (5, 'start', 'true', 2.3, 6)"));
			System.out.println("INSERT INTO table1 (Int, Str, Bool, Doub, Int2) VALUES (4, 'start', 'false', 2.7, 5)");
			System.out.println(database
					.execute("INSERT INTO table1 (Int, Str, Bool, Doub, Int2) VALUES (4, 'start', 'false', 2.7, 5)"));
			System.out.println("INSERT INTO table1 (Int, Str, Bool, Doub, Int2) VALUES (6, 'start', 'true', 2.3, 6)");
			System.out.println(database
					.execute("INSERT INTO table1 (Int, Str, Bool, Doub, Int2) VALUES (6, 'start', 'true', 2.3, 6)"));
			System.out.println("INSERT INTO table2 (Num, Word, Truth, Dec, Num2) VALUES (7, 'start', 'true', 2.3, 5)");
			System.out.println(database
					.execute("INSERT INTO table2 (Num, Word, Truth, Dec, Num2) VALUES (7, 'start', 'true', 2.3, 5)"));
			System.out.println("INSERT INTO table2 (Num, Word, Truth, Dec, Num2) VALUES (5, 'start', 'true', 1.3, 9)");
			System.out.println(database
					.execute("INSERT INTO table2 (Num, Word, Truth, Dec, Num2) VALUES (5, 'start', 'true', 1.3, 9)"));
			System.out.println("UPDATE table1 SET Str = 'changed'");
			System.out.println(database.execute("UPDATE table1 SET Str = 'changed'"));
			System.out.println("UPDATE table1 SET Str = 'success' WHERE Int = Int2");
			System.out.println(database.execute("UPDATE table1 SET Str = 'success' WHERE Int = Int2"));
			System.out.println("UPDATE table1 SET Str = 'pass' WHERE Int < Int2 AND Bool = 'true'");
			System.out.println(database.execute("UPDATE table1 SET Str = 'pass' WHERE Int < Int2 AND Bool = 'true'"));
			System.out.println(
					"UPDATE table1 SET Str = 'success' WHERE Int < Int2 AND (Str = 'changed' OR Bool = 'true')");
			System.out.println(database.execute(
					"UPDATE table1 SET Str = 'success' WHERE Int < Int2 AND (Str = 'changed' OR Bool = 'true')"));
			System.out.println("UPDATE table1 SET Str = 'nothing' WHERE Int = Int2 AND Str = 'fake'");
			System.out.println(database.execute("UPDATE table1 SET Str = 'nothing' WHERE Int = Int2 AND Str = 'fake'"));
			System.out.println("CREATE INDEX Doub_Index on table1 (Doub)");
			System.out.println(database.execute("CREATE INDEX Doub_Index on table1 (Doub)"));
			System.out.println("SELECT table1.Int FROM table1 WHERE Int > 4");
			System.out.println(database.execute("SELECT table1.Int FROM table1 WHERE Int > 4"));
			System.out.println("SELECT table1.Int, table2.Num FROM table1, table2");
			System.out.println(database.execute("SELECT table1.Int, table2.Num FROM table1, table2"));
			System.out.println(
					"SELECT table1.Int, table2.Num FROM table1, table2 WHERE table1.Int > 4 AND table2.Num > 3");
			System.out.println(database.execute(
					"SELECT table1.Int, table2.Num FROM table1, table2 WHERE table1.Int > 4 AND table2.Num > 3"));
			System.out.println("SELECT * FROM table1 ORDER BY Int ASC");
			System.out.println(database.execute("SELECT * FROM table1 ORDER BY Int ASC"));
			System.out.println("SELECT * FROM table1 ORDER BY Doub DESC, Int ASC)");
			System.out.println(database.execute("SELECT * FROM table1 ORDER BY Doub DESC, Int ASC"));
			System.out.println("SELECT AVG(Int) FROM table1");
			System.out.println(database.execute("SELECT AVG(Int) FROM table1"));
			System.out.println("SELECT COUNT(Int) FROM table1");
			System.out.println(database.execute("SELECT COUNT(Int) FROM table1"));
			System.out.println("SELECT MAX(Int) FROM table1");
			System.out.println(database.execute("SELECT MAX(Int) FROM table1"));
			System.out.println("SELECT MIN(Int) FROM table1");
			System.out.println(database.execute("SELECT MIN(Int) FROM table1"));
			System.out.println("SELECT SUM(Int) FROM table1");
			System.out.println(database.execute("SELECT SUM(Int) FROM table1"));
			System.out.println("DELETE FROM table1 WHERE Doub > 2.3");
			System.out.println(database.execute("DELETE FROM table1 WHERE Doub > 2.3"));
			System.out.println("DELETE FROM table1");
			System.out.println(database.execute("DELETE FROM table1"));
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
		}
	}
}
