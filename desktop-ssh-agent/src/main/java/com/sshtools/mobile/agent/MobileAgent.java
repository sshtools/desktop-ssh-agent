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
