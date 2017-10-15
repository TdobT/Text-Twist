package clientPackage;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import commonPackage.Multicast_rankings;
import commonPackage.RMI_client_interface;
import commonPackage.RMI_server_interface;
import commonPackage.UDP_words;

import static javax.swing.GroupLayout.Alignment.*;

public class GUI_logic extends JFrame implements ActionListener, ListSelectionListener{

	public enum GUI_state {
		LOGIN, LOBBY, NEWGAME, PLAYING
	}
	
	// Action commands:
	private final String 
			LOGIN = "Login", REGISTER = "Register", NEWGAME = "New Game", ACCEPTGAME = "Accept Game",
			DECLINEGAME = "Decline Invite", RANKINGS = "Update Ranking", LOGOUT = "Logout", 
			SENDINVITES = "Send invites", CANCEL = "Cancel", ADDALL = "Add All", SWITCH = "< >", 
			UPDATE = "Refresh", REMOVEALL = "Remove All", SENDWORD = "Send Word", SURREND = "Surrend",
			HELP = "Help", TIMER = "Timer", GAMETIMER = "GameTimer";
			
	private static final long serialVersionUID = -4932694628587849289L;
	private GUI_state state;
	private RMI_server_interface serverRMI;
	private RMI_client_interface userStub;
	private int REGISTRY_PORT;
	private String REGISTRY_HOST;
	private Registry registry;
	private InetAddress serverAddress;
	private InetAddress multicastAddress;
	private int TCP_PORT;
	private int UDP_PORT;
	private DatagramSocket dataSocket;
	
	private Dimension screenDimension;
	public String username;
	private String password;
	private GamesData currentPlayingGame;
	private GamesContainer games = new GamesContainer();
	
	/*
	 * The session ID is used to comunicate with Server (look at
	 * RMI_server_interface).
	 * if it's set to 0 it means it's not connected to the Server
	*/
	private int sessionID = 0;

	// Elements of Login GUI (must be global in the class to access them in other methods)
	private GroupLayout layout = null;
	private JTextField txtUsername = null;
	private JPasswordField txtPassword = null;
	
	// Elements of Lobby GUI
	private JLabel lblUsername = null;
	private JList<GamesData> jListInvites;
	private JButton butAcceptInvite = null;
	private JButton butDeclineInvite = null;
	private DefaultListModel<GamesData> listInv = new DefaultListModel<GamesData>();
	private DefaultListModel<String> listRank = new DefaultListModel<String>();
	
    // Elements of New Game GUI
	private JList<String> jListOnline;
	private JList<String> jListInvited;
	private JButton butSwitch = null;
	private JButton butSend = null;
	private DefaultListModel<String> listOn = null;
	private DefaultListModel<String> listIn = null;
	
	// Elements of Game GUI
	private JLabel lblTimer = null;
	private JLabel lblState = null;
	private JLabel lblErrors = null;
	private JLabel lblLetters = null;
	private JLabel lblRanking = null;
	private JButton butSurrend = null;
	private JButton butSendWord = null;
	private int timeLeft = 0;
	private boolean gameStarted = false;
	private Timer waitingForWords = null;
	private Timer gameTimer = null;
	private JTextField txtSendWord = null;
	private DefaultListModel<String> listFWords = null;
	private DefaultListModel<String> listLRanking = null;
	// To stop the worker thread whos waiting for the word
	private AcceptGameTask acceptTask = null;

    

	GUI_logic(int REGISTRY_PORT, String REGISTRY_HOST, int TCP_PORT, int UDP_PORT, InetAddress MULT_ADDR) {

		this.REGISTRY_PORT = REGISTRY_PORT;
		this.REGISTRY_HOST = REGISTRY_HOST;
		this.TCP_PORT = TCP_PORT;
		this.UDP_PORT = UDP_PORT;
		this.multicastAddress = MULT_ADDR;
		
		try {

			serverAddress = InetAddress.getByName(REGISTRY_HOST);
			dataSocket = new DatagramSocket();
			dataSocket.setSoTimeout(2000);
			
			RMI_client_interface user = new RMI_client(this);
			
			// Client Stub for callback comunications
			userStub = (RMI_client_interface) UnicastRemoteObject.exportObject(user, 0);
			
			// Server RMI element for RMI comunication
			registry = LocateRegistry.getRegistry(this.REGISTRY_HOST, this.REGISTRY_PORT);
			serverRMI = (RMI_server_interface) registry.lookup(RMI_server_interface.OBJECT_NAME);
		
		} catch (NotBoundException e) {
			System.out.println("Errore nella localizzazione del registro RMI. Impossibile continuare");
			e.printStackTrace();
			System.exit(-1);
		} catch (RemoteException e) {
			System.out.println("Errore nell'esportazione dello stub RMI. Impossibile continuare");
			e.printStackTrace();
			System.exit(-1);
		} catch (SocketException e) {
			System.out.println("Errore nella creazione del socket UDP. Impossibile continuare");
			e.printStackTrace();
			System.exit(-1);
		} catch (UnknownHostException e) {
			System.out.println("Impossibile leggere l'indirizzo del server. Impossibile continuare");
			e.printStackTrace();
			System.exit(-1);
		}

		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		// SETS RIGHT OPERATION WHEN TRYING TO CLOSE
		this.addWindowListener(new ClosingOp());

		screenDimension = Toolkit.getDefaultToolkit().getScreenSize();
        
		createAndShowLoginGUI();
	}
	
	
	
	/*
	 * The next functions are helpers made to easily create objects
	 * 
	 */

    private JTextField makeText(String command) {
    	JTextField txt = new JTextField("");
    	txt.setActionCommand(command);
    	txt.addActionListener(this);
    	return txt;
    }
    
    private JButton makeButton(String caption) {
        JButton b = new JButton(caption);
        b.setActionCommand(caption);
        b.addActionListener(this);
        return b;
    }
    
