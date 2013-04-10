import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
//(w) 2007 Thomas Arni, InIT, ZHW
public abstract class Index extends HashMap<String,HashMap<String,Integer>>{

	//Methode gibt den Index auf einfache Art und Weise aus.
	public void printIndex(){
		//Hash ausgeben
		Iterator it = this.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry)it.next();
			System.out.println(entry.getKey() + "-->"+ entry.getValue());
		}  
    }
	
    //Methode liefert fuer einen uebergebenen Term <term> die Haeufigkeit im entsprechenden Dokument <filename>.
	public abstract int getTermFrequencyInOneDocument(String filename, String term);
	
	//Fuegt Term dem nicht-inverten Index hinzu
    public abstract void put(String filename, String term, Integer termFrequency);

}
