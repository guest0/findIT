
import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
//(w) 2007 Thomas Arni, InIT, ZHW
public class MiniRetrieve {

    private static String queryDirectory = "queries";	//Verzeichnis mit allen Anfragen
    private static String documentDirectory = "documents"; //Verzeichnis mit allen Dokuementen
    private static String stopwordDirectory = "stopwords";
    private static double qNorm = 0;
    private static final int numberOfResults = 10;
    private static int numberOfFiles = 0;
    private static InvertedIndex myInvertedIndex = new InvertedIndex();
    private static NonInvertedIndex myNonInvertedIndex = new NonInvertedIndex();
    private static QueryIndex myQueryIndex = new QueryIndex();
    private static HashMap<String, Double> accuHash = null;
    private static HashMap<String, Double> dNorm = new HashMap<String, Double>();
    private static HashMap<String, Double> idf = new HashMap<String, Double>();
    private static TreeMap<Integer, HashMap<String, Double>> myResultTreeMap = new TreeMap<Integer, HashMap<String, Double>>(new KeyComparator());

    public static void main(String[] args) {
	MiniRetrieve myMiniRetrieve = new MiniRetrieve();

	if (args.length == 0) {
	    myMiniRetrieve.createQueryHash(queryDirectory);
	    myMiniRetrieve.createIndexes(documentDirectory, stopwordDirectory);
	    myMiniRetrieve.calculateIdfAndNorms();
	} else if (args.length == 2) {
	    myMiniRetrieve.createQueryHash(args[1]);
	    myMiniRetrieve.createIndexes(args[0], args[2]);
	    myMiniRetrieve.calculateIdfAndNorms();
	}
	myMiniRetrieve.processQueries();
	myMiniRetrieve.printInvertedIndexKey();
	//myMiniRetrieve.printResults();
    }//end main-method

    //Verarbeite Anfragen
    private void processQueries() {
	Iterator itQueryIds = myQueryIndex.entrySet().iterator(); //Iterator ueber alle Anfragen
	while (itQueryIds.hasNext()) {
	    accuHash = new HashMap<String, Double>(10000);
	    qNorm = 0;

	    Map.Entry queryIdMap = (Map.Entry) itQueryIds.next();
	    HashMap currentQuery = (HashMap) queryIdMap.getValue();
	    int queryId = Integer.parseInt(queryIdMap.getKey().toString());

	    Iterator itTerms = currentQuery.entrySet().iterator(); //Iterator ueber alle Terme der Anfrage
	    while (itTerms.hasNext()) {
		Map.Entry queryTermMap = (Map.Entry) itTerms.next();
		String currentQueryTerm = queryTermMap.getKey().toString(); //liefert akutellen Anfrageterm
		if (!idf.containsKey(currentQueryTerm)) {
		    idf.put(currentQueryTerm, Math.log(1 + numberOfFiles));
		}
		Integer queryTermFrequency = (Integer) currentQuery.get(currentQueryTerm);
		double b = queryTermFrequency * idf.get(currentQueryTerm); //qtf * idf
		qNorm += b * b;

		// if query term occurs in collection
		if (myInvertedIndex.containsKey(currentQueryTerm)) {
		    HashMap documents = (HashMap) myInvertedIndex.get(currentQueryTerm);
		    Iterator itmap = documents.entrySet().iterator();  //Durchlauft fuer jedes Querywort alle Dokumente, die das Wort enthalten
		    while (itmap.hasNext()) {
			Map.Entry query = (Map.Entry) itmap.next();
			String currentFileName = query.getKey().toString();
			double a = myInvertedIndex.getTermFrequencyInOneDocument(currentFileName, currentQueryTerm) * idf.get(currentQueryTerm);
			if (accuHash.containsKey(currentFileName)) {
			    double accuValue = accuHash.get(currentFileName);
			    accuValue += a * b;
			    accuHash.put(currentFileName, accuValue);
			} else {
			    accuHash.put(currentFileName, a * b);
			}
		    } // end while
		}
	    }//end while		
	    qNorm = Math.sqrt(qNorm);
	    this.normalizeVectors(myNonInvertedIndex); //Norm berechnen 
	    //Fuege Anfragenummer "queryId" und Accumulator "accuHash" dem ResultTreeMap hinzu
	    myResultTreeMap.put(queryId, accuHash);
	}
    }

