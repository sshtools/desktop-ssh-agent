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
package com.sshtools.desktop.agent.term;

import java.awt.Desktop;
import java.io.IOException;
import java.util.Properties;
import java.util.prefs.Preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import com.sshtools.terminal.emulation.VDUColor;
import com.sshtools.terminal.vt.swt.SWTScrollBar;
import com.sshtools.terminal.vt.swt.SWTTerminalPanel;
import com.sshtools.terminal.vt.swt.SWTURIFinder;

public class TerminalDisplay {
	
	final static String PROFILE_PROPERTY_BACKGROUND_COLOR = "BACKGROUND_COLOR";
	final static String PROFILE_PROPERTY_FOREGROUND_COLOR = "FOREGROUND_COLOR";
	final static String PROFILE_PROPERTY_COLOR_PRINTING = "COLOR_PRINTING";
	final static String PROFILE_PROPERTY_CURSOR_FOREGROUND = "CURSOR_FOREGROUND";
	final static String PROFILE_PROPERTY_CURSOR_BACKGROUND = "CURSOR_BACKGROUND";
	final static String PROFILE_PROPERTY_CURSOR_STYLE = "CURSOR_STYLE";
	final static String PROFILE_PROPERTY_CURSOR_BLINK = "CURSOR_BLINK";
	
	final static Preferences PREFS = Preferences.userNodeForPackage(TerminalDisplay.class);
	final static String PREF_THEME = "theme";
	
	SWTTerminalPanel stp = null;
	Shell frame;
	
	public void runTerminal(String title, AbstractTerminalConnector connector) {

			GridLayout layout = new GridLayout(2, false);

			frame = new Shell();
			frame.setLayout(layout);
			frame.setText(title);
			frame.addShellListener(new ShellAdapter() {
				public void shellClosed(ShellEvent arg0) {
					stp.getControl().dispose();
					new  Thread() {
						public void run() {
							connector.disconnect();
						}
					}.start();
					
				}
			});

			// Create a terminal
			stp = new SWTTerminalPanel(frame);
			stp.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			stp.getVDUBuffer().addTitleChangeListener((terminal, t) -> frame.setText(t));
			
			configureTerminal(stp);
		
			// Scrollbar
			SWTScrollBar sb = new SWTScrollBar(frame, stp);
			sb.getNativeComponent().setLayoutData(new GridData(SWT.END, SWT.FILL, false, false));
			
			// Context menu
		    final Clipboard cb = new Clipboard(Display.getCurrent());
			Menu ctxmenu = new Menu(stp.getControl());
			MenuItem copy = new MenuItem(ctxmenu, SWT.NONE);
			copy.setText("Copy");
			copy.addListener (SWT.Selection, e -> {
		        String textData = stp.getVDUBuffer().getSelection();
		        TextTransfer textTransfer = TextTransfer.getInstance();
		        cb.setContents(new Object[] { textData },
		            new Transfer[] { textTransfer });
			});
			MenuItem paste = new MenuItem(ctxmenu, SWT.NONE);
			paste.setText("Paste");
			paste.addListener(SWT.Selection, e -> {
		        TextTransfer transfer = TextTransfer.getInstance();
		        String data = (String) cb.getContents(transfer);
		        if (data != null) {
		        	try {
						connector.writeString(data);
					} catch (IOException e1) {
					}
		        }
		    });
			

			MenuItem theme = new MenuItem (ctxmenu, SWT.CASCADE);
			theme.setText ("&Theme");
			Menu thememenu = new Menu(ctxmenu.getShell(), SWT.DROP_DOWN);
			theme.setMenu (thememenu);
			
			for(TerminalTheme t : TerminalTheme.getThemes()) {
				MenuItem tm = new MenuItem(thememenu, SWT.NONE);
				tm.setText(t.getName());
				tm.addListener(SWT.Selection, e -> {
					setTheme(stp, t);
					PREFS.put(PREF_THEME, t.getName());
			    });
			}
			stp.addMouseListener(new MouseListener() {
				@Override
				public void mouseUp(MouseEvent e) {
				}
				
				@Override
				public void mouseDown(MouseEvent e) {
					if(e.button == 3) {
						ctxmenu.setVisible(true);
					}
				}
				
				@Override
				public void mouseDoubleClick(MouseEvent e) {
				}
			});

			// Scan for URLs occurring in output
			new SWTURIFinder(stp, (uri, button) -> Desktop.getDesktop().browse(uri));

			// Start the connector
			connector.startTerminal(this);

			// Size and show frame
			frame.setSize(1024, 480);
			frame.open();

	}
	
	private void setTheme(SWTTerminalPanel stp, TerminalTheme theme) {
		Properties tprops = theme.getProperties();
		stp.setDefaultBackground(tprops.containsKey(PROFILE_PROPERTY_BACKGROUND_COLOR) ? new VDUColor(tprops.getProperty(PROFILE_PROPERTY_BACKGROUND_COLOR)) : VDUColor.BLACK);
		stp.setDefaultForeground(tprops.containsKey(PROFILE_PROPERTY_FOREGROUND_COLOR) ? new VDUColor(tprops.getProperty(PROFILE_PROPERTY_FOREGROUND_COLOR)) : VDUColor.GREEN);
		stp.setCursorColors(tprops.containsKey(PROFILE_PROPERTY_CURSOR_FOREGROUND) ? new VDUColor(tprops.getProperty(PROFILE_PROPERTY_CURSOR_FOREGROUND)) : VDUColor.BLACK,
				tprops.containsKey(PROFILE_PROPERTY_CURSOR_BACKGROUND) ? new VDUColor(tprops.getProperty(PROFILE_PROPERTY_CURSOR_BACKGROUND)) : VDUColor.GREEN);
		stp.setCursorBlink(tprops.containsKey(PROFILE_PROPERTY_CURSOR_BLINK) ? "true".equalsIgnoreCase(tprops.getProperty(PROFILE_PROPERTY_CURSOR_BLINK)) : true);
		stp.setCursorStyle(tprops.containsKey(PROFILE_PROPERTY_CURSOR_STYLE) ? Integer.parseInt(tprops.getProperty(PROFILE_PROPERTY_CURSOR_STYLE)) : SWTTerminalPanel.CURSOR_BLOCK);
	
	}
	
	private void configureTerminal(SWTTerminalPanel stp) {
		stp.setCenter(false);
		
		String themeName = PREFS.get(PREF_THEME, "");
		if(!themeName.equals("")) {
			TerminalTheme theme = TerminalTheme.getTheme(themeName);
			if(!theme.equals(TerminalTheme.CUSTOM)) {
				setTheme(stp, theme);
				return;
			}
		}
		stp.setDefaultBackground(VDUColor.BLACK);
		stp.setDefaultForeground(VDUColor.GREEN);
		stp.setCursorColors(VDUColor.BLACK, VDUColor.GREEN);
		stp.setCursorBlink(true);
		stp.setCursorStyle(SWTTerminalPanel.CURSOR_BLOCK);
	
	}
	
	public void close() {
		frame.getDisplay().asyncExec(new Runnable() {
			public void run() {
				frame.dispose();
			}
		});	
	}
}
