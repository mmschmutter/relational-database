import java.util.Scanner;

public class Main {
	private static Database database;
	private static Scanner scanner = new Scanner(System.in);

	public static void main(String[] args) throws Exception {
		database = new Database();
		database.update();
		readInput();
	}

	private static void readInput() {
		System.out.println("Enter SQL Query:");
		String input = scanner.nextLine();
		try {
			System.out.println(database.execute(input));
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
		}
		readInput();
	}
}