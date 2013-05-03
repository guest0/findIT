import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;


public class ThesaurusUtils {

	public static HashMap<String, Double> sortHashMap(HashMap<String, Double> input) {
		Map<String, Double> tempMap = new HashMap<String, Double>();
		for (String wsState : input.keySet()){
			tempMap.put(wsState,input.get(wsState));
		}
		List<String> mapKeys = new ArrayList<String>(tempMap.keySet());
		List<Double> mapValues = new ArrayList<Double>(tempMap.values());
		HashMap<String, Double> sortedMap = new LinkedHashMap<>();
		TreeSet<Double> sortedSet = new TreeSet<Double>(mapValues);
		Object[] sortedArray = sortedSet.toArray();
		int size = sortedArray.length;
		for (int i=size-1; i>=0; i--) {
			sortedMap.put(mapKeys.get(mapValues.indexOf(sortedArray[i])), 
					(Double)sortedArray[i]);
		}
		/*for (int i=0; i<size; i++){
			sortedMap.put(mapKeys.get(mapValues.indexOf(sortedArray[i])), 
					(Double)sortedArray[i]);
		}*/
		return sortedMap;
	}

}
