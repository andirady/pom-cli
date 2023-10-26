package com.github.andirady.pomcli.impl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import com.github.andirady.pomcli.GetJavaMajorVersion;

public class GetJavaMajorVersionTestImpl implements GetJavaMajorVersion {

    private static ThreadLocal<String> result = new ThreadLocal<>();

    public void setResult(String value) {
        result.set(value);
    }

    @Override
    public BufferedReader getBufferedReader() throws IOException {
        if (result.get() == null) {
            return new GetJavaMajorVersionImpl().getBufferedReader();
        }

        var baos = new ByteArrayInputStream(result.get().getBytes());
        return new BufferedReader(new InputStreamReader(baos));
    }
    
}
