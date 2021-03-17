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
package com.sshtools.desktop.agent.term;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.hypersocket.json.utils.HypersocketUtils;
import com.sshtools.agent.client.SshAgentClient;
import com.sshtools.agent.exceptions.AgentNotAvailableException;
import com.sshtools.client.AbstractKeyboardInteractiveCallback;
import com.sshtools.client.ClientAuthenticator;
import com.sshtools.client.ExternalKeyAuthenticator;
import com.sshtools.client.KeyboardInteractiveAuthenticator;
import com.sshtools.client.KeyboardInteractivePrompt;
import com.sshtools.client.KeyboardInteractivePromptCompletor;
import com.sshtools.client.PasswordAuthenticator;
import com.sshtools.client.SessionChannelNG;
import com.sshtools.client.SshClient;
import com.sshtools.client.SshClientContext;
import com.sshtools.client.shell.ShellTimeoutException;
import com.sshtools.client.tasks.ShellTask;
import com.sshtools.client.tasks.Task;
import com.sshtools.common.knownhosts.KnownHostsKeyVerification;
import com.sshtools.common.logger.Log;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.ChannelOpenException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.desktop.agent.DesktopAgent;
import com.sshtools.desktop.agent.JsonConnection;
import com.sshtools.terminal.emulation.Terminal;
import com.sshtools.terminal.emulation.TerminalInputStream;
import com.sshtools.terminal.emulation.TerminalOutputStream;

/**
 * A helper class that demonstrates the simplest use of the terminal, connecting
 * up the two streams
 */
public abstract class AbstractTerminalConnector {

	DesktopAgent agent;
	TerminalDisplay term;
	
	protected Terminal vt;
	protected final String serverName;
	protected final int serverPort;
	protected final String username;
	
	protected InputStream tin;
	protected OutputStream tout;
	
	protected Set<String> completedAuthentications = new HashSet<String>();
	protected Set<String> availableAuthentications = new HashSet<String>();
	/**
	 * @param vt
	 *            the terminal
	 */
	public AbstractTerminalConnector(DesktopAgent agent, JsonConnection serverConnection) {
		this.agent = agent;
		this.serverName = serverConnection.getHostname();
		this.serverPort = serverConnection.getPort();
		this.username = serverConnection.getUsername();
	}
	
	public AbstractTerminalConnector(DesktopAgent agent, String serverName, int serverPort, String username) {
		this.agent = agent;
		this.serverName = serverName;
		this.serverPort = serverPort;
		this.username = username;
	}

	public void startTerminal(TerminalDisplay term) {
		this.vt = term.stp.getVDUBuffer();
		this.term = term;
		new TerminalThread().start();
	}
	
	class TerminalThread extends Thread {
		
		TerminalThread() {
			setDaemon(true);
		}
		@Override
		public void run() {
			try {
				tin = new TerminalInputStream(vt);
				tout =  new TerminalOutputStream(vt);
				runConnector(vt);
			} catch(Throwable t) {
				Log.error("Terminal thread failed", t);
			} 
		}
	}
	
	protected void writeString(String str) throws IOException {
		tout.write(str.getBytes());
		tout.flush();
	}
	
	protected void writeLine(String str) throws IOException {
		writeString(String.format("%s\r\n", str));
	}
	
	protected void writeLine() throws IOException {
		writeLine("");
	}
	
