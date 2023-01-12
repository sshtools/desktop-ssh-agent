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

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public abstract class SafeThreadedForm {

	protected Shell shell;
	
	protected void setupShell(Display display) {
		Runnable r = new Runnable() {
			public void run() {
				shell = createShell(display);
			}
		};
		
		if(!display.getThread().equals(Thread.currentThread())) {
			display.syncExec(r);
		} else {
			r.run();
		}
	}
	
	protected abstract Shell createShell(Display display);
	
	protected void executeThreadSafe(Runnable r) {
		if(!shell.getDisplay().getThread().equals(Thread.currentThread())) {
			shell.getDisplay().syncExec(r);
		} else {
			r.run();
		}
	}
}
