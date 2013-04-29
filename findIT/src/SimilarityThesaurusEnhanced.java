import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class SimilarityThesaurusEnhanced{

	private InvertedIndex myInvertedIndex;
	private NonInvertedIndex myNonInvertedIndex;

	private HashMap<String, Double> coeff									= new HashMap<String, Double>();
	private HashMap<String, HashMap<String, Double>> sim					= new HashMap<String, HashMap<String, Double>>();
	private HashMap<String, HashMap<String, Double>> similarityThesaurus	= new HashMap<String, HashMap<String, Double>>();

	public SimilarityThesaurusEnhanced(InvertedIndex myInvertedIndex, NonInvertedIndex myNonInvertedIndex) {
		this.myInvertedIndex	= myInvertedIndex;
		this.myNonInvertedIndex	= myNonInvertedIndex;
	}

	public HashMap<String, HashMap<String, Double>> computeSimilarityThesaurus() {

		calcSimilarities();
		calcNormalization();

		return similarityThesaurus;
	}

	//calculates the term-term similarities
	@SuppressWarnings("rawtypes")
	private void calcSimilarities() {
		double termWeight1, termWeight2, termWeightSqr, termWeightTot;

		Iterator itDocumentIds	= myNonInvertedIndex.entrySet().iterator();

		while(itDocumentIds.hasNext()) {

			Map.Entry documentIdMap	= (Map.Entry) itDocumentIds.next();
			String currentDocument	= documentIdMap.getKey().toString();
			HashMap documentTerms	= (HashMap) documentIdMap.getValue();
			Iterator itTerms1		= documentTerms.entrySet().iterator();

			while(itTerms1.hasNext()) {
				Map.Entry documentTermMap1	= (Map.Entry) itTerms1.next();
				String currentTerm1			= documentTermMap1.getKey().toString();
				Iterator itTerms2			= documentTerms.entrySet().iterator();

				termWeight1		= calcTermWeight(currentDocument, currentTerm1);
				termWeightSqr	= Math.pow(termWeight1, 2);

				if(coeff.get(currentTerm1) == null) {
					coeff.put(currentTerm1, termWeightSqr);
				} else {
					coeff.put(currentTerm1, coeff.get(currentTerm1) + termWeightSqr);
				}
				
				Map.Entry documentTermMap2	= (Map.Entry) itTerms2.next();
				while(!documentTermMap2.equals(documentTermMap1)) {
					documentTermMap2	= (Map.Entry) itTerms2.next();
				}
				boolean firstTime	= true;
				boolean lastEntryIt1	= !itTerms1.hasNext();

				while(itTerms2.hasNext() || lastEntryIt1) {
					if(!firstTime) {
						documentTermMap2	= (Map.Entry) itTerms2.next();
					} firstTime		= false;
					lastEntryIt1	= false;
					
					String currentTerm2					= documentTermMap2.getKey().toString();
					HashMap<String, Double> simTerm2	= sim.get(currentTerm1);
					termWeight2							= calcTermWeight(currentDocument, currentTerm1);

					if(simTerm2 == null || simTerm2.get(currentTerm2) == null) {
						termWeightTot	= termWeight1 * termWeight2;
					} else {
						termWeightTot	= simTerm2.get(currentTerm2) + termWeight1 * termWeight2;
					}
					put(currentTerm1, currentTerm2, termWeightTot, sim);
				}
			}
		}
	}

	//calculates the normalization of term-term similarities
	@SuppressWarnings("rawtypes")
	private void calcNormalization() {
		int documentFrequency1, documentFrequency2, nrOfDocuments;
		double normalization, denominator, numerator;

		Iterator itTerms1	= myInvertedIndex.entrySet().iterator();
		nrOfDocuments		= myNonInvertedIndex.size();

		while(itTerms1.hasNext()) {
			Map.Entry termDocumentMap1	= (Map.Entry) itTerms1.next();
			String currentTerm1			= termDocumentMap1.getKey().toString();

			HashMap termDocuments1		= (HashMap) termDocumentMap1.getValue();
			documentFrequency1			= termDocuments1.size();

			if(documentFrequency1 >= 2 || documentFrequency1 <= nrOfDocuments/10) {
				Iterator itTerms2		= myInvertedIndex.entrySet().iterator();
				
				Map.Entry termDocumentMap2	= (Map.Entry) itTerms2.next();
				while(!termDocumentMap2.equals(termDocumentMap1)) {
					termDocumentMap2	= (Map.Entry) itTerms2.next();
				}
				boolean firstTime		= true;
				boolean lastEntryIt1	= !itTerms1.hasNext();

				while(itTerms2.hasNext() || lastEntryIt1) {
					if(!firstTime) {
						termDocumentMap2	= (Map.Entry) itTerms2.next();
					} firstTime		= false;
					lastEntryIt1	= false;
					
					String currentTerm2			= termDocumentMap2.getKey().toString();

					HashMap termDocuments2		= (HashMap) termDocumentMap2.getValue();
					documentFrequency2			= termDocuments2.size();

					if(documentFrequency2 >= 2 || documentFrequency2 <= nrOfDocuments/10) {
						HashMap<String, Double> simTerm2	= sim.get(currentTerm1);
						if(!(simTerm2.get(currentTerm2) == null) && simTerm2.get(currentTerm2) > 0) {
							HashMap<String, Double> similarityTerm2	= similarityThesaurus.get(currentTerm1);

							denominator	= simTerm2.get(currentTerm2);
							numerator	= Math.sqrt(coeff.get(currentTerm1) * coeff.get(currentTerm2));
							
							if(similarityTerm2 == null || similarityTerm2.get(currentTerm2) == null) {
								normalization	= denominator / numerator;
							} else {
								normalization	= similarityTerm2.get(currentTerm2) + denominator / numerator;
							}
							put(currentTerm1, currentTerm2, normalization, similarityThesaurus);
						}
					}
				}
			}
		}
	}

	//calculates the unnormalized weight of a document in a term
	private double calcTermWeight(String document, String term) {
		double termWeight, ff, maxff, iif;

		ff			= getFF(document, term);
		maxff		= getMaxFF(term);
		iif			= getIIF(document);
		termWeight	= (0.5 + 0.5 * ff/maxff) * iif;

		return termWeight;
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
		Iterator itDocumentIds	= termDocuments.entrySet().iterator();

		while(itDocumentIds.hasNext()) {

			Map.Entry termDocumentMap	= (Map.Entry) itDocumentIds.next();

			if(maxff < (int) termDocumentMap.getValue()) {
				maxff	= (int) termDocumentMap.getValue();
			}
		} return maxff;
	}

	//inserts the featureWeights into the term-document HashMap
	private void put(String arg1, String arg2, Double featureWeight, HashMap<String, HashMap<String, Double>> hashMap) {
		HashMap<String, Double> map	= hashMap.get(arg1);

		if (map == null) {
			map = new HashMap<String, Double>();
			hashMap.put(arg1, map);
		}
		map.put(arg2, featureWeight);
	}

}
