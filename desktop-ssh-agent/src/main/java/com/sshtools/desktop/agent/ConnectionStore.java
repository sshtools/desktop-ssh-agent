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
package com.sshtools.desktop.agent;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sshtools.common.logger.Log;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.IOUtils;

public class ConnectionStore {

	Set<JsonConnection> localConnections;
	
	public ConnectionStore() throws IOException {
		loadCachedConnections();
	}
	
	private void loadCachedConnections() {
		
		File file = new File(AbstractAgentProcess.CONF_FOLDER, "connections.json");
		
		localConnections = new HashSet<>();
		if(file.exists())  {
			
			ObjectMapper mapper = new ObjectMapper();
			
			try {
				localConnections.addAll(mapper.readValue(IOUtils.readUTF8StringFromFile(file), new TypeReference<List<JsonConnection>>() { }));
			} catch (IOException e) {
				Log.error("Could not read local connection cache", e);
			}
		}
	}
	
	private void saveCachedConnections() {
		
		File file = new File(AbstractAgentProcess.CONF_FOLDER, "connections.json");
		ObjectMapper mapper = new ObjectMapper();
		try {
			IOUtils.writeUTF8StringToFile(file, mapper.writeValueAsString(localConnections));
		} catch (IOException e) {
			Log.error("Could not write local connection cache", e);
		}
	}

	public  List<JsonConnection> getConnections() {		
		return new ArrayList<>(localConnections);
	}

	public JsonConnection createConnection(String name, String hostname, Integer port, String remoteUsername, Set<String> aliases, Set<SshPublicKey> hostKeys) {
		
		try {
			Set<String> keys = new TreeSet<String>();
			for(SshPublicKey key : hostKeys) {
				keys.add(SshKeyUtils.getOpenSSHFormattedKey(key));
			}
			
			JsonConnection con = new JsonConnection();
			con.setName(name);
			con.setHostname(hostname);
			con.setPort(port);
			con.setUsername(remoteUsername);
			con.setAliases(aliases.toArray(new String[0]));
			con.setHostKeys(keys.toArray(new String[0]));
			
			localConnections.add(con);
			saveCachedConnections();
			return con;

		} catch(IOException e ) {
			Log.error("Failed to list connections", e);
			throw new IllegalStateException(e.getMessage(), e);
		} 
	}

	public void deleteConnection(JsonConnection con) throws IOException {
		
		localConnections.remove(con);
		saveCachedConnections();
		
	}

	public JsonConnection updateConnection(String oldName, String name, String hostname, Integer port, String remoteUsername, Set<String> aliases, Set<SshPublicKey> hostKeys) {

		try {
			Set<String> keys = new TreeSet<String>();
			for(SshPublicKey key : hostKeys) {
				keys.add(SshKeyUtils.getOpenSSHFormattedKey(key));
			}
			
			JsonConnection con = null;
			for(JsonConnection c : localConnections) {
				if(c.getName().equals(oldName)) {
					con = c;
					break;
				}
			}
			
			if(Objects.isNull(con)) {
				return createConnection(name, hostname, port, remoteUsername, aliases, hostKeys);
			}
	
			if(Objects.isNull(con)) {
				con = new JsonConnection();
			}
			con.setName(name);
			con.setHostname(hostname);
			con.setPort(port);
			con.setUsername(remoteUsername);
			con.setAliases(aliases.toArray(new String[0]));
			con.setHostKeys(keys.toArray(new String[0]));
			
			saveCachedConnections();
			return con;

		} catch(IOException e ) {
			Log.error("Failed to list connections", e);
			throw new IllegalStateException(e.getMessage(), e);
		} 
	}

}
