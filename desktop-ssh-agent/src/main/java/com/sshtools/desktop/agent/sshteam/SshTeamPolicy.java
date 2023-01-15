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

import java.util.Collection;

import com.hypersocket.json.JsonResource;

public class SshTeamPolicy extends JsonResource {

	boolean enforcePolicy;
	
	int validForDays;
	
	int minimumRSA;
	
	Collection<PublicKeyType> requiredTypes;

	public boolean isEnforcePolicy() {
		return enforcePolicy;
	}

	public void setEnforcePolicy(boolean enforcePolicy) {
		this.enforcePolicy = enforcePolicy;
	}

	public int getValidForDays() {
		return validForDays;
	}

	public void setValidForDays(int validForDays) {
		this.validForDays = validForDays;
	}

	public int getMinimumRSA() {
		return minimumRSA;
	}

	public void setMinimumRSA(int minimumRSA) {
		this.minimumRSA = minimumRSA;
	}

	public Collection<PublicKeyType> getRequiredTypes() {
		return requiredTypes;
	}

	public void setRequiredTypes(Collection<PublicKeyType> requiredTypes) {
		this.requiredTypes = requiredTypes;
	}
	
	
	
}
