package structures;

import java.util.ArrayList;


// keeps a list of (double,T) couples sorted by key
public class SortedMap<T> {

	@SuppressWarnings("hiding")
	private class Element<T> {
		protected double key;
		protected T value;

		public Element(double key, T value) {
			this.key = key;
			this.value = value;
		}
		
		public String toString() {
			return "( " + key + " , " + value.toString() + " )";
		}
	}

	private ArrayList<Element<T>> list;
	private int maxSize;
	private boolean maximizeKey;

	public SortedMap(int maxSize, boolean maximizeKey) {
		this.maxSize = maxSize;
		this.maximizeKey = maximizeKey;
		this.list = new ArrayList<Element<T>>();
	}

	// adds couple to the list at the right spot if it fits, else does nothing
	public void add(double key, T value) {
		if (list.size() == maxSize) {
			if (!maximizeKey && key < list.get(0).key) {
				list.remove(0);
			} else if (maximizeKey && key > list.get(0).key) {
				list.remove(0);
			} else {
				return;
			}
		}
		int index = 0;
		while (index < maxSize) {
			if (index < list.size() && ((!maximizeKey && key > list.get(index).key) || (maximizeKey && key < list.get(index).key))) {
				list.add(index, new Element<T>(key, value));
				break;
			} else if(index == list.size()) {
				list.add(new Element<T>(key, value));
				break;
			}
			index++;
		}
	}

	public double getKey(int index) {
		return list.get(index).key;
	}

	public T getValue(int index) {
		return list.get(index).value;
	}

	public int size() {
		return list.size();
	}

	public int maxSize() {
		return maxSize;
	}

	public String toString() {
		return list.toString();
	}
}
