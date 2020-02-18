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

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import com.hypersocket.json.JsonClient;
import com.hypersocket.json.JsonResponse;
import com.hypersocket.json.RequestParameter;
import com.sshtools.common.logger.Log;

public class DeauthorizeAgent extends AbstractAgentProcess {

	

	
	DeauthorizeAgent(String[] args) throws IOException {
		
		super();
		
		if(StringUtils.isBlank(authorization)) {
			System.err.println("This device does not have an authorization code to deauthorize!");
			System.exit(1);
		}
		
		if(StringUtils.isAnyBlank(username, hostname)) {
			throw new IOException("Missing username and/or hostname from configuration.");
		}
		
		for(String arg : args) {
			if(arg.startsWith("--password=")) {
				password = StringUtils.substringAfter(arg, "--password=");
			}
		}
		
		System.out.println(String.format("Deauthorizing the device %s", deviceName));
		
		try {
			if(!CONF_FOLDER.exists()) {
				CONF_FOLDER.mkdirs();
			}
			
			File key = new File(CONF_FOLDER, "key");
			
			JsonClient client = logonClient();
			
			try {
				JsonResponse response = client.doPost("api/agent/deauthorize", JsonResponse.class,
						new RequestParameter("token", authorization),
						new RequestParameter("username", username));
	
				if(!response.isSuccess()) {
					throw new IOException(response.getMessage());
				}
				
				key.delete();
				CONF_FOLDER.delete();
				
				saveProperty("authorization", "");
				System.out.println("Device has been deauthorized");
			
			} finally {
				try {
					client.logoff();
				} catch(Throwable t) { }
			}
			
		} catch(Throwable t) {
			System.err.println("The device could not be deauthorized. " + t.getMessage());
			Log.error("Failed to deauthorize device", t);
		}
		
		
	}

	public static void main(String[] args) throws IOException {
		new DeauthorizeAgent(args);
	}
}
