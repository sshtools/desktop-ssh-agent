package com.sshtools.mobile.agent;

import java.io.IOException;

import org.eclipse.swt.widgets.Display;

import com.sshtools.desktop.agent.DesktopAgent;

public class MobileAgent extends DesktopAgent {

	MobileAgent(Display display, Runnable restartCallback, Runnable shutdownCallback) throws IOException {
		super(display, restartCallback, shutdownCallback);
	}

}
