import java.io.*;

class ReaderWriter implements Serializable {
	private int changes;
	Database database;

	ReaderWriter(Database database) {
		this.database = database;
		changes = 0;
	}

	protected void log(String query) throws IOException {
		Long time = System.currentTimeMillis();
		FileWriter writer = new FileWriter("./log.txt", true);
		writer.write(time + ":" + query + "\n");
		writer.close();
	}

	protected void backup() {
		Long time = System.currentTimeMillis();
		try {
			FileOutputStream fileOut = new FileOutputStream("./DB_" + time + ".db");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(this.database);
			out.close();
			fileOut.close();
			System.out.println("Backup saved in ./DB_" + time + ".db");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected static Database load() {
		Database database = null;
		String newest = getNewestBackup();
		if (newest != null) {
			try {
				FileInputStream fileIn = new FileInputStream("./" + newest);
				ObjectInputStream in = new ObjectInputStream(fileIn);
				database = (Database) in.readObject();
				in.close();
				fileIn.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException c) {
				c.printStackTrace();
			}
			System.out.println("Loading backup from " + newest + "...");
		}
		return database;
	}

	protected void rowChange(int number) {
		this.changes += number;
		if (this.changes >= 5) {
			backup();
			this.changes = 0;
		}
	}

	protected static String getNewestBackup() {
		String newest = null;
		File dir = new File("./");
		File[] dbFiles = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String filename) {
				return filename.endsWith(".db");
			}
		});
		if (dbFiles.length != 0) {
			newest = dbFiles[0].getName();
			for (File dbFile : dbFiles) {
				Long newestTime = Long.parseLong(newest.substring(3, newest.length() - 3));
				Long thisTime = Long.parseLong(dbFile.getName().substring(3, dbFile.getName().length() - 3));
				if (thisTime > newestTime) {
					newest = dbFile.getName();
				}
			}
		}
		return newest;
	}
}
