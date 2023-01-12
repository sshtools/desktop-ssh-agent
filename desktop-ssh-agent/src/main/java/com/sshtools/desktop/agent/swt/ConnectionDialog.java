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

import java.io.IOException;
import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import com.sshtools.common.ssh.SshException;
import com.sshtools.desktop.agent.DesktopAgent;
import com.sshtools.desktop.agent.JsonConnection;

public class ConnectionDialog extends Dialog {

	final String SAVE_BUTTON = "Save";
	
	Shell shell;
	Text name;
	Text hostname;
	Text port; 
	Text username;
	Button keyWizard;
	DesktopAgent agent; 
	JsonConnection edit;
	boolean useWizard = true;
	
	public ConnectionDialog(Shell parent, DesktopAgent agent) {
		this(parent, agent, 0);
	}
	
	public ConnectionDialog(Shell parent, DesktopAgent agent, JsonConnection edit) {
		this(parent, agent, 0);
		this.edit = edit;
		this.useWizard = false;
	}
	
	public ConnectionDialog(Shell parent, DesktopAgent agent, int  style) {
		super(new Shell(parent.getDisplay()), SWT.DIALOG_TRIM | style);
        this.shell = getParent();
        this.agent = agent;
	}

	public JsonConnection getConnection() {
		return edit;
	}
	
	public boolean open()
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
        
        return edit != null && useWizard;
    }
	
	protected void createContents(final Shell shell) {
		
        shell.setLayout(new GridLayout(1, true));
        shell.setText(edit!=null ? "Update" : "Create" + " Connection");
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.widthHint = 600;
        
		final TabFolder tabFolder = new TabFolder(shell, SWT.BORDER);
		tabFolder.setLayoutData(data);
		
		TabItem tabTerminal = new TabItem(tabFolder, SWT.NULL);
		tabTerminal.setText("Connection");

		
		tabTerminal.setControl(new GridComposite(tabFolder));
		
		data = new GridData(GridData.HORIZONTAL_ALIGN_END);
		Button b = new Button(shell, SWT.PUSH);
		b.setText("Save");
		b.setLayoutData(data);
		
		b.addSelectionListener(new SelectionAdapter()
        {
            public void widgetSelected(SelectionEvent event)
            {
            	String n = name.getText();
            	String h = hostname.getText();
            	String p = port.getText();
            	String u = username.getText();
            	useWizard = keyWizard.getSelection();
            	
            	new Thread() {
            		public void run() {
            			try {
            				edit = agent.saveConnection(n, h, Integer.parseInt(p), u, Objects.isNull(edit) ? null : edit.getName());
            				
            				shell.getDisplay().asyncExec(new Runnable() {
            					public void run() {
            						shell.dispose();
            					}
            				});
            				
            			} catch(IllegalStateException | NumberFormatException | SshException | IOException e) {
    						SWTUtil.showError("Save Error", e.getMessage());
            			}
            		}
            	}.start();
                
                
            }
        });
	}


	class GridComposite extends Composite {

		  public GridComposite(Composite c) {
		    super(c, SWT.SINGLE);
		    GridLayout layout = new GridLayout(3, true);
		    layout.marginBottom = layout.marginTop = layout.marginLeft = layout.marginRight = 8;
			this.setLayout(layout);

		    Label l0 = new Label(this, SWT.NONE);
		    l0.setText("Name");

		    name = new Text(this, SWT.SINGLE | SWT.BORDER);
		    GridData data = new GridData(GridData.FILL_HORIZONTAL);
		    data.horizontalSpan = 3;
		    name.setLayoutData(data);
		    if(edit!=null) {
		    	name.setText(edit.getName());
		    }
		    Label l7 = new Label(this, SWT.NONE);
		    l7.setText("Provide a name to identify this server.");
		    l7.setLayoutData(data);
		    l7.setForeground(getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
		   
		    
		    Label l1 = new Label(this, SWT.NONE);
		    l1.setText("Hostname");

		    hostname = new Text(this, SWT.SINGLE | SWT.BORDER);
		    data.horizontalSpan = 3;
		    hostname.setLayoutData(data);
		    if(edit!=null) {
		    	hostname.setText(edit.getHostname());
		    }
		    
		    Label l2 = new Label(this, SWT.NONE);
		    l2.setText("The hostname of the remote SSH server.");
		    l2.setLayoutData(data);
		    l2.setForeground(getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
		    
		    Label l3 = new Label(this, SWT.NONE);
		    l3.setLayoutData(data);
		    l3.setText("Port");

		    port = new Text(this, SWT.SINGLE | SWT.BORDER);
		    port.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		    port.setText("22");
		    if(edit!=null) {
		    	port.setText(String.valueOf(edit.getPort()));
		    }
		    
		    Label l4 = new Label(this, SWT.NONE);
		    l4.setText("The port on which the SSH service is running on the remote server.");
		    l4.setLayoutData(data);
		    l4.setForeground(getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
		    
		    Label l5 = new Label(this, SWT.NONE);
		    l5.setText("Username");

		    username = new Text(this, SWT.SINGLE | SWT.BORDER);
		    username.setLayoutData(data);
		    username.setText("");
		    if(edit!=null) {
		    	username.setText(edit.getUsername());
		    }
		    Label l6 = new Label(this, SWT.NONE);
		    l6.setText("The username of the account you want to login to on the remote server.");
		    l6.setLayoutData(data);
		    l6.setForeground(getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
		  
		    new Label(this, SWT.NONE).setText("");
		    
			keyWizard = new Button(this, SWT.CHECK);
			keyWizard.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			keyWizard.setSelection(useWizard);
			keyWizard.setLayoutData(data);
			keyWizard.setText("Connect terminal and run the public key wizard upon save.");
		  }
		    
	}

}
