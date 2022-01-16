package com.github.andirady.pomcli;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class SearchCommandTest {

	@Test
	@Disabled
	void test() {
		var search = new SearchCommand();
		search.arg = new SearchCommand.Exclusive();
		search.arg.c = "Unchecked";
		assertDoesNotThrow(search::run);
	}
}
