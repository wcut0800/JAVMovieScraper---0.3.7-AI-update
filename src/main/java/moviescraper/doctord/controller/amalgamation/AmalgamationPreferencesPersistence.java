package moviescraper.doctord.controller.amalgamation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import moviescraper.doctord.controller.siteparsingprofile.SiteParsingProfile.ScraperGroupName;
import moviescraper.doctord.model.dataitem.DataItemSource;
import moviescraper.doctord.model.dataitem.DefaultDataItemSource;

/**
 * Gson-based persistence for amalgamation preferences.
 * Replaces json-io to avoid Enum deserialization issues on Java 17+.
 */
public final class AmalgamationPreferencesPersistence {

	private static final String SETTINGS_FILE_NAME = "AmalgamationSettings.json";

	private static final Type MAP_TYPE = new TypeToken<Map<String, ScraperGroupPreferenceDTO>>() {}.getType();

	/** DTO for JSON - avoids reflection on Enum/DataItemSource */
	static class ScraperGroupPreferenceDTO {
		String scraperGroupName;
		DataItemSourcePreferenceDTO overallOrdering;
		Map<String, DataItemSourcePreferenceDTO> customOrderings;
	}

	static class DataItemSourcePreferenceDTO {
		List<DataItemSourceEntryDTO> order;
	}

	static class DataItemSourceEntryDTO {
		String className;
		boolean disabled;
	}

	private static DataItemSource createDataItemSource(String className, boolean disabled) {
		try {
			DataItemSource source;
			if (DefaultDataItemSource.class.getName().equals(className)) {
				source = DefaultDataItemSource.DEFAULT_DATA_ITEM_SOURCE.createInstanceOfSameType();
			} else {
				@SuppressWarnings("unchecked")
				Class<? extends DataItemSource> clazz = (Class<? extends DataItemSource>) Class.forName(className);
				source = clazz.getDeclaredConstructor().newInstance();
			}
			source.setDisabled(disabled);
			return source;
		} catch (Exception e) {
			throw new JsonParseException("Cannot create DataItemSource for " + className, e);
		}
	}

	static AllAmalgamationOrderingPreferences fromDTO(Map<String, ScraperGroupPreferenceDTO> dtoMap) {
		AllAmalgamationOrderingPreferences prefs = new AllAmalgamationOrderingPreferences();
		for (Map.Entry<String, ScraperGroupPreferenceDTO> e : dtoMap.entrySet()) {
			ScraperGroupName groupName = ScraperGroupName.valueOf(e.getKey());
			ScraperGroupPreferenceDTO dto = e.getValue();
			DataItemSourceAmalgamationPreference overall = fromDTO(dto.overallOrdering);
			ScraperGroupAmalgamationPreference pref = new ScraperGroupAmalgamationPreference(groupName, overall);
			if (dto.customOrderings != null) {
				for (Map.Entry<String, DataItemSourcePreferenceDTO> ce : dto.customOrderings.entrySet()) {
					try {
						pref.setCustomOrderingForField(ce.getKey(), fromDTO(ce.getValue()));
					} catch (NoSuchFieldException | SecurityException ex) {
						// skip unknown field
					}
				}
			}
			prefs.putScraperGroupAmalgamationPreference(groupName, pref);
		}
		return prefs;
	}

	private static DataItemSourceAmalgamationPreference fromDTO(DataItemSourcePreferenceDTO dto) {
		if (dto == null || dto.order == null || dto.order.isEmpty()) {
			return new DataItemSourceAmalgamationPreference(DefaultDataItemSource.DEFAULT_DATA_ITEM_SOURCE);
		}
		LinkedList<DataItemSource> list = new LinkedList<>();
		for (DataItemSourceEntryDTO entry : dto.order) {
			list.add(createDataItemSource(entry.className, entry.disabled));
		}
		return new DataItemSourceAmalgamationPreference(list);
	}

	static Map<String, ScraperGroupPreferenceDTO> toDTO(AllAmalgamationOrderingPreferences prefs) {
		Map<String, ScraperGroupPreferenceDTO> dtoMap = new HashMap<>();
		for (Map.Entry<ScraperGroupName, ScraperGroupAmalgamationPreference> e : prefs.getAllAmalgamationOrderingPreferences().entrySet()) {
			ScraperGroupPreferenceDTO dto = new ScraperGroupPreferenceDTO();
			dto.scraperGroupName = e.getKey().name();
			dto.overallOrdering = toDTO(e.getValue().getOverallAmalgamationPreference());
			if (e.getValue().getCustomOrderingsMap() != null && !e.getValue().getCustomOrderingsMap().isEmpty()) {
				dto.customOrderings = new HashMap<>();
				for (Map.Entry<String, DataItemSourceAmalgamationPreference> ce : e.getValue().getCustomOrderingsMap().entrySet()) {
					dto.customOrderings.put(ce.getKey(), toDTO(ce.getValue()));
				}
			}
			dtoMap.put(dto.scraperGroupName, dto);
		}
		return dtoMap;
	}

	private static DataItemSourcePreferenceDTO toDTO(DataItemSourceAmalgamationPreference pref) {
		DataItemSourcePreferenceDTO dto = new DataItemSourcePreferenceDTO();
		dto.order = new ArrayList<>();
		for (DataItemSource source : pref.getAmalgamationPreferenceOrder()) {
			DataItemSourceEntryDTO entry = new DataItemSourceEntryDTO();
			entry.className = source.getClass().getName();
			entry.disabled = source.isDisabled();
			dto.order.add(entry);
		}
		return dto;
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static AllAmalgamationOrderingPreferences load(File file) throws IOException {
		try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
			Map<String, ScraperGroupPreferenceDTO> dtoMap = GSON.fromJson(reader, MAP_TYPE);
			if (dtoMap == null || dtoMap.isEmpty()) {
				return null;
			}
			return fromDTO(dtoMap);
		}
	}

	public static void save(AllAmalgamationOrderingPreferences prefs, File file) throws IOException {
		Map<String, ScraperGroupPreferenceDTO> dtoMap = toDTO(prefs);
		try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
			GSON.toJson(dtoMap, MAP_TYPE, writer);
		}
	}

	public static String getSettingsFileName() {
		return SETTINGS_FILE_NAME;
	}
}