    //	Erstellen des invertierten und nicht-invertierten Index
    private void createIndexes(String documentDirectory, String stopwordDirectory) {
	File documentDir = new File(documentDirectory);
	File stopwordDir = new File(stopwordDirectory);
	if (documentDir.isDirectory() && documentDir.exists() && stopwordDir.isDirectory() && stopwordDir.exists()) {
	    File[] files = documentDir.listFiles(); //File-Array mit allen Dateien im Verzeichnis
	    numberOfFiles = files.length;
	    for (int i = 0; i < files.length; i++) { //allen Files durchlaufen und Inhalt indexieren
		String fileContent = Utilities.readFile(files[i].getAbsolutePath());
		Pattern p = Pattern.compile("[\\W]+");
		// Split input with the pattern
		String[] tokens = p.split(fileContent);
		String[] stopwords = p.split(Utilities.readFile(stopwordDir.listFiles()[0].getAbsolutePath()));    //read stopwords
		String filename = files[i].getName();
		for (int j = 0; j < tokens.length; j++) {
		    String currentToken = tokens[j].trim().toLowerCase(); //in Variable currentToken ist der aktuelle Term aus eniem Dokument
		    //System.err.println("currentToken " + currentToken);
		    //Aufbau invertierter Index
		    if (myInvertedIndex.containsKey(currentToken)) { //Existiert Token schon
			if (myInvertedIndex.get(currentToken).containsKey(filename)) { //Existiert Filename schon
			    int counter = myInvertedIndex.getTermFrequencyInOneDocument(filename, currentToken);
			    counter++;
			    if (currentToken.equals("give")) System.out.println("%%%%%%%%%\t"+currentToken);
			    myInvertedIndex.put(filename, currentToken, counter); //Falls Filename und Token schon existieren -> erhoehe Anzahl
			} else {
			    insertIntoInvertedIndex(currentToken, filename, stopwords);
			} //Falls Filename noch nicht existiert (Token aber schon) setze Anzahl auf 1
		    } else {
			insertIntoInvertedIndex(currentToken, filename, stopwords);
		    }
		    //Aufbau nicht-invertierter Index
		    if (myNonInvertedIndex.containsKey(filename)) {
			if (myNonInvertedIndex.get(filename).containsKey(currentToken)) { //Existiert Token schon
			    int counter = myNonInvertedIndex.getTermFrequencyInOneDocument(filename, currentToken);
			    counter++;
			    myNonInvertedIndex.put(filename, currentToken, counter); //Falls Filename und Token schon existieren -> erhoehe Anzahl
			} else {
			    myNonInvertedIndex.put(filename, currentToken, 1);
			}
		    } else {
			myNonInvertedIndex.put(filename, currentToken, 1);
		    }
		}
	    } // end for-loop
	} // end if
	else {
	    System.out.println("Verzeichnis nicht gefunden. Bitte Verzeichnis korrekt angeben!");
	}
    }

    // Normalisiert Laenge der Vektoren
    private void normalizeVectors(NonInvertedIndex myNonInvertedIndex) {
	Iterator itAccu = accuHash.entrySet().iterator();
	while (itAccu.hasNext()) {
	    Map.Entry accuMapEntry = (Map.Entry) itAccu.next();
	    String filename = accuMapEntry.getKey().toString();

	    double dNormValue = dNorm.get(filename);
	    if (dNormValue == 0) {
		accuHash.put(filename, 0.0d);
		continue;
	    }
	    double tmp = accuHash.get(filename) * 1000;
	    accuHash.put(filename, tmp / (dNormValue * qNorm));
	}
    }

