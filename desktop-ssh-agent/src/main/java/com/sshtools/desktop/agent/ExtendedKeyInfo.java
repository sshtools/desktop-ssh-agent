package com.sshtools.desktop.agent;

import java.io.File;

import com.sshtools.agent.KeyConstraints;

public class ExtendedKeyInfo extends KeyConstraints {
		File file;
		boolean teamKey = false;
		String name;
		ExtendedKeyInfo(File file, String name) {
			super();
			this.name = name;
			this.file = file;
		}
		
		public File getFile() {
			return file;
		}

		public boolean isTeamKey() {
			return teamKey;
		}

		public void setTeamKey(boolean teamKey) {
			this.teamKey = teamKey;
		}
		
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
	}