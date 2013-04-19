import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.*;

public class FileCreator {

	private static String trecCollectionSourceFile				= "data/source/collection/irg_collection.trec";
	private static String trecCollectionDestinationDirectory	= "data/destination/collection/";

	private static String trecQuerySourceFile					= "data/source/queries/irg_queries.trec";
	private static String trecQueryDestinationDirectory			= "data/destination/queries/";


	public static void main(String argv[]) {
		FileCreator	creator	= new FileCreator();

		creator.createFiles(trecCollectionSourceFile, trecCollectionDestinationDirectory);
		creator.createFiles(trecQuerySourceFile, trecQueryDestinationDirectory);
	}

	public void createFiles(String sourceFile, String destinationDirectory) {
		try {

			File trecFile 						= new File(sourceFile);
			DocumentBuilderFactory dbFactory	= DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder			= dbFactory.newDocumentBuilder();
			Document doc						= dBuilder.parse(trecFile);

			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("DOC");
			String fileName, fileContent;

			for (int i = 0; i < nList.getLength(); i++) {

				Node nNode = nList.item(i);

				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement	= (Element) nNode;
					fileName			= eElement.getElementsByTagName("recordId").item(0).getTextContent();
					fileContent			= eElement.getElementsByTagName("text").item(0).getTextContent();

					// Create file
					FileWriter fstream = new FileWriter(destinationDirectory + fileName);
					BufferedWriter out = new BufferedWriter(fstream);
					out.write(fileContent);
					out.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}