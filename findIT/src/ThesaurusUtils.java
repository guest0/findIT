import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class ThesaurusUtils {

	private static InvertedIndex invertedIndex;
	private static NonInvertedIndex nonInvertedIndex;
	private static HashMap<String, Double> weightedValueOfTermsOfAllDocs	= new HashMap<String, Double>();
	
	//calculates the unnormalized weight of a document in a term
	public static double calcTermWeight(String document, String term, InvertedIndex myInvertedIndex, 
																		NonInvertedIndex myNonInvertedIndex) {
		invertedIndex		= myInvertedIndex;
		nonInvertedIndex	= myNonInvertedIndex;
		
		double termWeight, ff, maxff, iif;

		ff			= getFF(document, term);
		maxff		= getMaxFF(term);
		iif			= getIIF(document);
		termWeight	= (0.5 + 0.5 * ff/maxff) * iif;

		return termWeight;
	}

	//calculates inverse item frequency
	private static double getIIF(String document) {
		int n				= invertedIndex.size();
		int documentLength	= nonInvertedIndex.get(document).size();

		return Math.log(n/documentLength);
	}

	//calculates feature frequency
	private static double getFF(String document, String term) {
		return invertedIndex.getTermFrequencyInOneDocument(document, term);
	}

	//calculates maximum feature frequency
	@SuppressWarnings("rawtypes")
	private static double getMaxFF(String term) {
		int maxff	= 0;

		HashMap termDocuments	= invertedIndex.get(term);
		Iterator itDocumentIds	= termDocuments.entrySet().iterator();

		while(itDocumentIds.hasNext()) {

			Map.Entry termDocumentMap	= (Map.Entry) itDocumentIds.next();

			if(maxff < (int) termDocumentMap.getValue()) {
				maxff	= (int) termDocumentMap.getValue();
			}
		} return maxff;
	}
	
	public static boolean termWeightAlreadyCalculated(String term) {
		return weightedValueOfTermsOfAllDocs.containsKey(term);
	}
	
	public static HashMap<String, Double> getWeightedValueOfTermsOfAllDocuments(InvertedIndex myInvertedIndex, NonInvertedIndex myNonInvertedIndex) {
		//value of weight -> how???
		if (weightedValueOfTermsOfAllDocs.size() == 0) {
			for (String term : invertedIndex.keySet()) {
				double value	= 0;
				for (String document : nonInvertedIndex.keySet()) {
					value	+= Math.pow(calcTermWeight(document, term, myInvertedIndex, myNonInvertedIndex), 2);
				}
				weightedValueOfTermsOfAllDocs.put(term, Math.sqrt(value));
			}
		} else {
			System.out.println("scho voll");
			System.exit(0);
		}
		return weightedValueOfTermsOfAllDocs;
	}

}
