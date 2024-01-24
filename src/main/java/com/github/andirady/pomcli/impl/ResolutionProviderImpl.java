package com.github.andirady.pomcli.impl;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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

    public ResolutionProviderImpl() {
        this.repoSystem = ServiceLoader.load(RepositorySystemSupplier.class).findFirst().orElseThrow().get();
    }

    @Override
    public Optional<Dependency> findByArtifactId(Model model, String artifactId, String scope) {
        var props = model.getProperties().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
        var parent = model.getParent();
        var artifact = new DefaultArtifact(
                model.getGroupId() instanceof String s ? s : Objects.requireNonNull(parent).getGroupId(),
                model.getArtifactId(), null, model.getPackaging(),
                model.getVersion() instanceof String s ? s : Objects.requireNonNull(parent).getVersion(), props,
                (File) null);
        System.out.println(artifact);
        var repositories = List.of(
                new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build());

        var results = new CopyOnWriteArrayList<Dependency>();
        var sessionBuilder = new SessionBuilderSupplier(repoSystem).get().withLocalRepositoryBaseDirectories(
                Path.of(System.getProperty("user.home"), ".m2", "repository").toFile());
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
