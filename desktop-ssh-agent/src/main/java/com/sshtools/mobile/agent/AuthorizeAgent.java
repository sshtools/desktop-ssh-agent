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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.Base64;

import org.apache.commons.lang3.StringUtils;

import com.hypersocket.json.JsonClient;
import com.hypersocket.json.JsonResponse;
import com.hypersocket.json.JsonStatusException;
import com.hypersocket.json.RequestParameter;
import com.hypersocket.json.utils.HypersocketUtils;
import com.sshtools.common.logger.Log;
import com.sshtools.common.publickey.SshKeyPairGenerator;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;

public class AuthorizeAgent extends AbstractAgentProcess {

	
	boolean silent = false;
	boolean forceOverwrite = false;
	AuthorizeAgent(String[] args) throws IOException {
		
		super();
		
		String deviceName = this.deviceName;
		
		for(String arg : args) {
			if(arg.startsWith("--name=")) {
				deviceName = StringUtils.substringAfter(arg, "--name=");
			}
			if(arg.startsWith("--password=")) {
				password = StringUtils.substringAfter(arg, "--password=");
			}
			if(arg.startsWith("--silent")) {
				silent = true;
			}
			if(arg.startsWith("--force")) {
				if(arg.startsWith("--force=")) {
					forceOverwrite = Boolean.valueOf(arg.substring(8));
				} else {
					forceOverwrite = true;
				}
				
			}
		}
		
		if(StringUtils.isBlank(deviceName)) {
			deviceName = InetAddress.getLocalHost().getHostName();
		}
		
		if(StringUtils.isAnyBlank(username, hostname) && silent) {
			throw new IOException("Missing username or hostname from configuration items");
		}
		
		if(StringUtils.isBlank(username)) {
			username = readLine("Username: ");
			
			if(readLine("Configure Host? ").equalsIgnoreCase("y")) {
				hostname = null;
			}
		}
		
		if(StringUtils.isBlank(hostname)) {
			hostname = readLine("Hostname: ");
			port = Integer.parseInt(readLine("Port: "));
			strictSSL = readLine("StrictSSL: ").equalsIgnoreCase("y");
		}
		
		
		System.out.println(String.format("Authorizing the device %s", deviceName));
		
		try {

			SshKeyPair pair = SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.ECDSA, 521);			
			String key = SshKeyUtils.getFormattedKey(pair.getPublicKey(), "Desktop SSH Agent");
			byte[] newToken = pair.getPrivateKey().sign(generateToken(deviceName, username, 
					key, 
					StringUtils.defaultString(authorization)));
			
			JsonClient client = logonClient();
			
			try {
				
				JsonResponse response;
				boolean overwrite = forceOverwrite;
				
				if(!overwrite) {
					do {
						response = client.doPost("api/agent/verify/" + deviceName + "/",
								JsonResponse.class, new RequestParameter("authorization", HypersocketUtils.checkNull(authorization)));
					
						if(!response.isSuccess()) {
							
							if(silent) {
								System.err.println(String.format("You already have a device named %s", deviceName));
								System.exit(1);
							} else {
								String answer = readLine("You already have a device named %s. Would you like to overwrite? [Y] ", deviceName);
								if(!answer.toLowerCase().equals("y") && !answer.toLowerCase().equals("yes") && !answer.equals("")) {
									deviceName = readLine("Enter Device Name: ");
									continue;
								} else {
									overwrite = true;
									break;
								}
							}
							
						}
						
						break;
					} while(true);
				}

				response = client.doPost("api/agent/authorize", JsonResponse.class,
						new RequestParameter("previousToken", StringUtils.defaultString(authorization)),
						new RequestParameter("token", Base64.getUrlEncoder().encodeToString(newToken)),
						new RequestParameter("version", "1"),
						new RequestParameter("deviceName", deviceName),
						new RequestParameter("username", username),
						new RequestParameter("overwrite", String.valueOf(overwrite)),
						new RequestParameter("key", key));
	
				if(!response.isSuccess()) {
					throw new IOException(response.getMessage());
				}
				
				String previousToken = authorization;
				authorization = response.getMessage();
								
				validateAuthorization(client, username, key, previousToken, deviceName, authorization);
				
				saveProperty("username", username);
				saveProperty("hostname", hostname);
				saveProperty("port", String.valueOf(port));
				saveProperty("strictSSL", String.valueOf(strictSSL));
				saveProperty("authorization", authorization);
				saveProperty("deviceName", deviceName);
				saveProperty("privateKey", SshKeyUtils.getFormattedKey(pair, ""));
				saveProperty("publicKey", key);
				
				System.out.println("Device has been authorized");
				
			} finally {
				try {
					client.logoff();
				} catch(Throwable t) { }
			}
			
		} catch(Throwable t) {
			System.err.println(String.format("The device could not be authorized: %s", t.getMessage()));
			Log.error("Failed to authorize device", t);
			System.exit(1);
		}
		
		
	}
	
	private void validateAuthorization(JsonClient client, String username, String key, String previousToken, String deviceName, String authorization) throws IOException, JsonStatusException, SshException {
	
		String authorizedKeys = client.doGet("api/agent/authorizedKeys/" + username);
		
		byte[] data = generateToken(deviceName, username, key, previousToken);

		BufferedReader reader = new BufferedReader(new StringReader(authorizedKeys));
		String publicKey;
		while((publicKey = reader.readLine()) != null) {
			
			SshPublicKey k = SshKeyUtils.getPublicKey(publicKey);
			
			if(!k.verifySignature(Base64.getUrlDecoder().decode(authorization), data)) {
				continue;
			}
			
			return;
		}
		
		throw new IOException("Invalid signature in authorization response");
		
	}

	private byte[] generateToken(String deviceName, String principalName, String key, String previousToken) throws UnsupportedEncodingException {
		
		StringBuffer buffer = new StringBuffer();
		buffer.append(deviceName);
		buffer.append("|");
		buffer.append(principalName);
		buffer.append("|");
		buffer.append(key);
		buffer.append("|");
		buffer.append(StringUtils.defaultString(previousToken, ""));
		
		return buffer.toString().getBytes("UTF-8");
	}
	
	public static void main(String[] args) throws IOException {
		new AuthorizeAgent(args);
	}
}
