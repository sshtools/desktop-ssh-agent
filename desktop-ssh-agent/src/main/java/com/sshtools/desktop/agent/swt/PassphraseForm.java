package com.sshtools.desktop.agent.swt;

import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class PassphraseForm extends InputForm {

	Button saveToKeyChain;
	boolean save;
	boolean offerSave = true;
	
	public PassphraseForm(Display display, String title, String message, String defaultValue, boolean offerSave) {
		super(display, title, message, defaultValue, true);
		this.offerSave = offerSave;
	}
	
	public boolean isSaveToKeyChain() {
		return save;
	}

	protected void onCaptureInput() {
		this.save = Objects.nonNull(saveToKeyChain) && saveToKeyChain.getSelection();
	}
	
	@Override
	protected void buildComponents(Shell shell) {
		
		super.buildComponents(shell);
		
		if(offerSave) {
			saveToKeyChain = new Button(shell, SWT.CHECK);
			saveToKeyChain.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			saveToKeyChain.setSelection(true);
			saveToKeyChain.setText("Save passphrase to local key chain.");
		}
	}
}
