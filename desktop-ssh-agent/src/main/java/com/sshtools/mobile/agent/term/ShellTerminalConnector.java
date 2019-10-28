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
package com.sshtools.mobile.agent.term;

import java.io.IOException;

import com.sshtools.client.SshClient;
import com.sshtools.client.SshClientContext;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.mobile.agent.JsonConnection;
import com.sshtools.mobile.agent.MobileAgent;
import com.sshtools.terminal.emulation.Terminal;

public class ShellTerminalConnector extends AbstractTerminalConnector {
	
	SshClient ssh = null;
	
	public ShellTerminalConnector(MobileAgent agent, JsonConnection serverConnection) {
		super(agent, serverConnection);
	}

	public ShellTerminalConnector(MobileAgent agent, String serverName, int serverPort, String username) {
		super(agent, serverName, serverPort, username);
	}

	@Override
	protected void runConnector(Terminal vt) {

		try {
			writeLine("Connecting...");
		} catch (IOException e2) {
		}
		
		try {
			this.ssh = new SshClient(serverName, serverPort, username) {

				@Override
				protected void configure(SshClientContext sshContext) throws SshException, IOException {
					configureContext(sshContext);
				}
			};
			
			vt.clearScreen();
			writeLine("Authenticating...");

			if(!authenticate(ssh)) {
				writeLine();
				writeLine("Authenticaiton failed.");

			} else {
				vt.clearScreen();
				ssh.runTask(createSession(ssh));
			}
			ssh.disconnect();
		} catch (IOException | SshException |ChannelOpenException e) {
			try {
				writeLine(String.format("ERROR: %s", e.getMessage()));
			} catch (IOException e1) {
			}
		}
	}

	@Override
	public void disconnect() {
		if(ssh!=null) {
			ssh.disconnect();
		}
	}
	
	

}
