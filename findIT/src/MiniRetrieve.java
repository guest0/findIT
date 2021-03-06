
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;


public class MiniRetrieve {

	//private static String queryDirectory		= "queries";
	//private static String documentDirectory		= "documents";
	private static String queryDirectory		= "data/destination/queries";
	private static String documentDirectory		= "data/destination/collection";

	private static String rankingFileName			= "rankingTmp.trac_eval";
	private static String rankingFileDestination	= "data/destination/ranking/";

	private static double qNorm					= 0;
	private static final int numberOfResults	= 20;
	private static int numberOfFiles			= 0;

	private static InvertedIndex myInvertedIndex		= new InvertedIndex();
	private static NonInvertedIndex myNonInvertedIndex	= new NonInvertedIndex();
	private static QueryIndex myQueryIndex				= new QueryIndex();

	private static HashMap<String, Double> accuHash	= null;
	private static HashMap<String, Double> dNorm	= new HashMap<String, Double>();
	private static HashMap<String, Double> idf		= new HashMap<String, Double>();

	private static HashMap<String, HashMap<String, Double>> similarityThesaurus;
	private static HashMap<String, HashMap<String, Double>> expandedQuery;

	@SuppressWarnings("unchecked")
	private static TreeMap<Integer, HashMap<String, Double>> myResultTreeMap = new TreeMap<Integer, HashMap<String, Double>>(new KeyComparator());


	public static void main(String[] args) {
		MiniRetrieve myMiniRetrieve 			= new MiniRetrieve();
		SimilarityThesaurusEnhanced myThesaurus	= new SimilarityThesaurusEnhanced(myInvertedIndex, myNonInvertedIndex);
		if (args.length == 0) {
			myMiniRetrieve.handleInput(documentDirectory, queryDirectory);
		} else if (args.length == 2) {
			myMiniRetrieve.handleInput(args[1], args[0]);
		}
		similarityThesaurus	= myThesaurus.computeSimilarityThesaurus();
		myMiniRetrieve.calculateIdfAndNorms();
		myMiniRetrieve.processQueries();
		QueryExpansion queryExpansion	= new QueryExpansion(similarityThesaurus, myMiniRetrieve);
		expandedQuery					= queryExpansion.expandQuery();
		myMiniRetrieve.calcNewRSV();
		myMiniRetrieve.writeResults();
		myMiniRetrieve.printAverageQueryRSV();
	}//end main-method

