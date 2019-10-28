package com.sshtools.mobile.agent;

import com.hypersocket.json.JsonResponse;

public class JsonSignRequestStatus extends JsonResponse {

	String signature;
	
	public JsonSignRequestStatus() {
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}
	
	
}
