package com.github.andirady.pomcli;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class GetLatestVersionTest {

	@Test
	void test() {
		var latestVersion = new GetLatestVersion().execute(QuerySpec.of("org.apache.logging.log4j:log4j-api"));
		assertTrue(latestVersion.isPresent());
	}

	@Test
	void testNotFound() {
		var latestVersion = new GetLatestVersion()
				.execute(QuerySpec.of("foobar:foobar" + Instant.now().toEpochMilli()));
		assertTrue(latestVersion.isEmpty());
	}
}