	//handle documents
	private void handleInput(String docDirectory, String queDirectory) {
		File documentDir = new File(docDirectory);
		File queryDir = new File(queDirectory);

		if (documentDir.isDirectory() && documentDir.exists()) {
			File[] files = documentDir.listFiles();
			numberOfFiles = files.length;

			for (int i = 0; i < numberOfFiles; i++) {
				String fileContent	= Utilities.readFile(files[i].getAbsolutePath());
				String filename		= files[i].getName();
				Pattern p			= Pattern.compile("[\\W]+");
				String[] originalTokens = p.split(fileContent);
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
				String fileContent	= Utilities.readFile(files[i].getAbsolutePath());
				String queryId		= files[i].getName();
				Pattern p			= Pattern.compile("[\\W]+");
				String[] originalTokens = p.split(fileContent);
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
	@SuppressWarnings("rawtypes")
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
				myInvertedIndex.put(filename, currentToken, 1);
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
	@SuppressWarnings("rawtypes")
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

	@SuppressWarnings("rawtypes")
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
			String currentToken = tokens[i];
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
			myQueryIndex.put(queryId, currentToken, 1);
		}
	}
	
	private void calcNewRSV() {
		for (String queryId : expandedQuery.keySet()) {
			accuHash = new HashMap<String, Double>(10000);
			qNorm = 0;
			for (String queryTerm : expandedQuery.get(queryId).keySet()) {
				double b	= expandedQuery.get(queryId).get(queryTerm);
				qNorm += Math.pow(b, 2);
				if (myInvertedIndex.containsKey(queryTerm)) {
					for (String document : myInvertedIndex.get(queryTerm).keySet()) {
						double a = myInvertedIndex.getTermFrequencyInOneDocument(document, queryTerm) * idf.get(queryTerm);
						if (accuHash.containsKey(document)) {
							double accuValue = accuHash.get(document);
							accuValue += a * b;
							accuHash.put(document, accuValue);
						} else {
							accuHash.put(document, a * b);
						}
					}
				}
			}//end while		
			qNorm = Math.sqrt(qNorm);
			this.normalizeVectors(myNonInvertedIndex);
			myResultTreeMap.put(Integer.parseInt(queryId), accuHash);
		}
	}

	//initializes the Stopwordfilter
	private String[] runStopwordfilter(String[] originalTokens) {
		return Stopwordfilter.filterStopwords(originalTokens);
	}

	//initializes the Stemmer
	private String[] runStemmer(String[] filteredTokens) {
		Stemmer stemmer = new Stemmer();
		String[] tmpTokens = new String[100000];
		int tokenIndex = 0;
		String[] stemmedTokens;
		String tmpToken;
		char tmpChar;
		char[] tmpLetters;
		char[] tmpNumbers;
		char[] tmpMixed;
		int numberIndex, letterIndex, mixedIndex;
		boolean prevWasLetter, stemMixed;

		for (int i = 0; i < filteredTokens.length; i++) {
			tmpToken	= filteredTokens[i];
			tmpLetters	= new char[tmpToken.length()];
			tmpNumbers	= new char[tmpToken.length()];
			tmpMixed	= new char[tmpToken.length()];
			numberIndex		= 0;
			letterIndex		= 0;
			mixedIndex		= 0;
			prevWasLetter	= true;
			stemMixed		= false;
			
			for (int j = 0; j < tmpToken.length(); j++) {
				tmpChar		= tmpToken.charAt(j);
				tmpMixed[mixedIndex++]	= tmpChar;
				
				if(Character.isLetter(tmpChar)) {
					if(!prevWasLetter) {
						if(numberIndex > 3) {
							stemMixed	= false;
							tmpMixed	= new char[tmpToken.length()];
							mixedIndex	= 0;
							
							stemmer.add(tmpNumbers, tmpNumbers.length);
							stemmer.stem();
							tmpTokens[tokenIndex++]	= stemmer.toString();
						} else {
							stemMixed	= true;
						}
						tmpNumbers	= new char[tmpToken.length()];
						numberIndex	= 0;
					}
					tmpLetters[letterIndex++]	= tmpChar;
					prevWasLetter	= true;
				} else {
					if(prevWasLetter) {
						if(letterIndex > 3) {
							stemMixed	= false;
							tmpMixed	= new char[tmpToken.length()];
							mixedIndex	= 0;

							stemmer.add(tmpLetters, tmpLetters.length);
							stemmer.stem();
							tmpTokens[tokenIndex++]	= stemmer.toString();
						} else {
							stemMixed	= true;
						}
						tmpLetters	= new char[tmpToken.length()];
						letterIndex	= 0;
					}
					tmpNumbers[numberIndex++]	= tmpChar;
					prevWasLetter	= false;
				}
				
			}
			if(stemMixed) {
				if(mixedIndex > 2 && numberIndex < 4 && letterIndex < 4) {
					stemmer.add(tmpMixed, tmpMixed.length);
					stemmer.stem();
					tmpTokens[tokenIndex++]	= stemmer.toString();
				}
				if(numberIndex > 3) {
					stemmer.add(tmpNumbers, tmpNumbers.length);
					stemmer.stem();
					tmpTokens[tokenIndex++]	= stemmer.toString();
				}
				if(letterIndex > 3) {
					stemmer.add(tmpLetters, tmpLetters.length);
					stemmer.stem();
					tmpTokens[tokenIndex++]	= stemmer.toString();
				}
			}
			if(!stemMixed) {
				if(numberIndex > 1) {
					stemmer.add(tmpNumbers, tmpNumbers.length);
					stemmer.stem();
					tmpTokens[tokenIndex++]	= stemmer.toString();
				}
				if(letterIndex > 1) {
					stemmer.add(tmpLetters, tmpLetters.length);
					stemmer.stem();
					tmpTokens[tokenIndex++]	= stemmer.toString();
				}
			}
		}

		stemmedTokens = new String[tokenIndex];
		for (int k = 0; k < tokenIndex; k++) {
			stemmedTokens[k] = tmpTokens[k];
		}

		return stemmedTokens;
	}

	//handle unknown word from query
	/*private String handleUnknownQueryWord(String queryToken) {
		List<String> commons = new LinkedList<>();
		int longestRun = 0;
		String mostCommonWord = "";
		for (String currentToken : myInvertedIndex.keySet()) {

			int deviation = Levenshtein.levenshtein(queryToken, currentToken);
			if (deviation > 0 && deviation <= 2 && queryToken.length() > 2 && currentToken.length() >= queryToken.length()) {
				commons.add(currentToken);
				int distance = commonCharSequence(queryToken, currentToken);
				if (distance > longestRun) {
					mostCommonWord = currentToken;
					longestRun = distance;
				}
			}

		}
		System.out.println("Most common word for " + queryToken + ":\t" + mostCommonWord + " - " + longestRun + "\n\t" + commons.toString());
		if (longestRun / queryToken.length() > .6) {	//adjust parameter for "better" results
			return mostCommonWord;
		}
		return queryToken;
	}

	public static int commonCharSequence(String queryToken, String currentToken) {
		int record = 0, common = 0, distance = 1;
		int i = 0, n = 0;
		while (i < queryToken.length() && i + distance <= queryToken.length() && queryToken.length() - record > i) {
			n = 0;
			while (n < currentToken.length() && n + distance <= currentToken.length() && i + distance <= queryToken.length()) {
				if (queryToken.substring(i, i + distance).equals(currentToken.substring(n, n + distance))) {
					if (n + distance <= currentToken.length()) {
						common++;
						distance++;
					}
				} else {
					common = 0;
					distance = 1;
					n++;
				}
				if (common > record) {
					record = common;
				}
			}
			i++;
		}
		return record;
	}*/

	//Printouts
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void writeResults() {
		Iterator itaccu	= myResultTreeMap.entrySet().iterator();
		
		try {
			FileWriter fstream	= new FileWriter(rankingFileDestination + rankingFileName);
			BufferedWriter out	= new BufferedWriter(fstream);
			
			FileWriter stemmTest	= new FileWriter(rankingFileDestination + "stemmerTest");
			BufferedWriter stemmOut	= new BufferedWriter(stemmTest);

			List sortedKeys = new ArrayList(myInvertedIndex.keySet());
			Collections.sort(sortedKeys);
			Iterator itlist	= sortedKeys.iterator();
			
			while (itlist.hasNext()) {
				String token = itlist.next().toString();
				stemmOut.write(token + "\r\n");
			}
			stemmOut.close();
			
			while (itaccu.hasNext()) {
				Map.Entry m = (Map.Entry) itaccu.next();
				HashMap accuHash = (HashMap) m.getValue();
				Utilities.writeTrecResultOutput(m.getKey().toString(), accuHash, numberOfResults, out);
			}
			out.close();
		} catch(IOException e) {
			System.err.println("Error while writing trec_eval: " + e.toString());
		}
	}
	
	public void printAverageQueryRSV() {
		System.out.println("\naverages:");
		double totalAverage	= 0;
		for (Integer id : myResultTreeMap.keySet()) {
			String queryId	= id.toString();
			double sumRSV	= 0;
			HashMap<String, Double> ranking	= ThesaurusUtils.sortHashMap(myResultTreeMap.get(id));
			double counter	= 0;
			for (String document : ranking.keySet()) {
				if (counter < numberOfResults) {
					sumRSV	+= ranking.get(document);
				} else {
					break;
				}
				counter++;
			}
			System.out.println(queryId + ":\t" + sumRSV/numberOfResults);
			totalAverage	+= sumRSV/numberOfResults;
		}
		System.out.println("\ntotal average:\t" + totalAverage/50 + "\t\tonly true for skb-collection!!");	// 50 queries ...
	}
	
	//getter
	public QueryIndex getQueryIndex() {
		return myQueryIndex;
	}
	
	public NonInvertedIndex getNonInvertedIndex() {
		return myNonInvertedIndex;
	}
	
	public HashMap<String, Double> getIdf() {
		return idf;
	}
	
	public HashMap<String, Double> getAccuHash() {
		return accuHash;
	}
	
	public HashMap<String, Double> getAccuHashOfQueryId(String queryId) {
		return myResultTreeMap.get(Integer.parseInt(queryId));
	}
}