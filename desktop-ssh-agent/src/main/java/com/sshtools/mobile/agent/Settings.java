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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import com.hypersocket.json.utils.HypersocketUtils;
import com.sshtools.common.logger.Log;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.components.SshPublicKey;

public class Settings {

	static Settings instance;
	
	static final File SETTINGS_FILE = new File(MobileAgent.CONF_FOLDER, "preferences.properties");
	String terminalCommand;
	String terminalArguments;
	boolean useBuiltInTerminal;
	boolean useDarkIcon;
	Set<Long> favoriteIds = new HashSet<Long>();
	Set<File> keyfiles= new HashSet<File>();
	
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
			
			terminalCommand = properties.getProperty("terminalCommand");
			terminalArguments = properties.getProperty("terminalArguments");
			useBuiltInTerminal = Boolean.valueOf(properties.getProperty("useBuiltInTerminal", "true"));
			useDarkIcon = Boolean.valueOf(properties.getProperty("useDarkIcon", "false"));

			if(properties.containsKey("favorites")) {
				String[] ids = properties.get("favorites").toString().split(",");
				for(String id : ids) {
					try {
						favoriteIds.add(Long.parseLong(id));
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
		properties.put("terminalCommand", terminalCommand);
		properties.put("terminalArguments", terminalArguments);
		properties.put("favorites", HypersocketUtils.csv(favoriteIds.toArray()));
		properties.put("useBuiltInTerminal", String.valueOf(useBuiltInTerminal));
		properties.put("useDarkIcon", String.valueOf(useDarkIcon));
		
		StringBuffer buf = new StringBuffer();
		for(File keyfile : keyfiles) {
			if(buf.length() > 0) {
				buf.append(File.pathSeparator);
			}	
			buf.append(keyfile.getAbsolutePath());
		}
		properties.put("loadFiles", buf.toString());
		
		try(FileOutputStream out = new FileOutputStream(SETTINGS_FILE)) {
			properties.store(out, "Mobile Agent Preferences");
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

	public boolean isFavorite(Long id) {
		return favoriteIds.contains(id);
	}
	
	public void setFavorite(Long id) throws IOException {
		favoriteIds.add(id);
		save();
	}
	
	public void removeFavorite(Long id) throws IOException {
		favoriteIds.remove(id);
		save();
	}

	public boolean toggleFavorite(Long id) throws IOException {
		if(isFavorite(id)) {
			removeFavorite(id);
		} else {
			setFavorite(id);
		}
		return isFavorite(id);
	}

	public void setUseBuiltInTerminal(boolean selection) {
		this.useBuiltInTerminal = selection;
	}
	
	public boolean getUseBuiltInTerminal() {
		return useBuiltInTerminal;
	}

	public void addTemporaryKey(File keyfile) throws FileNotFoundException, IOException {
		keyfiles.add(keyfile);
		save();
	}
	
	public void removeTemporaryKey(File keyfile) throws FileNotFoundException, IOException {
		keyfiles.remove(keyfile);
		save();
	}

	public void removeAllKeys() throws FileNotFoundException, IOException {
		keyfiles.clear();
		save();
	}

	public void removeTemporaryKey(SshPublicKey key) throws IOException {
		File selected = null;
		for(File file : keyfiles) {
			try {
				if(SshKeyUtils.getPublicKey(file).equals(key)) {
					selected = file;
					break;
				}
			} catch (Exception e) {
				Log.debug(String.format("%s cannot be loaded!", file.getName()), e);
			}
		}
		if(!Objects.isNull(selected)) {
			removeTemporaryKey(selected);
		}
	}

	public void setUseDarkIcon(boolean useDarkIcon) {
		this.useDarkIcon = useDarkIcon;
	}
	
	public boolean getUseDarkIcon() {
		return useDarkIcon;
	}
	
	
}
