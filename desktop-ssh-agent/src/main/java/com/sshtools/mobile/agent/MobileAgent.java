package com.sshtools.mobile.agent;

import java.io.IOException;

import org.eclipse.swt.widgets.Display;

import com.sshtools.desktop.agent.DesktopAgent;

/**
 * This class extends DesktopAgent for compatibility with older versions.
 * @author lee
 *
 */
public class MobileAgent extends DesktopAgent {

	/**
	 * 
	 * @param display
	 * @param restartCallback
	 * @param shutdownCallback
	 * @throws IOException
	 */
	MobileAgent(Display display, Runnable restartCallback, Runnable shutdownCallback) throws IOException {
		super(display, restartCallback, shutdownCallback);
	}

}