	protected String promptForYesNo(String msg) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(tin));
		String answer;
		do {
			writeString(msg);	
			answer = reader.readLine();
			msg = "\r\nPlease type 'yes' or 'no': ";
		} while(!answer.equalsIgnoreCase("YES") && !answer.equalsIgnoreCase("NO"));
		
		writeLine();
		return answer;
	}
	
	protected String promptForAnswer(String msg) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(tin));
		writeLine(msg);
		return reader.readLine();
	}
	
	protected boolean authenticate(SshClient ssh) throws SshException, IOException {
		
		availableAuthentications.addAll(ssh.getAuthenticationMethods());
		
		try {
			SshAgentClient agent = SshAgentClient.connectOpenSSHAgent(
					"Desktop SSH Agent",
					this.agent.getAgentSocketPath());

			boolean success = false;
			if(!agent.listKeys().isEmpty()) {
				success = ssh.authenticate(new ExternalKeyAuthenticator(agent), 30000);
			}
		
			if(!success) {
				writeLine("The agent does not have any suitable public keys.");
			} else if(success && !ssh.isAuthenticated()) {
				completedAuthentications.add("publickey");
				writeLine("Further authentication is required.");
			} else {
				writeLine("Authenticated using public key authentication.");
				completedAuthentications.add("publickey");
			}

		} catch(AgentNotAvailableException e) {
			writeLine("The agent is not available for authentication.");
		}
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(tin));
		
		while((ssh.isConnected() && !ssh.isAuthenticated())) {
			
			Set<String> auths = new HashSet<>(ssh.getAuthenticationMethods());
			ClientAuthenticator auth;
			if(auths.contains("keyboard-interactive")) {
				auth = new KeyboardInteractiveAuthenticator(new AbstractKeyboardInteractiveCallback() {
					
					public void showPrompts(String name, String instruction,
							KeyboardInteractivePrompt[] prompts, 
							KeyboardInteractivePromptCompletor completor) {

						try {
							if(StringUtils.isNotBlank(name)) {
								writeLine(name);
							}
							if(StringUtils.isNotBlank(instruction)) {
								writeLine(instruction);
							}
							
							for(KeyboardInteractivePrompt prompt : prompts) {
								writeString(prompt.getPrompt());
								vt.setLocalEcho(true);
								vt.setMaskInput(!prompt.echo());
								prompt.setResponse(reader.readLine());
								writeLine();
							}
							vt.setLocalEcho(false);
							completor.complete();
						} catch (IOException e) {
							Log.error("Failed processing keyboard-interactive prompts", e);
							completor.cancel();
						}
					}
				});
			} else if(auths.contains("password")) {
				
				writeString("Your password: ");
				vt.setMaskInput(true);
				vt.setEchoChar('*');
				String pwd = reader.readLine();
				auth = new PasswordAuthenticator(pwd);
				writeLine();
			} else {
				writeLine("There are not any more authentication methods we can try.");
				writeLine(HypersocketUtils.csv(ssh.getAuthenticationMethods().toArray(new String[0])));
				throw new IOException("Exhausted authentication methods");
			}
			
			ssh.authenticate(auth, 30000L);
		}
		
		return ssh.isConnected() && ssh.isAuthenticated();
	}

	protected abstract void runConnector(Terminal vt);

	protected Task createSession(SshClient ssh) throws SshException, ChannelOpenException {

		return new ShellTask(ssh) {

			@Override
			protected void beforeStartShell(SessionChannelNG session) {
				session.allocatePseudoTerminal("xterm", vt.getColumns(), vt.getRows());
				vt.addCloseListener((terminal) -> session.close());
				vt.addResizeListener((terminal, cols, rows, remote) -> session.changeTerminalDimensions(cols, rows, 0, 0));
				vt.setInput((data, off, len) -> {
				    try {
						session.sendData(data, off, len);
					} catch (Exception e) {
						if(!session.isClosed()) {
							throw e;
						}
					}
				});
			}

			@Override
			protected void onCloseSession(SessionChannelNG session) {
				ssh.disconnect();
				super.onCloseSession(session);
			}

			@Override
			protected void onOpenSession(SessionChannelNG session)
					throws IOException, SshException, ShellTimeoutException {

				byte[] tmp = new byte[1024];
				int r;
				while((r = session.getInputStream().read(tmp)) > -1) {
					tout.write(tmp, 0, r);
				}
				
				session.close();
			}
	
		};
	}

	protected void configureContext(SshClientContext context) throws SshException, IOException {
		
		File knownHostsFile = new File(new File(System.getProperty("user.home"),".ssh"), "known_hosts");
		if(!knownHostsFile.exists()) {
			knownHostsFile.getParentFile().mkdirs();
			knownHostsFile.createNewFile();
		}
		
		try(InputStream in = new FileInputStream(knownHostsFile)) {
			context.setHostKeyVerification(new KnownHostsKeyVerification(in) {
				public void onInvalidHostEntry(String entry) throws SshException {
					try {
						tout.write(String.format("WARNING: known_host file entry is invalid %s\r\n", entry).getBytes());
						tout.flush();
					} catch (IOException e) {
						throw new SshException(e);
					}	
				}
	
				public void onHostKeyMismatch(String host,
						List<SshPublicKey> allowedHostKey, SshPublicKey actualHostKey)
						throws SshException {
					// TODO change and warn or disconnect
					onUnknownHost(host, actualHostKey);
				}
	
				public void onUnknownHost(String host, SshPublicKey key)
						throws SshException {
					try {
						tout.write(String.format("The authenticity of host '%s' can't be established.\r\n", host).getBytes());
						tout.flush();
						tout.write(String.format("%s key fingerprint is %s.\r\n", key.getAlgorithm(), SshKeyUtils.getFingerprint(key)).getBytes());
						tout.flush();
						
						boolean localEcho = vt.isLocalEcho();
						vt.setLocalEcho(true);
						BufferedReader r = new BufferedReader(new InputStreamReader(tin));
						String answer;
						String msg = "Are you sure you want to continue connecting (yes/no)? ";
						do {
							tout.write(msg.getBytes());
							tout.flush();	
							
							answer = r.readLine();
							msg = "\r\nPlease type 'yes' or 'no': ";
						} while(!answer.equalsIgnoreCase("YES") && !answer.equalsIgnoreCase("NO"));
						
						vt.setLocalEcho(localEcho);
						
						if(answer.equalsIgnoreCase("YES")) {
							addEntry(key, "", host);
							FileUtils.writeStringToFile(knownHostsFile, this.toString(), "UTF-8");
						}
					
					} catch (IOException e) {
						throw new SshException(e);
					}
					
					
				}
	
				protected void onRevokedKey(String host, SshPublicKey key) {
					try {
						tout.write(String.format("%s key fingerprint %s HAS BEEN REVOKED!", key.getAlgorithm(), SshKeyUtils.getFingerprint(key)).getBytes());
						tout.flush();
					} catch (IOException e) {
					}	
				}
			});
		}
	}

	public abstract void disconnect();
}