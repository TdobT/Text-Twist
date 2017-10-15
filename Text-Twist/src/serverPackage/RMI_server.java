package serverPackage;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.ArrayList;

import commonPackage.RMI_client_interface;
import commonPackage.RMI_server_interface;
import serverPackage.User.userState;

public class RMI_server extends RemoteObject implements RMI_server_interface{

	private static final long serialVersionUID = 7321894133525981176L;

	private UserContainer users;
	private String serverName;
	private int port;
	private InetAddress inetServer;
	private ServerState stateServer;
	
	RMI_server(String serverName, int port, UserContainer users, ServerState stateServer) {
		this.serverName = serverName;
		this.port = port;
		try {
			this.inetServer = InetAddress.getByName(serverName);
		} catch (UnknownHostException e) {

			e.printStackTrace();
		}
		
		this.users = users;
		this.stateServer = stateServer;
	}
	
	public int getPort() { return this.port; }
	
	public String getName() { return this.serverName; }
	
	public InetAddress getAddress() { return this.inetServer; }
	
	public UserContainer getUser() { return this.users; }
	
	
	@Override
	public boolean register(String name, String password) {
		
		User user = new User(name, password);
		if (users.addUser(user)) {
			stateServer.addUser(user);
			return true;
		} 
		return false;
	}

	@Override
	public int login(String name, String password, RMI_client_interface clientCallback) {

		User user = users.getUser(name);
		if (user == null) return -2;
		if (!user.verifyPassword(password)) return -1;
		if (user.getState().equals(userState.ONLINE)) {
			// verifies it is still online
			
			try {
				user.getCallback().isOnline();
				
				// unreacheable if the user is not online
				return -3;
			} catch (RemoteException e) {
				// in this case the user wasn't online, so it can login again.
			}
		}

		user.setState(userState.ONLINE);
		user.setCallback(clientCallback);
		
		return user.getSessionID();
	}

	@Override
	public int logout(String name, String password) {

		User user = users.getUser(name);
		if (user == null) return -1;
		if (!user.verifyPassword(password)) return -2;
		
		System.out.println("User " + user.getName() + " settato offline");
		user.setState(userState.OFFLINE);
		user.resetCallback();
		
		return 0;
	}

	@Override
	public ArrayList<String> requestOnlineUsers() throws RemoteException {

		return users.getOnlineUsers();
	}

	

}
