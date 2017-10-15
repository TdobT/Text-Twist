package clientPackage;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class GUI_state {

	public enum GUI_stat {
		LOGIN, LOBBY, PLAYING
	}
	
	GUI_stat currentState;
	JFrame loginGUI;
	
	GUI_state() {
		run_GUI();
	}
	

    /**
     * Create the login GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event dispatch thread.
     */
    private void createAndShowLoginGUI() {
        //Create and set up the window.
        loginGUI = new JFrame("Login / Register");
        loginGUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        final Login newContentPane = new Login(loginGUI);
        newContentPane.setOpaque(true); //content panes must be opaque
        loginGUI.setContentPane(newContentPane);

        //Make sure the focus goes to the right component
        //whenever the frame is initially given the focus.
        loginGUI.addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent e) {
                newContentPane.resetFocus();
            }
        });

        //Display the window.
        loginGUI.pack();
        loginGUI.setVisible(true);
    }

	
	private void run_GUI() {

		currentState = GUI_stat.LOGIN;
		
		//Schedule a job for the event dispatch thread:
		//creating and showing this application's GUI.
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				//Turn off metal's use of bold fonts
				UIManager.put("swing.boldMetal", Boolean.FALSE);
				createAndShowLoginGUI();
			}
		});

	}
	


    public static void main(String[] args) {

    	GUI_state gui = new GUI_state();
    }
}
