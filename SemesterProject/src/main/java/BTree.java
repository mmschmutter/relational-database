import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

/******************************************************************************
 *  Limitations: Assumes MAX is even and MAX >= 4
 * NOTE: Edited by Judah Diament to increase readability (renamed variables, added comments, formating, etc.)
 * original java file: http://algs4.cs.princeton.edu/code/edu/princeton/cs/algs4/BTree.java
 *
 ******************************************************************************/

/**
 * The {@code BTree} class represents an ordered symbol table of generic
 * key-value pairs. It supports the <em>put</em>, <em>get</em>,
 * <em>contains</em>, <em>size</em>, and <em>is-empty</em> methods. A symbol
 * table implements the <em>associative array</em> abstraction: when associating
 * a value with a key that is already in the symbol table, the convention is to
 * replace the old value with the new value. Unlike {@link java.util.Map}, this
 * class uses the convention that values cannot be {@code null}—setting the
 * value associated with a key to {@code null} is equivalent to deleting the key
 * from the symbol table.
 * <p>
 * This implementation uses a B-tree. It requires that the key type implements
 * the {@code Comparable} interface and calls the {@code compareTo()} and method
 * to compare two keys. It does not call either {@code equals()} or
 * {@code hashCode()}. The <em>get</em>, <em>put</em>, and <em>contains</em>
 * operations each make log<sub><em>m</em></sub>(<em>n</em>) probes in the worst
 * case, where <em>n</em> is the number of key-value pairs and <em>m</em> is the
 * branching factor. The <em>size</em>, and <em>is-empty</em> operations take
 * constant time. Construction takes constant time.
 * <p>
 * For additional documentation, see
 * <a href="http://algs4.cs.princeton.edu/62btree">Section 6.2</a> of
 * <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 */
public class BTree<Key extends Comparable<Key>, Value> implements Serializable
{
    //max children per B-tree node = MAX-1 (must be an even number and greater than 2)
    private static final int MAX = 4;
    private Node root; //root of the B-tree
    private Node leftMostExternalNode;
    private int height; //height of the B-tree
    private int n; //number of key-value pairs in the B-tree

    //B-tree node data type
    private static final class Node implements Serializable
    {
	private int entryCount; // number of entries
	private Entry[] entries = new Entry[BTree.MAX]; // the array of children
	private Node next;
	private Node previous;
	
	// create a node with k entries
	private Node(int k)
	{
	    this.entryCount = k;
	}
	
	private void setNext(Node next)
	{
	    this.next = next;
	}
	private Node getNext()
	{
	    return this.next;
	}
	private void setPrevious(Node previous)
	{
	    this.previous = previous;
	}
	private Node getPrevious()
	{
	    return this.previous;
	}	
	
	private Entry[] getEntries()
	{
	    return Arrays.copyOf(this.entries, this.entryCount);
	}
    }

    //internal nodes: only use key and child
    //external nodes: only use key and value
    public static class Entry implements Serializable
    {
	private Comparable key;
	private Object val;
	private Node child;

	public Entry(Comparable key, Object val, Node child)
	{
	    this.key = key;
	    this.val = val;
	    this.child = child;
	}
	public Object getValue()
	{
	    return this.val;
	}
	public Comparable getKey()
	{
	    return this.key;
	}
    }

    /**
     * Initializes an empty B-tree.
     */
    public BTree()
    {
	this.root = new Node(0);
	this.leftMostExternalNode = this.root;
    }

    /**
     * Returns true if this symbol table is empty.
     * 
     * @return {@code true} if this symbol table is empty; {@code false}
     *         otherwise
     */
    public boolean isEmpty()
    {
	return this.size() == 0;
    }

    /**
     * @return the number of key-value pairs in this symbol table
     */
    public int size()
    {
	return this.n;
    }

    /**
     * @return the height of this B-tree
     */
    public int height()
    {
	return this.height;
    }
    
    /**
     * returns a list of all the entries in the Btree, ordered by key
     * @return
     */
    public ArrayList<Entry> getOrderedEntries()
    {
	Node current = this.leftMostExternalNode;
	ArrayList<Entry> entries = new ArrayList<>();
	while(current != null)
	{
	    for(Entry e : current.getEntries())
	    {
		if(e.val != null)
		{
		    entries.add(e);
		}
	    }
	    current = current.getNext();
	}
	return entries;
    }
    
    public Entry getMinEntry()
    {
	Node current = this.leftMostExternalNode;
	while(current != null)
	{
	    for(Entry e : current.getEntries())
	    {
		if(e.val != null)
		{
		    return e;
		}
	    }
	}
	return null;	
    }

