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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.install4j.api.launcher.ApplicationLauncher;
import com.sshtools.jaul.AbstractUpdateService;
import com.sshtools.jaul.AppRegistry.App;
import com.sshtools.jaul.Install4JUpdater.Install4JUpdaterBuilder;
import com.sshtools.jaul.UpdateableAppContext;

public class Install4JUpdateService extends AbstractUpdateService {

	static Logger log = LoggerFactory.getLogger(Install4JUpdateService.class);

	private final App app;

	Install4JUpdateService(UpdateableAppContext context, String version, App app) {
		super(context, version);
		this.app = app;
		
		/* Force loading of I4J so if it doesn't exist we know earlier */
		ApplicationLauncher.isNewArchiveInstallation();
	}

	@Override
	protected String doUpdate(boolean checkOnly) throws IOException {
		return Install4JUpdaterBuilder.builder().
				withCheckOnly(checkOnly).
				withConsoleMode(true).
				withCurrentVersion(getCurrentVersion()).
				withLauncherId(app.getLauncherId()).
				withUpdateUrl(app.getUpdatesUrl().get().replace("${phase}", getContext().getPhase().name().toLowerCase())).
				onExit((e) -> System.exit(e)).build().call();

	}

}
