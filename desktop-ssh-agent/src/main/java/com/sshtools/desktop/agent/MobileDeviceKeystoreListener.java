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

import java.io.IOException;

import com.sshtools.agent.KeyConstraints;
import com.sshtools.common.ssh.components.SshPrivateKey;
import com.sshtools.common.ssh.components.SshPublicKey;

public interface MobileDeviceKeystoreListener {

	boolean addKey(SshPrivateKey prvkey, SshPublicKey pubkey, String description, KeyConstraints cs) throws IOException;

	boolean deleteAllKeys();

	void onKeysChanged();

	boolean deleteKey(SshPublicKey pubkey);

}
