package clientPackage;

import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;

import commonPackage.RMI_client_interface;

public class RMI_client extends RemoteObject implements RMI_client_interface{

	private static final long serialVersionUID = 8520172379567158477L;

	private GUI_logic clientGUI;

	RMI_client(GUI_logic clientGUI) {
		this.clientGUI = clientGUI;
	}

	
	/*
	 * (non-Javadoc)
	 * @see commonPackage.RMI_client_interface#gameCall(java.lang.String, int, int)
	 */
	@Override
	public void gameCall(String creator, int gameID, int multicastPort) throws RemoteException {

		// not adding the game if it belongs to the creator
		if (!creator.equals(clientGUI.username)) clientGUI.addGameRequest(creator, gameID, multicastPort);

	}

	/*
	 * (non-Javadoc)
	 * @see commonPackage.RMI_client_interface#gameCancelled(java.lang.String, int)
	 */
	@Override
	public void gameCancelled(String creator, int gameID) throws RemoteException {

		clientGUI.removeGameRequest(gameID);

	}

	/* 
	 * (non-Javadoc)
	 * @see commonPackage.RMI_client_interface#gameCancelled(java.lang.String, int)
	 * This methos have the only meaning to let the Server know if the
	 * Client is still online. If the Client fail to respond 
	 * (a RemoteException is thrown), the Server sets the client offline.
	*/
	@Override
	public boolean isOnline() throws RemoteException {
		return true;
	}

}
