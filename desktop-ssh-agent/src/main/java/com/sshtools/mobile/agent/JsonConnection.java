package com.sshtools.mobile.agent;

import com.hypersocket.json.JsonAssignableResource;

public class JsonConnection extends JsonAssignableResource {

	String  hostname;
	int port;
	String username;
	String[] aliases;
	String[] hostKeys;
	
	public String getHostname() {
		return hostname;
	}
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String[] getAliases() {
		return aliases;
	}
	public void setAliases(String[] aliases) {
		this.aliases = aliases;
	}
	public String[] getHostKeys() {
		return hostKeys;
	}
	public void setHostKeys(String[] hostKeys) {
		this.hostKeys = hostKeys;
	}
	
	
}
