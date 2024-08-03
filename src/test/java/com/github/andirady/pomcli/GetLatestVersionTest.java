/**
 * Copyright 2021-2024 Andi Rady Kurniawan
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

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

    @ParameterizedTest
    @ValueSource(strings = { "-rc", "-rc1", "-alpha", "-alpha1", "-beta", "-beta1" })
    void testExclude(String word) throws Exception {
        interface InputStreamResponse extends HttpResponse<InputStream> {
        }

        var httpClient = Mockito.mock(HttpClient.class);
        var httpResponse = Mockito.mock(InputStreamResponse.class);

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(new ByteArrayInputStream("""
                <?xml version="1.0" encoding="UTF-8"?>
                <metadata>
                  <groupId>foobar</groupId>
                  <artifactId>foobar</artifactId>
                  <versioning>
                    <latest>2.0.0%1$s</latest>
                    <release>2.0.0%1$s</release>
                    <versions>
                      <version>0.8.0</version>
                      <version>0.9.0</version>
                      <version>1.0.0</version>
                      <version>2.0.0%1$s</version>
                    </versions>
                    <lastUpdated>20240701154556</lastUpdated>
                  </versioning>
                </metadata>
                """.formatted(word).getBytes()));
        doReturn(httpResponse).when(httpClient).send(any(), any());

        var latestVersion = new GetLatestVersion(httpClient);
        var version = latestVersion.execute(QuerySpec.of("foobar:foobar")).orElseThrow();
        assertEquals("1.0.0", version);
    }
}