    private <U> JList<U> makeListScroller(DefaultListModel<U> list) {

    	JList<U> jList = new JList<U>(list);
    	jList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    	jList.setLayoutOrientation(JList.VERTICAL_WRAP);
    	jList.setVisibleRowCount(-1);
    	jList.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    	return jList;
    }

    private GroupLayout setLayout(int width, int height) {

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setPreferredSize(new Dimension(width, height));
        getContentPane().removeAll();
        getContentPane().setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        return layout;
    }
     
    
    /*
     * This are the main functions used to set the graphics.
     * 
     * - LoginGUI is the login interface, where the user log in and register.
     * 
     * - LobbyGUI is the lobby interface, where the user can accept, decline,
     *  	create new games and see the global ranking.
     * 
     * - NewGameGUi is an interface made to create new game, and in it the
     * 		User can select the online player with he wants to play.
     * 
     * - GameGUI is the interface to play the game. 
     * 
     */
    
    private void createAndShowLoginGUI() {

    	state = GUI_state.LOGIN;
    	
        //Create and set up the window.
    	JLabel lblUsername = new JLabel("Username: ");
    	JLabel lblPassword = new JLabel("Password: ");
    	JButton butLogin = makeButton(LOGIN);
    	JButton butRegister = makeButton(REGISTER);
    	txtPassword = new JPasswordField("");
    	txtPassword.setActionCommand(LOGIN);
    	txtPassword.addActionListener(this);
    	txtUsername = makeText(LOGIN);
      
    	// Dimension for Login GUI
        GroupLayout layout = setLayout(screenDimension.width / 5, screenDimension.height / 8);
        
        layout.setHorizontalGroup(layout.createSequentialGroup()
    		.addGroup(layout.createParallelGroup()
				.addComponent(lblUsername)
				.addComponent(lblPassword))
    		.addGroup(layout.createParallelGroup()
        		.addComponent(txtUsername)
        		.addComponent(txtPassword)
        		.addGroup(layout.createSequentialGroup()
    				.addComponent(butLogin)
    				.addComponent(butRegister)))
        );
       
        layout.linkSize(SwingConstants.HORIZONTAL, butLogin, butRegister);

        layout.setVerticalGroup(layout.createSequentialGroup()
        	.addGroup(layout.createParallelGroup()
        		.addComponent(lblUsername)
        		.addComponent(txtUsername))
        	.addGroup(layout.createParallelGroup()
        		.addComponent(lblPassword)
        		.addComponent(txtPassword))
            .addGroup(layout.createParallelGroup(BASELINE)
                .addComponent(butLogin)
                .addComponent(butRegister))
        );
        
        setTitle("Login / Register");
        
        //Display the window.
        pack();
        setVisible(true);
    	
        this.setLocation((screenDimension.width - this.getWidth()) / 2 - 30
        		, (screenDimension.height - this.getHeight()) / 2 - 30);
    }

    
    private void createAndShowLobbyGUI() {

        state = GUI_state.LOBBY;
        
    	JLabel lblGameRequest = new JLabel("Games Request:");
    	JLabel lblRanking = new JLabel("Rankings:");
    	JButton butNewGame = makeButton(NEWGAME);
    	JButton butRankings = makeButton(RANKINGS);
    	JButton butHelp = makeButton(HELP);
    	JButton butLogout = makeButton(LOGOUT);
    	JScrollPane listInvites = new JScrollPane(jListInvites = makeListScroller(listInv));
    	JScrollPane listRanking = new JScrollPane(makeListScroller(listRank));
    	butAcceptInvite = makeButton(ACCEPTGAME);
    	butAcceptInvite.setEnabled(false);
    	butDeclineInvite = makeButton(DECLINEGAME);
    	butDeclineInvite.setEnabled(false);
    	lblUsername = new JLabel("Welcome " + username + "!");

    	jListInvites.addListSelectionListener(this);
    	listRank.removeAllElements();
    	
    	butNewGame.setToolTipText("Start a new game!");
		
        layout = setLayout(screenDimension.width / 2, screenDimension.height / 2);

        layout.setHorizontalGroup(layout.createSequentialGroup()
        	.addGroup(layout.createParallelGroup()
        		.addComponent(lblUsername)
        		.addComponent(butNewGame)
        		.addComponent(butAcceptInvite)
        		.addComponent(butDeclineInvite)
        		.addComponent(butRankings)
        		.addComponent(butHelp)
        		.addComponent(butLogout))
        	.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
        		.addComponent(lblGameRequest)
        		.addComponent(listInvites))
        	.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
        		.addComponent(lblRanking)
        		.addComponent(listRanking))
        );

        layout.linkSize(SwingConstants.HORIZONTAL, butNewGame, butAcceptInvite, 
        				butDeclineInvite, butRankings, butLogout, butHelp);
        layout.linkSize(SwingConstants.VERTICAL, lblGameRequest, lblRanking);
        
        layout.setVerticalGroup(layout.createSequentialGroup()
        	.addGroup(layout.createParallelGroup()
        		.addComponent(lblUsername)
        		.addComponent(lblGameRequest)
        		.addComponent(lblRanking))
        	.addGroup(layout.createParallelGroup()
        		.addGroup(layout.createSequentialGroup()
        			.addComponent(butNewGame)
        		    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                            GroupLayout.DEFAULT_SIZE, 10)
        			.addComponent(butAcceptInvite)
        		    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                            GroupLayout.DEFAULT_SIZE, 10)
        			.addComponent(butDeclineInvite)
        		    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                            GroupLayout.DEFAULT_SIZE, 10)
        			.addComponent(butRankings)
        		    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                            GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        			.addComponent(butHelp)
        		    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                            GroupLayout.DEFAULT_SIZE, 10)
        			.addComponent(butLogout))
        		.addComponent(listInvites)
        		.addComponent(listRanking))
		    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED,
                    10, 10)
        );
        
        setTitle("Lobby");
        pack();
        this.setLocation((Toolkit.getDefaultToolkit().getScreenSize().width - this.getWidth()) / 2,
        		(Toolkit.getDefaultToolkit().getScreenSize().height - this.getHeight()) / 2);
        
    }
    
    
    private void createAndShowNewGameGUI() {

        state = GUI_state.NEWGAME;
        
    	JLabel lblOnlinePlayers = new JLabel("Online Players: ");
    	JLabel lblSelectedPlayers = new JLabel("Invited Players: ");
    	listOn = new DefaultListModel<String>();
    	listIn = new DefaultListModel<String>();
    	JScrollPane listOnline = new JScrollPane(jListOnline = makeListScroller(listOn));
    	JScrollPane listInvited = new JScrollPane(jListInvited = makeListScroller(listIn));
    	JButton butCancel = makeButton(CANCEL);
    	JButton butAddAll = makeButton(ADDALL);
    	JButton butRemoveAll = makeButton(REMOVEALL);
    	JButton butUpdate = makeButton(UPDATE);
    	butSend = makeButton(SENDINVITES);
    	butSend.setEnabled(false);
    	butSwitch = makeButton(SWITCH);
    	butSwitch.setEnabled(false);
    	
    	jListInvited.addListSelectionListener(this);
    	jListOnline.addListSelectionListener(this);
    	
    	GroupLayout layout = setLayout(screenDimension.width / 4, screenDimension.height / 3);
        
        layout.setHorizontalGroup(layout.createSequentialGroup()
        	.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
        		.addComponent(lblOnlinePlayers)
        		.addComponent(listOnline)
        		.addComponent(butSend))
        	.addGroup(layout.createParallelGroup()
        		.addComponent(butSwitch)
        		.addComponent(butAddAll)
        		.addComponent(butRemoveAll)
        		.addComponent(butUpdate))
        	.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
        		.addComponent(lblSelectedPlayers)
        		.addComponent(listInvited)
        		.addComponent(butCancel))
        );
        
        layout.setVerticalGroup(layout.createSequentialGroup()
        	.addGroup(layout.createParallelGroup()
        		.addComponent(lblOnlinePlayers)
        		.addComponent(lblSelectedPlayers))
        	.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
        		.addComponent(listOnline)
        		.addGroup(layout.createSequentialGroup()
        			.addComponent(butSwitch)
        			.addComponent(butAddAll)
        			.addComponent(butRemoveAll)
        			.addComponent(butUpdate))
        		.addComponent(listInvited))
        	.addGroup(layout.createParallelGroup()
        		.addComponent(butSend)
        		.addComponent(butCancel))
        );

        layout.linkSize(SwingConstants.HORIZONTAL, butSend, butCancel);
        layout.linkSize(SwingConstants.HORIZONTAL, butSwitch, butAddAll, butRemoveAll, butUpdate);

        setTitle("New Game");
        pack();

        this.setLocation((Toolkit.getDefaultToolkit().getScreenSize().width - this.getWidth()) / 2,
        		(Toolkit.getDefaultToolkit().getScreenSize().height - this.getHeight()) / 2);
        // Request a list of online users
        (new FindOnlineUsersTask()).execute();
    }
    
    
    private void createAndShowGameGUI() {

        state = GUI_state.PLAYING;
        
    	JLabel lblLet = new JLabel("Letters: ");
    	JLabel lblFWords = new JLabel("Found words: ");
    	JPanel pnlErrors = new JPanel();
    	JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
    	JSeparator separator2 = new JSeparator(SwingConstants.VERTICAL);
    	JButton butHelp = makeButton(HELP);
    	butSendWord = makeButton(SENDWORD);
    	listFWords = new DefaultListModel<String>();
    	listLRanking = new DefaultListModel<String>();
    	JScrollPane listFoundWords = new JScrollPane(makeListScroller(listFWords));
    	JScrollPane listLocalRanking = new JScrollPane(makeListScroller(listLRanking));
    	butSurrend = makeButton(SURREND);
    	
    	// Setting paramters for game start
    	gameStarted = false;
    	lblState = new JLabel("Waiting for other players");
    	lblTimer = new JLabel("Time Left: 420");
    	lblErrors = new JLabel("");
    	lblLetters = new JLabel("");
    	lblRanking = new JLabel("Rankings:");
    	waitingForWords = new Timer(1000, this);
    	waitingForWords.setActionCommand(TIMER);
    	timeLeft = 420; // 7 minutes to receive the words from the server
    	txtSendWord = makeText(SENDWORD);
    	txtSendWord.setEnabled(false);
    	lblLetters.setFont(new Font("Courier New", Font.CENTER_BASELINE, 16));
    	lblLetters.setForeground(Color.BLUE);
    	lblErrors.setForeground(Color.RED);
    	lblRanking.setForeground(Color.RED);
    	pnlErrors.add(lblState);
    	pnlErrors.add(lblTimer);
    	pnlErrors.add(lblErrors);
    	pnlErrors.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    	pnlErrors.setBackground(Color.GRAY);
    	
    	waitingForWords.start();
    	
    	GroupLayout layout = setLayout(screenDimension.width / 3 + 60, 100 + screenDimension.height / 3);
    	
    	// used to allign butSurrend and butHelp
    	int maxSize;
    	if (butSurrend.getPreferredSize().width > butHelp.getPreferredSize().width) 
    		maxSize = butSurrend.getPreferredSize().width;
    	else maxSize = butHelp.getPreferredSize().width;
    	
    	layout.setHorizontalGroup(layout.createSequentialGroup()
    		.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
    			.addComponent(lblLet)
    			.addComponent(lblFWords)
    			.addComponent(listFoundWords))
    		.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
    			.addComponent(lblLetters)
    			.addGroup(layout.createSequentialGroup()
    				.addComponent(separator, 3, 3, 3)
    				.addGroup(layout.createParallelGroup()
    					.addComponent(txtSendWord, 0, 2 * maxSize + 5, Short.MAX_VALUE)
    					.addComponent(butSendWord, 0, 2 * maxSize + 5, Short.MAX_VALUE)
    					.addGroup(layout.createSequentialGroup()
    						.addComponent(butHelp)
    						.addComponent(butSurrend))
    					.addComponent(pnlErrors, 0, 2 * maxSize + 5, 2 * maxSize + 5))
    				.addComponent(separator2, 3, 3, 3)))
    		.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
        		.addComponent(lblRanking)
        		.addComponent(listLocalRanking))
    	);
    	
    	layout.setVerticalGroup(layout.createSequentialGroup()
    		.addGroup(layout.createParallelGroup()
    			.addComponent(lblLet)
    			.addComponent(lblLetters))
    		.addGroup(layout.createParallelGroup()
    			.addComponent(lblRanking)
    			.addComponent(lblFWords))
    		.addGroup(layout.createParallelGroup()
    			.addComponent(listFoundWords)
    			.addComponent(separator)
    			.addGroup(layout.createSequentialGroup()
    				.addComponent(txtSendWord, 
    						txtSendWord.getPreferredSize().height, 
    						txtSendWord.getPreferredSize().height,
    						txtSendWord.getPreferredSize().height)
    				.addComponent(butSendWord)
    				.addGroup(layout.createParallelGroup()
    					.addComponent(butHelp)
    					.addComponent(butSurrend))
    				.addComponent(pnlErrors))
    			.addComponent(separator2)
    			.addComponent(listLocalRanking))
    	);

        layout.linkSize(SwingConstants.HORIZONTAL, butSurrend, butHelp);
        layout.linkSize(SwingConstants.HORIZONTAL, butSendWord, txtSendWord);

        setTitle("Playing");
        pack();
        this.setLocation((Toolkit.getDefaultToolkit().getScreenSize().width - this.getWidth()) / 2,
        		(Toolkit.getDefaultToolkit().getScreenSize().height - this.getHeight()) / 2);
        
    }
    
    
    /*
     * valueChanged and actionPerformed are action listener made to
     * perform the right action after an User made an action.
     *
     */
    
    public void valueChanged(ListSelectionEvent e) {

    	if (e.getSource() == jListInvites) {
    		if (jListInvites.getSelectedIndex() == -1) {

            	//No selection, disable buttons.
        		butAcceptInvite.setEnabled(false);
        		butDeclineInvite.setEnabled(false);
    		} else {
    			
    			//Selection, enable the buttons.
        		butAcceptInvite.setEnabled(true);
        		butDeclineInvite.setEnabled(true);
    		}
    	} else {
    		if (jListOnline == null || jListInvited == null) return;
    		if (jListOnline.getSelectedIndex() == -1 && jListInvited.getSelectedIndex() == -1) {
    			
    			//No selection, disable button.
    			butSwitch.setEnabled(false);
    		} else {

            	//Selection, enable the button.
            	if (e.getSource() == jListInvited) {
            		jListOnline.clearSelection();
            		jListInvited.setSelectedIndex(jListInvited.getSelectedIndex());
            		butSwitch.setEnabled(true);
            	}
            	else {
            		jListInvited.clearSelection();
            		jListOnline.setSelectedIndex(jListOnline.getSelectedIndex());
            		butSwitch.setEnabled(true);
            	}
    		}
    	}
    }

    
	@Override
	public void actionPerformed(ActionEvent action) {

		switch(action.getActionCommand()) {
			case LOGIN : verifyAndStartLogin(); break;
			case REGISTER : verifyAndStartRegister(); break;
			case NEWGAME : createAndShowNewGameGUI(); break;
			case ACCEPTGAME : tryToAccept(true); break;
			case DECLINEGAME : tryToAccept(false); break;
			case RANKINGS : (new RankingTask()).execute(); break;
			case LOGOUT : (new LogoutTask()).execute(); break;
			case SENDINVITES : (new SendGameRequestTask()).execute(); break;
			case CANCEL : createAndShowLobbyGUI(); break;
			case ADDALL : addAllList(); break;
			case REMOVEALL : removeAllList(); break;
			case SWITCH : switchList(); break;
			case UPDATE : update(); break;
			case SURREND : if (gameTimer != null) gameTimer.stop();
						   if (waitingForWords != null) waitingForWords.stop();
						   if (acceptTask != null) acceptTask.interrupt();
						   createAndShowLobbyGUI(); break;
			case SENDWORD : addAGameWord(); break;
			case TIMER : updateTimerWait(); break;
			case GAMETIMER : gameEnd(); break;
			case HELP : help(); break;
			default : System.out.println("Errore nella lettura del comando: comando non esistente.");
		}
	}
	
	
	/*
	 * This are self-explanatory function made to interact with 
	 * worker threads. 
	 * Some of them are made to create and start new worker threads,
	 * and some to let the GUI thread do the remaining work that
	 * a normal worker thread can't do.
	 */

	private void addAGameWord() {
		
		if (txtSendWord == null || lblLetters.getText() == null) return;
		if (lblLetters.getText().equals("")) return;
		String fWord = txtSendWord.getText();
		if (fWord.equals("")) {
			System.out.println("You should set a word!");
			lblErrors.setText("Choose a word!");
			lblErrors.setForeground(Color.RED);
			return;
		}
		if (isValidWord(fWord, lblLetters.getText())) {
			listFWords.addElement(txtSendWord.getText());
			txtSendWord.setText("");
			lblErrors.setText("Added word!");
			lblErrors.setForeground(Color.GREEN);
			
		} else {
			lblErrors.setText("Not valid word!");
			lblErrors.setForeground(Color.RED);
		}
	}
	private boolean isValidWord(String word, String base) {

		// controls if it is an already choosen word
		for (int i = 0; i < listFWords.size(); i++) {
			if (word.equals(listFWords.getElementAt(i))) return false;
		}
		
		ArrayList<String> w = new ArrayList<String>(), b = new ArrayList<String>();
		for (int j = 0; j < word.length(); j++) w.add(word.substring(j, j+1)); 
		for (int j = 0; j < base.length(); j++) b.add(base.substring(j, j+1));
		
		// Strings implements comparable
		b.sort(null);
		w.sort(null);
		
		if (b.size() < w.size()) return false;
		
		// the words single letters group must be a subset of the base single letter group so,
		// when searching for a letter in a position i in the first array, it can already start
		// at position i in the second array.
		int j = 0, i = 0;
		
		while (i < w.size() && j < b.size()) {
			if (w.get(i).equals(b.get(j))) {
				i++;
				j++;
			} else j++;
		}
		return (i == w.size());
		
	}
	private void gameEnd() {

		if (timeLeft > 0) {
			timeLeft--;
			lblState.setText("Games started!");
			lblTimer.setText("Time left: " + timeLeft);
		} else {

			gameTimer.stop();
			
			String[] words = new String[listFWords.size()];
			for (int i = 0; i < listFWords.size(); i++) words[i] = listFWords.getElementAt(i);
			
			(new SendWordsTask(words)).execute();
			lblErrors.setText("Waiting for results!");
			lblState.setText("Games ended!");
			lblTimer.setText("---");
		}
	}
	private void updateTimerWait() {
		if (!gameStarted) {
			
			if (timeLeft > 0) {

				timeLeft--;
				lblTimer.setText("Time left: " + timeLeft);
			}
			else {
				// Game cancelled; it still needs to wait until the server cancell it
				waitingForWords.stop();
			}
		}
	}
	private void update() {
		switch(state) {
			case LOGIN: break;
			case LOBBY: break;
			case NEWGAME: (new FindOnlineUsersTask()).execute(); break;
			case PLAYING: break;
			default: System.out.println("Errore: stato inconsistente.");;
		}
	}
	private void addAllList() {
		int dim = listOn.size();
		for (int i = 0; i < dim; i++) {
			listIn.addElement(listOn.getElementAt(0));
			listOn.remove(0);
		}
		butSend.setEnabled(!listIn.isEmpty());
	}
	private void removeAllList() {
		int dim = listIn.size();
		for (int i = 0; i < dim; i++) {
			listOn.addElement(listIn.getElementAt(0));
			listIn.remove(0);
		}
		butSend.setEnabled(!listIn.isEmpty());
	}
	private void switchList() {
		if (jListInvited.isSelectionEmpty()) {
			if (!jListOnline.isSelectionEmpty()) {
				String val = jListOnline.getSelectedValue();
				listOn.remove(jListOnline.getSelectedIndex());
				listIn.addElement(val);
			}
		} else {
			if (jListOnline.isSelectionEmpty()) {
				String val = jListInvited.getSelectedValue();
				listIn.remove(jListInvited.getSelectedIndex());
				listOn.addElement(val);
			}
		}
		butSend.setEnabled(!listIn.isEmpty());
	}
	private void verifyAndStartLogin() {
		
		if (!state.equals(GUI_state.LOGIN)) state = GUI_state.LOGIN;
		if (txtUsername == null || txtUsername.getText().equals("")) {
			System.out.println("Devi inserire un username!");
			JOptionPane.showMessageDialog(this,
				    "You must choose an username!",
				    "Missing Username",
				    JOptionPane.ERROR_MESSAGE);
		}
		else if (txtPassword == null || txtPassword.getPassword().length == 0) {
			System.out.println("Devi inserire una password!");
			JOptionPane.showMessageDialog(this,
				    "You must choose a password!",
				    "Missing Password",
				    JOptionPane.ERROR_MESSAGE);
			
		}
		else {
			password = String.valueOf(txtPassword.getPassword());
			username = txtUsername.getText();
			txtUsername.setText("");
			txtPassword.setText("");
			(new LoginTask()).execute();
		}
	}
	private void verifyAndStartRegister() {

		if (!state.equals(GUI_state.LOGIN)) state = GUI_state.LOGIN;
		if (txtUsername == null || txtUsername.getText().equals("")) {
			System.out.println("Devi inserire un username!");
			JOptionPane.showMessageDialog(this,
				    "You must choose an username!",
				    "Missing Username",
				    JOptionPane.ERROR_MESSAGE);
		}
		else if (txtPassword == null || txtPassword.getPassword().length == 0) {
			System.out.println("Devi inserire una password!");
			JOptionPane.showMessageDialog(this,
				    "You must choose a password!",
				    "Missing Password",
				    JOptionPane.ERROR_MESSAGE);
		}
		else {
			password = String.valueOf(txtPassword.getPassword());
			username = txtUsername.getText();
			txtUsername.setText("");
			txtPassword.setText("");
			(new RegisterTask()).execute();
		}
	}
	private void tryToAccept(boolean accept) {

		if (jListInvites.isSelectionEmpty()) {
			System.out.println("Nessun oggetto selezionato!");
			JOptionPane.showMessageDialog(this,
				    "You must select an object!",
				    "No object selected",
				    JOptionPane.ERROR_MESSAGE);
		} else {
			
			if (accept) {
				
				/*
				 * Accept the game:
				 * erase every element on game invite list,
				 * and prepare an array to tell wich gameID
				 * he's refusing by accepting this game
				 * 
				 */
				
				currentPlayingGame = jListInvites.getSelectedValue();
				
				listInv.remove(jListInvites.getSelectedIndex());

				int[] refusedGames = new int[listInv.size()];

				for (int i = 0; i < listInv.size(); i++) {
					refusedGames[i] = listInv.getElementAt(i).getID();
				}
				listInv.removeAllElements();

				acceptTask = new AcceptGameTask(accept, refusedGames);
				acceptTask.execute();
				
				createAndShowGameGUI();
				
			} else {
				
				/*
				 * Refusing the game:
				 * erase the refused game from game invite list,
				 * sets the refuse array to this only element.
				 * 
				 */
				int[] refusedGame = new int[1];
				
				refusedGame[0] = listInv.getElementAt(jListInvited.getSelectedIndex()).getID();
				listInv.remove(jListInvites.getSelectedIndex());

				acceptTask = new AcceptGameTask(accept, refusedGame);
				acceptTask.execute();
				
			}
		}
	}
	private void help() {

		if (state.equals(GUI_state.LOBBY)) {
			JOptionPane.showMessageDialog(this, 
					"This is the Lobby interface.                 " + System.lineSeparator() +
					"From here you can:                           " + System.lineSeparator() +
					"- Create a new game, inviting online players," + System.lineSeparator() + 
					"- Accept  a  game  invite from other players," + System.lineSeparator() +
					"- Refuse  a  game  invite from other players," + System.lineSeparator() + 
					"- Update  the  Rankings and see you position," + System.lineSeparator() +
					"- Logout."       +      System.lineSeparator() + System.lineSeparator() +
					"- Creating   and   accepting   a   game  will" + System.lineSeparator() +
					"	automatically   refuse   all  the  others." + System.lineSeparator(),
					"Help", 
					JOptionPane.INFORMATION_MESSAGE);
		} else if (state.equals(GUI_state.PLAYING)){
			JOptionPane.showMessageDialog(this, 
					"This  is  the interface of a game session.   " + System.lineSeparator() +
					"From here you can play your game:            " + System.lineSeparator() +
					"- Write  a word and press Send to send it,   " + System.lineSeparator() +
					"- You  can't send the same word two times,   " + System.lineSeparator() +
					"- You  should  use only the given letters,   " + System.lineSeparator() + 
					"- After  the  end of the timer, the result   " + System.lineSeparator() +
					"    will  be  sent  and shown in rankings,   " + System.lineSeparator() +
					"- You  can  surrend  by  pressing Surrend;   " + System.lineSeparator() +
					"    By  surrending  the game will continue   " + System.lineSeparator() +
					"    for  other  players  and you can still   " + System.lineSeparator() + 
					"    and you can find your result in global   " + System.lineSeparator() +
					"    rankings.",
					"Help", 
					JOptionPane.INFORMATION_MESSAGE);
		}
		
	}
	private Socket openConnection() throws IOException {
		Socket socket = null;
		socket = new Socket(REGISTRY_HOST, TCP_PORT);
		return socket;
	}
	private void closeConnection(Socket socket, ObjectOutputStream out, ObjectInputStream in) 
			throws IOException {
		socket.close();
		out.close();
		in.close();
	}
	public void addGameRequest(String creator, int gameID, int multicastPort) {

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
            	
            	// can't accept games request while not online
            	if (!state.equals(GUI_state.LOBBY)) {
            		
            		return;
            	}
            	
            	GamesData game = new GamesData(creator, gameID, multicastPort);
            	games.addGame(game);
            	listInv.addElement(game);
            }
        });
	}
	public void removeGameRequest(int gameID) {

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
            	GamesData game = games.getGameByID(gameID);
            	games.removeGame(game);
            	listInv.removeElement(game);
            }
        });
	}
	private void sendAllertMessage(String mex, String label, int err) {

		JOptionPane.showMessageDialog(this, mex, label, err);
	}
	private void sendCloseMessage() {

        if (JOptionPane.showConfirmDialog(this, "Are you sure ?") == JOptionPane.OK_OPTION){
            this.setVisible(false);
            try {
            	if (username != null && password != null) serverRMI.logout(username, password);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
            System.exit(0);
            
        }
	}
	
	
	private class ClosingOp implements WindowListener {

		// USED TO ASK FOR CLOSING
		
		@Override
		public void windowClosed(WindowEvent e) {}
		
		@Override
		public void windowActivated(WindowEvent e) {}

		@Override
		public void windowClosing(WindowEvent e) {
			sendCloseMessage();
		}

		@Override
		public void windowDeactivated(WindowEvent e) {}

		@Override
		public void windowDeiconified(WindowEvent e) {}

		@Override
		public void windowIconified(WindowEvent e) {}

		@Override
		public void windowOpened(WindowEvent e) {}
		
	}
	
	
	private class LoginTask extends SwingWorker<Void, Void> {

		private int result = -3;
		
		@Override
		protected Void doInBackground() {

			// comunicate with server: tries to login with RMI
			// and gives him a callback for game invites
			
			try {
				result = serverRMI.login(username, password, userStub);
			} catch (RemoteException e) {
				result = -4;
			}
			return null;
		}
		
		@Override
	    protected void done() {
			
			if (result >= 0) {
				System.out.println("Login avvenuto con sucesso");
				sessionID = result;

				createAndShowLobbyGUI();
			} else if (result == -1) {

				sendAllertMessage("You chose the wrong password!",
					    	"Wrong Password",
					    	JOptionPane.ERROR_MESSAGE);
				
			} else if (result == -2) {

				sendAllertMessage("This Account doens't Exist!",
					    	"Account non existent",
					    	JOptionPane.ERROR_MESSAGE);
				
			} else if (result == -3) {

				sendAllertMessage("This user is already logged in!",
					    	"Already Logged User",
					    	JOptionPane.ERROR_MESSAGE);
			} else if (result == -4) {

				sendAllertMessage("Comunication Error; impossible to login!",
					    	"Error",
					    	JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	
	private class RegisterTask extends SwingWorker<Void, Void> {

		private boolean success = false;
		
		@Override
		protected Void doInBackground() {

			// comunicates with server: tries to register with RMI
			try {
				success = serverRMI.register(username, password);
			} catch (RemoteException e) {
				
			}
			
			return null;
		}
		
		@Override
	    protected void done() {

			if (success) {
				
				System.out.println("Registrazione avvenuta con sucesso");
				// starts login automatically
				
				(new LoginTask()).execute();
			} else {

				sendAllertMessage("This account already exist!",
					    	"Account already existent",
					    	JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	

	private class RankingTask extends SwingWorker<Multicast_rankings, Void> {

		private boolean success = false;
		
		@Override
		protected Multicast_rankings doInBackground() {
			
			Multicast_rankings rankings = null;
			try {
				
				// TCP comunication to get rankings
				Socket socket = openConnection();
				ObjectOutputStream writer = new ObjectOutputStream (socket.getOutputStream());
				ObjectInputStream reader = new ObjectInputStream (socket.getInputStream());
				
				writer.writeInt(2);
				writer.flush();
				
				rankings = (Multicast_rankings) reader.readObject();
				
				success = true;
				
				closeConnection(socket, writer, reader);
				
			} catch (IOException e) { // to ignore errors in socket closing
			} catch (ClassNotFoundException e) {
			}
			return rankings;
		}
		
		@Override
	    protected void done() {
			
			if (success && state.equals(GUI_state.LOBBY)) {
				
				try {
					
					Multicast_rankings rankings = get();
					
					System.out.println("Lettura classifica avvenuta con successo");
					
					String[] rankEl = rankings.getStringElements();
					listRank.clear();
					for (int i = 0; i < rankEl.length; i++) 
						listRank.addElement((i+1) + "°: " + rankEl[i]);
					
					
				} catch (InterruptedException | ExecutionException e) {
					System.out.println("Impossibile leggere la classifica");
					e.printStackTrace();
				}
			} else {
				
				sendAllertMessage("Comunication Error; impossible to read rankings!",
				    	"Error",
				    	JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	
	private class LogoutTask extends SwingWorker<Void, Void> {

		private boolean success = false;
		
		@Override
		protected Void doInBackground() {

			// tries to logout via RMI
			try {
				success = 0 == serverRMI.logout(username, password);
			} catch (RemoteException e) {
				System.out.println("Impossibile effettuare il logout."
						+ " Persa la connessione col Server.");
				
			}
			
			return null;
		}

		@Override
	    protected void done() {

			if (success) {
				System.out.println("Logout successful");
				username = "";
				password = "";
				sessionID = 0;
				createAndShowLoginGUI();
			} else {

				System.out.println("Internal error: cannot logout");
				// It still logout 
				username = "";
				password = "";
				sessionID = 0;
				sendAllertMessage("Errors doing Logout", 
						"Error with Server", 
						JOptionPane.ERROR_MESSAGE);
				createAndShowLoginGUI();
			}
		}
	}
	 
	
	private class AcceptGameTask extends SwingWorker<Void, Void> {

		private boolean accepted;
		private int[] refusedGame;
		private boolean success = false;
		private String word = null;
		Socket socket;
		
		AcceptGameTask(boolean accepted, int[] refusedGame) {
			this.accepted = accepted;
			this.refusedGame = refusedGame;
		}
		
		@Override
		protected Void doInBackground() {
			
			try {
					
				/* 
				 * Use TCP comunication to answer a game.
				 * 
				 * This method could be invoked for 3 different reasons:
				 * 1 - an user accepted a game and want to warn the server
				 * 		(and refuse all the other games he's been invited)
				 * 
				 * 2 - an user refused a game and want to warn the server
				 * 
				 * 3 - an user created a game, and want to refuse all the 
				 * 		game invited.
				 * 
				 * In any case, some other games may be refused.
				 * 
				 */
				
				socket = openConnection();
				ObjectOutputStream writer = new ObjectOutputStream (socket.getOutputStream());
				ObjectInputStream reader = new ObjectInputStream (socket.getInputStream());
	
				writer.writeInt(1);
				writer.writeUTF(username);
				writer.writeInt(sessionID);
				writer.writeInt(currentPlayingGame.getID());
				writer.writeBoolean(accepted);
				
				int len = 0;
				if (refusedGame != null) len = refusedGame.length;
				
				writer.writeInt(len);
				
				for (int i = 0; i < len; i++) {
					writer.writeInt(refusedGame[i]);
				}

				// if he refused the game, the server automatically closes the connection.
				writer.flush();
				switch (reader.readInt()) {
					case -1: 
					case -2: 
					case -3: return null;
					case 0: 
						
						try {
							word = reader.readUTF();
							success = true;
						} catch (IOException e) {

							// If it arrives here, the server annulled the game.
							success = false;
						} 
						
						break;
					default:;
				}

				closeConnection(socket, writer, reader);
				
			} catch (IOException e) {
			}
			
			return null;
		}
		

		@Override
	    protected void done() {

			if (waitingForWords != null) waitingForWords.stop();
			if (success) startGame(word);
			else {
				System.out.println("Partita annullata");
				sendAllertMessage("The game has been canceled!",
				    	"canceled game",
				    	JOptionPane.ERROR_MESSAGE);
				
				createAndShowLobbyGUI();
			}
		}
		
		public void interrupt() {
			try {
				if (socket != null) socket.close();
			} catch (IOException e) {
				// il socket non si può chiudere o è già chiuso
			}
		}
	}
	private void startGame(String word) {

		lblLetters.setText(word);
		txtSendWord.setEnabled(true);
		gameTimer = new Timer(1000, this);
		gameTimer.setActionCommand(GAMETIMER);
		gameTimer.start();
		timeLeft = 10;
		gameStarted = true;
	}
	
	
	private class FindOnlineUsersTask extends SwingWorker<ArrayList<String>, Void> {

		@Override
		protected ArrayList<String> doInBackground() {
			
			ArrayList<String> onlineUsers = null;
        
			try {
				onlineUsers = serverRMI.requestOnlineUsers();
			} catch (RemoteException e) {
				System.out.println("Impossibile accedere alla lista degli utenti online");
				e.printStackTrace();
			}
			
			return onlineUsers;
		}

		@Override
	    protected void done() {

			System.out.println("Refresh avvenuto con successo");
			try {
				ArrayList<String> onlineUsers = get();
				if (onlineUsers != null) {
					listOn.removeAllElements();
					listIn.removeAllElements();
					for (String user: onlineUsers) if (!user.equals(username))listOn.addElement(user);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	private class SendGameRequestTask extends SwingWorker<Void, Void> {

		private boolean success = false;
		
		@Override
		protected Void doInBackground() {

			try {
				
				// Communicates with server with TCP to create a new game.
				// After the communication, if it went right, it accepts
				// automatically the game request from the server.
				
				Socket socket = openConnection();
				ObjectOutputStream writer = new ObjectOutputStream (socket.getOutputStream());
				ObjectInputStream reader = new ObjectInputStream (socket.getInputStream());
				
				writer.writeInt(0);
				writer.writeInt(listIn.size());
				writer.writeUTF(username);
				writer.writeInt(sessionID);
				for (int i = 0; i < listIn.size(); i++) {
					writer.writeUTF(listIn.getElementAt(i));
				}
				writer.flush();
				
				if (success = reader.readBoolean()) {
					int gameID = reader.readInt();
					int gamePort = reader.readInt();
					currentPlayingGame = new GamesData(username, gameID, gamePort);

					closeConnection(socket, writer, reader);
				}
				
				System.out.println("Invio dati server");

			} catch (IOException e) {
				System.out.println("errore nell'invio della lista di inviti al server");
			}

			return null;
		}
		
		@Override
	    protected void done() {

			if (success) {
				
				System.out.println("invio richiesta partita avvenuto con successo");

				callAcceptGame();
				createAndShowGameGUI(); 
			}
		}
		
	}
	private void callAcceptGame() {
		
		int[] refusedGames = new int[listInv.size()];
		
		for (int i = 0; i < listInv.size(); i++) {
			refusedGames[i] = listInv.getElementAt(i).getID();
		}
		
		listInv.removeAllElements();
		acceptTask = new AcceptGameTask(true, refusedGames);
		acceptTask.execute();
	}
	
	
	private class SendWordsTask extends SwingWorker<Void, Void> {

		String[] words;
		
		SendWordsTask(String[] words) {
			this.words = words;
		}
		
		@Override
		protected Void doInBackground() {
			
			try {

				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				ObjectOutputStream os = new ObjectOutputStream(outputStream);
				// data dimension is limited by 1'000'000 bytes.
				byte[] data = null;
				UDP_words sendWords = new UDP_words(words, currentPlayingGame.getID(), username);
				os.writeObject(sendWords);
				data = outputStream.toByteArray();

				DatagramPacket sendPacket = new DatagramPacket(data, data.length, serverAddress, UDP_PORT);
				dataSocket.send(sendPacket);
				
				System.out.println("Pacchetto UDP inviato con le parole scelte");
				
				System.out.println("Avvio il task per la lettura multicast dei risultati");
				
				(new ReadMulticastRankings()).execute();
				
				
			} catch (IOException e) {
				System.out.println("Errore nell'invio delle parole.");
				e.printStackTrace();
			}
			
			return null;
		}
		
		@Override
		protected void done() {
			
			txtSendWord.setEnabled(false);
			butSendWord.setEnabled(false);
		}
	}
	
	
	private class ReadMulticastRankings extends SwingWorker<Multicast_rankings, Void> {
		
		@Override
		protected Multicast_rankings doInBackground() {

			// rankings data is limited by 1'000'000 bytes.
			byte [ ] date = new byte[10000000];
			Multicast_rankings rankings = null;
			
			try {
				DatagramPacket dp = new DatagramPacket (date, date.length);
				MulticastSocket ms = new MulticastSocket (currentPlayingGame.getPort());
				ms.joinGroup(multicastAddress);
				
				ms.receive(dp);

				ByteArrayInputStream in = new ByteArrayInputStream(dp.getData());
				ObjectInputStream is = new ObjectInputStream(in);
				
				
				rankings = (Multicast_rankings) is.readObject();
				
				ms.close();
				in.close();
				
			} catch(IOException e) {
				
				System.out.println("Errore di comunicazione, impossibile leggere la classifica");
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				
				System.out.println("Errore nella conversione dell'oggetto della classe specificata!");
				e.printStackTrace();
			}
			
			return rankings;
		}
		
		@Override
	    protected void done() {
			
			Multicast_rankings rankings = null;
			try {
				rankings = get();
			} catch (InterruptedException | ExecutionException e) {
				// Errors in reading rankings
				lblRanking.setForeground(Color.gray);
				lblErrors.setText("Errore nella ricezione della classifica!");
				butSurrend.setText("    Exit    ");
				pack();
			}
			if (rankings != null) {
				
				// Adds the elements to the list after the comunication with server
				System.out.println("Lettura classifica avvenuta con successo");
				lblRanking.setForeground(Color.blue);
				lblErrors.setText("Classifica arrivata!");
				lblErrors.setForeground(Color.BLUE);
				butSurrend.setText("    Exit    ");
				
				pack();

				String[] elements = rankings.getStringElements();
				String userPosition = "-- :";
				listLRanking.clear();
				int pos = 1;
				for (String el: elements) {
					listLRanking.addElement(pos + "°: " + el);
					if (el.split(" ")[0].equals(username)) userPosition = pos + "°!";
					pos++;
				}
				sendAllertMessage(
						"You arrived " + userPosition, 
						"Congratulations!", 
						JOptionPane.INFORMATION_MESSAGE);

			}
		}
	}
}
