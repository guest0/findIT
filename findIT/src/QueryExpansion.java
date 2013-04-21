import java.util.HashMap;


public class QueryExpansion extends SimilarityThesaurusEnhanced {

	private QueryIndex queryMap;
	private HashMap<String, HashMap<String, Double>> simqt	= new HashMap<String, HashMap<String, Double>>();
	//				queryT			docTerm	sim
	private HashMap<String, Double> qcVector;
	private HashMap<String, Double> weightedValueOfTermsOfAllDocs;
	private HashMap<String, HashMap<String, Double>> similarityThesaurus;

	public QueryExpansion(QueryIndex myQueryIndex, InvertedIndex myInvertedIndex, NonInvertedIndex myNonInvertedIndex, HashMap<String,
			HashMap<String, Double>> similarityThesaurus) {
		super(myInvertedIndex, myNonInvertedIndex);
		queryMap	= myQueryIndex;
		this.similarityThesaurus	= similarityThesaurus;
	}

	public void expandQuery() {
		calcQcVector();
		weightedValueOfTermsOfAllDocs	= ThesaurusUtils.getWeightedValueOfTermsOfAllDocuments(myInvertedIndex, myNonInvertedIndex);
		getSimilarityBetweenQueryTermAndDocTerms();		//possible also implemented in thesaurusUtils
	}

	private void calcQcVector() {	// formel 18
		qcVector	= new HashMap<>();
		for (String queryId : queryMap.keySet()) {
			for (String queryTerm : queryMap.get(queryId).keySet()) {
				/*for (String document : myNonInvertedIndex.keySet()) {
					System.out.println("\n\n--------\n");
					double termWeightNom	= ThesaurusUtils.calcTermWeight(document, term, myInvertedIndex, myNonInvertedIndex);
					double termWeightDenom	= weightedValueOfTermsOfAllDocs.get(queryTerm);
					double termWeight		= termWeightNom/termWeightDenom;
					double value		= qcVector.get(queryTerm) * termWeight;
					if (qcVector.containsKey(queryTerm)) {
						qcVector.put(queryTerm, qcVector.get(queryTerm) + value);
					} else {
						qcVector.put(queryTerm, value);
					}
				}*/
				double value	= 0;
				for (String docTerm : similarityThesaurus.get(queryTerm).keySet()) {
					value	+= similarityThesaurus.get(queryTerm).get(docTerm);
				}
				qcVector.put(queryTerm, value);
			}
		}
	}

	private void getSimilarityBetweenQueryTermAndDocTerms() {	// formel 19
		System.out.println("heeere");
		System.out.println(weightedValueOfTermsOfAllDocs);
		for (String queryTerm : qcVector.keySet()) {
			HashMap<String, Double> tmpSim	= new HashMap<String, Double>();
			for (String docTerm : myInvertedIndex.keySet()) {
				double value	= qcVector.get(queryTerm) * weightedValueOfTermsOfAllDocs.get(docTerm);
				tmpSim.put(docTerm, value);
			}
			simqt.put(queryTerm, tmpSim);
		}
		System.out.println(simqt.toString());
	}

	//printouts
	public void printQueryTerms() {
		for (String queryId : queryMap.keySet()) {
			System.out.print(queryId+"\t");
			for (String term : queryMap.get(queryId).keySet()) {
				System.out.print(term+", ");
			}
			System.out.print("\n");
		}
	}

}