    public Entry getMaxEntry()
    {
	ArrayList<Entry> entries = this.getOrderedEntries();
	return entries.get(entries.size()-1);
    }
    
    /**
     * Returns the value associated with the given key.
     *
     * @param key the key
     * @return the value associated with the given key if the key is in the
     *         symbol table and {@code null} if the key is not in the symbol
     *         table
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    public Value get(Key key)
    {
	if (key == null)
	{
	    throw new IllegalArgumentException("argument to get() is null");
	}
	Entry entry = this.get(this.root, key, this.height);
	if(entry != null)
	{
	    return (Value)entry.val;
	}
	return null;
    }

    private Entry get(Node currentNode, Key key, int height)
    {
	Entry[] entries = currentNode.entries;

	//current node is external (hence height == 0)
	if (height == 0)
	{
	    for (int j = 0; j < currentNode.entryCount; j++)
	    {
		if(isEqual(key, entries[j].key))
		{
		    //found desired key. Return its value
		    return entries[j];
		}
	    }
	    //didn't find the key
	    return null;
	}

	//current node is internal (height > 0)
	else
	{
	    for (int j = 0; j < currentNode.entryCount; j++)
	    {
		//if (we are at the last key in this node OR the key we 
		//are looking for is less than the next key, i.e. the
		//desired key must be in the subtree below the current entry),
		//then recurse into the current entry’s child
		if (j + 1 == currentNode.entryCount || less(key, entries[j + 1].key))
		{
		    return this.get(entries[j].child, key, height - 1);
		}
	    }
	    //didn't find the key
	    return null;
	}
    }

    /**
     * 
     * @param key
     */
    public void delete(Key key)
    {
	put(key, null);
    }
    
    /**
     * Inserts the key-value pair into the symbol table, overwriting the old
     * value with the new value if the key is already in the symbol table. If
     * the value is {@code null}, this effectively deletes the key from the
     * symbol table.
     *
     * @param key the key
     * @param val the value
     * @throws IllegalArgumentException if {@code key} is {@code null}
     */
    public void put(Key key, Value val)
    {
	if (key == null)
	{
	    throw new IllegalArgumentException("argument key to put() is null");
	}
	//if the key already exists in the b-tree, simply replace the value
	Entry alreadyThere = this.get(this.root, key, this.height);
	if(alreadyThere != null)
	{
	    alreadyThere.val = val;
	    return;
	}
	
	Node newNode = this.put(this.root, key, val, this.height);
	this.n++;
	if (newNode == null)
	{
	    return;
	}

	//split the root:
	//Create a new node to be the root.
	//Set the old root to be new root's first entry.
	//Set the node returned from the call to put to be new root's second entry
	Node newRoot = new Node(2);
	newRoot.entries[0] = new Entry(this.root.entries[0].key, null, this.root);
	newRoot.entries[1] = new Entry(newNode.entries[0].key, null, newNode);
	this.root = newRoot;
	//a split at the root always increases the tree height by 1
	this.height++;
    }

    /**
     * 
     * @param currentNode
     * @param key
     * @param val
     * @param height
     * @return null if no new node was created (i.e. just added a new Entry into an existing node). If a new node was created due to the need to split, returns the new node
     */
    private Node put(Node currentNode, Key key, Value val, int height)
    {
	int j;
	Entry newEntry = new Entry(key, val, null);

	//external node
	if (height == 0)
	{
	    //find index in currentNode’s entry[] to insert new entry
	    for (j = 0; j < currentNode.entryCount; j++)
	    {
		if (less(key, currentNode.entries[j].key))
		{
		    break;
		}
	    }
	}

	// internal node
	else
	{
	    //find index in node entry array to insert the new entry
	    for (j = 0; j < currentNode.entryCount; j++)
	    {
		//if (we are at the last key in this node OR the key we 
		//are looking for is less than the next key, i.e. the
		//desired key must be added to the subtree below the current entry),
		//then do a recursive call to put on the current entry’s child
		if ((j + 1 == currentNode.entryCount) || less(key, currentNode.entries[j + 1].key))
		{
		    //increment j (j++) after the call so that a new entry created by a split
		    //will be inserted is in the next slot 
		    Node newNode = this.put(currentNode.entries[j++].child, key, val, height - 1);
		    if (newNode == null)
		    {
			return null;
		    }
		    //if the call to put returned a node, it means I need to add a new entry to
		    //the current node
		    newEntry.key = newNode.entries[0].key;
		    newEntry.val = null;
		    newEntry.child = newNode;
		    break;
		}
	    }
	}
	//shift entries over one place to make room for new entry
	for (int i = currentNode.entryCount; i > j; i--)
	{
	    currentNode.entries[i] = currentNode.entries[i - 1];
	}
	//add new entry
	currentNode.entries[j] = newEntry;
	currentNode.entryCount++;
	if (currentNode.entryCount < BTree.MAX)
	{
	    //no structural changes needed in the tree
	    //so just return null
	    return null;
	}
	else
	{
	    //will have to create new entry in the parent due
	    //to the split, so return the new node, which is 
	    //the node for which the new entry will be created
	    return this.split(currentNode, height);
	}
    }

