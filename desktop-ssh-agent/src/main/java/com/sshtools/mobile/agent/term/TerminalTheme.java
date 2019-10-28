package com.sshtools.mobile.agent.term;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class TerminalTheme {
	private String name;
	private String path;
	private Properties properties;
	private static List<TerminalTheme> themes;
	
	public final static TerminalTheme CUSTOM = new TerminalTheme("Custom", "");

	TerminalTheme(String name, String path) {
		this.name = name;
		this.path = path;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

	public Properties getProperties() {
		if (properties == null) {
			properties = new Properties();
			try (InputStream in = TerminalTheme.class.getResourceAsStream(path)) {
				if(in == null)
					throw new FileNotFoundException(String.format("No such theme resource %s for theme %s", path, name));
				properties.load(in);
			} catch (IOException ioe) {
				throw new RuntimeException("Failed to load themes.", ioe);
			}
		}
		return properties;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TerminalTheme other = (TerminalTheme) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	public static List<TerminalTheme> getThemes() {
		if (themes == null) {
			themes = new ArrayList<>();
			try (BufferedReader br = new BufferedReader(
					new InputStreamReader(TerminalTheme.class.getResourceAsStream("/themes.properties")))) {
				String line = null;
				while ((line = br.readLine()) != null) {
					line = line.trim();
					if (!line.startsWith("#")) {
						int idx = line.indexOf('=');
						if (idx != -1) {
							themes.add(new TerminalTheme(line.substring(0, idx), line.substring(idx + 1)));
						}
					}
				}
			} catch (IOException ioe) {
				throw new RuntimeException("Failed to load themes.", ioe);
			}
		}
		return themes;
	}

	public static TerminalTheme getTheme(String name) {
		if(name == null || name.equals("") || name.equals(CUSTOM.getName()))
			return CUSTOM;
		for (TerminalTheme t : getThemes()) {
			if (t.getName().equals(name))
				return t;
		}
		return CUSTOM;
	}

	public boolean isCustom() {
		return this.equals(CUSTOM);
	}
}
