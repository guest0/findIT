import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;


public class SimilarityCalc extends HashMap<String,HashMap<String,Double>> {

	private double qNorm							= 0;
	private final int numberOfResults			= 10;
	private int numberOfFiles					= 0;

	private InvertedIndex myInvertedIndex;
	private NonInvertedIndex myNonInvertedIndex;
	private QueryIndex myQueryIndex;
	private HashMap<String, Double> accuHash;
	private HashMap<String, Double> dNorm;
	private HashMap<String, Double> idf;

	public SimilarityCalc(InvertedIndex myInvertedIndex, NonInvertedIndex myNonInvertedIndex, 
			HashMap<String, Double> accuHash, HashMap<String, Double> dNorm, HashMap<String, Double> idf) {
		this.myInvertedIndex	= myInvertedIndex;
		this.myNonInvertedIndex	= myNonInvertedIndex;
		this.accuHash			= accuHash;
		this.dNorm				= dNorm;
		this.idf				= idf;
	}
	
	@SuppressWarnings("rawtypes")
	public void calcFeatureWeight() {
		
		int ff, maxff, iif;
		

		Zaehler= numerator
		Nenner= denominator 
		Iterator itDocumentIds	= myNonInvertedIndex.entrySet().iterator();
		
		maxff	= getMaxFF();
		
		while(itDocumentIds.hasNext()) {
			
			Map.Entry documentIdMap	= (Map.Entry) itDocumentIds.next();
			HashMap currentDocument	= (HashMap) documentIdMap.getValue();
			
			Iterator itTerms		= currentDocument.entrySet().iterator();
			
			while(itTerms.hasNext()) {
				Map.Entry documentTermMap	= (Map.Entry) itTerms.next();
				
			}
		}
		

		double fWeight	= ((0.5 + 0.5 * ff1/maxff) * iif) / Math.sqrt(SUM(Math.pow(((0.5 + 0.5 * ff2/maxff) * iif),2)));
	}
	
	@SuppressWarnings("rawtypes")
	private int getMaxFF(String) {
		int maxff			= 0;
		Iterator itTerms	= myInvertedIndex.entrySet().iterator();
		
		while(itTerms.hasNext()) {
			
			Map.Entry termMap	= (Map.Entry) itTerms.next();
			HashMap currentTerm	= (HashMap) termMap.getValue();
			
			if(maxff < currentTerm.size()) {
				maxff	= currentTerm.size();
			}
		} return maxff;
	}
	
	
	//inserts the featureWeights into the term-document HashMap
	private void put(String term, String filename, Double dokumentGewichtung) {
		HashMap<String, Double> map	= this.get(term);
		
		if (map == null) {
			map = new HashMap<String, Double>();
			this.put(term, map);
		}
		map.put(filename, dokumentGewichtung);
	}


}