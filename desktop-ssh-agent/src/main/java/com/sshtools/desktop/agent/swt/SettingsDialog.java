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

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.desktop.agent.DesktopAgent;
import com.sshtools.desktop.agent.Settings;
import com.sshtools.desktop.agent.Settings.IconMode;
import com.sshtools.desktop.agent.sshteam.SshTeamHelper;

public class SettingsDialog extends Dialog {

	
	final String SAVE_BUTTON = "Save";
	
	Shell shell;
	Text terminalCommand;
	Text terminalArguments; 
	Button builtInTerminal;
	Button keyWizard;
	Combo iconMode;
	Text username;
	Text deviceName;
	Text hostname;
	Text port;
	Button strictSSL;
	Button authorize;
	Button synchronizeKeys;
	
	Text logonboxUsername;
	Text logonboxDomain;
	Text logonboxPort;
	
	DesktopAgent agent;
	
	public SettingsDialog(Display parent, DesktopAgent agent) {
		this(parent, 0, agent);
	}
	
	public SettingsDialog(Display parent, int style, DesktopAgent agent) {
		super(new Shell(parent), SWT.DIALOG_TRIM | style);
        shell = getParent();
        shell.setText("Preferences");
		shell.setImage(new Image(parent, Image.class.getResourceAsStream("/new_icon.png")));
		this.agent = agent;
	}

	public void open()
    {
        shell.setText(getText());
        createContents(shell);
        shell.pack();
        
        Point size = shell.computeSize(-1, -1);
        Rectangle screen = shell.getDisplay().getMonitors()[0].getBounds();
        shell.setBounds((screen.width-size.x)/2, (screen.height-size.y)/2, size.x, size.y);
        
        shell.open();
        shell.forceActive();
        
        Display display = getParent().getDisplay();
        while (!shell.isDisposed())
        {
            if (!display.readAndDispatch())
            {
                display.sleep();
            }
        }
    }
	
