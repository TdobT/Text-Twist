package clientPackage;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;

public class Login extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;
	private static String OK = "ok";
    private static String HELP = "help";

    private JFrame controllingFrame; //needed for dialogs
    private JPasswordField passwordField;
    private JTextField usernameField;

    public Login(JFrame f) {
        controllingFrame = f;

        //Create everything.

        //A border that puts 10 extra pixels at the sides and
        //bottom of each pane.
        Border paneEdge = BorderFactory.createEmptyBorder(0,10,10,10);
        Border loweredetched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);

        //First pane: simple borders
        JPanel simpleBorders = new JPanel(new GridLayout(0, 2));
        
        
        usernameField = new JTextField(10);
        usernameField.setActionCommand(OK);
        usernameField.addActionListener(this);
        
        passwordField = new JPasswordField(10);
        passwordField.setActionCommand(OK);
        passwordField.addActionListener(this);

        JLabel userLabel = new JLabel("Enter the username: ");
        userLabel.setLabelFor(usernameField);
        
        JLabel passLabel = new JLabel("Enter the password: ");
        passLabel.setLabelFor(passwordField);

        JComponent buttonPane = createButtonPanel();

        JPanel textPane = new JPanel(new GridLayout(0, 2));
        textPane.add(userLabel);
        textPane.add(usernameField);
        textPane.add(passLabel);
        textPane.add(passwordField);

        simpleBorders.add(textPane);
        simpleBorders.add(buttonPane);
        simpleBorders.setBorder(loweredetched);
        //simpleBorders.set
        
        
        add(simpleBorders);
    }


    protected JComponent createButtonPanel() {
        JPanel p = new JPanel(new GridLayout(0,1));
        JButton okButton = new JButton("OK");
        JButton helpButton = new JButton("Help");

        okButton.setActionCommand(OK);
        helpButton.setActionCommand(HELP);
        okButton.addActionListener(this);
        helpButton.addActionListener(this);

        p.add(okButton);
        p.add(helpButton);

        return p;
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

        if (OK.equals(cmd)) { //Process the password.
            char[] input = passwordField.getPassword();
            if (isPasswordCorrect(input)) {
                JOptionPane.showMessageDialog(controllingFrame,
                    "Success! You typed the right password.");
            } else {
                JOptionPane.showMessageDialog(controllingFrame,
                    "Invalid password. Try again.",
                    "Error Message",
                    JOptionPane.ERROR_MESSAGE);
            }

            //Zero out the possible password, for security.
            Arrays.fill(input, '0');

            passwordField.selectAll();
            resetFocus();
        } else { //The user has asked for help.
            JOptionPane.showMessageDialog(controllingFrame,
                "You can get the password by searching this example's\n"
              + "source code for the string \"correctPassword\".\n"
              + "Or look at the section How to Use Password Fields in\n"
              + "the components section of The Java Tutorial.");
        }
    }

    /**
     * Checks the passed-in array against the correct password.
     * After this method returns, you should invoke eraseArray
     * on the passed-in array.
     */
    private static boolean isPasswordCorrect(char[] input) {
        boolean isCorrect = true;
        char[] correctPassword = { 'b', 'u', 'g', 'a', 'b', 'o', 'o' };

        if (input.length != correctPassword.length) {
            isCorrect = false;
        } else {
            isCorrect = Arrays.equals (input, correctPassword);
        }

        //Zero out the password.
        Arrays.fill(correctPassword,'0');

        return isCorrect;
    }

    //Must be called from the event dispatch thread.
    protected void resetFocus() {
        usernameField.requestFocusInWindow();
    }

}

