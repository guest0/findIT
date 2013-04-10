import java.util.HashMap;
//(w) 2007 Thomas Arni, InIT, ZHW
public class InvertedIndex extends Index{

    //Methode liefert fuer einen uebergebenen Term <term> die Haeufigkeit im entsprechenden Dokument <filename>.
	public int getTermFrequencyInOneDocument(String filename, String term) {
        HashMap<String,Integer> map = this.get(term);
        if (map == null)
            return 0;
        else return map.get(filename);
    } 
	
	//Fuegt Term dem inverted Index hinzu
    public void put(String filename, String term, Integer termFrequency) {
        HashMap<String,Integer> map = this.get(term);
        if (map == null) {
            map = new HashMap<String,Integer>();
            this.put(term, map);
        }
        map.put(filename, termFrequency);
    }
    
    //Methode gibt fuer den uebergebenen Term <term> die Dokumentenhaeufigkeit in der ganzen Kollektion an
    public int getDocumentFrequency(String term){
    	HashMap<String,Integer> map =this.get(term);
    	return map.size();    	
    }  
}
