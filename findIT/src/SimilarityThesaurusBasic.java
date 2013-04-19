import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class SimilarityThesaurusBasic {

	private InvertedIndex myInvertedIndex;
	private NonInvertedIndex myNonInvertedIndex;

	private HashMap<String, HashMap<String, Double>> matrixB;
	//private HashMap<String, HashMap<String, Double>> matrixBT;
	
	//row: term i, column: term j	0 < i,j < matrixS.size()
	private HashMap<String, HashMap<String, Double>> matrixS;

	public SimilarityThesaurusBasic(InvertedIndex myInvertedIndex, NonInvertedIndex myNonInvertedIndex) {
		this.myInvertedIndex	= myInvertedIndex;
		this.myNonInvertedIndex	= myNonInvertedIndex;
	}
	
	public HashMap<String, HashMap<String, Double>> calcSimilarityThesaurus() {
		calcFeatureWeight();
		
		return matrixS;
	}

	@SuppressWarnings("rawtypes")
	private void calcFeatureWeight() {
		double numerator, denominator, featureWeight;

		Iterator itDocumentIds	= myNonInvertedIndex.entrySet().iterator();

		while(itDocumentIds.hasNext()) {

			Map.Entry documentIdMap	= (Map.Entry) itDocumentIds.next();
			String currentDocument	= documentIdMap.getKey().toString();
			HashMap documentTerms	= (HashMap) documentIdMap.getValue();

			Iterator itTerms		= documentTerms.entrySet().iterator();

			while(itTerms.hasNext()) {
				Map.Entry documentTermMap	= (Map.Entry) itTerms.next();
				String currentTerm			= documentTermMap.getKey().toString();

				numerator	= calcNumerator(currentDocument, currentTerm);
				denominator	= calcDenominator(currentTerm);

				featureWeight	= numerator/denominator;
				put(currentTerm, currentDocument, featureWeight, matrixB);
			}
		}
	}

	private double calcNumerator(String document, String term) {
		double numerator, ff, maxff, iif;

		ff			= getFF(document, term);
		maxff		= getMaxFF(term);
		iif			= getIIF(document);
		numerator	= (0.5 + 0.5 * ff/maxff) * iif;

		return numerator;
	}

	@SuppressWarnings("rawtypes")
	private double calcDenominator(String term) {
		double denominator = 0, ff, maxff, iif, value;

		HashMap termDocuments	= myInvertedIndex.get(term);
		Iterator itDocuments	= termDocuments.entrySet().iterator();

		while(itDocuments.hasNext()) {

			Map.Entry termDocumentMap	= (Map.Entry) itDocuments.next();
			String currentDocument		= termDocumentMap.getKey().toString();

			ff			= getFF(currentDocument, term);
			maxff		= getMaxFF(term);
			iif			= getIIF(currentDocument);
			value		= (0.5 + 0.5 * ff/maxff) * iif;
			denominator	+= Math.pow(value, 2);
		}
		denominator	= Math.sqrt(denominator);

		return denominator;
	}

	//calculates inverse item frequency
	private double getIIF(String document) {
		int n				= myInvertedIndex.size();
		int documentLength	= myNonInvertedIndex.get(document).size();

		return Math.log(n/documentLength);
	}

	//calculates feature frequency
	private double getFF(String document, String term) {

		return myInvertedIndex.getTermFrequencyInOneDocument(document, term);
	}

	//calculates maximum feature frequency
	@SuppressWarnings("rawtypes")
	private double getMaxFF(String term) {
		int maxff	= 0;

		HashMap termDocuments	= myInvertedIndex.get(term);
		Iterator itDocuments	= termDocuments.entrySet().iterator();

		while(itDocuments.hasNext()) {

			Map.Entry termDocumentMap	= (Map.Entry) itDocuments.next();

			if(maxff < (int) termDocumentMap.getValue()) {
				maxff	= (int) termDocumentMap.getValue();
			}
		} return maxff;
	}


	//inserts the featureWeights into the term-document HashMap
	private void put(String arg1, String arg2, Double featureWeight, HashMap<String, HashMap<String, Double>> matrix) {
		HashMap<String, Double> map	= matrix.get(arg1);

		if (map == null) {
			map = new HashMap<String, Double>();
			matrix.put(arg1, map);
		}
		map.put(arg2, featureWeight);
	}

}