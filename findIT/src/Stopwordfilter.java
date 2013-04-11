import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;


public class Stopwordfilter
{  
	private static String stopwordDirectory = "stopwords";

	public static String[] filterStopwords(String[] originalTokens) {
	    List<String> filteredTokens	= new LinkedList<String>();
	    File stopwordDir	= new File(stopwordDirectory);

	    if (stopwordDir.isDirectory() && stopwordDir.exists()) {
	        File[] files = stopwordDir.listFiles();
		String filename	= files[0].getAbsolutePath();
	        String[] stopwords	= splitFileStringIntoTokens(Utilities.readFile(filename));
	        for (String token : originalTokens) {
		    boolean isStopword	= false;
		    for (String stopword : stopwords) {
		        token	= token.trim().toLowerCase();
		        stopword	= stopword.trim().toLowerCase();
		        if (token.equals(stopword)) {
			    isStopword	= true;
			}
		    }
		    if (isStopword == false) {
			filteredTokens.add(token);
		    }
		}
		return filteredTokens.toArray(new String[0]);
	    } else {
		System.out.println("STOPWORD-Verzeichnis nicht gefunden. Bitte Verzeichnis korrekt angeben!");
		return null;
	    }
	}
	
	private static String[] splitFileStringIntoTokens(String fileContent) {
	    Pattern p	= Pattern.compile("[\\W]+");
	    return p.split(fileContent);
	}

}