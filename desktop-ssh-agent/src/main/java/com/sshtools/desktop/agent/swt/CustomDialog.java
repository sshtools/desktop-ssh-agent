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
package com.sshtools.desktop.agent.swt;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class CustomDialog extends Dialog
{
    private String message = "";
    private int icon;
    private Shell shell;
    private String[] buttons;
    private String selected = null;
    
    public CustomDialog(Shell parent, int icon, int style, String... buttons)
    {
        // Pass the default styles here
        super(new Shell(parent.getDisplay()), SWT.PRIMARY_MODAL | SWT.DIALOG_TRIM | style);
        shell = getParent();
        this.buttons = buttons;
        this.icon = icon;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public String open()
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
        
        return selected;
    }

    /**
     * Creates the dialog's contents
     * 
     * @param shell
     *            the dialog window
     */
    protected void createContents(final Shell shell)
    {
    	int column = Math.max(buttons.length, 3);
 
        shell.setLayout(new GridLayout(column, true));

        // Show the message
        CLabel label = new CLabel(shell, SWT.WRAP | SWT.MULTI);
        String[] messages = splitMessage(StringUtils.defaultString(message));
        StringBuilder builder = new StringBuilder();
        for(String m : messages) {
        	if(StringUtils.isBlank(m)) {
        		builder.append("\r\n");
        		continue;
        	}
        	builder.append(WordUtils.wrap(m, 70));
        }
        label.setText(builder.toString());
        label.setImage(shell.getDisplay().getSystemImage(icon));
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalSpan = column;
        data.widthHint = 500;
        label.setLayoutData(data);

        while(column > buttons.length) {
        	new Label(shell, SWT.NONE);
        	column--;
        }
        
        for(int i = buttons.length-1;i>=0;i--) {
	        Button b = new Button(shell, SWT.PUSH);
	        b.setText(buttons[i]);
	        data = new GridData(SWT.FILL, SWT.END, true, true);
	        
	        b.setLayoutData(data);
	        final int index = i;
	        b.addSelectionListener(new SelectionAdapter()
	        {
	            public void widgetSelected(SelectionEvent event)
	            {
	                selected = buttons[index];
	                shell.dispose();
	            }
	        });
	        if(i==0) {
	        		shell.setDefaultButton(b);
	        }
        }
    }

    private String[] splitMessage(String message) {
		
    	List<String> tmp = new ArrayList<String>();
    	int beginIndex = 0;
    	int endIndex = 0;
    	while(endIndex < message.length()) {
    		endIndex = message.indexOf("\r\n", beginIndex);
    		String currentMessage;
    		if(endIndex==-1) {
    			tmp.add(currentMessage = message.substring(beginIndex));
    			break;
    		} else {
    			tmp.add(currentMessage = message.substring(beginIndex, endIndex));
    		}
    		if(StringUtils.isNotBlank(currentMessage)) {
    			tmp.add("\r\n");
    		}
    		
    		beginIndex = endIndex + 2;
    	}
    	return tmp.toArray(new String[0]);
	}

	public static void main(String[] args)
    {
        CustomDialog dialog = new CustomDialog(new Shell(), SWT.ICON_ERROR, SWT.ON_TOP | SWT.SYSTEM_MODAL, "Import", "Use Locally");
        dialog.setText("Title");
        dialog.setMessage("Message  that is long enough to make the  dialog pretty big sitting here on a modal situation");
        
        dialog.open();
    }
}