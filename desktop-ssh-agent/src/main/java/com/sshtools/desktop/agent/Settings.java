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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.hypersocket.json.utils.HypersocketUtils;
import com.sshtools.common.ssh.components.SshPublicKey;

public class Settings {
	
	public enum IconMode {
		AUTO, DARK, LIGHT
	}

	static Settings instance;
	
	static final File SETTINGS_FILE = new File(DesktopAgent.CONF_FOLDER, "preferences.properties");
	
	private String terminalCommand;
	private String terminalArguments;
	private boolean useBuiltInTerminal;

	private Set<String> favoriteIds = new HashSet<String>();
	private Set<File> keyfiles = new HashSet<>();
	private IconMode iconMode = IconMode.AUTO;
	private boolean synchronizeKeys = false;
	
	private String logonboxDomain;
	private int logonboxPort = 443;
	private String logonboxUsername;
	
	private String sshteamDomain;
	private int sshteamPort = 443;
	private String sshteamUsername;
	
	private boolean strictSSL = true;
	
	Settings() {
		terminalCommand = "";
		terminalArguments = "";
	}
	
	public Set<File> getKeyFiles() {
		return keyfiles;
	}
	
	public void load() throws IOException {
		
		Properties properties =  new  Properties();
		
		if(SETTINGS_FILE.exists()) {
			try(FileInputStream in = new FileInputStream(SETTINGS_FILE)) {
				properties.load(in);
			}
			
			terminalCommand = properties.getProperty("terminalCommand", "");
			terminalArguments = properties.getProperty("terminalArguments", "");
			useBuiltInTerminal = Boolean.valueOf(properties.getProperty("useBuiltInTerminal", "true"));
			
			iconMode = IconMode.valueOf(properties.getProperty("iconMode", IconMode.AUTO.name()));

			logonboxDomain = properties.getProperty("logonboxDomain");
			logonboxUsername = properties.getProperty("logonboxUsername");
			logonboxPort = Integer.parseInt(properties.getProperty("logonboxPort", "443"));
			
			sshteamDomain = properties.getProperty("sshteamDomain");
			sshteamUsername = properties.getProperty("sshteamUsername");
			sshteamPort = Integer.parseInt(properties.getProperty("sshteamPort", "443"));
			
			synchronizeKeys = Boolean.valueOf(properties.getProperty("synchronizeKeys", "false"));
			strictSSL = Boolean.valueOf(properties.getProperty("strictSSL", "true"));
			
			if(properties.containsKey("favorites")) {
				String[] names = properties.get("favorites").toString().split(",");
				for(String name : names) {
					try {
						favoriteIds.add(name);
					} catch (NumberFormatException e) {
					}
				}
			}
			
			if(properties.containsKey("loadFiles")) {
				
				String[] keyfiles = properties.getProperty("loadFiles").split(File.pathSeparator);
				for(String keyfile : keyfiles) {
					File file = new File(keyfile);
					this.keyfiles.add(file);
				}
			}
		}
	}
	
	public void save() throws FileNotFoundException, IOException {
		Properties properties = new Properties();
		properties.put("terminalCommand",  StringUtils.defaultString(terminalCommand));
		properties.put("terminalArguments",  StringUtils.defaultString(terminalArguments));
		properties.put("favorites", HypersocketUtils.csv(favoriteIds.toArray()));
		properties.put("useBuiltInTerminal", String.valueOf(useBuiltInTerminal));

		properties.put("logonboxDomain", StringUtils.defaultString(logonboxDomain));
		properties.put("logonboxUsername",  StringUtils.defaultString(logonboxUsername));
		properties.put("logonboxPort", String.valueOf(logonboxPort));
		
		properties.put("sshteamDomain",  StringUtils.defaultString(sshteamDomain));
		properties.put("sshteamUsername",  StringUtils.defaultString(sshteamUsername));
		properties.put("sshteamPort", String.valueOf(sshteamPort));
		
		properties.put("synchronizeKeys", String.valueOf(synchronizeKeys));
		properties.put("strictSSL", String.valueOf(strictSSL));
		
		properties.put("iconMode", iconMode.name());
		
		StringBuffer buf = new StringBuffer();
		for(File keyfile : keyfiles) {
			if(buf.length() > 0) {
				buf.append(File.pathSeparator);
			}	
			buf.append(keyfile.getAbsolutePath());
		}
		properties.put("loadFiles", buf.toString());
		
		SETTINGS_FILE.getParentFile().mkdirs();
		try(FileOutputStream out = new FileOutputStream(SETTINGS_FILE)) {
			properties.store(out, "Desktop SSH Agent Preferences");
		}
	}
	
