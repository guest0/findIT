import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
//(w) 2007 Thomas Arni, InIT, ZHW
public class Utilities {
	
    //Methode gibt fuer ein uebergebene Datei <filename> den Inhalt als String zurueck.
    public static String readFile(String filename){
		BufferedReader inFile;
		String line = "";
		String returnString = "";
		try{
			inFile = new BufferedReader(new FileReader(filename));
			while ( ( line=inFile.readLine() ) != null){
				returnString = returnString + " " + line;
			}
		}
		catch (IOException e){
			System.err.println("Fehler beim Einlesen der Datei: " + e.toString());
		}
		return returnString;
	}
	
	//Sortiert den uebergebenen HashMap und gibt diesen im Trec-Format aus , Zeile 128-135
	public static void writeTrecResultOutput(String queryId, HashMap accuHash, int numberOfResults){
		List<Map.Entry<String, Double>> list = new ArrayList<Map.Entry<String, Double>>(accuHash.entrySet());
  		Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
			public int compare(Map.Entry<String, Double> left,
					Map.Entry<String, Double> right) {
				if (left.getValue() < right.getValue()) return +1;
				else if (left.getValue() > right.getValue()) return -1;
				else return 0;
			}
		});
		Iterator itaccu = list.iterator();
		int counter = 0;
		while(itaccu.hasNext() && (counter <= numberOfResults)){
			counter++;
			Map.Entry m = (Map.Entry) itaccu.next();
			System.out.println(queryId + " Q0 " + m.getKey().toString() + " " + counter + " " + accuHash.get(m.getKey().toString()) + " miniRetrieve");
		}		
	}
}
