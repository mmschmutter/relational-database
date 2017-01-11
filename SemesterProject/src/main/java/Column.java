import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;

interface Column extends Serializable {
	abstract String getColumnName();

	abstract String getColumnType();

	abstract String toString();

	abstract void index();

	abstract void put(Object[] entry);

	abstract int getColumnNum();

	abstract boolean isIndexed();

	abstract boolean isUnique();

	abstract boolean isNotNull();

	abstract String getDefaultValue();

	abstract void deleteRow(Object[] row);

	abstract LinkedList<Object[]> getIndexed(Object key);

	abstract ArrayList<BTree.Entry> getOrdered();
}
