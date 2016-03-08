package de.fastcrafter.castlewars;

import java.util.HashMap;
import java.util.Map;

public class Statistics {
	private String Language = new String("eng");
	private Map<String, Integer> PlayerKills = new HashMap<String, Integer>();
	private Map<String, Integer> FlagsCaptured = new HashMap<String, Integer>();
	private Map<String, Integer> FlagsRegained = new HashMap<String, Integer>();


	public String getLanguage() {
		return Language;
	}

	public void setLanguage(String language) {
		Language = language;
	}

	public Map<String, Integer> getPlayerKills() {
		return PlayerKills;
	}

	public void setPlayerKills(Map<String, Integer> playerKills) {
		PlayerKills = playerKills;
	}

	public Map<String, Integer> getFlagsCaptured() {
		return FlagsCaptured;
	}

	public void setFlagsCaptured(Map<String, Integer> flagsCaptured) {
		FlagsCaptured = flagsCaptured;
	}

	public Map<String, Integer> getFlagsRegained() {
		return FlagsRegained;
	}

	public void setFlagsRegained(Map<String, Integer> flagsRegained) {
		FlagsRegained = flagsRegained;
	}
}
