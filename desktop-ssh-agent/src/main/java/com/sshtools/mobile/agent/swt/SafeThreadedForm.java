package com.sshtools.mobile.agent.swt;

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
