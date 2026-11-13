/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import net.runelite.client.config.ConfigManager;

/**
 * Loads and saves one account's progress as a JSON value in the RuneLite profile, so it follows the account across
 * computers the same way other per-account plugin data does.
 */
final class GoPersistence
{
	/** Config key the save is stored under. */
	static final String KEY_SAVE = "save";

	/** Config key a save that could not be read is copied to, so it is not lost when a fresh save replaces it. */
	static final String KEY_UNREADABLE = "saveUnreadable";

	/** Config key the save is copied to just before a reset, as a safety net. There is no way to restore it. */
	static final String KEY_BEFORE_RESET = "saveBeforeReset";

	/** Where the JSON is kept. A seam, so tests can use a plain map instead of a live {@link ConfigManager}. */
	interface Store
	{
		/**
		 * Reads a value for the current account.
		 *
		 * @param key the config key
		 * @return the stored text, or null when nothing is stored
		 */
		String get(String key);

		/**
		 * Writes a value for the current account.
		 *
		 * @param key the config key
		 * @param value the text to store
		 */
		void set(String key, String value);
	}

	private final Store store;
	private final Gson gson;

	GoPersistence(ConfigManager configManager, Gson gson)
	{
		this(backedBy(configManager), gson);
	}

	GoPersistence(Store store, Gson gson)
	{
		this.store = store;
		this.gson = gson;
	}

	/**
	 * Adapts a live config manager to the {@link Store} seam.
	 *
	 * @param configManager the RuneLite config manager
	 * @return a store that reads and writes the current account's profile
	 */
	static Store backedBy(ConfigManager configManager)
	{
		return new Store()
		{
			@Override
			public String get(String key)
			{
				return configManager.getRSProfileConfiguration(GielinorGoConfig.GROUP, key);
			}

			@Override
			public void set(String key, String value)
			{
				configManager.setRSProfileConfiguration(GielinorGoConfig.GROUP, key, value);
			}
		};
	}

	/**
	 * Loads the current account's save.
	 *
	 * @return the save, or a fresh one when nothing is stored or the stored value cannot be read
	 */
	SaveData load()
	{
		String json = store.get(KEY_SAVE);
		if (json == null || json.trim().isEmpty())
			return SaveData.fresh();

		try
		{
			SaveData data = gson.fromJson(json, SaveData.class);
			if (data != null)
				return data.sanitized();
		}
		catch (JsonParseException e)
		{
			// Handled below: the unreadable save is set aside and a fresh one starts.
		}

		store.set(KEY_UNREADABLE, json);
		return SaveData.fresh();
	}

	/**
	 * Copies the stored save to {@link #KEY_BEFORE_RESET}, replacing any earlier copy. The save itself is not touched.
	 * Nothing is copied when no save is stored.
	 */
	void backUp()
	{
		String json = store.get(KEY_SAVE);
		if (json == null || json.trim().isEmpty())
			return;

		store.set(KEY_BEFORE_RESET, json);
	}

	/**
	 * Saves progress for the current account.
	 *
	 * @param data the save to write
	 */
	void save(SaveData data)
	{
		store.set(KEY_SAVE, gson.toJson(data));
	}
}
