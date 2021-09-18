package com.github.andirady.mq;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class SearchTest {

	@Test
	@Disabled
	void test() {
		var search = new Search();
		search.arg = new Search.Exclusive();
		search.arg.c = "Unchecked";
		assertDoesNotThrow(search::run);
	}
}
