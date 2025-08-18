/**
 * Copyright 2021-2025 Andi Rady Kurniawan
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.andirady.pomcli;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.StringJoiner;

public record QuerySpec(String groupId, String artifactId, String version) {

	public static QuerySpec of(String spec) {
		var parts = spec.split(":");
		return switch (parts.length) {
			case 1 -> new QuerySpec(null, parts[0], null);
			case 2 -> new QuerySpec(parts[0], parts[1], null);
			case 3 -> new QuerySpec(parts[0], parts[1], parts[2]);
			default -> throw new IllegalArgumentException("Invalid spec: " + spec);
		};
	}

	public URI toURI() {
		try {
			return new URI("https", "search.maven.org", "/solrsearch/select", "q=" + toString() + "&start=0&rows=1",
					null);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public String toString() {
		var parts = new StringJoiner(" AND ");
		if (groupId != null) {
			parts.add("g:" + groupId);
		}

		if (artifactId != null) {
			parts.add("a:" + artifactId);
		}

		if (version != null) {
			parts.add("v:" + version);
		}

		return parts.toString();
	}
}
