/**
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
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
package com.sshtools.desktop.agent.swt;

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

public class InputForm extends SafeThreadedForm {
	
	Label label1, label2;
	Text inputField;
	Text text;
	boolean ret;
	String theInput;
	
	String title;
	String message;
	String defaultValue;
	boolean isPassword;
	
	public InputForm(Display display, String title, String message, String defaultValue, boolean isPassword) {
		
		this.title = title;
		this.message = message;
		this.defaultValue = defaultValue;
		this.isPassword = isPassword;
		
		setupShell(display);
	}
	
	public boolean isSuccess() {
		return ret;
	}
	
	public String getInput() {
		return theInput;
	}
	
	public boolean show() {
		
		executeThreadSafe(new Runnable() {
			public void run() {
				SWTUtil.center(shell);
				shell.open();
				shell.forceActive();
				
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
		new InputForm(Display.getDefault(), "Test", "Please enter a name to identify this key.", "id_dsa", false).show();
	}

	@Override
	protected Shell createShell(Display display) {
		
		Shell shell = new Shell(display, SWT.TITLE | SWT.PRIMARY_MODAL | SWT.DIALOG_TRIM | SWT.CLOSE);
		shell.setLayout(new GridLayout(1, false));
		shell.setText(title);

		buildComponents(shell);

		Button button = new Button(shell, SWT.PUSH);
		button.setText("Submit");
		shell.setDefaultButton(button);
		button.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				
			
				if(StringUtils.isBlank(inputField.getText())) {
					MessageBox messageBox = new MessageBox(shell, SWT.OK |
							   SWT.ICON_WARNING | SWT.ON_TOP);
							  messageBox.setMessage("Please enter a valid password!");
							  messageBox.open();
					return;
				}
				
				theInput = inputField.getText();
				onCaptureInput();
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

	protected void onCaptureInput() { }
	protected void buildComponents(Shell shell) {
		
		Label m = new Label(shell, SWT.NULL);
		m.setText(message);

		inputField = new Text(shell, SWT.SINGLE | SWT.BORDER);
		if(isPassword) {
			inputField.setEchoChar('*');
		}
		inputField.setTextLimit(255);
		inputField.setText(defaultValue);
		inputField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}
}