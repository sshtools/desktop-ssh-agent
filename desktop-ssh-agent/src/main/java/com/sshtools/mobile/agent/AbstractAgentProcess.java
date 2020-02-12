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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import com.hypersocket.json.JsonClient;
import com.hypersocket.json.JsonStatusException;

public class AbstractAgentProcess {

	public static File CONF_FOLDER = new File(System.getProperty("agent.configDir",
			System.getProperty("user.home") + File.separator + ".mobile-agent"));
	File agentProperties = new File(CONF_FOLDER, "agent.properties");
	
	String hostname;
	int port;
	boolean strictSSL;
	String username;
	String authorization;
	String deviceName;
	String password;
	
	
	AbstractAgentProcess() throws IOException {
		
		Properties properties = loadProperties();
		
		hostname = properties.getProperty("hostname", "gateway.jadaptive.com");
		port = Integer.parseInt(properties.getProperty("port", "443"));
		strictSSL = Boolean.parseBoolean(properties.getProperty("strictSSL", "true"));
		username = properties.getProperty("username");
		authorization = properties.getProperty("authorization");
		deviceName = properties.getProperty("deviceName");
		
	}
	
	protected Properties loadProperties() throws IOException {
		Properties properties = new Properties();
		
		try {
			FileInputStream in = new FileInputStream(agentProperties);
			try {
				properties.load(in);
			} finally {
				in.close();
			}
		} catch(FileNotFoundException ex) { 	
		}
		return properties;
	}
	
	protected void saveProperty(String name, String value) throws IOException {
		
		Properties properties = loadProperties();
		
		properties.setProperty(name, value);
		if(!agentProperties.exists()) {
			agentProperties.getParentFile().mkdirs();
			agentProperties.createNewFile();
		}
		
		FileOutputStream out = new FileOutputStream(agentProperties);
		try {
			properties.store(out, "Saved by agent process");
		} finally {
			out.close();
		}
	}
	
	public static String readLine(String format, Object... args) throws IOException {
	    if (System.console() != null) {
	        return System.console().readLine(format, args);
	    }
	    System.out.print(String.format(format, args));
	    BufferedReader reader = new BufferedReader(new InputStreamReader(
	            System.in));
	    return reader.readLine();
	}
	
	public static char[] readPassword(String format, Object... args)
	        throws IOException {
	    if (System.console() != null)
	        return System.console().readPassword(format, args);
	    return readLine(format, args).toCharArray();
	}
	
	protected JsonClient createClient() throws IOException {
		return new JsonClient(hostname, port, !strictSSL);
	}
	
	protected JsonClient logonClient() throws IOException {
		
		JsonClient client = new JsonClient(hostname, port, !strictSSL);
		
		boolean promptForPassword = StringUtils.isBlank(password);
		for(int i=0;i<3;i++ ) {
			try {
				if(promptForPassword) {
					password = new String(readPassword("Password: "));
				}
				client.logon(username, password);
				break;
			} catch(IOException | JsonStatusException e) {
				System.err.println(e.getMessage());
			}
			if(!promptForPassword) {
				break;
			}
		}
		if(!client.isLoggedOn()) {
			if(promptForPassword) {
				System.err.println("Authentication failed too many times.");
			} else {
				System.err.println("Bad username or password.");
			}
			System.exit(1);					
		}
		
		return client;
	}

	public String getHostname() {
		return hostname;
	}

	public int getPort() {
		return port;
	}

	public boolean isStrictSSL() {
		return strictSSL;
	}

	public String getUsername() {
		return username;
	}

	public String getDeviceName() {
		return deviceName;
	}
	
	
}