	public static Settings getInstance() {
		return instance ==  null ? instance = new Settings() : instance;
	}

	public String getTerminalCommand() {
		return terminalCommand;
	}

	public void setTerminalCommand(String terminalCommand) {
		this.terminalCommand = terminalCommand;
	}

	public String getTerminalArguments() {
		return terminalArguments;
	}

	public void setTerminalArguments(String terminalArguments) {
		this.terminalArguments = terminalArguments;
	}

	public boolean isFavorite(String name) {
		return favoriteIds.contains(name);
	}
	
	public void setFavorite(String name) throws IOException {
		favoriteIds.add(name);
		save();
	}
	
	public void removeFavorite(String name) throws IOException {
		favoriteIds.remove(name);
		save();
	}

	public boolean toggleFavorite(String name) throws IOException {
		if(isFavorite(name)) {
			removeFavorite(name);
		} else {
			setFavorite(name);
		}
		return isFavorite(name);
	}

	public void setUseBuiltInTerminal(boolean selection) {
		this.useBuiltInTerminal = selection;
	}
	
	public boolean getUseBuiltInTerminal() {
		return useBuiltInTerminal;
	}

	public void addPrivateKey(SshPublicKey key, File keyfile) throws FileNotFoundException, IOException {
		keyfiles.add(keyfile);
		save();
	}
	
	public void removePrivateKey(File keyfile) throws FileNotFoundException, IOException {
		keyfiles.remove(keyfile);
		save();
	}

	public void removeAllKeys() throws FileNotFoundException, IOException {
		keyfiles.clear();
		save();
	}


	public void setIconMode(IconMode iconMode) {
		this.iconMode = iconMode;
	}
	
	public IconMode getIconMode() {
		return iconMode;
	}

	public boolean isSynchronizeKeys() {
		return synchronizeKeys;
	}

	public void setSynchronizeKeys(boolean synchronizeKeys) {
		this.synchronizeKeys = synchronizeKeys;
	}

	public String getLogonboxDomain() {
		return logonboxDomain;
	}

	public void setLogonboxDomain(String logonboxDomain) {
		this.logonboxDomain = logonboxDomain;
	}

	public int getLogonboxPort() {
		return logonboxPort;
	}

	public void setLogonboxPort(int logonboxPort) {
		this.logonboxPort = logonboxPort;
	}

	public String getLogonboxUsername() {
		return logonboxUsername;
	}

	public void setLogonboxUsername(String logonboxUsername) {
		this.logonboxUsername = logonboxUsername;
	}

	public String getSshteamDomain() {
		return sshteamDomain;
	}

	public void setSshteamDomain(String sshteamDomain) {
		this.sshteamDomain = sshteamDomain;
	}

	public int getSshteamPort() {
		return sshteamPort;
	}

	public void setSshteamPort(int sshteamPort) {
		this.sshteamPort = sshteamPort;
	}

	public String getSshteamUsername() {
		return sshteamUsername;
	}

	public void setSshteamUsername(String sshteamUsername) {
		this.sshteamUsername = sshteamUsername;
	}

	public boolean isStrictSSL() {
		return strictSSL;
	}

	public void setStrictSSL(boolean strictSSL) {
		this.strictSSL = strictSSL;
	}
	
}
