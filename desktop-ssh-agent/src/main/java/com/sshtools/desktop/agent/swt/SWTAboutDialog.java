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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

import com.sshtools.desktop.agent.DesktopAgent;
import com.sshtools.jaul.ArtifactVersion;

public class SWTAboutDialog extends Dialog {
	private Shell shell;
	private Composite accessory;
	private Label newVersionLabel;
	private Button checkUpdateButton;
	private DesktopAgent agent;

	public SWTAboutDialog(Display parent, String closeText, String title, final Image image, String message, String description,
			String copyright, final String link, DesktopAgent agent) {
		super(new Shell(parent), SWT.ON_TOP | SWT.TITLE | SWT.CLOSE | SWT.BORDER | SWT.RESIZE | SWT.APPLICATION_MODAL);
		
		this.agent = agent;
		
		// Create the dialog window
		shell = new Shell(parent, getStyle());
		shell.setText(getText());
		shell.setLayout(new GridLayout(1, true));
		shell.setImage(image);
		// Common About Details
		Composite commonAboutDetails = new Composite(shell, 0);
		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.verticalSpacing = 10;
		GridData data = new GridData(GridData.CENTER, GridData.CENTER, true, true);
		// data.widthHint = 120;
		// data.heightHint = 48;
		commonAboutDetails.setLayoutData(data);
		commonAboutDetails.setLayout(gridLayout);
		// Common About Details Components
		Canvas canvas = new Canvas(commonAboutDetails, SWT.NONE);
		canvas.addPaintListener((e) -> e.gc.drawImage(image, 0, 0));
		data = new GridData(GridData.CENTER, GridData.CENTER, true, true);
		data.widthHint = 64;
		data.heightHint = 64;
		canvas.setLayoutData(data);
		Label messageLabel = new Label(commonAboutDetails, SWT.WRAP | SWT.CENTER);
		messageLabel.setFont(SWTUtil.newFont(parent, messageLabel.getFont(), 18, SWT.BOLD));
		messageLabel.setText(message);
		data = new GridData();
		data.widthHint = 400;
		data.horizontalAlignment = GridData.CENTER;
		data.grabExcessHorizontalSpace = true;
		messageLabel.setLayoutData(data);
		Label descriptionLabel = new Label(commonAboutDetails, SWT.WRAP | SWT.CENTER);
		descriptionLabel.setText(description);
		data = new GridData();
		data.widthHint = 400;
		data.horizontalAlignment = GridData.CENTER;
		data.grabExcessHorizontalSpace = true;
		descriptionLabel.setLayoutData(data);
		Label versionLabel = new Label(commonAboutDetails, SWT.WRAP | SWT.CENTER);
		versionLabel.setText("Version " + ArtifactVersion.getVersion("desktop-ssh-agent", "com.sshtools", "desktop-ssh-agent"));
		data = new GridData();
		data.widthHint = 400;
		data.horizontalAlignment = GridData.CENTER;
		data.grabExcessHorizontalSpace = true;
		versionLabel.setLayoutData(data);
		if (copyright != null) {
			Label copyrightLabel = new Label(commonAboutDetails, SWT.WRAP | SWT.CENTER);
			copyrightLabel.setText(copyright);
			copyrightLabel.setFont(SWTUtil.newFont(parent, copyrightLabel.getFont(), 8, 0));
			data = new GridData();
			data.horizontalAlignment = GridData.CENTER;
			data.grabExcessHorizontalSpace = true;
			copyrightLabel.setLayoutData(data);
		}
		if (link != null) {
			Link linkButton = new Link(commonAboutDetails, SWT.CENTER);
			linkButton.setText("<a href=\"" + link + "\">" + link + "</a>");
			data = new GridData();
			data.horizontalAlignment = GridData.CENTER;
			data.grabExcessHorizontalSpace = true;
			linkButton.setLayoutData(data);
			linkButton.addSelectionListener(new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) {
				}

				public void widgetSelected(SelectionEvent e) {
					if (Desktop.isDesktopSupported()) {
						try {
							Desktop.getDesktop().browse(new URI(link));
						} catch (IOException | URISyntaxException e1) {
						}
					}
				}
			});
		}

		newVersionLabel = new Label(commonAboutDetails, SWT.WRAP | SWT.CENTER);
		data = new GridData();
		data.widthHint = 400;
		data.horizontalAlignment = GridData.CENTER;
		data.grabExcessHorizontalSpace = true;
		newVersionLabel.setLayoutData(data);

		checkUpdateButton = new Button(commonAboutDetails, SWT.CENTER);
		data = new GridData();
		data.horizontalAlignment = GridData.CENTER;
		data.grabExcessHorizontalSpace = true;
		checkUpdateButton.setLayoutData(data);
		checkUpdateButton.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				if (agent.getUpdateService().getAvailableVersion() == null)
					agent.updateCheck((v) -> {
					}, (ex) -> {
					});
				else {
					agent.update((v) -> {
					});
				}
			}
		});
		
		agent.getUpdateService().setOnBusy((b) -> parent.asyncExec(() -> updateButtonStatus(false)));
		agent.getUpdateService().setOnAvailableVersion((v) -> parent.asyncExec(() -> updateButtonStatus(false)));
		updateButtonStatus(true);
		
		
		// Accessory
		accessory = new Composite(commonAboutDetails, 0);
		accessory.setLayout(new GridLayout(1, false));
		data = new GridData();
		data.horizontalAlignment = GridData.CENTER;
		data.grabExcessHorizontalSpace = true;
		accessory.setLayoutData(data);

		shell.pack();
		SWTUtil.center(shell);
		shell.open();
		while (!shell.isDisposed()) {
			if (!parent.readAndDispatch())
				parent.sleep();
		}
	}

	public Composite getAccessory() {
		return accessory;
	}
	
	private void updateButtonStatus(boolean initial) {
		if(agent.getUpdateService().isUpdating()) {
			checkUpdateButton.setText("Please Wait");
			checkUpdateButton.setEnabled(false);
			newVersionLabel.setVisible(true);
			if(agent.getUpdateService().isCheckOnly())
				newVersionLabel.setText("Checking ..");
			else
				newVersionLabel.setText("Updating ..");
		}
		else if (agent.getUpdateService().isNeedsUpdating()) {
			checkUpdateButton.setEnabled(true);
			newVersionLabel.setVisible(true);
			checkUpdateButton.setText("Update");
			newVersionLabel.setText("Version " + agent.getUpdateService().getAvailableVersion() + " is available.");
		}
		else if(initial) {
			checkUpdateButton.setEnabled(true);
			newVersionLabel.setVisible(false);
			checkUpdateButton.setText("Check For Update");
		}
		else {
			checkUpdateButton.setEnabled(true);
			newVersionLabel.setVisible(true);
			newVersionLabel.setText("No updates are available.");
			checkUpdateButton.setText("Check For Update");
		}
	}
}
