import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;


public class QueryExpansion {

	private QueryIndex queryMap;
	private HashMap<String, HashMap<String, Double>> simqt	= new HashMap<String, HashMap<String, Double>>();
	//				queryId			qTerm	sim
	private HashMap<String, HashMap<String, Double>> weightQueryTerm	= new HashMap<String, HashMap<String, Double>>();
	//				queryId			qTerm	weight
	private HashMap<String, HashMap<String, Double>> similarityThesaurus;
	private HashMap<String, HashMap<String, Double>> goodTerms	= new HashMap<String, HashMap<String,Double>>();
	private HashMap<String, HashMap<String, Double>> badTerms	= new HashMap<String, HashMap<String,Double>>();	//vollständigkeitshalber
	private HashMap<String, HashMap<String, Double>> additionalTerms	= new HashMap<String, HashMap<String, Double>>();

	private final int NUMBER_FOR_ADDITIONAL_TERMS	= 10;	//number / average (hardcore) ..(?) are possible

	public QueryExpansion(HashMap<String, HashMap<String, Double>> similarityThesaurus) {
		queryMap				= MiniRetrieve.myQueryIndex;
		this.similarityThesaurus	= similarityThesaurus;
	}

	public HashMap<String, HashMap<String, Double>> expandQuery() {
		fillInQueryTermWeights();
		setGoodAndBadSearchTerms();
		calcSimQueryTerm();
		setAdditionalTerms();
		setWeightForAdditionalQueryTerms();
		addNewTermsToQuery();
		return weightQueryTerm;		//weightQueryTerm here = expanded query with weighted terms
	}

	private void setGoodAndBadSearchTerms() {		// step 1
		for (String queryId : queryMap.keySet()) {
			List<Map.Entry<String, Double>> topDocuments	= rankDocumentsByRSV(queryId, 10);
			HashMap<String, Double> tmp	= new HashMap<String, Double>();
			for (String queryTerm : queryMap.get(queryId).keySet()) {
				for (Map.Entry<String, Double> entry : topDocuments) {
					if (MiniRetrieve.myNonInvertedIndex.get(entry.getKey()).containsValue(queryTerm) &&
							MiniRetrieve.myNonInvertedIndex.getTermFrequencyInOneDocument(entry.getKey(), queryTerm) > 0) {
						tmp.put(queryTerm, weightQueryTerm.get(queryId).get(queryTerm));
					}
				}
			}
			goodTerms.put(queryId, tmp);
			tmp.clear();
			for (String queryTerm : queryMap.get(queryId).keySet()) {
				if (!(goodTerms.containsValue(queryTerm))) {
					tmp.put(queryTerm, weightQueryTerm.get(queryId).get(queryTerm));
				}
			}
			badTerms.put(queryId, tmp);
		}
	}

	private void calcSimQueryTerm() {	// step 2
		fillInQueryTermWeights();
		for (String queryId : goodTerms.keySet()) {
			HashMap<String, Double> tmp	= new HashMap<String, Double>();
			for (String goodTerm : goodTerms.get(queryId).keySet()) {
				if (tmp.get(goodTerm) == null) {
					tmp.put(goodTerm, 0d);
				}
				tmp.put(goodTerm, tmp.get(goodTerm) + calcSumQiSIM(queryId, goodTerm));		// goodTerms? (oder queryTerms?)
			}
			simqt.put(queryId, tmp);
		}
	}

	private void setAdditionalTerms() {		// step 3
		rankSimilarityQueryTerms();
		for (String queryId : goodTerms.keySet()) {
			HashMap<String, Double> tmp	= new HashMap<String, Double>();
			for (String term : simqt.get(queryId).keySet()) {
				for (int i=NUMBER_FOR_ADDITIONAL_TERMS; i>=0; i--) {
					tmp.put(term, simqt.get(queryId).get(term));
				}
			}
			additionalTerms.put(queryId, tmp);
		}
	}

	private void setWeightForAdditionalQueryTerms() {		// step 4
		for (String queryId : goodTerms.keySet()) {
			double denominator	= 0;
			for (String goodTerm : goodTerms.get(queryId).keySet()) {
				denominator	+= weightQueryTerm.get(queryId).get(goodTerm);
			}
			for (String newQueryTerm : additionalTerms.get(queryId).keySet()) {
				double nominator	= simqt.get(queryId).get(newQueryTerm);
				additionalTerms.get(queryId).put(newQueryTerm, nominator/denominator);
			}
		}
	}
	
	private void addNewTermsToQuery() {		// step 5
		for (String queryId : additionalTerms.keySet()) {
			for (String newQueryTerm : additionalTerms.get(queryId).keySet()) {
				weightQueryTerm.get(queryId).put(newQueryTerm, additionalTerms.get(queryId).get(newQueryTerm));
			}
		}
	}

	//############################################################################################
	//between steps	
	private void fillInQueryTermWeights() {
		for (String queryId : queryMap.keySet()) {
			HashMap<String, Double> tmp	= new HashMap<String, Double>();
			for (String queryTerm : queryMap.get(queryId).keySet()) {
				int queryTermFrequency = queryMap.get(queryId).get(queryTerm);
				double value	= queryTermFrequency * MiniRetrieve.idf.get(queryTerm);
				tmp.put(queryTerm, value);
			}
			weightQueryTerm.put(queryId, tmp);
		}
	}

	private double calcSumQiSIM(String queryId, String queryTerm) {		// term-similaritäts-gewichte aufsummieren?
		double termWeightSum	= 0;
		double termWeight	= weightQueryTerm.get(queryId).get(queryTerm);
		if (similarityThesaurus.get(queryTerm) == null) {
			return termWeight;
		}
		for (String term : similarityThesaurus.get(queryTerm).keySet()) {
			termWeightSum	+= similarityThesaurus.get(queryTerm).get(term) * termWeight;
		}
		return termWeightSum;
	}

	private void rankSimilarityQueryTerms() {	// step 3
		for (String queryId : simqt.keySet()) {
			HashMap<String, Double> sortedSimqt = new HashMap<String, Double>();
			sortedSimqt = ThesaurusUtils.sortHashMap(simqt.get(queryId));
			simqt.put(queryId, sortedSimqt);
		}
	}

	//############################################################################################
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

	//############################################################################################
	//additionals
	private List<Map.Entry<String, Double>> rankDocumentsByRSV(String queryId, int numberOfResults) {
		List<Map.Entry<String, Double>> list	= new ArrayList<Map.Entry<String, Double>>(MiniRetrieve.accuHash.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
			public int compare(Map.Entry<String, Double> left, Map.Entry<String, Double> right) {
				if (left.getValue() < right.getValue()) {
					return +1;
				} else {
					if (left.getValue() > right.getValue()) {
						return -1;
					} else {
						return 0;
					}
				}
			}
		});
		Iterator itaccu		= list.iterator();
		int counter			= 0;
		while(itaccu.hasNext() && (counter < numberOfResults)) {
			counter++;
			Map.Entry m	= (Map.Entry) itaccu.next();
			//System.out.println(queryId + " Q0 " + m.getKey().toString() + " " + counter + " " + MiniRetrieve.accuHash.get(m.getKey().toString()) + " findIT");
		}
		return list;
	}

}