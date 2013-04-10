import java.util.HashMap;
//(w) 2007 Thomas Arni, InIT, ZHW
public class NonInvertedIndex extends Index{ //noninvindex filename->word->freq
   
    //Methode liefert fuer einen uebergebenen Term <term> die Haeufigkeit im entsprechenden Dokument <filename>.
	public int getTermFrequencyInOneDocument(String filename, String term) {
        HashMap<String,Integer> map = this.get(filename);
        if (map == null)
            return 0;
        else return map.get(term);
    } 
    
	//Fuegt Term dem nicht-inverten Index hinzu
    public void put(String filename, String term, Integer termFrequency) {
        HashMap<String,Integer> map = this.get(filename);
        if (map == null) {
            map = new HashMap<String,Integer>();
            this.put(filename, map);
        }
        map.put(term, termFrequency);
    }
}
