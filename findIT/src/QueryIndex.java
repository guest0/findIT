import java.util.HashMap;
//(w) 2007 Thomas Arni, InIT, ZHW
public class QueryIndex extends Index{ //<queryId, <Token,Frequency>>

	//Methode liefert fuer einen uebergebenen Term <term> die Haeufigkeit in der entsprechenden Anfrag <filename>.
	public int getTermFrequencyInOneDocument(String queryId, String currentToken) {
        HashMap<String,Integer> map = this.get(queryId);
        if (map == null)
            return 0;
        else return map.get(currentToken);
    }
	
	//Fuegt dem AnfrageHash den Term und dessen Haeufigkeit hinzu
    public void put(String queryId, String term, Integer termFrequency) {
        HashMap<String,Integer> map = this.get(queryId);
        if (map == null) {
            map = new HashMap<String,Integer>();
            this.put(queryId, map);
        }
        map.put(term, termFrequency);
    }
}
