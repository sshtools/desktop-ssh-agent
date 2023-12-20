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
package com.sshtools.desktop.agent.term;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;

import com.sshtools.client.SshClient;
import com.sshtools.client.SshClient.SshClientBuilder;
import com.sshtools.client.sftp.SftpClient.SftpClientBuilder;
import com.sshtools.common.publickey.authorized.AuthorizedKeyFile;
import com.sshtools.common.sftp.PosixPermissions.PosixPermissionsBuilder;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.desktop.agent.DesktopAgent;
import com.sshtools.desktop.agent.JsonConnection;
import com.sshtools.terminal.emulation.Terminal;

public class ShellTerminalConnector extends AbstractTerminalConnector {
	
	SshClient ssh = null;
	boolean useKeyWizard = false;
	public ShellTerminalConnector(DesktopAgent agent, JsonConnection serverConnection) {
		super(agent, serverConnection);
	}

	public ShellTerminalConnector(DesktopAgent agent, String serverName, int serverPort, String username, boolean useKeyWizard) {
		super(agent, serverName, serverPort, username);
		this.useKeyWizard = useKeyWizard;
	}

	@Override
	protected void runConnector(Terminal vt) {

		try {
			writeLine("Connecting...");
		} catch (IOException e2) {
		}
		
		try {
			this.ssh = SshClientBuilder.create().
					withTarget(serverName, serverPort).
					withUsername(username).
					onConfigure(this::configureContext).
					build(); 
			
			vt.clearScreen();
			
			if(!ssh.isAuthenticated()) {
				writeLine("Authenticating...");
				if(!authenticate(ssh)) {
					writeLine();
					writeLine("Authenticaiton failed.");
					return;
				}
			}

			if(useKeyWizard) {
				if(availableAuthentications.contains("publickey")) {
					if(!completedAuthentications.contains("publickey")) {
						synchronizeAuthorizedKeys();
					}
				}
			}
			
			vt.clearScreen();
			
			ssh.runTask(createSession(ssh));
			
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

	
	public void synchronizeAuthorizedKeys() throws IOException {
		
		var keys = agent.getKeys();
		
		if(keys.isEmpty()) {
			writeLine("If you actually had some keys I would be able configure them! Please add keys to the agent first.");
			promptForAnswer("Press a key to continue.");
			return;
		}
		
		try {
			
			writeLine("Public key authentication is available.");
			var answer = promptForYesNo("Do you want to configure this server with your keys? ");
			if("YES".equalsIgnoreCase(answer) || "Y".equalsIgnoreCase(answer)) {
				writeLine("Checking ~/.ssh/authorized_keys");
				try(var sftp = SftpClientBuilder.create().withClient(ssh).build()) {
				
					var authorizedKeys = new AuthorizedKeyFile();
					try {
						sftp.stat(".ssh/authorized_keys");
						writeLine("Opening ~/.ssh/authorized_keys");
						try(InputStream in  = sftp.getInputStream(".ssh/authorized_keys")) {
							authorizedKeys.load(in);
						}
					} catch(SftpStatusException e) {
						
					}
					
					for(var entry : keys.entrySet()) {
						var key = entry.getKey();
						var comment = entry.getValue();
						
						if(!authorizedKeys.isAuthorizedKey(key)) {
							
							answer = promptForYesNo(String.format("Do you want to add the %s key %s %s? ", 
									StringUtils.defaultIfBlank(comment, "[No Comment]"), key.getAlgorithm(), key.getFingerprint()));
							if("YES".equalsIgnoreCase(answer) || "Y".equalsIgnoreCase(answer)) {
								writeLine(String.format("Adding %s %s", key.getAlgorithm(), key.getFingerprint()));
								authorizedKeys.addKey(key, comment);
							}
						}
					}
					
					writeLine("Saving ~/.ssh/authorized_keys");
					
					try(var out = sftp.getOutputStream(".ssh/authorized_keys")) {
						out.write(authorizedKeys.getFormattedFile().getBytes("UTF-8"));
					}
					
					sftp.chmod(PosixPermissionsBuilder.create().fromBitmask(0644).build(), ".ssh/authorized_keys");
					
					promptForAnswer("Authorized keys configuration complete. Press a key to continue.");
				}
			}
		} catch(Throwable t) {
			writeLine(t.getMessage());
			writeLine("Ooops, something went wrong there. You will need to manually configure your keys.");
		}
		
	}
	
	

}
