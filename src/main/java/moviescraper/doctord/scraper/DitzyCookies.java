package moviescraper.doctord.scraper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Cookie store for use with Jsoup connections. Supports loading from Netscape-format
 * cookie jar files and per-host cookie storage.
 */
public class DitzyCookies {

	private final Map<String, Map<String, String>> cookieStore = new HashMap<>();

	/**
	 * Returns cookies for the given URL's host. Used by Jsoup Connection.cookies().
	 */
	public Map<String, String> getCookies(URL url) {
		String host = url.getHost();
		Map<String, String> hostCookies = cookieStore.get(host);
		if (hostCookies == null) {
			hostCookies = cookieStore.get("." + host);
		}
		return hostCookies != null ? new HashMap<>(hostCookies) : new HashMap<>();
	}

	/**
	 * Adds cookies for the given host.
	 */
	public void addCookies(String host, Map<String, String> cookies) {
		cookieStore.merge(host, new HashMap<>(cookies), (existing, added) -> {
			existing.putAll(added);
			return existing;
		});
	}

	/**
	 * Loads cookies from a Netscape-format cookie jar file.
	 */
	public void LoadCookieJar(File cookieJar) throws IOException {
		if (cookieJar == null || !cookieJar.exists()) {
			return;
		}
		try (BufferedReader reader = new BufferedReader(new FileReader(cookieJar))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				String[] parts = line.split("\t");
				if (parts.length >= 7) {
					String domain = parts[0];
					String name = parts[5];
					String value = parts[6];
					Map<String, String> hostCookies = cookieStore.computeIfAbsent(domain, k -> new HashMap<>());
					hostCookies.put(name, value);
				}
			}
		}
	}
}
