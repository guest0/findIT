import java.util.*;
//(w) 2007 Thomas Arni, InIT, ZHW
public class KeyComparator implements Comparator{
 
	public int compare(Object o1, Object o2){
	    return ((Integer)o1).compareTo(((Integer)o2));
	}
	public KeyComparator(){

	}
}