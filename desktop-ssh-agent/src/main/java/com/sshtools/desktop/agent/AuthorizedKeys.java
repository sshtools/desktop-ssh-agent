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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.hypersocket.json.JsonClient;
import com.hypersocket.json.JsonStatusException;
import com.sshtools.common.util.IOUtils;

public class AuthorizedKeys extends AbstractAgentProcess {

	AuthorizedKeys() throws IOException {
		super();
		
		JsonClient client = new JsonClient(hostname, port, !isStrictSSL(), false);
		client.setPath("/app");
		
		try(InputStream in = IOUtils.toInputStream(
				client.doGet("api/authenticator/authorizedKeys/" + username), "UTF-8")) {
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String key;
			while((key = reader.readLine())!=null) {
				System.out.println(key);
			}
			
		} catch (JsonStatusException e) {
			System.err.println("ERROR: " + e.getMessage());
		} 
	}

	public static void main(String[] args) throws IOException {
		new AuthorizedKeys();
	}
}
