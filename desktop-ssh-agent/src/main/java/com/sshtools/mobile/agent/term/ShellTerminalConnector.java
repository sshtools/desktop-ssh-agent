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
