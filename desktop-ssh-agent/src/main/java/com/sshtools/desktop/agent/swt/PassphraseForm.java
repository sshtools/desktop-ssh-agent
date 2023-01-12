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