    /**
     * split node in half
     * @param currentNode
     * @return new node
     */
    private Node split(Node currentNode, int height)
    {
	Node newNode = new Node(BTree.MAX / 2);
	//by changing currentNode.entryCount, we will treat any value
	//at index higher than the new currentNode.entryCount as if
	//it doesn't exist
	currentNode.entryCount = BTree.MAX / 2;
	//copy top half of h into t
	for (int j = 0; j < BTree.MAX / 2; j++)
	{
	    newNode.entries[j] = currentNode.entries[BTree.MAX / 2 + j];
	}
	//external node
	if (height == 0)
	{
	    newNode.setNext(currentNode.getNext());
	    newNode.setPrevious(currentNode);
	    currentNode.setNext(newNode);
	}
	return newNode;
    }

    // comparison functions - make Comparable instead of Key to avoid casts
    private static boolean less(Comparable k1, Comparable k2)
    {
	return k1.compareTo(k2) < 0;
    }

    private static boolean isEqual(Comparable k1, Comparable k2)
    {
	return k1.compareTo(k2) == 0;
    }

    /**
     * Unit tests the {@code BTree} data type.
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args)
    {
	BTree<Integer, String> st = new BTree<Integer, String>();
	st.put(1, "one");
	st.put(2, "two");
	st.put(3, "three");
	st.put(4, "four");
	st.put(5, "five");
	st.put(6, "six");
	st.put(7, "seven");
	st.put(8, "eight");
	st.put(9, "nine");
	st.put(10, "ten");
	st.put(11, "eleven");
	st.put(12, "twelve");
	st.put(13, "thirteen");
	st.put(14, "fourteen");
	st.put(15, "fifteen");
	st.put(16, "sixteen");
	st.put(17, "seventeen");
	st.put(18, "eighteen");
	st.put(19, "nineteen");
	st.put(20, "twenty");
	st.put(21, "twenty one");
	st.put(22, "twenty two");
	st.put(23, "twenty three");
	st.put(24, "twenty four");
	st.put(25, "twenty five");
	st.put(26, "twenty six");
	
	System.out.println("Size: " + st.size());
	System.out.println("Height: " + st.height);
	System.out.println("Key-value pairs, sorted by key:");
	ArrayList<Entry> entries = st.getOrderedEntries();
	for(Entry e : entries)
	{
	    System.out.println("key = " + e.getKey() + "; value = " + e.getValue());	    
	}
	
	Entry min = st.getMinEntry();
	System.out.println("Minimum Entry: " + "key = " + min.getKey() + "; value = " + min.getValue());
	
	Entry max = st.getMaxEntry();
	System.out.println("Maximum Entry: " + "key = " + max.getKey() + "; value = " + max.getValue());
	
	st.delete(1);
	min = st.getMinEntry();
	System.out.println("Minimum Entry after deleting 1: " + "key = " + min.getKey() + "; value = " + min.getValue());

	st.delete(26);
	max = st.getMaxEntry();
	System.out.println("Maximum Entry after deleting 26: " + "key = " + max.getKey() + "; value = " + max.getValue());

	System.out.println("Key-value pairs, sorted by key:");
	entries = st.getOrderedEntries();
	for(Entry e : entries)
	{
	    System.out.println("key = " + e.getKey() + "; value = " + e.getValue());	    
	}
    }
}

/******************************************************************************
 * Copyright 2002-2016, Robert Sedgewick and Kevin Wayne.
 *
 * This file is part of algs4.jar, which accompanies the textbook
 *
 * Algorithms, 4th edition by Robert Sedgewick and Kevin Wayne, Addison-Wesley
 * Professional, 2011, ISBN 0-321-57351-X. http://algs4.cs.princeton.edu
 *
 *
 * algs4.jar is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * algs4.jar is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * algs4.jar. If not, see http://www.gnu.org/licenses.
 ******************************************************************************/