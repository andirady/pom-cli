package com.github.andirady.pomcli.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.github.andirady.pomcli.GetJavaMajorVersion;

public class GetJavaMajorVersionImpl implements GetJavaMajorVersion {

    @Override
    public BufferedReader getBufferedReader() throws IOException {
        var p = new ProcessBuilder("java", "-version").redirectErrorStream(true).start();
        var is = p.getInputStream();
        return new BufferedReader(new InputStreamReader(is));
    }
}
