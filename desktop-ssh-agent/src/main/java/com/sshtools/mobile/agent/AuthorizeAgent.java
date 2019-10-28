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
import java.net.InetAddress;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;

import com.hypersocket.json.JsonClient;
import com.hypersocket.json.JsonResponse;
import com.hypersocket.json.RequestParameter;
import com.hypersocket.json.utils.HypersocketUtils;
import com.sshtools.common.logger.Log;
import com.sshtools.common.publickey.RsaUtils;
import com.sshtools.common.publickey.SshKeyPairGenerator;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshRsaPrivateKey;

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
			
			
			SshKeyPair pair = SshKeyPairGenerator.generateKeyPair(SshKeyPairGenerator.SSH2_RSA, 2048);			
			
			String token = deviceName + "|" + hostname + "|" + port + "|" + username + "|" + RandomUtils.nextLong();
			token = RsaUtils.encrypt((SshRsaPrivateKey)pair.getPrivateKey(), token);
			
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

//				System.out.println("An authentication request has been sent to your mobile device. Please authorize it to continue.");
				
				response = client.doPost("api/agent/authorize", JsonResponse.class,
						new RequestParameter("previousToken", StringUtils.defaultString(authorization)),
						new RequestParameter("token", token),
						new RequestParameter("username", username),
						new RequestParameter("overwrite", String.valueOf(overwrite)),
						new RequestParameter("key", SshKeyUtils.getFormattedKey(pair.getPublicKey(), "Mobile SSH Agent Device")));
	
				if(!response.isSuccess()) {
					throw new IOException(response.getMessage());
				}
				
				token = response.getMessage();
								
				saveProperty("username", username);
				saveProperty("hostname", hostname);
				saveProperty("port", String.valueOf(port));
				saveProperty("strictSSL", String.valueOf(strictSSL));
				saveProperty("authorization", token);
				saveProperty("deviceName", deviceName);
			
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
	
	public static void main(String[] args) throws IOException {
		new AuthorizeAgent(args);
	}
}