    private void calculateIdfAndNorms() {
	Iterator itFileNames = myNonInvertedIndex.entrySet().iterator(); //Iteartor ueber alle Dokumente
	while (itFileNames.hasNext()) {
	    Map.Entry fileNameMap = (Map.Entry) itFileNames.next();
	    String filename = fileNameMap.getKey().toString();
	    HashMap terms = (HashMap) fileNameMap.getValue();
	    dNorm.put(filename, 0.0d);

	    Iterator itTerms = terms.entrySet().iterator(); //Iterator ueber alle Terme
	    while (itTerms.hasNext()) {
		Map.Entry queryTermMap = (Map.Entry) itTerms.next();
		String currentTerm = queryTermMap.getKey().toString();
		if (!idf.containsKey(currentTerm)) {
		    double value = Math.log((1.0d + numberOfFiles) / (1.0d + myInvertedIndex.getDocumentFrequency(currentTerm)));
		    idf.put(currentTerm, value);
		}
		double a = myNonInvertedIndex.getTermFrequencyInOneDocument(filename, currentTerm) * idf.get(currentTerm);
		if (dNorm.containsKey(filename)) {
		    double tmp = (double) dNorm.get(filename);
		    double value = tmp + (a * a);
		    dNorm.put(filename, value);
		} else {
		    dNorm.put(filename, a * a);
		}
	    }//end while
	    double dNormValue = dNorm.get(filename);
	    dNorm.put(filename, Math.sqrt(dNormValue));
	}
    }

    //Einlesen der Anfragen aus Verzeichnis
    private void createQueryHash(String directory) {
	File myDirectory = new File(directory);
	if (myDirectory.isDirectory() && myDirectory.exists()) {
	    File[] files = myDirectory.listFiles(); //File-Array mit allen Anfrage-Dateien im Verzeichnis
	    for (int i = 0; i < files.length; i++) { //alle Files durchlaufen und Inhalt indexieren
		String fileContent = Utilities.readFile(files[i].getAbsolutePath());
		String queryId = files[i].getName(); //entspricht Datei-Namen
		tokenizeQuery(queryId, fileContent);
	    } // end for-loop
	} // end if
	else {
	    System.out.println("Verzeichniss nicht gefunden. Bitte Verzeichniss korrekt angeben!");
	}
    } // end method createInvertedIndex

    //	Anfrage-Tokenisierung und in QueryHash ablegen
    public void tokenizeQuery(String queryId, String queryString) {
	Pattern p = Pattern.compile("[\\W]+");
	String[] queryTokens = p.split(queryString);
	for (int j = 0; j < queryTokens.length; j++) {
	    String currentToken = queryTokens[j].trim().toLowerCase(); //in Variable currentToken ist der aktuelle Term aus eniem Dokument
	    if (myQueryIndex.containsKey(queryId)) { //Existiert aktuelle Query "queryId" schon
		if (myQueryIndex.get(queryId).containsKey(currentToken)) { //Existiert aktueller Token "currentToken" schon
		    int counter = myQueryIndex.getTermFrequencyInOneDocument(queryId, currentToken);
		    counter++;
		    myQueryIndex.put(queryId, currentToken, counter); //Falls Query und Token schon existieren -> erhoehe Anzahl
		} else {
		    myQueryIndex.put(queryId, currentToken, 1); //Falls Query "queryId" noch nicht existiert (Token aber schon) setze Anzahl auf 1
		}
	    } else {
		myQueryIndex.put(queryId, currentToken, 1);
	    }
	}
    }

    //Stopwords
    private void insertIntoInvertedIndex(String currentToken, String filename, String[] stopwords) {
	for (String curStopword : stopwords) {
	    if (currentToken.equals(curStopword) && !(currentToken.equals(""))) {
	    //if (currentToken.equals("give") && !(currentToken.equals(""))) {
		System.out.println("word gestoppt: " + currentToken);
	    } else {
		myInvertedIndex.put(filename, currentToken, 1);
	    }
	}
    }

    //Printouts
    private void printResults() {
	Iterator itaccu = myResultTreeMap.entrySet().iterator();
	while (itaccu.hasNext()) {
	    Map.Entry m = (Map.Entry) itaccu.next();
	    HashMap accuHash = (HashMap) m.getValue();
	    //Utilities.writeTrecResultOutput(m.getKey().toString(), accuHash, numberOfResults);
	}
    }

    private void printInvertedIndexKey() {
	for (String key : myInvertedIndex.keySet()) {
	    System.out.println(key);
	}
	System.out.println("\ntotal: " + myInvertedIndex.size());
    }

    private void printNonInvertexIndexKey() {
	System.out.println("not implemented yet");
    }
}