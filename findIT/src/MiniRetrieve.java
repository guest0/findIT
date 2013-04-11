
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
//(w) 2007 Thomas Arni, InIT, ZHW
public class MiniRetrieve {

	private static String queryDirectory = "queries";	//Verzeichnis mit allen Anfragen
	private static String documentDirectory = "documents"; //Verzeichnis mit allen Dokuementen
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
			myMiniRetrieve.handleInput(documentDirectory, queryDirectory);
			//myMiniRetrieve.createQueryHash(queryDirectory);
			//myMiniRetrieve.createIndexes(documentDirectory);
		} else if (args.length == 2) {
			myMiniRetrieve.handleInput(args[1], args[0]);
			//myMiniRetrieve.createQueryHash(args[1]);
			//myMiniRetrieve.createIndexes(args[0]);
		}
		myMiniRetrieve.calculateIdfAndNorms();
		myMiniRetrieve.processQueries();
		myMiniRetrieve.printInvertedIndexKey();
		//myMiniRetrieve.printResults();
	}//end main-method

	//handle documents
	private void handleInput(String docDirectory, String queDirectory) {
		File documentDir	= new File(docDirectory);
		File queryDir		= new File(queDirectory);

		if (documentDir.isDirectory() && documentDir.exists()) {
			File[] files = documentDir.listFiles();
			numberOfFiles = files.length;

			for (int i = 0; i < numberOfFiles; i++) {
				String fileContent		= Utilities.readFile(files[i].getAbsolutePath());
				String filename			= files[i].getName();
				Pattern p 				= Pattern.compile("[\\W]+");
				String[] originalTokens	= p.split(fileContent);
				String[] filteredTokens;
				String[] stemmedTokens;

				filteredTokens	= runStopwordfilter(originalTokens);
				stemmedTokens	= runStemmer(filteredTokens);

				createIndexes(stemmedTokens, filename);

			}
		} else {
			System.out.println("DOCUMENT-Verzeichnis nicht gefunden. Bitte Verzeichnis korrekt angeben!");
		}

		//handle queries
		if (queryDir.isDirectory() && queryDir.exists()) {
			File[] files = queryDir.listFiles();

			for (int i = 0; i < files.length; i++) {
				String fileContent		= Utilities.readFile(files[i].getAbsolutePath());
				String queryId			= files[i].getName();
				Pattern p				= Pattern.compile("[\\W]+");
				String[] originalTokens	= p.split(fileContent);
				String[] filteredTokens;
				String[] stemmedTokens;

				filteredTokens	= runStopwordfilter(originalTokens);
				stemmedTokens	= runStemmer(filteredTokens);

				createQueryHash(stemmedTokens, queryId);

			}
		} else {
			System.out.println("QUERY-Verzeichniss nicht gefunden. Bitte Verzeichniss korrekt angeben!");
		}
	}


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
	private void createIndexes(String[] tokens, String filename) {

		for (int i = 0; i < tokens.length; i++) {
			String currentToken = tokens[i];

			//invertedIndex
			if (myInvertedIndex.containsKey(currentToken)) { 												//if token exists
				if (myInvertedIndex.get(currentToken).containsKey(filename)) { 								//if filename exists
					int counter = myInvertedIndex.getTermFrequencyInOneDocument(filename, currentToken);
					counter++;
					myInvertedIndex.put(filename, currentToken, counter); 									//increment counter
				} else {
					myInvertedIndex.put(filename, currentToken, 1);
				}
			} else {
				myInvertedIndex.put(filename,currentToken,1);
			}

			//nonInvertedIndex
			if (myNonInvertedIndex.containsKey(filename)) {													//if filename exists
				if (myNonInvertedIndex.get(filename).containsKey(currentToken)) { 							//if token exists
					int counter = myNonInvertedIndex.getTermFrequencyInOneDocument(filename, currentToken);
					counter++;
					myNonInvertedIndex.put(filename, currentToken, counter); 								//increment counter
				} else {
					myNonInvertedIndex.put(filename, currentToken, 1);
				}
			} else {
				myNonInvertedIndex.put(filename, currentToken, 1);
			}
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
	private void createQueryHash(String[] tokens, String queryId) {

		for (int i = 0; i < tokens.length; i++) {
			String currentToken = tokens[i].trim().toLowerCase();
			if (myQueryIndex.containsKey(queryId)) {
				if (myQueryIndex.get(queryId).containsKey(currentToken)) {
					int counter = myQueryIndex.getTermFrequencyInOneDocument(queryId, currentToken);
					counter++;
					myQueryIndex.put(queryId, currentToken, counter);
				} else {
					myQueryIndex.put(queryId, currentToken, 1);
				}
			} else {
				myQueryIndex.put(queryId, currentToken, 1);
			}
		}
	}

	//Stopwords
	private String[] runStopwordfilter(String[] originalTokens) {
		return Stopwordfilter.filterStopwords(originalTokens);
	}


	/** Initialises the Stemmer. It reads text from a
	 * a list of files, stems each word, and writes the result to standard
	 * output. Note that the word stemmed is expected to be in lower case:
	 * forcing lower case must be done outside the Stemmer class.
	 * Usage: Stemmer file-name file-name ...
	 */
	private String[] runStemmer(String[] filteredTokens) {
		String[] stemmedTokens;
		
		char[] w = new char[501];
		Stemmer stemmer = new Stemmer();
		
		for (int i = 0; i < args.length; i++)
			try
		{
				FileInputStream in = new FileInputStream(args[i]); // JEDES FILE EINLESEN

				try { 
					while(true) {		// ARBEITET BIS 
						int ch = in.read();
						if (Character.isLetter((char) ch)) {
							int j = 0;
							while(true) {
								ch = Character.toLowerCase((char) ch);
								w[j] = (char) ch;
								if (j < 500) j++;
								ch = in.read();
								if (!Character.isLetter((char) ch)) {
									/* to test add(char ch) */
									for (int c = 0; c < j; c++) {
										stemmer.add(w[c]);
									}

									/* or, to test add(char[] w, int j) */
									/* s.add(w, j); */

									stemmer.stem();

									{  String u;

									/* and now, to test toString() : */
									u = stemmer.toString();

									/* to test getResultBuffer(), getResultLength() : */
									/* u = new String(s.getResultBuffer(), 0, s.getResultLength()); */

									System.out.print(u);
									}
									break;
								}
							}
						}
						if (ch < 0) break;
						System.out.print((char)ch);
					}
				}
				catch (IOException e)
				{  System.out.println("error reading " + args[i]);
				break;
				}
		}
		catch (FileNotFoundException e)
		{  System.out.println("file " + args[i] + " not found");
		break;
		}
		
		return stemmedTokens;
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