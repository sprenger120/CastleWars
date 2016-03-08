package de.fastcrafter.castlewars;

import java.util.HashMap;
import java.util.Map;

public class Messages {
	private Map<String,String> Msgs = new HashMap<String,String>();

	public Map<String, String> getMsgs() {
		return Msgs;
	}

	public void setMsgs(Map<String, String> msgs) {
		Msgs = msgs;
	}
}
