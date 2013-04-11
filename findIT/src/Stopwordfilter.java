import java.io.File;


public class Stopwordfilter
{  
	private static String stopwordDirectory = "stopwords";

	public static String[] filterStopwords(String[] originalTokens) {
		String[] filteredTokens	= new String[originalTokens.length];
		File stopwordDir		= new File(stopwordDirectory);

		if (stopwordDir.isDirectory() && stopwordDir.exists()) {
			File[] files = stopwordDir.listFiles();
				
				/////////////////////////////////////////////////////////////////////////////////
				//////							KÜNDIGS CODE HERE						/////////
				//////																	/////////
				//////						BITTE EINBAUEN (WICHTIG):					/////////
				//////	String currentToken = originalTokens[i].trim().toLowerCase();	/////////
				//////																	/////////
				/////////////////////////////////////////////////////////////////////////////////
			
			return filteredTokens;
		} else {
			System.out.println("STOPWORD-Verzeichnis nicht gefunden. Bitte Verzeichnis korrekt angeben!");
			return null;
		}
	}

}