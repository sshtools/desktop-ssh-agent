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

import org.apache.commons.lang3.StringUtils;

import com.hypersocket.json.JsonClient;
import com.hypersocket.json.JsonResponse;
import com.sshtools.common.logger.Log;

public class CheckAuthorization extends AbstractAgentProcess {

	CheckAuthorization(String[] args) throws IOException {
		
		super();
		
		String deviceName = this.deviceName;
		
		if(StringUtils.isAnyBlank(deviceName, username, hostname, privateKey, publicKey)) {
			throw new IOException("Missing configuration items");
		}
		
		
		System.out.println(String.format("Checking authorizing of device %s", deviceName));
		
		try {

			JsonClient client = new JsonClient(hostname, port, !strictSSL, false);
			client.setPath("/app");
			
			try {
				JsonResponse response;
				response = client.doPost("api/agent/check",
						JsonResponse.class, 
						generateAuthorizationParameters());
			
				if(response.isSuccess()) {
					Log.info("Device has been authorized");
				} else {
					Log.warn("Device IS NOT authorized");
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
