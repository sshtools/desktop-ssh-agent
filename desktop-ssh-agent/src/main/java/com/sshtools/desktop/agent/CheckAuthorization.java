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
package com.sshtools.desktop.agent;

import java.io.IOException;
import java.util.Base64;

import org.apache.commons.lang3.StringUtils;

import com.hypersocket.json.JsonClient;
import com.hypersocket.json.JsonResponse;
import com.hypersocket.json.RequestParameter;
import com.hypersocket.json.utils.HypersocketUtils;
import com.sshtools.common.logger.Log;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.components.SshKeyPair;

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
			
			JsonClient client = new JsonClient(hostname, port, !strictSSL, false);
			client.setPath("/app");
			
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

	public static void main(String[] args) throws IOException {
		new CheckAuthorization(args);
	}
}
