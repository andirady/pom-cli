package com.github.andirady.pomcli.aether;

import java.util.Map;

import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.transport.jdk.JdkTransporterFactory;

public class MavenRepositorySystemSupplier extends RepositorySystemSupplier {

    @Override
    protected Map<String, TransporterFactory> getTransporterFactories() {
        var result = super.getTransporterFactories();
        result.put(JdkTransporterFactory.NAME, new JdkTransporterFactory());
        return result;
    }
}
