package commonPackage;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface RMI_server_interface extends Remote {


	public final static String OBJECT_NAME="RMI_server";
	
	
	
	/*
	 * Return Encoding:
	 * true  : registration successful
	 * false : registration failed
	 */
	public boolean register(String name, String password) throws RemoteException;
	
	
	
	/*
	 * Return Encoding:
	 * >0  : login successfull
	 * -1 : account non existent
	 * -2 : wrong password
	 * -3 : already logged int
	 * 
	 * The number returned from the login call is the session ID necessary
	 * for future comunication with Server. If the Client needs to send
	 * any type of message to the server, it must use this number,
	 * so the Server can assure it's the right Client doing the operation.
	 */
	public int login(String name, String password, RMI_client_interface clientCallback) throws RemoteException;

	
	
	/*
	 * Return Encoding:
	 * 0  : logout successfull
	 * -1 : account non existent
	 * -2 : wrong password
	 * 
	 * the logout needs the password to prevent anyone to logout a generic account.
	 */
	public int logout(String name, String password) throws RemoteException;
	
	
	
	/*
	 * Returns a String[] containing all the users online in that moment
	 */
	public ArrayList<String> requestOnlineUsers() throws RemoteException;
	
}
