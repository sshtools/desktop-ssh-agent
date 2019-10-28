/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Desktop SSH Agent.
 *
 * Desktop SSH Agent is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Desktop SSH Agent is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Desktop SSH Agent.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.mobile.agent.swt;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class LoginForm extends SafeThreadedForm {
	
	Label label1, label2;
	Text username;
	Text password;
	Text text;
	boolean ret;
	boolean showUsername;
	String message;
	
	String theUsername;
	String thePassword;
	
	public LoginForm(Display display, boolean showUsername, String message) {
		this.showUsername = showUsername;
		this.message = message;
		setupShell(display);
	}
	
	public boolean isSuccess() {
		return ret;
	}
	
	public String getUsername() {
		return theUsername;
	}
	
	public String getPassword() {
		return thePassword;
	}
	
	public boolean logon() {
		
		executeThreadSafe(new Runnable() {
			public void run() {
				shell.open();

				while (!shell.isDisposed()) {
					if (!shell.getDisplay().readAndDispatch()) {
						shell.getDisplay().sleep();
					}
				}
			}
		});

		return ret;
	}

	public static void main(String[] args) {
		new LoginForm(Display.getDefault(), false, "You need to supply your password in order to delete these keys.").logon();
	}

	@Override
	protected Shell createShell(Display display) {
		
		Shell shell = new Shell(display, SWT.TITLE | SWT.PRIMARY_MODAL | SWT.DIALOG_TRIM | SWT.CLOSE);
		shell.setLayout(new GridLayout(1, false));
		shell.setText("Login");

		Label m = new Label(shell, SWT.NULL);
		m.setText(message);
		
		if(showUsername) {
			label1 = new Label(shell, SWT.NULL);
			label1.setText("Username: ");
	
			username = new Text(shell, SWT.SINGLE | SWT.BORDER);
			username.setText("");
			username.setTextLimit(255);
			username.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}
		label2 = new Label(shell, SWT.NULL);
		label2.setText("Password: ");

		password = new Text(shell, SWT.SINGLE | SWT.BORDER);
		password.setEchoChar('*');
		password.setTextLimit(255);
		password.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Button button = new Button(shell, SWT.PUSH);
		button.setText("Submit");
		shell.setDefaultButton(button);
		button.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				
				if(showUsername && StringUtils.isBlank(username.getText())) {
					MessageBox messageBox = new MessageBox(shell, SWT.OK |
							   SWT.ICON_WARNING | SWT.ON_TOP);
							  messageBox.setMessage("Please enter a valid username!");
							  messageBox.open();
					return;
				}
				
				if(showUsername) {
					theUsername = username.getText();
				}
				if(StringUtils.isBlank(password.getText())) {
					MessageBox messageBox = new MessageBox(shell, SWT.OK |
							   SWT.ICON_WARNING | SWT.ON_TOP);
							  messageBox.setMessage("Please enter a valid password!");
							  messageBox.open();
					return;
				}
				
				thePassword = password.getText();
				
				ret = true;
				shell.dispose();
			}
		});
		
		
		button.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		
		shell.setBounds((shell.getDisplay().getBounds().width / 2) - 200 ,
				(shell.getDisplay().getBounds().height / 2) - 200, 400, 400);
		shell.pack();
		
		return shell;
	}
}