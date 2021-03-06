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
package com.sshtools.desktop.agent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;

import com.hypersocket.json.JsonClient;
import com.hypersocket.json.JsonResourceStatus;
import com.hypersocket.json.JsonStatusException;
import com.hypersocket.json.RequestParameter;
import com.hypersocket.json.utils.FileUtils;
import com.sshtools.agent.InMemoryKeyStore;
import com.sshtools.agent.KeyConstraints;
import com.sshtools.agent.openssh.OpenSSHConnectionFactory;
import com.sshtools.agent.server.SshAgentServer;
import com.sshtools.agent.win32.AbstractNamedPipe;
import com.sshtools.agent.win32.NamedPipeServer;
import com.sshtools.common.knownhosts.KnownHostsKeyVerification;
import com.sshtools.common.logger.Log;
import com.sshtools.common.publickey.InvalidPassphraseException;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.publickey.SshPrivateKeyFile;
import com.sshtools.common.publickey.SshPrivateKeyFileFactory;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPrivateKey;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.ssh.components.jce.JCEProvider;
import com.sshtools.desktop.agent.Settings.IconMode;
import com.sshtools.desktop.agent.swt.ConnectionDialog;
import com.sshtools.desktop.agent.swt.CustomDialog;
import com.sshtools.desktop.agent.swt.InputForm;
import com.sshtools.desktop.agent.swt.SWTAboutDialog;
import com.sshtools.desktop.agent.swt.SWTUtil;
import com.sshtools.desktop.agent.swt.SettingsDialog;
import com.sshtools.desktop.agent.term.ShellTerminalConnector;
import com.sshtools.desktop.agent.term.TerminalDisplay;
import com.sshtools.twoslices.Toast;
import com.sshtools.twoslices.ToastType;
import com.sshtools.twoslices.ToasterFactory;
import com.sshtools.twoslices.ToasterSettings;

public class DesktopAgent extends AbstractAgentProcess implements MobileDeviceKeystoreListener {

	public final static String WINDOWS_NAMED_PIPE = "mobile-ssh-agent";
	public final static String SSH_AGENT_PIPE = AbstractNamedPipe.NAMED_PIPE_PREFIX + WINDOWS_NAMED_PIPE;
	
	SshAgentServer server;
	MobileDeviceKeystore keystore;
	Display display;
	Shell shell;
	Table keyTable;
	Shell keyShell;
	Table knownHostsTable;
	Shell knownHostsShell;
	Shell connectionsShell;
	Table connectionsTable;
	KnownHostsKeyVerification knownHosts = new KnownHostsKeyVerification();
	FileAlterationMonitor monitor; 
	Process pageantProcess;
	
	org.eclipse.swt.widgets.Menu swtConnections;
	Path agentSocketPath;
	Map<String,JsonConnection> connections = new HashMap<String,JsonConnection>();
	Map<SshPublicKey, String> deviceKeys = new HashMap<SshPublicKey, String>();
	Timer timer;
	
	InMemoryKeyStore localKeys = new InMemoryKeyStore();
	AtomicBoolean online = new AtomicBoolean();
	Runnable restartCallback;
	Runnable shutdownCallback;
	
	private IconMode iconMode;
	private TrayItem item;
	private Thread darkModePoll;
	private String lastIcon;
	
	boolean quiting;
	
	protected DesktopAgent(Display display, Runnable restartCallback, Runnable shutdownCallback) throws IOException {

		super();
		this.display = display;
		this.restartCallback = restartCallback;
		this.shutdownCallback = shutdownCallback;
		
		JCEProvider.enableBouncyCastle(true);
		
		shell = new Shell(display, SWT.NONE);
		shell.setImage(new Image(display, Image.class.getResourceAsStream("/new_icon.png")));
		
		try {
			
			Settings.getInstance().load();
			
			keystore = new MobileDeviceKeystore(this, localKeys);
			keystore.setListener(this);
			
			if (SystemUtils.IS_OS_WINDOWS) {
				startupWindows();
			} else if (SystemUtils.IS_OS_LINUX) {
				startupLinux();
			} else if (SystemUtils.IS_OS_MAC) {
				startupOSX();
			}

			Runtime.getRuntime().addShutdownHook(new Thread("Shutdown-Thread") {
				public void run() {
					quit(false);
				}
			});

			setupSystemTray();

			loadKeys();
			loadKnownHostsFromFile();
			
			timer = new Timer("Network Check", true);
			timer.scheduleAtFixedRate(new TimerTask() {
				
				boolean firstRun = true;
				long lastUpdated = System.currentTimeMillis();
				
				@Override
				public void run() {
					try {
						
						boolean hasCredentials = !StringUtils.isAnyBlank(username, authorization, deviceName);
						
						if(hasCredentials) {
							boolean wasOffline = !online.getAndSet(keystore.ping());
							if(online.get() && (firstRun || wasOffline)) {
								if(Log.isInfoEnabled()) {
									Log.info("The agent is back online");
								}
								Toast.toast(ToastType.INFO, "Agent Connection", String.format("The agent has connected to %s", hostname));
								firstRun = true;
							} else if(!online.get() && (firstRun || !wasOffline)) {
								if(Log.isInfoEnabled()) {
									Log.info("The agent is offline");
								}
								Toast.toast(ToastType.WARNING, "Agent Connection", String.format("The agent %s connected to %s", 
										firstRun ? "could not be " : "is no longer", hostname));
							}
							
							if(online.get() && (firstRun || System.currentTimeMillis() - lastUpdated > 60000L * 10)) {
								lastUpdated= System.currentTimeMillis();
								loadConnections();
								loadDeviceKeys();
							}
						}

						
					} catch (Throwable e) {
						Log.error("Network check error", e);
					} finally {
						firstRun = false;
					}
				}
			}, 0L, 30000L);
					
			
			runSWT();
		} catch (Throwable t) {
			Log.error("Failed to startup MobileAgent", t);
			showFatalError(t.getMessage());
		}
	}

	private void loadKeys() {
		
		for(File keyfile : Settings.getInstance().getKeyFiles()) {
			if(!keyfile.exists() || keyfile.isDirectory()) {
				continue;
			}
			try(InputStream in = new FileInputStream(keyfile)) {
        		
            		SshPrivateKeyFile file = SshPrivateKeyFileFactory.parse(in);
            		SshKeyPair pair = null;
            		if(file.isPassphraseProtected()) {
            			for(int i=0;i<3;i++) {
            				InputForm form = new InputForm(display,  "Passphrase Required",
            						String.format("Please enter your passphrase for key file %s", keyfile.getName()), 
            						"", true);
            				if(form.show()) {
            					try {
								pair = file.toKeyPair(form.getInput());
							} catch (InvalidPassphraseException e) {
								SWTUtil.showError("Add Key", "Invalid passphrase!");
								continue;
							}
            					break;
            				} 
            			}
            		} else {
            			try {
						pair = file.toKeyPair(null);
					} catch (InvalidPassphraseException e) {
						SWTUtil.showError("Add Key", "An unexpected passphrase error occurred on a key that reported to not have any passphrase.");
					}
            		}
            		
            		if(pair==null) {
            			SWTUtil.showError("Add Key", String.format("The key %s could not be read.", keyfile.getName()));
            		} else {
            			localKeys.addKey(pair, keyfile.getName(), new KeyConstraints());
            		}
    		
            	} catch(IOException ex) {
            		SWTUtil.showError("Add Key", String.format("An unexpected error occurred.\r\n\r\n%s", ex.getMessage()));
            	} 
		}
		
	}

	private void runSWT() {
		try {
			while (!shell.isDisposed()) {
				try {
					if (!display.readAndDispatch())
						display.sleep();
				} catch (Throwable e) {
					if(!display.isDisposed()) {
						SWTUtil.showError("SWT", e.getMessage());
					} else {
						break;
					}
				}
			}
			
		} finally {
			quit(false);
		}
	}

	private void showFatalError(String message) {

		if(quiting) {
			return;
		}
		
		if (message == null) {
			message = "No error message provided. Please contact support@jadaptive.com";
		}
		
		SWTUtil.showError("Fatal Error", message);

		shell.dispose();
		display.dispose();

		System.exit(0);
	}

