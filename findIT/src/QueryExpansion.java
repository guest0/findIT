import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;


public class QueryExpansion {

	private NonInvertedIndex myNonInvertedIndex;
	private HashMap<String, Double> idf;
	private QueryIndex queryMap;
	private MiniRetrieve myMiniRetrieve;

	private HashMap<String, HashMap<String, Double>> simqt	= new HashMap<String, HashMap<String, Double>>();
	//				queryId			qTerm	sim
	private HashMap<String, HashMap<String, Double>> weightQueryTerm	= new HashMap<String, HashMap<String, Double>>();
	//				queryId			qTerm	weight
	private HashMap<String, HashMap<String, Double>> similarityThesaurus;
	private HashMap<String, HashMap<String, Double>> goodTerms	= new HashMap<String, HashMap<String,Double>>();
	private HashMap<String, HashMap<String, Double>> badTerms	= new HashMap<String, HashMap<String,Double>>();	//vollständigkeitshalber
	private HashMap<String, HashMap<String, Double>> additionalTerms	= new HashMap<String, HashMap<String, Double>>();

	// variables to adjust result
	private final int NUMBER_TOP_RATED_DOCUMENTS	= 10;	//
	private final int NUMBER_FOR_ADDITIONAL_TERMS	= 4;	//number / average (hardcore) ..(?) are possible

	public QueryExpansion(HashMap<String, HashMap<String, Double>> similarityThesaurus, MiniRetrieve myMiniRetrieve) {
		queryMap			= myMiniRetrieve.getQueryIndex();
		myNonInvertedIndex	= myMiniRetrieve.getNonInvertedIndex();
		idf					= myMiniRetrieve.getIdf();
		this.myMiniRetrieve	= myMiniRetrieve;
		this.similarityThesaurus	= similarityThesaurus;
	}

	public HashMap<String, HashMap<String, Double>> expandQuery() {
		fillInQueryTermWeights();
		setGoodAndBadSearchTerms();
		calcSimQueryTerm();
		setAdditionalTerms();
		setWeightForAdditionalQueryTerms();
		addNewTermsToQuery();
		try {
			FileWriter fw	= new FileWriter("weight.txt");
			BufferedWriter bw	= new BufferedWriter(fw);
			for (String queryId : weightQueryTerm.keySet()) {
				bw.write(queryId + "\r\n" + queryMap.get(queryId)+"\r\n");
				bw.write(additionalTerms.get(queryId)+"\r\n");
				bw.write(weightQueryTerm.get(queryId)+"\r\n\r\n");
			}
			bw.close();
		}
		catch (Exception e) {}
		return weightQueryTerm;		//weightQueryTerm here = expanded query with weighted terms
	}

	private void setGoodAndBadSearchTerms() {		// step 1
		for (String queryId : queryMap.keySet()) {
			HashMap<String, Double> topDocuments	= rankDocumentsByRSV(queryId, NUMBER_TOP_RATED_DOCUMENTS);
			HashMap<String, Double> tmp	= new HashMap<String, Double>();
			for (String queryTerm : queryMap.get(queryId).keySet()) {
				for (String document : topDocuments.keySet()) {
					if (myNonInvertedIndex.get(document).containsKey(queryTerm)) {
						tmp.put(queryTerm, weightQueryTerm.get(queryId).get(queryTerm));
					}
				}
			}
			goodTerms.put(queryId, tmp);
			tmp	= new HashMap<String, Double>();
			for (String queryTerm : queryMap.get(queryId).keySet()) {
				if (!(goodTerms.get(queryId).containsKey(queryTerm))) {
					tmp.put(queryTerm, weightQueryTerm.get(queryId).get(queryTerm));
				}
			}
			badTerms.put(queryId, tmp);
			//System.out.println("good: "+goodTerms.get(queryId)+"\nbad: "+badTerms.get(queryId)+"\n"+queryMap.get(queryId)+"\n");	// see splitting
		}
	}

	private void calcSimQueryTerm() {	// step 2
		for (String queryId : goodTerms.keySet()) {
			//HashMap<String, Double> tmp	= new HashMap<String, Double>();
			for (String goodTerm : goodTerms.get(queryId).keySet()) {
				/*if (tmp.get(goodTerm) == null) {
					tmp.put(goodTerm, 0d);
				}
				tmp.put(goodTerm, tmp.get(goodTerm) + calcSumQiSIM(queryId, goodTerm));		// put in the "similar" terms??*/
				simqt.put(queryId, getWeightedSimWithGoodTerms(queryId));
			}
			//simqt.put(queryId, tmp);
		}
	}

	private void setAdditionalTerms() {		// step 3
		rankSimilarityQueryTerms();
		for (String queryId : goodTerms.keySet()) {
			HashMap<String, Double> tmp	= new HashMap<String, Double>();
			int counter	= 0;
			for (String term : simqt.get(queryId).keySet()) {
				if (counter < NUMBER_FOR_ADDITIONAL_TERMS) {
					tmp.put(term, simqt.get(queryId).get(term));
				} else {
					break;
				}
				counter++;
			}
			additionalTerms.put(queryId, tmp);
			/*System.out.println(additionalTerms.get(queryId));		// see added terms	(1 word in query and as additional term..?) 
			System.out.println(queryMap.get(queryId)+"\n");*/
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
				double value		= nominator/denominator;
				additionalTerms.get(queryId).put(newQueryTerm, value);
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
				double value	= queryTermFrequency * idf.get(queryTerm);
				tmp.put(queryTerm, value);
			}
			weightQueryTerm.put(queryId, tmp);
		}
	}

	private HashMap<String, Double> getWeightedSimWithGoodTerms(String queryId) {
		HashMap<String, Double> tmp	= new HashMap<>();
		for (String term : similarityThesaurus.keySet()) {
			double sum	= 0;
			for (String goodTerm : goodTerms.get(queryId).keySet()) {
				if (!term.equals(goodTerm)) {
					if (similarityThesaurus.get(goodTerm) != null && similarityThesaurus.get(goodTerm).get(term) != null) {
						sum	+= weightQueryTerm.get(queryId).get(goodTerm) * similarityThesaurus.get(goodTerm).get(term);
					}
				}
			}
			tmp.put(term, sum);
		}
		return tmp;
	}

	/*private double calcSumQiSIM(String queryId, String goodTerm) {		// term-similaritäts-gewichte aufsummieren?
		double termWeightSum	= 0;
		double goodTermWeight	= weightQueryTerm.get(queryId).get(goodTerm);
		/*if (similarityThesaurus.get(goodTerm) == null) {	// jeder gute term wurde in einem dokument gefunden -> aus dokumenten wurd sim berechnet
			return goodTermWeight;
		}*//*
		for (String term : similarityThesaurus.get(goodTerm).keySet()) {
			termWeightSum	+= similarityThesaurus.get(goodTerm).get(term) * goodTermWeight;	// correct?
		}
		return termWeightSum;
	}*/

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
	private HashMap<String, Double> rankDocumentsByRSV(String queryId, int numberOfResults) {
		HashMap<String, Double> rankedDocuments	= ThesaurusUtils.sortHashMap(myMiniRetrieve.getAccuHashOfQueryId(queryId));
		HashMap<String, Double> topDocuments	= new HashMap<>();
		int counter	= 0;
		for (String doc : rankedDocuments.keySet()) {
			if (counter < numberOfResults) {
				topDocuments.put(doc, rankedDocuments.get(doc));
			} else {
				break;
			}
			counter++;
		}
		return topDocuments;
	}

}