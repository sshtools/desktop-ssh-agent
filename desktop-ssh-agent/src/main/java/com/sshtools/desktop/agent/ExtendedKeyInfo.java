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