	public String getAgentSocketPath() {
		return agentSocketPath.toString();
	}
	
	public void showAboutBox() {

		SWTUtil.safeAsyncExec(new Runnable() {
			public void run() {
				new SWTAboutDialog(display, "Close", "About",
						new Image(display, DesktopAgent.class.getResourceAsStream("/new_icon.png")),
						"Desktop SSH Agent", "Part of the JADAPTIVE Key Server solution.", "\u00a9 2003-2020 JADAPTIVE Limited",
						"https://www.jadaptive.com");
			}
		});

	}
	
	public void showSettings() {

		SWTUtil.safeAsyncExec(new Runnable() {
			public void run() {
				SettingsDialog settings = new SettingsDialog(display, DesktopAgent.this);
				settings.open();
			}
		});

	}

	private void setupSystemTray() throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		display.asyncExec(() -> setupSWTTray());
	}

	private void loadConnections() {
		synchronized(connections) {
			if(Log.isInfoEnabled()) {
				Log.info("Reloading connections");
			}

			Map<String,JsonConnection> deletedConnections = new HashMap<String,JsonConnection>();
			deletedConnections.putAll(connections);
			for(JsonConnection con : connections.values()) {
				removeConnection(con);
			}

			connections.clear();
			for(JsonConnection con : keystore.getConnections()) {

				if(!deletedConnections.containsKey(con.getName())) {
					if(Log.isInfoEnabled()) {
						Log.info("Added connection {}@{}:{} ({})", con.getUsername(), con.getHostname(), con.getPort(), con.getId());
					}
				} else {
					if(Log.isInfoEnabled()) {
						Log.info("Existing connection {}@{}:{} ({})", con.getUsername(), con.getHostname(), con.getPort(), con.getId());
					}					
				}
				
				connections.put(con.getName(), con);
				deletedConnections.remove(con.getName());
				
				if(Settings.getInstance().isFavorite(con.getName())) {
					SWTUtil.safeAsyncExec(new Runnable() {
						public void run() {
							addFavoriteConnection(con);
						}
					});
					
				}
				
			}
		}
		
	}
	
	public void resetIcon() {
		IconMode mode = Settings.getInstance().getIconMode();
		if(mode != this.iconMode) {
			this.iconMode = mode;
			if(iconMode == IconMode.AUTO) {
				darkModePoll = new Thread("DarkModePoll") {

					public void run() {
						try {
							while(darkModePoll != null) {
								display.asyncExec(() -> {
									String icon = getIconForMode();
									if(!icon.equals(lastIcon)) {
										lastIcon = icon;
										 item.setImage(new Image(display, Image.class.getResourceAsStream(icon)));
									}
								});
								Thread.sleep(60000);
							}
						}
						catch(Exception e) {
						}
					}
				};
				darkModePoll.setDaemon(true);
				darkModePoll.start();
			}
			else {
				if(darkModePoll != null) {
					Thread t = darkModePoll;
					darkModePoll = null;
					t.interrupt();
				}

				display.asyncExec(() -> {
					String icon = getIconForMode();
					if(!icon.equals(lastIcon)) {
						lastIcon = icon;
						 item.setImage(new Image(display, Image.class.getResourceAsStream(icon)));
					}
				});
			}
		}
	}
	
	String getIconForMode() {
		switch(iconMode) {
		case DARK:
			return "/new_icon_white.png";
		case LIGHT:
			return "/new_icon_black.png";
		default:
			if(Display.isSystemDarkTheme())
				return "/new_icon_white.png";
			else
				return "/new_icon_black.png";
		}
	}
	
	private void setupSWTTray() {
		resetIcon();
		Image image = new Image(display, Image.class.getResourceAsStream(lastIcon = getIconForMode()));
		final Tray tray = display.getSystemTray();
		if (tray == null) {
			System.out.println("The system tray is not available");
		} else {
			if(Log.isInfoEnabled()) {
				Log.info("Setting up SWT tray");
			}
			item = new TrayItem(tray, SWT.NONE);
			
			ToasterFactory.setSettings(new ToasterSettings().setParent(item).setAppName("Desktop SSH Agent"));
			item.setToolTipText("The agent is running");

			final org.eclipse.swt.widgets.Menu menu = new org.eclipse.swt.widgets.Menu(shell, SWT.POP_UP);

			org.eclipse.swt.widgets.MenuItem mConnections = new org.eclipse.swt.widgets.MenuItem(menu, SWT.CASCADE);
			mConnections.setText("Favorites");
			swtConnections = new org.eclipse.swt.widgets.Menu(menu);
			mConnections.setMenu(swtConnections);
			
			org.eclipse.swt.widgets.MenuItem mCreate = new org.eclipse.swt.widgets.MenuItem(menu, SWT.PUSH);
			mCreate.setText("Connections");
			mCreate.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					showConnections();
				}
			});
			
			org.eclipse.swt.widgets.MenuItem mHosts = new org.eclipse.swt.widgets.MenuItem(menu, SWT.PUSH);
			mHosts.setText("Known Hosts");
			mHosts.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					showKnownHosts();
				}
			});
			
			org.eclipse.swt.widgets.MenuItem mKeys = new org.eclipse.swt.widgets.MenuItem(menu, SWT.PUSH);
			mKeys.setText("Private Keys");
			mKeys.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					showKeyTable();
				}
			});
			
			org.eclipse.swt.widgets.MenuItem mPrefs = new org.eclipse.swt.widgets.MenuItem(menu, SWT.PUSH);
			mPrefs.setText("Preferences");
			mPrefs.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					showSettings();
				}
			});
		
			org.eclipse.swt.widgets.MenuItem mAbout = new org.eclipse.swt.widgets.MenuItem(menu, SWT.PUSH);
			mAbout.setText("About");
			mAbout.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					showAboutBox();
				}
			});

			new org.eclipse.swt.widgets.MenuItem(menu, SWT.SEPARATOR);
			org.eclipse.swt.widgets.MenuItem mi = new org.eclipse.swt.widgets.MenuItem(menu, SWT.PUSH);
			mi.setText("Quit");
			mi.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					SWTUtil.safeAsyncExec(new Runnable() {
						public void run() {
							quit(true);
						}
					});
				}
			});

			menu.setDefaultItem(mAbout);

			item.addListener(SWT.MenuDetect, new Listener() {
				public void handleEvent(Event event) {
					menu.setVisible(true);
				}
			});
			item.setImage(image);

		}
	}