	protected void createContents(final Shell shell) {
		
        shell.setLayout(new GridLayout(1, true));
        
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.widthHint = 600;
    
		final TabFolder tabFolder = new TabFolder(shell, SWT.NONE);
		tabFolder.setLayoutData(data);
		
		TabItem tabTerminal = new TabItem(tabFolder, SWT.NONE);
		tabTerminal.setText("Terminal");
		tabTerminal.setControl(new TerminalPreferencePanel(tabFolder));
	
		TabItem tabAccount = new TabItem(tabFolder, SWT.NONE);
		tabAccount.setText("Key Management");
		tabAccount.setControl(new AccountPreferencePanel(tabFolder));
		
		TabItem tabLogonbox = new TabItem(tabFolder, SWT.NONE);
		tabLogonbox.setText("LogonBox Authenticator");
		tabLogonbox.setControl(new LogonBoxPreferencePanel(tabFolder));
		
		TabItem tabUI = new TabItem(tabFolder, SWT.NONE);
		tabUI.setText("Options");
		tabUI.setControl(new UserInterfacePreferencePanel(tabFolder));
		
		data = new GridData(GridData.HORIZONTAL_ALIGN_END);
		Button b = new Button(shell, SWT.PUSH);
		b.setText("Save");
		b.setLayoutData(data);
		
		b.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent event)
            {
                saveSettings();
                shell.dispose();
            }
        });
	}
	
	protected void saveSettings() {
		
		try {
			Settings.getInstance().setUseBuiltInTerminal(builtInTerminal.getSelection());
			Settings.getInstance().setIconMode(IconMode.values()[iconMode.getSelectionIndex()]);
			Settings.getInstance().setTerminalCommand(terminalCommand.getText());
			Settings.getInstance().setTerminalArguments(terminalArguments.getText());
			Settings.getInstance().setSynchronizeKeys(synchronizeKeys.getSelection());
			
			Settings.getInstance().setLogonboxDomain(logonboxDomain.getText());
			Settings.getInstance().setLogonboxUsername(logonboxUsername.getText());
			Settings.getInstance().setLogonboxPort(Integer.parseInt(logonboxPort.getText()));
			
			Settings.getInstance().setSshteamDomain(hostname.getText());
			Settings.getInstance().setSshteamUsername(username.getText());
			Settings.getInstance().setSshteamPort(Integer.parseInt(port.getText()));
			
			Settings.getInstance().setStrictSSL(!strictSSL.getSelection());
			Settings.getInstance().save();
			

			agent.resetIcon();
			
			if(Settings.getInstance().isSynchronizeKeys()) {
				Collection<SshPublicKey> results = SshTeamHelper.verifyAccess(Settings.getInstance().getSshteamUsername(), 
						Settings.getInstance().getSshteamDomain(),
						Settings.getInstance().getSshteamPort(), agent.getLocalKeyStore());
				
				if(results.isEmpty()) {
					SWTUtil.showInformation("Desktop Agent", 
							"To start synchronization you must upload one of the public keys from this agent to your ssh.team account");
				}
			}
		} catch (Throwable e) {
			SWTUtil.showError("Preferences", 
					String.format("Could not save preferences!\r\n%s",e.getMessage()));
		}
	}

	class AccountPreferencePanel extends Composite {
	
		public AccountPreferencePanel(Composite c) {
			super(c, SWT.NO_BACKGROUND);
		    GridLayout layout = new GridLayout(1, true);
		    layout.marginBottom = layout.marginTop = layout.marginLeft = layout.marginRight = 8;
			this.setLayout(layout);
			
			Link linkButton = new Link(this, SWT.WRAP);
//			Connect your <a href=\"https://jadaptive.com/app/manpage/en/article/3472779\">LogonBox Authenticator</a> app to use LogonBox credentials to log into your SSH services.
			linkButton.setText("Connect to a <a href=\"https://ssh.team/\">ssh.team</a> domain to synchronize your keys with your key management account.");
			linkButton.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					try {
						Desktop.getDesktop().browse(new URI("https://ssh.team/"));
					} catch (IOException | URISyntaxException e1) {
					}
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
				}
				
			});
			
			new Label(this, SWT.NONE).setText("To push keys from this agent to your key server ensure you have at least one public key");
			new Label(this, SWT.NONE).setText("configured on the server from your agents private key list.");
			new Label(this, SWT.NONE).setText("");
			new Label(this, SWT.NONE).setText("Account Name");
		    
		    username = new Text(this, SWT.SINGLE | SWT.BORDER);
		    username.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		    username.setText(StringUtils.defaultString(Settings.getInstance().getSshteamUsername()));
		    
		    new Label(this, SWT.NONE).setText("Hostname");
		    
		    hostname = new Text(this, SWT.SINGLE | SWT.BORDER);
		    hostname.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		    hostname.setText(StringUtils.defaultString(Settings.getInstance().getSshteamDomain()));
		    
		    new Label(this, SWT.NONE).setText("Port");
		    
		    port = new Text(this, SWT.SINGLE | SWT.BORDER);
		    port.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		    port.setText(String.valueOf(Settings.getInstance().getSshteamPort()));
		    new Label(this, SWT.NONE);
		    
		    synchronizeKeys = new Button(this, SWT.CHECK);
		    synchronizeKeys.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		    synchronizeKeys.setSelection(Settings.getInstance().isSynchronizeKeys());
		    synchronizeKeys.setText("Synchronize public keys with your ssh.team domain.");
		    new Label(this, SWT.NONE);
		   
		}
	}
	
	class LogonBoxPreferencePanel extends Composite {
		
		public LogonBoxPreferencePanel(Composite c) {
			super(c, SWT.NO_BACKGROUND);
		    GridLayout layout = new GridLayout(1, true);
		    layout.marginBottom = layout.marginTop = layout.marginLeft = layout.marginRight = 8;
			this.setLayout(layout);
			
			Link linkButton = new Link(this, SWT.WRAP);
			linkButton.setText("Connect your <a href=\"https://jadaptive.com/app/manpage/en/article/3472779\">LogonBox Authenticator</a> app to use LogonBox credentials to log into your SSH services.");
			linkButton.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					try {
						Desktop.getDesktop().browse(new URI("https://jadaptive.com/app/manpage/en/article/3472779"));
					} catch (IOException | URISyntaxException e1) {
					}
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
				}
				
			});
			
			new Label(this, SWT.NONE).setText("Account Name");
		    
		    logonboxUsername = new Text(this, SWT.SINGLE | SWT.BORDER);
		    logonboxUsername.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		    logonboxUsername.setText(StringUtils.defaultString(Settings.getInstance().getLogonboxUsername()));
		    
		    new Label(this, SWT.NONE).setText("Hostname");
		    
		    logonboxDomain = new Text(this, SWT.SINGLE | SWT.BORDER);
		    logonboxDomain.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		    logonboxDomain.setText(StringUtils.defaultString(Settings.getInstance().getLogonboxDomain()));
		    
		    new Label(this, SWT.NONE).setText("Port");
		    
		    logonboxPort = new Text(this, SWT.SINGLE | SWT.BORDER);
		    logonboxPort.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		    logonboxPort.setText(String.valueOf(Settings.getInstance().getLogonboxPort()));
		    new Label(this, SWT.NONE);
		    
		   
		}
	}
	
	class TerminalPreferencePanel extends Composite {

		  public TerminalPreferencePanel(Composite c) {
		    super(c, SWT.NO_BACKGROUND);
		    GridLayout layout = new GridLayout(1, true);
		    layout.marginBottom = layout.marginTop = layout.marginLeft = layout.marginRight = 8;
			this.setLayout(layout);
			
			
		    Label l1 = new Label(this, SWT.NONE);
		    l1.setText("Command");

		    terminalCommand = new Text(this, SWT.SINGLE | SWT.BORDER);
		    terminalCommand.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		    terminalCommand.setText(Settings.getInstance().getTerminalCommand());
		    
		    Label l2 = new Label(this, SWT.NONE);
		    l2.setText("Enter the path to the command you want executing when launching SSH connections.");
		    l2.setForeground(getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
		    
		    Label l3 = new Label(this, SWT.NONE);
		    l3.setText("Arguments");

		    terminalArguments = new Text(this, SWT.SINGLE | SWT.BORDER);
		    terminalArguments.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		    terminalArguments.setText(Settings.getInstance().getTerminalArguments());
		   
		    Label l4 = new Label(this, SWT.NONE);
		    l4.setText("The command arguments. Use ${host}, ${port} and ${username} to inject connection details.");
		    l4.setForeground(getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
		    
		    terminalCommand.setEnabled(!Settings.getInstance().getUseBuiltInTerminal());
	        terminalArguments.setEnabled(!Settings.getInstance().getUseBuiltInTerminal());
	        
	        Label l5 = new Label(this, SWT.NONE);
		    l5.setText("");
		    
			builtInTerminal = new Button(this, SWT.CHECK);
			builtInTerminal.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			builtInTerminal.setSelection(Settings.getInstance().getUseBuiltInTerminal());
			builtInTerminal.setText("Use the built-in terminal to launch connections.");
			builtInTerminal.addSelectionListener(new SelectionAdapter() {
			    @Override
			    public void widgetSelected(SelectionEvent event) {
			        terminalCommand.setEnabled(!((Button) event.getSource()).getSelection());
			        terminalArguments.setEnabled(!((Button) event.getSource()).getSelection());
			        keyWizard.setEnabled(((Button) event.getSource()).getSelection());
			    }
			});

		  }
	}
	
	class UserInterfacePreferencePanel extends Composite {

		  public UserInterfacePreferencePanel(Composite c) {
		    super(c, SWT.NO_BACKGROUND);
		    GridLayout layout = new GridLayout(1, true);
		    layout.marginBottom = layout.marginTop = layout.marginLeft = layout.marginRight = 8;
			this.setLayout(layout);

			new Label(this, SWT.NONE).setText("Tray icon mode");
		    
			iconMode = new Combo(this, SWT.READ_ONLY);
			iconMode.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			iconMode.setItems("Auto", "Dark", "Light");
			iconMode.select(Settings.getInstance().getIconMode().ordinal());
			iconMode.setText("Use a dark icon.");

		    
		    strictSSL = new Button(this, SWT.CHECK);
		    strictSSL.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		    strictSSL.setSelection(!Settings.getInstance().isStrictSSL());
		    strictSSL.setText("Allow self-signed certificates and invalid hostnames.");
		    new Label(this, SWT.NONE);
		  }
	}
}
