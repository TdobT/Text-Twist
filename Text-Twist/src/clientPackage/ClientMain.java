package clientPackage;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

import javax.swing.UIManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ClientMain {

	static int REGISTRY_PORT = 0;
	static int TCP_PORT = 0;
	static int UDP_PORT = 0;
	static String REGISTRY_HOST = null;
	static InetAddress MULTICAST_ADDRESS = null;
	
	public static void main(String[] args) {
		
    	String configFileName = "ClientConfiguration.xml";
    	
    	if (args.length == 1) configFileName = args[0];
    	else if (args.length > 1) {
    		System.out.println("Troppi argomenti; è possibile eseguire il programma con:");
    		System.out.println("0 argomenti: viene preso il nome del file di configurazione \"ClientConfiguration.xml\"");
    		System.out.println("1 argomento: viene preso il nome del file di configurazione indicato");
    		System.exit(-1);
    	}
    	
		try {
			File inputFile = new File(configFileName);
		    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		    Document doc = dBuilder.parse(inputFile);
		    doc.getDocumentElement().normalize();
		    
		    NodeList els = doc.getElementsByTagName("property");

	    	REGISTRY_HOST = els.item(0).getFirstChild().getTextContent();
	    	REGISTRY_PORT = Integer.parseInt(els.item(1).getFirstChild().getTextContent());
	    	TCP_PORT = Integer.parseInt(els.item(2).getFirstChild().getTextContent());
	    	UDP_PORT = Integer.parseInt(els.item(3).getFirstChild().getTextContent());
	    	String mult_addr_name = els.item(4).getFirstChild().getTextContent();

			MULTICAST_ADDRESS = InetAddress.getByName(mult_addr_name);
			if (!MULTICAST_ADDRESS.isMulticastAddress()) {
				System.out.println("Questo indirizzo non è Multicast. Impossibile continuare");
				System.exit(-1);
			}
			
		} catch (IOException ex) {
			System.out.println("Impossibile convertire la stringa data nell'indirizzo del server");
			ex.printStackTrace();
			System.exit(-1);
		} catch (NumberFormatException ex) {
			System.out.println("Errore nella lettura di una porta del file di configurazione. Impossibile continuare");
			System.exit(-1);
		} catch (ParserConfigurationException e2) {
			System.out.println("Errore nella conversione del file XML. Impossibile continuare");
			e2.printStackTrace();
			System.exit(-1);
		} catch (SAXException e2) {
			System.out.println("Errore nella lettura del file XML. Impossibile continuare");
			e2.printStackTrace();
			System.exit(-1);
		}
		
		
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(
                                //  "javax.swing.plaf.metal.MetalLookAndFeel");
                                //  "com.sun.java.swing.plaf.motif.MotifLookAndFeel");
                                UIManager.getCrossPlatformLookAndFeelClassName());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                new GUI_logic(REGISTRY_PORT, REGISTRY_HOST, TCP_PORT, UDP_PORT, MULTICAST_ADDRESS).setVisible(true);
            }
        });
    }
	

}
