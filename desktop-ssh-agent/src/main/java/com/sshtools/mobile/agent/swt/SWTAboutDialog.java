package com.sshtools.mobile.agent.swt;


import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

import com.sshtools.mobile.agent.SSHTOOLSVersion;

public class SWTAboutDialog extends Dialog {

	private Shell shell;
	private Composite accessory;

	public SWTAboutDialog(Shell parent, String closeText, String title, final Image image, String message, String description,
							String copyright, final String link) {
		super(parent, SWT.ON_TOP | SWT.TITLE | SWT.CLOSE | SWT.BORDER | SWT.RESIZE);


		// Create the dialog window
		shell = new Shell(getParent(), getStyle());
		shell.setText(getText());
		shell.setLayout(new GridLayout(1, true));
		
		// Common About Details

		Composite commonAboutDetails = new Composite(shell, 0);
		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.verticalSpacing = 10;
		GridData data = new GridData(GridData.CENTER, GridData.CENTER, true, true);
//		data.widthHint = 120;
//		data.heightHint = 48;
		commonAboutDetails.setLayoutData(data);		
		commonAboutDetails.setLayout(gridLayout);
		

		// Close
		Button close = new Button(shell, SWT.PUSH);
		close.setText(closeText);
		close.setFocus();
		close.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				shell.close();
			}
		});
		data = new GridData();
		data.horizontalAlignment = GridData.CENTER;
		data.grabExcessHorizontalSpace = true;
		close.setLayoutData(data);
		
		// Common About Details Components

		Canvas canvas = new Canvas(commonAboutDetails, SWT.CENTER);
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				e.gc.drawImage(image, 0, 0);
				
			}
		});
		data = new GridData(GridData.CENTER, GridData.CENTER, true, true);
		data.widthHint = 64;
		data.heightHint = 64;
		canvas.setLayoutData(data);

		Label messageLabel = new Label(commonAboutDetails, SWT.WRAP | SWT.CENTER);
		messageLabel.setFont(SWTUtil.newFont(parent.getDisplay(), messageLabel.getFont(), 18, SWT.BOLD));
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
		versionLabel.setText("Version " + SSHTOOLSVersion.getVersion());
		data = new GridData();
		data.widthHint = 400;
		data.horizontalAlignment = GridData.CENTER;
		data.grabExcessHorizontalSpace = true;
		versionLabel.setLayoutData(data);

		if (copyright != null) {
			Label copyrightLabel = new Label(commonAboutDetails, SWT.WRAP | SWT.CENTER);
			copyrightLabel.setText(copyright);
			copyrightLabel.setFont(SWTUtil.newFont(parent.getDisplay(), copyrightLabel.getFont(), 8, 0));
			data = new GridData();
			data.horizontalAlignment = GridData.CENTER;
			data.grabExcessHorizontalSpace = true;
			copyrightLabel.setLayoutData(data);
		}
		
		if(link != null) {
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
						} catch (IOException  | URISyntaxException e1) {
						}
					}
				}				
			}); 
		}

		// Accessory

		accessory = new Composite(commonAboutDetails, 0);		
		accessory.setLayout(new GridLayout(1, false));
		data = new GridData();
		data.horizontalAlignment = GridData.CENTER;
		data.grabExcessHorizontalSpace = true;
		accessory.setLayoutData(data);
	}
	
	public void open() {
		shell.pack();
		SWTUtil.center(shell);
		shell.open();
		shell.forceActive();
	}

	public Composite getAccessory() {
		return accessory;
	}
}
