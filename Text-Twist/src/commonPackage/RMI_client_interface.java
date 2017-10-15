package commonPackage;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMI_client_interface extends Remote {
	
	public final static String OBJECT_NAME="RMI_client";
	
	/*
	 * If some other Client creates a game, the server invokes this
	 * method to warn all the invited Clients.
	 */
	public void gameCall(String creator, int gameID, int multicastPort) throws RemoteException;
	
	/*
	 * If a Client accept a game, and for some reasong the Server
	 * cancell it, it invokes this methot to warn the Client
	 */
	public void gameCancelled(String creator, int gameID) throws RemoteException;

	/*
	 * This methos have the only meaning to let the Server know if the
	 * Client is still online. If the Client fail to respond 
	 * (a RemoteException is thrown), the Server sets the client offline.
	 */
	public boolean isOnline() throws RemoteException;
	
}