//	private void setupDorkboxTray() {
//		
//		if(Log.isInfoEnabled()) {
//			Log.info("Setting up Dorkbox tray");
//		}
//		
//		SystemTray.DEBUG = true;
//		SystemTray.AUTO_FIX_INCONSISTENCIES = true;
//		SystemTray systemTray = SystemTray.get();
//
//		if (systemTray == null) {
//			throw new RuntimeException("Unable to load SystemTray!");
//		}
//
//		if(Log.isInfoEnabled()) {
//			Log.info("System tray initialised imageSize={} menuSize={}", systemTray.getTrayImageSize(),
//					systemTray.getMenuImageSize());
//		}
//
//		systemTray.setImage(getClass().getResourceAsStream(Settings.getInstance().getUseDarkIcon() 
//				? "/new_icon_32x32.png" : "/white_icon_32x32.png"));
//		systemTray.setStatus("The agent is running");
//		systemTray.setTooltip("The agent is running");
//
//		dorkboxConnections = new Menu("Favorites");
//		dorkboxConnections.setShortcut('f');
//		
//		systemTray.getMenu().add(dorkboxConnections);
//		
//		systemTray.getMenu().add(new MenuItem("Connections", new ActionListener() {
//			@Override
//			public void actionPerformed(final ActionEvent e) {
//				showConnections();
//			}
//		})).setShortcut('c'); // case does not matterO
//		
//		
//		systemTray.getMenu().add(new MenuItem("Known Hosts", new ActionListener() {
//			@Override
//			public void actionPerformed(final ActionEvent e) {
//				showKnownHosts();
//			}
//		})).setShortcut('h'); // case does not matterO
//		
//		systemTray.getMenu().add(new MenuItem("Private Keys", new ActionListener() {
//			@Override
//			public void actionPerformed(final ActionEvent e) {
//				showKeyTable();
//			}
//		})).setShortcut('k'); // case does not matterO
//		
//		systemTray.getMenu().add(new MenuItem("Preferences", new ActionListener() {
//			@Override
//			public void actionPerformed(final ActionEvent e) {
//				showSettings();
//			}
//		})).setShortcut('p'); // case does not matterO
//		
//		systemTray.getMenu().add(new MenuItem("About", new ActionListener() {
//			@Override
//			public void actionPerformed(final ActionEvent e) {
//				showAboutBox();
//			}
//		})).setShortcut('a'); // case does not matterO
//
//		systemTray.getMenu().add(new JSeparator());
//		systemTray.getMenu().add(new MenuItem("Quit", new ActionListener() {
//			@Override
//			public void actionPerformed(final ActionEvent e) {
//				systemTray.shutdown();
//				SWTUtil.safeAsyncExec(new Runnable() {
//					public void run() {
//						quit(true);
//					}
//				});
//			}
//		})).setShortcut('q'); // case does not matterO
//
//		systemTray.setEnabled(true);
//	}
	
	public void addFavoriteConnection(JsonConnection con) {

		if(Log.isInfoEnabled()) {
			Log.info("Adding to favorite connections {}@{}:{} ({})", con.getUsername(), con.getHostname(), con.getPort(), con.getId());
		}
		
//		if(dorkboxConnections!=null) {
//			MenuItem m;
//			dorkboxConnections.add(m = new MenuItem(con.getName(), new ActionListener() {
//				@Override
//				public void actionPerformed(final ActionEvent e) {
//					new Thread() {
//						public void run() {
//							launchClient(con.getHostname(), con.getPort(), con.getUsername());
//						}
//					}.start();
//				}
//			}));
//			
//			dorkboxFavorites.put(con.getId(), m);
//			
//		} else 
		if(swtConnections!=null) {

			org.eclipse.swt.widgets.MenuItem item = new org.eclipse.swt.widgets.MenuItem(swtConnections, SWT.PUSH);
			item.setText(con.getName());
			item.setData(con);
			item.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					
					new Thread() {
						public void run() {
							launchClient(con.getHostname(), con.getPort(), con.getUsername());
						}
					}.start();
				}
			});
		}
	}
	
	private void removeConnection(JsonConnection con) {
		
		Runnable r = new Runnable() {
			public void run() {
				if(swtConnections!=null) {
					int count = swtConnections.getItemCount();
					if(count==1) {
						new org.eclipse.swt.widgets.MenuItem(swtConnections, SWT.SEPARATOR);
					}
					org.eclipse.swt.widgets.MenuItem toRemove = null;
					for(org.eclipse.swt.widgets.MenuItem m : swtConnections.getItems()) {
						if(m.getData()!=null) {
							JsonConnection c2 = (JsonConnection) m.getData();
							if(con.getName()==c2.getName()) {
								toRemove = m;
								break;
							}
						}
					}
					if(toRemove!=null) {
						toRemove.dispose();
					}
				}
			}
		};
		
		if(display.getThread().equals(Thread.currentThread())) {
			r.run();
		} else {
			SWTUtil.safeAsyncExec(r);
		}
		
	}
	
	private void launchClient(String hostname, int port, String username) {
		
		if(Settings.getInstance().getUseBuiltInTerminal()) {
			
			if(Log.isInfoEnabled()) {
				Log.info("Launching built-in client {}@{}:{}", username, hostname, port);
			}
			
			SWTUtil.safeAsyncExec(new Runnable() {
				public void  run() {
					 new TerminalDisplay().runTerminal(username + "@" + hostname + ":" + port,
							 new ShellTerminalConnector(DesktopAgent.this, hostname, port, username));
				}
			});
		} else {
			if(StringUtils.isNotBlank(Settings.getInstance().getTerminalCommand()))  {
				
				if(Log.isInfoEnabled()) {
					Log.info("Launching configured client {}@{}:{}", username, hostname, port);
				}
				
				new Thread() {
					public void run() {
						
						CommandExecutor command = new CommandExecutor(Settings.getInstance().getTerminalCommand());
						for(String arg : translateCommandline(Settings.getInstance().getTerminalArguments())) {
							command.addArg(arg.replace("${host}", hostname)
									.replace("${port}", String.valueOf(port))
									.replace("${username}", username)
									.replace("%HOST", hostname)
									.replace("%PORT", String.valueOf(port))
									.replace("%USERNAME", username));
						}
						
						try {
							command.set("SSH_AUTH_SOCK", agentSocketPath.toString());
							command.execute();
						} catch (IOException e) {
							Log.error("Failed to launch SSH command", e);
						}
					}
				}.start();
				

			} else {
				if(SystemUtils.IS_OS_MAC_OSX) {
					launchOSXClient(hostname, port, username);
				} else if(SystemUtils.IS_OS_WINDOWS) {
					launchWindowsClient(hostname, port, username);
				} else {
					launchLinuxClient(hostname, port, username);
				}
			}
		}
		
	}
	
	private void launchWindowsClient(String hostname, int port, String username) {
		
		if(Log.isInfoEnabled()) {
			Log.info("Launching Windows client {}@{}:{}", username, hostname, port);
		}
		
		new Thread() {
			public void run() {
				CommandExecutor command = new CommandExecutor("cmd.exe", "/C", 
						String.format("start cmd.exe /C ssh -l %s -p %d %s", 
										username, port, hostname));
				try {
					command.set("SSH_AUTH_SOCK", agentSocketPath.toString());
					int exit = command.execute();
					Log.info("Command returned " + exit);
				} catch (IOException e) {
					Log.error("Failed to launch SSH command", e);
				}
			}
		}.start();

	}

	private void launchLinuxClient(String hostname, int port, String username) {
			
		SWTUtil.safeAsyncExec(new Runnable() {
			public void run() {
				SWTUtil.showInformation("Connections", "To launch connections you need to setup your preferred terminal in Preferences. Preferences dialog will be launched after you click Ok.");
				showSettings();
			}
		});
	}

	private void launchOSXClient(String hostname, int port, String username) {
		
		if(Log.isInfoEnabled()) {
			Log.info("Launching OSX client {}@{}:{}", username, hostname, port);
		}
		
		new Thread() {
			public void run() {
				CommandExecutor command = new CommandExecutor("/usr/bin/osascript", "-e", 
						String.format("tell application \"Terminal\" to do script \"%s\" activate",  
								String.format("ssh -l %s -p %d %s; exit", 
										username, port, hostname)));
				try {
					command.set("SSH_AUTH_SOCK", agentSocketPath.toString());
					command.execute();
				} catch (IOException e) {
					Log.error("Failed to launch SSH command", e);
				}
			}
		}.start();
	}

	public void quit(boolean killSWT) {
		
		this.quiting = true;
		
		if(Log.isInfoEnabled()) {
			Log.info("Quitting ({})", killSWT);
		}
		
		Thread t = new Thread("Close-Agent-Thread") {
			public void run() {
				try {
					server.close();
				} catch (IOException e) {
				}				
			}
		};
		t.setDaemon(true);
		t.start();

		Thread t2 = new Thread("Close-Pagent-Thread") {
			public void run() {
				if(pageantProcess!=null) {
					
					if(Log.isInfoEnabled()) {
						Log.info("Closing pageant process");
					}
					pageantProcess.destroy();
					try {
						pageantProcess.waitFor(5, TimeUnit.SECONDS);
					} catch (InterruptedException e) {
					}
					if(pageantProcess.isAlive()) {
						try {
							
							if(Log.isInfoEnabled()) {
								Log.info("Killing pageant process");
							}
							pageantProcess.destroyForcibly().waitFor(5, TimeUnit.SECONDS);
							
							if(pageantProcess.isAlive()) {
								Log.warn("pageant-proxy.exe may still be running.");
							}
						} catch (InterruptedException e) {
						}
					}
					
				}
			}
		};
		
		t2.setDaemon(true);
		t2.start();
		
		if(monitor!=null) {
			
			if(Log.isInfoEnabled()) {
				Log.info("Stopping file monitor");
			}
			try {
				monitor.stop();
			} catch (Exception e) {
			}
		}
		
		if(killSWT) {
			
			if(Log.isInfoEnabled()) {
				Log.info("Killing SWT");
			}
			display.asyncExec(() -> shell.dispose());
			display.asyncExec(() -> display.dispose());
		}
	}

	private void startupOSX() throws IOException {

		if(Log.isInfoEnabled()) {
			Log.info("Starting OSX agent");
		}
		
		// Create socket
		CommandExecutor cmd = new CommandExecutor("mktemp", "-d", "/private/tmp/ssh.XXXXXXXX");
		int status = cmd.execute();

		if (!succeeded(status)) {
			throw new IOException("Could not create temporary directory for unix socket listener");
		}

		String listener = cmd.getCommandOutput().trim() + File.separator + "com.sshtools.mobile.agent";

		writePosixAgentInfo(listener);

		server = new SshAgentServer(new OpenSSHConnectionFactory(), keystore);

		server.startUnixSocketListener(listener);
		
		Files.setPosixFilePermissions(Paths.get(listener),
				new LinkedHashSet<>(Arrays.asList(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)));

	}

	private void startupLinux() throws IOException {

		if(Log.isInfoEnabled()) {
			Log.info("Starting Linux agent");
		}
		
		// Create socket
		CommandExecutor cmd = new CommandExecutor("mktemp", "-d",
				System.getProperty("java.io.tmpdir") + "/ssh.XXXXXXXXXX");
		int status = cmd.execute();

		if (!succeeded(status)) {
			throw new IOException("Could not create temporary directory for unix socket listener");
		}

		String listener = cmd.getCommandOutput().trim() + File.separator + "com.sshtools.mobile.agent";

		writePosixAgentInfo(listener);

		server = new SshAgentServer(new OpenSSHConnectionFactory(), keystore);

		server.startUnixSocketListener(listener);
		
		Files.setPosixFilePermissions(Paths.get(listener),
				new LinkedHashSet<>(Arrays.asList(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)));
	}

	private void startupWindows() throws IOException {
		
		if(Log.isInfoEnabled()) {
			Log.info("Starting Windows agent");
		}
		
		startupPageantProxy();
		
		server = new SshAgentServer(new OpenSSHConnectionFactory(), keystore);

		server.startListener(new NamedPipeAcceptor(new NamedPipeServer(WINDOWS_NAMED_PIPE)));

		agentSocketPath = Paths.get(SSH_AGENT_PIPE);
	}

	private void startupPageantProxy() {
		
		File pageant = new File(CONF_FOLDER, "pageant-proxy.exe");

		if(Boolean.getBoolean("disable.pageant")) {
			if(Log.isInfoEnabled()) {
				Log.info("Pageant startup is disabled");
			}
			return;
		}

		if(Log.isInfoEnabled()) {
			Log.info("Starting pageant proxy");
		}
		
		try(FileOutputStream out = new FileOutputStream(pageant)) {
			try(InputStream in = getClass().getResourceAsStream("/pageant.exe")) {
				IOUtils.copy(in, out);
			}
		} catch (IOException e) {
			Log.error("Could not setup pageant proxy executable", e);
		}
		
		if(pageant.exists()) {
			
			 ProcessBuilder builder = new ProcessBuilder(pageant.getAbsolutePath());
			 builder.environment().put("SSH_AUTH_SOCK", DesktopAgent.SSH_AGENT_PIPE);
			 try {
				pageantProcess = builder.start();
			} catch (IOException e) {
				Log.error("Could not start pageant process", e);
			}
		}
		
	}

	private void writePosixAgentInfo(String listener) throws IOException {

		if(Log.isInfoEnabled()) {
			Log.info("Linking agent socket to agent.sock");
		}
		
		Path userLink = Paths.get(System.getProperty("user.home"), ".desktop-ssh-agent", "agent.sock");
		userLink.getParent().toFile().mkdirs();
		userLink.toFile().delete();

		agentSocketPath = Files.createSymbolicLink(userLink, Paths.get(listener));

	}

	private boolean succeeded(int status) {
		return status == 0;
	}

	public static void runApplication(Runnable restartCallback,
			Runnable shutdownCallback) throws IOException {

		try {
			
			new DesktopAgent(new Display(), restartCallback, shutdownCallback);
		} catch(Throwable t) {
			Log.error("Error in runApplication", t);
		}
	}
	
	public void restartApplication() {
		restartCallback.run();
	}
	
	public void shutdownApplication() {
		shutdownCallback.run();
	}
	
	public static void main(String[] args) {

		try {
			new DesktopAgent(new Display(), new Runnable() {
				public void run() {
					System.exit(99);
				}
			}, new Runnable() {
				public void run() {
					System.exit(0);
				}
			});
		} catch (Throwable t) {
			Log.error("Error in main", t);
		}
	}

	public void showConnections() {
		if(connectionsShell != null && connectionsShell.isVisible()) {
			connectionsShell.setFocus();
			connectionsShell.forceActive();
			return;
		}
		
		SWTUtil.safeAsyncExec(new Runnable() {
			public void run() {
				
				connectionsShell = new Shell(display, SWT.TITLE | SWT.CLOSE | SWT.RESIZE | SWT.BORDER);
				connectionsShell.setLayout(new GridLayout(5, true));
				connectionsShell.setText("SSH Connections");
				connectionsTable = new Table(connectionsShell, SWT.BORDER | SWT.FULL_SELECTION);
				connectionsTable.setLinesVisible(true);
				connectionsTable.setHeaderVisible(true);
				GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
				data.horizontalSpan = 5;
				data.heightHint = 200;
				
				connectionsTable.setLayoutData(data);
				
				new Label(connectionsShell, SWT.NONE);
				new Label(connectionsShell, SWT.NONE);
				
				Button launchButton = new Button(connectionsShell, SWT.PUSH);
				launchButton.setLayoutData(new GridData(SWT.FILL, SWT.END, true, true));
				Button favoriteButton = new Button(connectionsShell, SWT.PUSH);
				favoriteButton.setLayoutData(new GridData(SWT.FILL, SWT.END, true, true));
				Button addButton = new Button(connectionsShell, SWT.PUSH);
				addButton.setLayoutData(new GridData(SWT.FILL, SWT.END, true, true));

				favoriteButton.setText("Toggle Favorite");
				favoriteButton.addSelectionListener(new SelectionAdapter() {
		            public void widgetSelected(SelectionEvent event)
		            {
		            	try {
							TableItem[] items = connectionsTable.getSelection();
							if(items!=null && items.length > 0) {
								JsonConnection con = (JsonConnection) items[0].getData();
							    boolean isFavorite = Settings.getInstance().toggleFavorite(con.getName());
							    if(isFavorite) {
							    	items[0].setImage(0, new Image(display, getClass().getResourceAsStream("/favorite-on.png")));
							    	addFavoriteConnection(con);
							    } else {
							    	items[0].setImage(0, new Image(display, getClass().getResourceAsStream("/favorite-off.png")));
							    	removeConnection(con);
							    }
							}
						} catch (IOException e) {
							SWTUtil.showError("Favorite", e.getMessage());
						}
		            }
				});
				
				launchButton.setText("Connect");
				launchButton.addSelectionListener(new SelectionAdapter() {
		            public void widgetSelected(SelectionEvent event)
		            {
		            	TableItem[] items = connectionsTable.getSelection();
		                if(items!=null && items.length > 0) {
		                	JsonConnection con = (JsonConnection) items[0].getData();
			                launchClient(con.getHostname(), con.getPort(), con.getUsername());
			                connectionsShell.setVisible(false);
		                }
		            }
				});
				
				addButton.setText("Add");
				
				data = new GridData(SWT.FILL, SWT.END, true, true);
				addButton.setLayoutData(data);
				addButton.setEnabled(true);
				addButton.addSelectionListener(new SelectionAdapter() {
		            public void widgetSelected(SelectionEvent event)
		            {
		            	SWTUtil.safeAsyncExec(new Runnable() {
		            		public void run() {
		            			ConnectionDialog dialog = new ConnectionDialog(connectionsShell, DesktopAgent.this);
				            	dialog.open();
		            		}
		            	});
		            	
		            }
				});
			
				
				displayConnections();
				
				// Create context menu
				org.eclipse.swt.widgets.Menu menuTable = new org.eclipse.swt.widgets.Menu(connectionsTable);
				connectionsTable.setMenu(menuTable);

				org.eclipse.swt.widgets.MenuItem mEdit = new org.eclipse.swt.widgets.MenuItem(menuTable, SWT.PUSH);
				mEdit.setText("Edit");
				mEdit.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						TableItem[] items = connectionsTable.getSelection();
		                if(items!=null && items.length > 0) {
			                	JsonConnection con = (JsonConnection) items[0].getData();
			                	ConnectionDialog dialog = new ConnectionDialog(connectionsShell, DesktopAgent.this, con);
			                	dialog.open();
		                }
					}
				});
				
				org.eclipse.swt.widgets.MenuItem mDelete = new org.eclipse.swt.widgets.MenuItem(menuTable, SWT.PUSH);
				mDelete.setText("Delete");
				mDelete.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						TableItem[] items = connectionsTable.getSelection();
		                if(items!=null && items.length > 0) {
		                	JsonConnection con = (JsonConnection) items[0].getData();
			                deleteConnection(con);
		                }
					}
				});

				connectionsTable.addListener(SWT.MenuDetect, new Listener() {
				  @Override
				  public void handleEvent(Event event) {
				    if (connectionsTable.getSelectionCount() <= 0) {
				      event.doit = false;
				    }
				  }
				});
				
				connectionsShell.pack();
				connectionsShell.addDisposeListener(new DisposeListener() {

					@Override
					public void widgetDisposed(DisposeEvent e) {
						if(connectionsTable!=null) {
							synchronized (connectionsTable) {
								connectionsTable = null;
								connectionsShell = null;
							}
						}
					}
				});
				connectionsShell.open();
				connectionsShell.forceActive();
			}
		});
	}
	
	private void deleteConnection(JsonConnection con) {
		
		SWTUtil.safeAsyncExec(new Runnable() {
			public void run() {
				CustomDialog dialog = new CustomDialog(connectionsShell, 
						SWT.ICON_QUESTION, 
						0, 
						"Delete", 
						"Cancel");
				dialog.setMessage(String.format("Are  you sure you want to delete %s", con.getName()));
				dialog.setText("Delete Connection?");
				
				String reply = dialog.open();
				
				if(reply.equals("Delete")) {
					new Thread() {
						public void run() {
							try {
								keystore.deleteConnection(con);
								connections.remove(con.getName());
								if(Settings.getInstance().isFavorite(con.getName())) {
									Settings.getInstance().removeFavorite(con.getName());
								}
								SWTUtil.safeAsyncExec(new Runnable() {
									public void run() {
										removeConnection(con);
										displayConnections();
									}
								});
							} catch (IOException e) {
								Log.error("Failed to delete", e);
								SWTUtil.showError("Delete Connection", e.getMessage());							
							}
						}
					}.start();
				}
				
			}
		});
		
	}
	
	private void displayConnections() {
		
		loadConnections();
		
		SWTUtil.safeAsyncExec(new Runnable() {
			public void run() {
				Log.info("Displaying connections");

				if (connectionsTable != null) {
					connectionsTable.removeAll();

					String[] titles = { "", "Name", "Hostname", "Port", "Username" };
					for (int i = 0; i < titles.length; i++) {
						TableColumn column = new TableColumn(connectionsTable, SWT.NONE);
						column.setText(titles[i]);
					}

					Image fav = new Image(display, DesktopAgent.class.getResourceAsStream("/favorite-on.png"));
					Image notfav = new Image(display, DesktopAgent.class.getResourceAsStream("/favorite-off.png"));

					synchronized(connections) {
						for(JsonConnection con : connections.values()) {
							TableItem item = new TableItem(connectionsTable, SWT.NONE);
							item.setData(con);
							if(Settings.getInstance().isFavorite(con.getName())) {
								item.setImage(0, fav);
							} else {
								item.setImage(0, notfav);
							}
							
							item.setText(1, con.getName());
							item.setText(2, con.getHostname());
							item.setText(3, String.valueOf(con.getPort()));
							item.setText(4, con.getUsername());
			
						}
					}
						
					for (int i = 0; i < titles.length; i++) {
						connectionsTable.getColumn(i).pack();
					}
				}
			}
		});
	}
	
	public void showKnownHosts() {
		if(knownHostsShell!=null && knownHostsShell.isVisible()) {
			knownHostsShell.setFocus();
			knownHostsShell.forceActive();
			return;
		}
		
		SWTUtil.safeAsyncExec(new Runnable() {
			public void run() {
				knownHostsShell = new Shell(display, SWT.SHELL_TRIM);
				knownHostsShell.setImage(new Image(display, Image.class.getResourceAsStream("/new_icon.png")));
				knownHostsShell.setLayout(new GridLayout(5, true));
				knownHostsShell.setText("Known Hosts");
				knownHostsTable = new Table(knownHostsShell, SWT.BORDER | SWT.FULL_SELECTION);
				knownHostsTable.setLinesVisible(true);
				knownHostsTable.setHeaderVisible(true);
				GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
				data.horizontalSpan = 5;
				data.heightHint = 200;
				
				knownHostsTable.setLayoutData(data);
				
				new Label(knownHostsShell, SWT.NONE);
				new Label(knownHostsShell, SWT.NONE);
				new Label(knownHostsShell, SWT.NONE);
				
				
				Button commentButton = new Button(knownHostsShell, SWT.PUSH);
				Button deleteButton = new Button(knownHostsShell, SWT.PUSH);
				
				commentButton.setText("Change Comment");
				data = new GridData(SWT.FILL, SWT.END, true, true);
				commentButton.setLayoutData(data);
				commentButton.setEnabled(false);
				commentButton.addSelectionListener(new SelectionAdapter() {
		            public void widgetSelected(SelectionEvent event)
		            {
		            		SWTUtil.safeAsyncExec(new Runnable() {
		            			public void run() {
		            				TableItem[] items = knownHostsTable.getSelection();
		    		                if(items!=null && items.length > 0) {
		    			                KnownHostsKeyVerification.KeyEntry entry = (KnownHostsKeyVerification.KeyEntry) items[0].getData();
		    			                
		    			                InputForm input = new InputForm(display, "Change Comment", "Please enter the updated comment:", entry.getComment(), false);
		    			                if(input.show()) {
			    			                	knownHosts.setComment(entry, input.getInput());
			    		                		saveKnownHosts();
			    		                		displayKnownHosts();
			    		                		deleteButton.setEnabled(false);
			    		                		commentButton.setEnabled(false);
		    			                }
		    		                		
		    		                		
		    		                } else {
		    		                		commentButton.setEnabled(false);
		    		                }
		            			}
		            		});
		                
		            }
		        });
				
				
				deleteButton.setText("Delete");
				data = new GridData(SWT.FILL, SWT.END, true, true);
				deleteButton.setLayoutData(data);
				deleteButton.setEnabled(false);
				deleteButton.addSelectionListener(new SelectionAdapter() {
		            public void widgetSelected(SelectionEvent event)
		            {
		            		SWTUtil.safeAsyncExec(new Runnable() {
		            			public void run() {
		            				TableItem[] items = knownHostsTable.getSelection();
		    		                if(items!=null && items.length > 0) {
		    			                StringBuffer buf = new StringBuffer();
		    			                buf.append("Are you sure you want to delete the following host key?\r\n\r\n");
		    			                KnownHostsKeyVerification.KeyEntry entry = (KnownHostsKeyVerification.KeyEntry) items[0].getData();
		    			                
		    			                buf.append(items[0].getText(0));
		    		                		buf.append(" ");
		    		                		buf.append(SshKeyUtils.getFingerprint(entry.getKey()));
		    			                
		    		                		SWTUtil.showQuestion("Known Hosts", buf.toString(), new Runnable() {
		    		                			public void run() {
		    		                				synchronized(DesktopAgent.this) {
			    		                				knownHosts.removeEntry(entry);
					    		                		saveKnownHosts();
					    		                		displayKnownHosts();
					    		                		deleteButton.setEnabled(false);
					    		                		commentButton.setEnabled(false);
		    		                				}
		    		                			}
		    		                		});
		    		                		
		    		                } else {
		    		                		deleteButton.setEnabled(false);
		    		                }
		            			}
		            		});
		                
		            }
		        });
				
				displayKnownHosts();
				
				knownHostsTable.addListener (SWT.Selection, event -> {
					if(event.item == null) {
						deleteButton.setEnabled(false);
						commentButton.setEnabled(false);
					} else {
						deleteButton.setEnabled(true);
						commentButton.setEnabled(true);
					}
				});
				
				knownHostsShell.pack();
				knownHostsShell.addDisposeListener(new DisposeListener() {

					@Override
					public void widgetDisposed(DisposeEvent e) {
						if(knownHostsTable!=null) {
							synchronized (knownHostsTable) {
								knownHostsTable = null;
								knownHostsShell = null;
							}
							if(monitor!=null) {
								try {
									monitor.stop();
								} catch (Exception e1) {
								}
								monitor = null;
							}
						}
					}
				});
				knownHostsShell.open();
				knownHostsShell.forceActive();
			}
		});
	}
	
	private synchronized void saveKnownHosts() {
		
		if(knownHosts!=null) {
			try(OutputStream out = new FileOutputStream(getKnownHostsFile())) {
				IOUtils.write(knownHosts.toString(), out, "UTF-8");
			} catch(IOException e) {
				SWTUtil.showError("Known Hosts", 
						String.format("An unexpected error occurred saving the known_hosts file!\r\n\r\n", 
								e.getMessage()));
			}
		}
	}
	
	private File getSSHFolder() {
		return new File(System.getProperty("user.home"),".ssh");
	}
	
	private File getKnownHostsFile() {
		return new File(getSSHFolder(), "known_hosts");
	}

	public void showKeyTable() {
		
		if(keyShell!=null && keyShell.isVisible()) {
			keyShell.setFocus();
			keyShell.forceActive();
			return;
		}
		
		SWTUtil.safeAsyncExec(new Runnable() {
			public void run() {
				keyShell = new Shell(display, SWT.SHELL_TRIM);
				keyShell.setLayout(new GridLayout(5, true));
				keyShell.setText("Private Keys");
				keyShell.setImage(new Image(display, Image.class.getResourceAsStream("/new_icon.png")));
				keyTable = new Table(keyShell, SWT.BORDER | SWT.FULL_SELECTION);
				keyTable.setLinesVisible(true);
				keyTable.setHeaderVisible(true);
				GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
				data.horizontalSpan = 5;
				data.heightHint = 200;
				keyTable.setLayoutData(data);

				displayKeys();
				
				/**
				 * Fill some space so buttons are right aligned.
				 */
				new Label(keyShell, SWT.NONE);
				new Label(keyShell, SWT.NONE);
				new Label(keyShell, SWT.NONE);
				
				Button deleteButton = new Button(keyShell, SWT.PUSH);
				deleteButton.setText("Delete Key");
				data = new GridData(SWT.FILL, SWT.END, true, true);
				deleteButton.setLayoutData(data);
				deleteButton.setEnabled(false);
				deleteButton.addSelectionListener(new SelectionAdapter()
		        {
		            public void widgetSelected(SelectionEvent event)
		            {
		            		SWTUtil.safeAsyncExec(new Runnable() {
		            			public void run() {
		            				TableItem[] items = keyTable.getSelection();
		    		                if(items!=null && items.length > 0) {
		    			                SshPublicKey key = (SshPublicKey) items[0].getData();
		    		                		deleteKey(key);
		    		                }
		            			}
		            		});
		                
		            }
		        });
			
				
				   
				Button addButton = new Button(keyShell, SWT.PUSH);
				addButton.setText("Add Key");
				data = new GridData(SWT.FILL, SWT.END, true, true);
				addButton.setLayoutData(data);
				addButton.addSelectionListener(new SelectionAdapter()
		        {
					public void widgetSelected(SelectionEvent event)
		            {
						SWTUtil.safeAsyncExec(new Runnable() {
			            		public void run() {
			            			FileDialog dialog = new FileDialog(keyShell, SWT.OPEN);
					            	dialog.setFilterExtensions(null);
					            	dialog.setFilterPath(getSSHFolder().getAbsolutePath());
					            	String result = dialog.open();
					            	
					            	if(result!=null) {

					            		Thread t = new Thread() {
					            			public void run() {
								            	// Load private key from file, prompting for passphrase if needed.
								            	File keyfile= new File(result);
								            	if(!keyfile.isAbsolute()) {
								            		keyfile = new File(keyfile.getAbsolutePath());
								            	}

								            	try(InputStream in = new FileInputStream(keyfile)) {
								            		
								            		SshPrivateKeyFile file = SshPrivateKeyFileFactory.parse(in);
								            		SshKeyPair pair = null;
								            		if(file.isPassphraseProtected()) {
								            			for(int i=0;i<3;i++) {
								            				InputForm form = new InputForm(display,  "Passphrase Required",
								            						String.format("Please enter your passphrase for key file %s", keyfile.getName()), 
								            						"", true);
								            				if(form.show()) {
								            					try {
																pair = file.toKeyPair(form.getInput());
															} catch (InvalidPassphraseException e) {
																SWTUtil.showError("Add Key", "Invalid passphrase!");
																continue;
															}
								            					break;
								            				} 
								            			}
								            		} else {
								            			try {
														pair = file.toKeyPair(null);
													} catch (InvalidPassphraseException e) {
														SWTUtil.showError("Add Key", "An unexpected passphrase error occurred on a key that reported to non have any passphrase.");
													}
								            		}
								            		
								            		if(pair==null) {
								            			SWTUtil.showError("Add Key", "The key file could not be read.");
								            		} else {
										        		ImportKey importKey = new ImportKey(keyfile, pair.getPrivateKey(), pair.getPublicKey(), keyfile.getName(), new KeyConstraints());
										        		display.syncExec(importKey);
								            		}
								        		
								            	} catch(IOException ex) {
								            		SWTUtil.showError("Add Key", String.format("An unexpected error occurred.\r\n\r\n%s", ex.getMessage()));
								            	} 
					            			}
					            		};
					            		t.start();
					            	}
			            		}
			            });
		            }
		        });
				
				keyTable.addListener (SWT.Selection, event -> {
					if(event.item == null) {
						deleteButton.setEnabled(false);
					} else {
						deleteButton.setEnabled(true);
					}
				});
				
				keyShell.pack();
				keyShell.addDisposeListener(new DisposeListener() {

					@Override
					public void widgetDisposed(DisposeEvent e) {
						if(keyTable!=null) {
							synchronized (keyTable) {
								keyTable = null;
								keyShell = null;
							}
						}
					}
				});
				keyShell.open();
				keyShell.forceActive();
			}
		});
	}
	
	private synchronized void displayKnownHosts() {
		
		SWTUtil.safeAsyncExec(new Runnable() {
			public void run() {
				Log.info("Reloading known_hosts");
	
				if (knownHostsTable != null) {
					knownHostsTable.removeAll();
	
					String[] titles = { "Names", "Fingerprint", "Key", "Comment" };
					for (int i = 0; i < titles.length; i++) {
						TableColumn column = new TableColumn(knownHostsTable, SWT.NONE);
						column.setText(titles[i]);
					}
	
					try {
						
						loadKnownHostsFromFile();
						
						for (KnownHostsKeyVerification.KeyEntry entry : knownHosts.getKeyEntries()) {
							TableItem item = new TableItem(knownHostsTable, SWT.NONE);
							
							String names = entry.getNames();
							
							if(entry.isRevoked()) {
								item.setForeground(display.getSystemColor(SWT.COLOR_RED));
								names = "@revoked " + names;
							}
			
							if(entry.isCertAuthority()) {
								names = "@cert-authority " + names;
							}
							if(entry.isHashedEntry()) {
								names = "<hashed>";
							}
							
							item.setData(entry);
							item.setText(0, names);
							item.setText(1, SshKeyUtils.getFingerprint(entry.getKey()));
							item.setText(2, SshKeyUtils.getFormattedKey(entry.getKey(), ""));
							item.setText(3, entry.getComment());
						}
	
					} catch(IOException | SshException ex) {
						SWTUtil.showError("Known Hosts", 
								String.format("There was an unexpected error parsing the known_hosts file.\r\n\r\n%s", 
										ex.getMessage()));
					}
					
					
					for (int i = 0; i < titles.length; i++) {
						knownHostsTable.getColumn(i).pack();
					}
					
					knownHostsTable.getColumn(2).setWidth(200);
				}
			}
		});
	}


	private void loadKnownHostsFromFile() throws IOException, SshException {
		synchronized(knownHosts) {
			
			if (monitor != null) {
				try {
					monitor.stop();
				} catch (Exception e) {
					Log.error("File monitor error", e);
				}
			}
			
			File khf = getKnownHostsFile();
			if(khf.exists()) {
				try (InputStream in = new FileInputStream(khf)) {
					knownHosts.load(in);

					FileAlterationObserver observer = new FileAlterationObserver(getSSHFolder());
				    observer.addListener(new FileAlterationListenerAdaptor() {
						
						@Override
						public void onFileDelete(File file) {
							if(file.getName().equals("known_hosts")) {
								displayKnownHosts();
							}
						}
						
						@Override
						public void onFileCreate(File file) {
							if(file.getName().equals("known_hosts")) {
								displayKnownHosts();
							}
						}
						
						@Override
						public void onFileChange(File file) {
							if(file.getName().equals("known_hosts")) {
								displayKnownHosts();
							}
						}
						
						@Override
						public void onDirectoryDelete(File directory) {
							displayKnownHosts();
						}
					});
				    
				    monitor = new FileAlterationMonitor(1000);
			    	    monitor.addObserver(observer);
			    	    try {
						monitor.start();
					} catch (Exception e) {
						Log.error("File monitor error", e);
					}
				}
			}
		}
	}
	
	private synchronized void displayKeys() {
		
		SWTUtil.safeAsyncExec(new Runnable() {
			public void run() {
				Log.info("Reloading keys");

				if (keyTable != null) {
					keyTable.removeAll();

					String[] titles = { "Description", "Device Key", "Type", "Fingerprint" };
					for (int i = 0; i < titles.length; i++) {
						TableColumn column = new TableColumn(keyTable, SWT.NONE);
						column.setText(titles[i]);
					}

					Map<SshPublicKey, String> keys = localKeys.getPublicKeys();
					for (Map.Entry<SshPublicKey, String> entry : keys.entrySet()) {
						TableItem item = new TableItem(keyTable, SWT.NONE);
						item.setData(entry.getKey());

						item.setText(0, entry.getValue());
						item.setText(1, StringUtils.center("", 10));
						item.setText(2, entry.getKey().getAlgorithm());
						item.setText(3, SshKeyUtils.getFingerprint(entry.getKey()));
						
					}
					
					synchronized (deviceKeys) {
						loadDeviceKeys();
						for (Map.Entry<SshPublicKey, String> entry : deviceKeys.entrySet()) {
							TableItem item = new TableItem(keyTable, SWT.NONE);
							item.setData(entry.getKey());
							if(!online.get()) {
								item.setForeground(display.getSystemColor(SWT.COLOR_RED));
							}
							item.setText(0, entry.getValue());
							item.setText(1, StringUtils.center("\u2714", 10));
							item.setText(2, entry.getKey().getAlgorithm());
							item.setText(3, SshKeyUtils.getFingerprint(entry.getKey()));
							
						}					
					}

					for (int i = 0; i < titles.length; i++) {
						keyTable.getColumn(i).pack();
					}

				}
			}
		});

	}
	@Override
	public boolean addKey(SshPrivateKey prvkey, SshPublicKey pubkey, String description, KeyConstraints cs)
			throws IOException {

		Log.info("Adding key {}", description);

		ImportKey importKey = new ImportKey(null, prvkey, pubkey, description, cs);
		display.syncExec(importKey);
		return importKey.isSuccess();
	}

	class ImportKey implements Runnable {

		private static final String CANCEL = "Cancel";
		private static final String USE_LOCALLY = "Use Locally";
		private static final String SEND_TO_DEVICE = "Send To Device";
		boolean ret;
		SshPublicKey pubkey;
		SshPrivateKey prvkey;
		String description;
		KeyConstraints cs;
		File keyfile;

		ImportKey(File keyfile, SshPrivateKey prvkey, SshPublicKey pubkey, String description, KeyConstraints cs) {
			this.keyfile = keyfile;
			this.prvkey = prvkey;
			this.pubkey = pubkey;
			this.description = description;
			this.cs = cs;
		}

		public void run() {
			
			if(isAuthorized()) {
				CustomDialog dialog = new CustomDialog(new Shell(), SWT.ICON_QUESTION, 0,
						SEND_TO_DEVICE, USE_LOCALLY, CANCEL);
				dialog.setText("Import Key");
				dialog.setMessage(String.format(
						"You are adding the following key to the Desktop SSH Agent:\r\n\r\n%s\r\n\r\n"  + 
				        "Do you want to store this permanently on your mobile device? You can also just use it locally on this computer.",
						description));
	
				String result = dialog.open();
				
				if (result == SEND_TO_DEVICE) {
					display.syncExec(new SendToDevice(this));
					return;
				} else if (result == CANCEL) {
					ret = false;
					return;
				}
			
			}

			SshKeyPair pair = new SshKeyPair();
			pair.setPrivateKey(prvkey);
			pair.setPublicKey(pubkey);

			try {
				localKeys.addKey(pair, description, cs);
				
				if(keyfile!=null) {
					Settings.getInstance().addTemporaryKey(pair.getPublicKey(), keyfile);
				}
				
				displayKeys();
				ret = true;
			} catch (IOException e) {
				Log.error("Failed to add temporary key", e);
				ret = false;
			}
			
		}

		boolean isSuccess() {
			return ret;
		}
	}

	class SendToDevice implements Runnable {
		
		ImportKey importKey;
		
		SendToDevice(ImportKey importKey) {
			this.importKey = importKey;
		}
		
		public void run() {
			
			if(!online.get()) {
				SWTUtil.showError("Import Key", "The gateway cannot be contacted to import the key.");
				return;
			}
			
			try {
				JsonClient client = keystore.getClient();

				InputForm input = new InputForm(display, "Name Required", "Please enter a name to identify this key.", FileUtils.lastPathElement(importKey.description), false);
				
				if(!input.show()) {
					return;
				}
				String name = input.getInput();
				SshKeyPair pair = new SshKeyPair();
				pair.setPrivateKey(importKey.prvkey);
				pair.setPublicKey(importKey.pubkey);
				String uuid = UUID.randomUUID().toString();
				SshPrivateKeyFile prv = SshPrivateKeyFileFactory.create(pair, 
						uuid, 
						SshPrivateKeyFileFactory.OPENSSH_FORMAT);

		        JsonResourceStatus response = client.doPost("api/userPrivateKeys/importKey", JsonResourceStatus.class,
		                generateAuthorizationParameters(new RequestParameter("name", name),
		                new RequestParameter("type", "private"),
		                new RequestParameter("deviceKey", "true"),
		                new RequestParameter("passphrase", uuid),
		                new RequestParameter("key", new String(prv.getFormattedKey()))));

		        if(response.isSuccess()) {
		        		Toast.toast(ToastType.INFO, "Import Key", response.getMessage());
		        } else {
		        		SWTUtil.showError("Import Key", response.getMessage());
		        }
				displayKeys();
				importKey.ret = true;

			} catch (IOException | JsonStatusException e) {
				SWTUtil.showError("Add Key", e.getMessage());
			} catch (IllegalStateException e) {
				// Ignore
			}
		}
	}
	
	
	class DeleteAllKeys implements Runnable {

		private static final String YES_I_WANT_TO_DELETE_ALL_KEYS = "Yes, I want to delete all keys";

		boolean ret;

		final String DELETE_ALL = "Delete All Keys";
		final String DELETE_TEMPORARY = "Delete Local Only";
		final String DELETE_CANCEL = "Cancel";

		DeleteAllKeys() {

		}

		public void run() {
			CustomDialog dialog = new CustomDialog(new Shell(), SWT.ICON_QUESTION, 0,
					DELETE_TEMPORARY, DELETE_ALL, DELETE_CANCEL);
			dialog.setText("Delete Keys");
			dialog.setMessage(
					"A request has been received to delete all keys?\r\n\r\n"
					+ "Do you want to delete all keys, or just temporary keys?");

			String result = dialog.open();

			if (result == DELETE_ALL) {
				
				CustomDialog dialog2 = new CustomDialog(new Shell(), SWT.ICON_WARNING, 0,
						YES_I_WANT_TO_DELETE_ALL_KEYS, DELETE_CANCEL);
				dialog2.setText("Confirm Delete");
				dialog2.setMessage(
						"You are about to delete all keys from your account.\r\n"
					   + "This includes keys that are stored exclusively on your mobile device.\r\n\r\n"
					   + "Are you sure you want to do this?");

				String result2 = dialog2.open();
				
				if(result2 == YES_I_WANT_TO_DELETE_ALL_KEYS) {
					
					try {
						
						if(!online.get()) {
							SWTUtil.showError("Delete Keys", "The gateway cannot be contacted to delete all keys.");
							return;
						}
						keystore.deleteDeviceKeys();
						
						localKeys.deleteAllKeys();
						Settings.getInstance().removeAllKeys();
						ret = true;
						
					} catch (IOException | JsonStatusException e) {
						SWTUtil.showError("Delete Keys", e.getMessage());
					}  catch(IllegalStateException  e) {
						// Ignore
					}
					
				}
				
			} else if (result == DELETE_TEMPORARY) {
				
				localKeys.deleteAllKeys();
				try {
					Settings.getInstance().removeAllKeys();
				} catch (IOException e) {
					SWTUtil.showError("Delete Keys", e.getMessage());
				}
				ret = true;
			}

		}

		boolean isSuccess() {
			return ret;
		}
	}
	
	class DeleteKey implements Runnable {

		boolean ret;

		final String DELETE = "Delete";
		final String DELETE_CANCEL = "Cancel";

		SshPublicKey key;
		String name;
		
		DeleteKey(SshPublicKey key) {
			this.key = key;
		}

		public void run() {
			CustomDialog dialog = new CustomDialog(new Shell(), SWT.ICON_QUESTION, 0,
					 DELETE, DELETE_CANCEL);
			dialog.setText("Delete Key");
			dialog.setMessage(
					"A request has been received to delete the following key?\r\n\r\n"
					+ SshKeyUtils.getFingerprint(key));

			String result = dialog.open();

			if (result == DELETE) {
				
					try {
						synchronized(deviceKeys) {
							if(deviceKeys.containsKey(key)) {
								
								if(!online.get()) {
									SWTUtil.showError("Delete Key", "The gateway cannot be contacted to delete the key.");
									return;
								}
								
								keystore.deleteDeviceKey(key);
								
							} else {
								Settings.getInstance().removeTemporaryKey(key);
								localKeys.deleteKey(key);

								displayKeys();
							}
								
							ret = true;
						}
						
					} catch (IOException | JsonStatusException e) {
						SWTUtil.showError("Delete Key", e.getMessage());
					}  catch(IllegalStateException  e) {
						// Ignore
					}
					
				}
		}

		boolean isSuccess() {
			return ret;
		}
	}

	@Override
	public boolean deleteAllKeys() {
		DeleteAllKeys deleteKeyTask = new DeleteAllKeys();
		display.syncExec(deleteKeyTask);
		return deleteKeyTask.isSuccess();
	}

	@Override
	public void onKeysChanged() {
		
		new Thread() {
			public void run() {
				
				if(online.get()) {
					loadDeviceKeys();
					displayKeys();
				}
			}
		}.start();
		
	}

	private void loadDeviceKeys() {
		synchronized (deviceKeys) {
			deviceKeys.clear();
			deviceKeys.putAll(keystore.getDeviceKeys());
			Log.info("Got {} device keys", deviceKeys.size());
		}
	}
	
	@Override
	public boolean deleteKey(SshPublicKey key) {
		DeleteKey deleteKeyTask = new DeleteKey(key);
		display.syncExec(deleteKeyTask);
		return deleteKeyTask.isSuccess();
	}
	
	/**
	 * [code borrowed from ant.jar]
	 * Crack a command line.
	 * @param toProcess the command line to process.
	 * @return the command line broken into strings.
	 * An empty or null toProcess parameter results in a zero sized array.
	 */
	public static String[] translateCommandline(String toProcess) {
	    if (toProcess == null || toProcess.length() == 0) {
	        //no command? no string
	        return new String[0];
	    }
	    // parse with a simple finite state machine

	    final int normal = 0;
	    final int inQuote = 1;
	    final int inDoubleQuote = 2;
	    int state = normal;
	    final StringTokenizer tok = new StringTokenizer(toProcess, "\"\' ", true);
	    final ArrayList<String> result = new ArrayList<String>();
	    final StringBuilder current = new StringBuilder();
	    boolean lastTokenHasBeenQuoted = false;

	    while (tok.hasMoreTokens()) {
	        String nextTok = tok.nextToken();
	        switch (state) {
	        case inQuote:
	            if ("\'".equals(nextTok)) {
	                lastTokenHasBeenQuoted = true;
	                state = normal;
	            } else {
	                current.append(nextTok);
	            }
	            break;
	        case inDoubleQuote:
	            if ("\"".equals(nextTok)) {
	                lastTokenHasBeenQuoted = true;
	                state = normal;
	            } else {
	                current.append(nextTok);
	            }
	            break;
	        default:
	            if ("\'".equals(nextTok)) {
	                state = inQuote;
	            } else if ("\"".equals(nextTok)) {
	                state = inDoubleQuote;
	            } else if (" ".equals(nextTok)) {
	                if (lastTokenHasBeenQuoted || current.length() != 0) {
	                    result.add(current.toString());
	                    current.setLength(0);
	                }
	            } else {
	                current.append(nextTok);
	            }
	            lastTokenHasBeenQuoted = false;
	            break;
	        }
	    }
	    if (lastTokenHasBeenQuoted || current.length() != 0) {
	        result.add(current.toString());
	    }
	    if (state == inQuote || state == inDoubleQuote) {
	        throw new RuntimeException("unbalanced quotes in " + toProcess);
	    }
	    return result.toArray(new String[result.size()]);
	}

	public void saveConnection(String name, String hostname, Integer port, String username, String oldName) throws SshException, IOException {
		
		Set<String> aliases = new TreeSet<String>();
		aliases.add(hostname);
		
		try {
			InetAddress addr = InetAddress.getByName(hostname);
			aliases.add(addr.getHostAddress());
			aliases.add(addr.getCanonicalHostName());
		} catch(UnknownHostException e) { }
		
		JsonConnection con;
		
		if(connections.containsKey(name) && (Objects.nonNull(oldName) && !oldName.equals(name))) {
			throw new IOException(String.format("A connection named %s already exists!", name));
		}
		
		boolean favorite = false;
		if(Objects.nonNull(oldName)) {
			favorite = Settings.getInstance().isFavorite(oldName);
			if(favorite) {
				Settings.getInstance().removeFavorite(oldName);
			}
		}
		
		if(Objects.isNull(oldName)) {
			con = keystore.createConnection(name, hostname, port, 
					username, aliases,Collections.emptySet());
		} else {
			con = keystore.updateConnection(oldName, name, hostname, 
					port, username, aliases, Collections.emptySet());
		}
		
		if(favorite) {
			Settings.getInstance().setFavorite(name);
		}
		
		SWTUtil.safeAsyncExec(new Runnable() {
			public void run() {
				synchronized(connections) {
					connections.put(con.getName(), con);
					displayConnections();
				}
			}
		});
	}

	public String getSocketPath() {
		return agentSocketPath.toString();
	}

	public String getAuthorizationToken() {
		return authorization;
	}

	public String getPrivateKey() {
		return privateKey;
	}
}
