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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.DefaultModelWriter;

public abstract class ModifyingCommandBase implements Callable<Integer> {

    static final Logger LOG = Logger.getLogger("");

    protected Model getModel(boolean mustExists, boolean standalone) throws IOException {
        var pomPath = getPomPath();

        Objects.requireNonNull(pomPath, "pomPath is null");

        var reader = new DefaultModelReader(null);
        if (Files.exists(pomPath)) {
            try (var is = Files.newInputStream(pomPath)) {
                return reader.read(is, null);
            }
        } else if (!mustExists) {
            LOG.fine(() -> pomPath + " does not exists. Creating a new one");
            return new NewPom().newPom(pomPath, standalone);
        } else {
            throw new FileNotFoundException(pomPath.toString());
        }
    }

    protected void saveModel(Model model) {
        if (model == null) {
            throw new IllegalStateException("Call getModel first");
        }

        var pomPath = getPomPath();
        var writer = new DefaultModelWriter();
        try (var os = Files.newOutputStream(pomPath)) {
            writer.write(os, null, model);
            LOG.fine(() -> "Changes saved to " + pomPath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Integer call() throws Exception {
        var model = getModel(getPomPathMustExists(), getStandalone());
        var rc = process(model);
        saveModel(model);
        return rc;
    }

    boolean getStandalone() {
        return false;
    }

    boolean getPomPathMustExists() {
        return false;
    }

    abstract Path getPomPath();

    public abstract int process(Model model) throws Exception;

}
