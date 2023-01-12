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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sshtools.common.logger.Log;

public class CommandExecutor {

	
	
	ProcessBuilder pb;
	List<String> args = new ArrayList<String>();
	StringBuffer buffer = new StringBuffer();
	File pwd = null;
	Map<String,String> env = new HashMap<String,String>();
	
	public CommandExecutor(String command) {
		args.add(command);
	}
	
	public CommandExecutor(String... cmdline){
		for(String arg : cmdline) {
			args.add(arg);
		}
	}
	
	public void set(String name, String value) {
		env.put(name, value);
	}
	
	public void addArg(String arg) {
		args.add(arg);
	}
	
	public void addArgs(String[] arguments) {
		for(String arg : arguments) {
			args.add(arg);
		}
	}
	
	public void setWorkingDirectory(File workingDirectory) {
		 pwd = workingDirectory;
	}
	
	public int execute() throws IOException {
		
		if(Log.isInfoEnabled()) {
			StringBuilder builder = new StringBuilder();
			for(String s : args) {
				builder.append(s);
				builder.append(' ');
			}
			
			Log.info("Executing command: {}", builder.toString().trim());
		}
		pb = new ProcessBuilder(args);
		
		if(pwd!=null) {
			pb.directory(pwd.getCanonicalFile());
		}
		
		pb.redirectErrorStream(true);
		
		pb.environment().putAll(env);
		
		Process p = pb.start();
		
		int r;
		while((r = p.getInputStream().read()) > -1) {
			buffer.append((char)r);
		}
		
		int exitCode;
		
		try {
			exitCode = p.waitFor();
		} catch (InterruptedException e) {
			throw new IOException(e.getMessage(), e);
		}
		
		if(Log.isDebugEnabled()) {
			Log.debug("Command output: {}", buffer.toString());
		}
		
		return exitCode;
	}
	
	public String getCommandOutput() {
		return buffer.toString();
	}
}
