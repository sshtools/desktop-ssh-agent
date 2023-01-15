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

