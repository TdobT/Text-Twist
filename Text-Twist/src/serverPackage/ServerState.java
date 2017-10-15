package serverPackage;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ServerState {
	
	String ID = "id";
	String stateFileName;
	UserContainer users;
	Document doc;
	
	ServerState (String stateFileName, UserContainer users) {
		this.stateFileName = stateFileName;
		this.users = users;

		boolean success = false;
		// Tries to open the file, otherwise it creates it
	    try {
	    	if (stateFileName == null || stateFileName.equals("")) {
	    		stateFileName = new String("ServerState.xml");
	    	}
	    	
    		File stateFile = new File(stateFileName);
		    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		    doc = dBuilder.parse(stateFile);
		    doc.getDocumentElement().normalize();

	    	success = true;
	    	
		} catch (IOException e) {
			System.out.println("Errore nell'apertura del file di stato;");
		} catch (ParserConfigurationException e) {
			System.out.println("Errore nella conversione del file XML;");
		} catch (SAXException e) {
			System.out.println("Errore nella lettura del file XML;");
		}
	    
	    if (!success) {
	    	System.out.println("Creazione nuovo file");
	    	createNewFile();
	    }
	}
	
	
	public void recover() {

		try {
			NodeList els = doc.getElementsByTagName("User");

			for (int i = 0; i < els.getLength(); i++) {
				Node node = els.item(i);

				if (node.getNodeType() == Node.ELEMENT_NODE) {

					Element el = (Element) node;

					// Decode the password and the salt, wich were Coded
					// to be written in the xml file
					
					String username = el.getAttribute(ID);
					byte[] codedPassword = 
							DatatypeConverter.parseHexBinary(
							el.getElementsByTagName("Password")
							.item(0)
							.getTextContent());

					byte[] salt = 
							DatatypeConverter.parseHexBinary(
							el.getElementsByTagName("Salt")
							.item(0)
							.getTextContent());
					

					int points = 
							Integer.parseInt(
									el.getElementsByTagName("Points").item(0).getTextContent()
									);

					if (!users.addUser(new User(username, codedPassword, salt, points)))
						System.out.println("User già esistente");;
				} 

			}
		} catch (NumberFormatException | DOMException e) {
			
			System.out.println("Errore nella lettura del file; creazione nuovo file");
			createNewFile();
		} 
	}
	
	private boolean createNewFile() {

		try {
			
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder;
			docBuilder = docFactory.newDocumentBuilder();
			
			// root elements
			doc = docBuilder.newDocument();
			
			Element rootElement = doc.createElement("class");
			doc.appendChild(rootElement);
			
			saveFile();
			
			
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			return false;
		} catch (TransformerException e) {
			System.out.println("Impossibile salvere il file");
			e.printStackTrace();
		}

		return true;
	}
	
	public void addUser(User user) {

		Element userEl = doc.createElement("User");
		doc.getFirstChild().appendChild(userEl);

		userEl.setAttribute("id", user.getName());

		// Encode password and salt, because they can't be stored 
		// "as is" in an xml file. They'll be Decoded when read
		
		Element psw = doc.createElement("Password");
		String pswCoded = DatatypeConverter.printHexBinary(user.getCodedPassword());
		psw.appendChild(doc.createTextNode(pswCoded));
		userEl.appendChild(psw);
		
		Element salt = doc.createElement("Salt");
		String saltCoded = DatatypeConverter.printHexBinary(user.getSalt());
		salt.appendChild(doc.createTextNode(saltCoded));
		userEl.appendChild(salt);
		
		Element points = doc.createElement("Points");
		points.appendChild(doc.createTextNode("0"));
		userEl.appendChild(points);
		doc.getDocumentElement().normalize();
		try {
			saveFile();
		} catch (TransformerException e) {
			System.out.println("Impossibile salvare il file");
			e.printStackTrace();
		}
	}
	
	public boolean updateUser(User user) {
			
		NodeList els = doc.getElementsByTagName("User");
		
		for (int i = 0; i < els.getLength(); i++) {
			Node node = els.item(i);

			if (node.getNodeType() == Node.ELEMENT_NODE) {

				Element el = (Element) node;
				
				if (el.getAttribute("id").equals(user.getName())) {
					el.getElementsByTagName("Points")
					.item(0)
					.setTextContent(
							user.getPoints() + "");
					try {
						saveFile();
					} catch (TransformerException e) {
						System.out.println("Impossibile salvere il file");
						e.printStackTrace();
					}
					return true;
				}
			}
		}
		
		return false;
	}
	
	private void saveFile() throws TransformerException{
		

		// write the content into xml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		
		StreamResult result = new StreamResult(new File(stateFileName));

		transformer.transform(source, result);

		System.out.println("File saved!");
		
	}
}
