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
import java.util.Base64;

import org.apache.commons.lang3.StringUtils;

import com.hypersocket.json.JsonClient;
import com.hypersocket.json.JsonResponse;
import com.hypersocket.json.JsonStatusException;
import com.hypersocket.json.RequestParameter;
import com.hypersocket.json.utils.HypersocketUtils;
import com.sshtools.common.logger.Log;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;

public class CheckAuthorization extends AbstractAgentProcess {

	CheckAuthorization(String[] args) throws IOException {
		
		super();
		
		String deviceName = this.deviceName;
		
		if(StringUtils.isAnyBlank(deviceName, username, hostname, privateKey, publicKey)) {
			throw new IOException("Missing configuration items");
		}
		
		
		System.out.println(String.format("Checking authorizing of device %s", deviceName));
		
		try {

			SshKeyPair pair = SshKeyUtils.getPrivateKey(privateKey, "");			
			
			JsonClient client = new JsonClient(hostname, port, !strictSSL);
			
			try {
				
				JsonResponse response;
				long timestamp = System.currentTimeMillis();

				byte[] auth = generateAuthorization(1, timestamp, authorization, username);
				String signature = Base64.getUrlEncoder().encodeToString(pair.getPrivateKey().sign(auth));
				
				response = client.doPost("api/agent/check",
						JsonResponse.class, 
						new RequestParameter("version", "1"),
						new RequestParameter("username", username),
						new RequestParameter("timestamp", String.valueOf(timestamp)),
						new RequestParameter("signature", signature),
						new RequestParameter("token", HypersocketUtils.checkNull(authorization)));
			
				if(response.isSuccess()) {
					System.out.println("Device has been authorized");
				} else {
					System.out.println("Device IS NOT authorized");
				}
				
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
	
	@SuppressWarnings("unused")
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
	
	private byte[] generateAuthorization(int version, long timestamp, String token, String principal) throws IOException {
		
		StringBuffer buffer = new StringBuffer();
		buffer.append(version);
		buffer.append("|");
		buffer.append(timestamp);
		buffer.append("|");
		buffer.append(token);
		buffer.append("|");
		buffer.append(principal);
		
		return buffer.toString().getBytes("UTF-8");
	}
	
	public static void main(String[] args) throws IOException {
		new CheckAuthorization(args);
	}
}
