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

import java.io.IOException;

import com.sshtools.agent.InMemoryKeyStore;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.components.SshPublicKey;

public class AuthorizedKeys extends AbstractAgentProcess {

	AuthorizedKeys() throws IOException {
		super();
		
		MobileDeviceKeystore store=  new  MobileDeviceKeystore(hostname, port, strictSSL, username, deviceName, authorization, new InMemoryKeyStore());
		for(SshPublicKey key : store.getPublicKeys().keySet()) {
			System.out.println(SshKeyUtils.getFormattedKey(key, ""));
		}
	}

	public static void main(String[] args) throws IOException {
		new AuthorizedKeys();
	}
}
