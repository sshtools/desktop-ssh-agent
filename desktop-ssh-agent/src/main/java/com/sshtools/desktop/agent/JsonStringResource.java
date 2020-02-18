package com.sshtools.desktop.agent;

import com.hypersocket.json.JsonRequestStatus;

public class JsonStringResource extends JsonRequestStatus {

	String resource;

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}
	
	
}
