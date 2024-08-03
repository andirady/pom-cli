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
package com.github.andirady.pomcli.impl;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.supplier.SessionBuilderSupplier;
import org.eclipse.aether.util.listener.ChainedRepositoryListener;

import com.github.andirady.pomcli.ResolutionProvider;

public class ResolutionProviderImpl implements ResolutionProvider {

    private static final Logger LOG = Logger.getLogger("");

    private RepositorySystem repoSystem;

    private List<RemoteRepository> repositories;

    private File localRepoDirectory;

    public ResolutionProviderImpl() {
        this.repoSystem = ServiceLoader.load(RepositorySystemSupplier.class).findFirst().orElseThrow().get();
        this.repositories = List.of(
                new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build());
        this.localRepoDirectory = Path.of(System.getProperty("user.home"), ".m2", "repository").toFile();
    }

    @Override
    public Model readModel(String groupId, String artifactId, String version) {
        var system = ServiceLoader.load(RepositorySystemSupplier.class).findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "No provider for " + RepositorySystemSupplier.class.getName()))
                .get();
        var artifact = new DefaultArtifact(groupId, artifactId, null, "pom", version);
        var sessionBuilder = new SessionBuilderSupplier(system).get()
                .withLocalRepositoryBaseDirectories(
                        localRepoDirectory);
        try (
                var session = sessionBuilder.build()) {
            var artifactRequest = new ArtifactRequest(artifact, repositories, null);
            var artifactResult = system.resolveArtifact(session, artifactRequest);
            var path = artifactResult.getArtifact().getFile().toPath();

            var reader = new DefaultModelReader(null);
            try (var is = Files.newInputStream(path)) {
                return reader.read(is, Map.of());
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Optional<Dependency> findByArtifactId(Model model, String groupId, String artifactId, String scope) {
        Objects.requireNonNull(model, "model is required");
        Objects.requireNonNull(artifactId, "artifactId is required");
        Objects.requireNonNull(scope, "scope is required");

        var props = model.getProperties().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
        var parent = model.getParent();
        var artifact = new DefaultArtifact(
                model.getGroupId() instanceof String s ? s : Objects.requireNonNull(parent).getGroupId(),
                model.getArtifactId(), null, model.getPackaging(),
                model.getVersion() instanceof String s ? s : Objects.requireNonNull(parent).getVersion(), props,
                (File) null);
        var results = new CopyOnWriteArrayList<Dependency>();
        var sessionBuilder = new SessionBuilderSupplier(repoSystem).get()
                .withLocalRepositoryBaseDirectories(localRepoDirectory);
        try (var session = sessionBuilder.build()) {
            sessionBuilder.setRepositoryListener(
                    new ChainedRepositoryListener(session.getRepositoryListener(), new AbstractRepositoryListener() {

                        @Override
                        public void artifactResolved(RepositoryEvent event) {
                            var resolvedArtifact = event.getArtifact();
                            if (!"pom".equals(resolvedArtifact.getExtension())) {
                                return;
                            }

                            var reader = new DefaultModelReader(null);
                            Model model;
                            try {
                                model = reader.read(resolvedArtifact.getFile(), Map.of());
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }

                            var found = Stream
                                    .concat(model.getDependencyManagement().getDependencies().stream(),
                                            model.getDependencies().stream())
                                    .filter(d -> d.getArtifactId().equals(artifactId)).findFirst().orElse(null);
                            if (found != null) {
                                results.add(found);
                            }
                        }

                    }));
        }

        try (var session = sessionBuilder.build()) {
            var artifactRequest = new ArtifactRequest(artifact, repositories, null);
            var artifactResult = repoSystem.resolveArtifact(session, artifactRequest);

            var descRequest = new ArtifactDescriptorRequest(artifactResult.getArtifact(), repositories, null);
            var descResult = repoSystem.readArtifactDescriptor(session, descRequest);
            LOG.fine(() -> "Collecting dependencies for " + artifact);
            var collectRequest = new CollectRequest();
            collectRequest.setRootArtifact(descResult.getArtifact());
            collectRequest.setRepositories(repositories);
            repoSystem.collectDependencies(session, collectRequest);
        } catch (DependencyCollectionException | ArtifactDescriptorException | ArtifactResolutionException e) {
            LOG.log(Level.FINE, "", e);
            throw new IllegalStateException(e);
        }

        return results.stream().findFirst();
    }

}
