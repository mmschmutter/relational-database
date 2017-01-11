import java.io.IOException;
import java.util.Scanner;
import net.sf.jsqlparser.JSQLParserException;

public class Main {
	private static Database database;
	private static Scanner scanner = new Scanner(System.in);

	public static void main(String[] args) throws JSQLParserException, IOException {
		database = new Database();
		database.update();
		readInput();
	}

	private static void readInput() throws JSQLParserException, IOException {
		System.out.println("Enter SQL Query:");
		String input = scanner.nextLine();
		System.out.println(database.execute(input));
		readInput();
	}
}