/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Kündig
 */
public class Levenshtein {

    public static int levenshtein(String original, String input) {
	/* Schritt 1 */
	int n = original.length();
	int m = input.length();
	int cost, minimum;
	char chOriginal, chInput;
	int[][] matrix;

	if (n == 0 || m == 0) {
	    return 0;
	}

	int hoehe = n + 1;
	int breite = m + 1;

	matrix = new int[hoehe][breite];

	/* Schritt 2 */
	for (int i = 0; i < hoehe; i++) {
	    matrix[i][0] = i;
	}

	for (int j = 0; j < breite; j++) {
	    matrix[0][j] = j;
	}

	/* Schritte 3 - 6 */
	for (int i = 1; i < hoehe; i++) {
	    chOriginal = original.charAt(i - 1);

	    for (int j = 1; j < breite; j++) {
		chInput = input.charAt(j - 1);

		/* Wenn Zeichen übereinstimmen -> Keine Substitution etc. notwendig -> 0 Kosten */
		if (chOriginal == chInput) {
		    cost = 0;
		} else {
		    cost = 1;
		}

		/* Berechnung des Minimums */
		minimum = matrix[i - 1][j] + 1;

		if ((matrix[i][j - 1] + 1) < minimum) {
		    minimum = matrix[i][j - 1] + 1;
		}

		if ((matrix[i - 1][j - 1] + cost) < minimum) {
		    minimum = matrix[i - 1][j - 1] + cost;
		}

		matrix[i][j] = minimum;
	    }
	}

	return matrix[n][m];
	//return matrix[n][m]/original.length();
    }
}
