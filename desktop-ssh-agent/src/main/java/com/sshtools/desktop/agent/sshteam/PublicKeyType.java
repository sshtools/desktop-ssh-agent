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
package com.sshtools.desktop.agent.sshteam;

import com.sshtools.synergy.ssh.SshContext;

public enum PublicKeyType {

	ED25519("ssh-ed25519", 256),
	ED448("ssh-ed448", 448),
	RSAwith2048bits("ssh-rsa", 2048),
	RSAwith3192bits("ssh-rsa", 3192),
	RSAwith4096bits("ssh-rsa", 4096),
	ECDSAwith256bits("ecdsa", 256, SshContext.PUBLIC_KEY_ECDSA_SHA2_NISPTP_256),
	ECDSAwith384bits("ecdsa", 384, SshContext.PUBLIC_KEY_ECDSA_SHA2_NISPTP_384),
	ECDSAwith521bits("ecdsa", 512, SshContext.PUBLIC_KEY_ECDSA_SHA2_NISPTP_521);
	
	String type;
	String algorithm;
	int bits;
	
	PublicKeyType(String type, int bits) {
		this(type, bits, type);
	}
	
	PublicKeyType(String type, int bits, String algorithm) {
		this.type = type;
		this.bits = bits;
		this.algorithm = algorithm;
	}
	
	@Override
	public String toString() {
		return name();
	}
	
	public String getGroupType() {
		return type;
	}

	public int getBits() {
		return bits;
	}

	public String getAlgorithm() {
		return algorithm;
	}
}

