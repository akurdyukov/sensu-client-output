package org.kurdyukov.sensu;

import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.Version;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Plugin metadata
 */
public class SensuClientOutputMetadata implements PluginMetaData {
    @Override
    public String getUniqueId() {
        return "sensu-client-output";
    }

    @Override
    public String getName() {
        return "SensuClientOutput";
    }

    @Override
    public String getAuthor() {
        return "Alik Kurdyukov <alik@kurdyukov.com>";
    }

    @Override
    public URI getURL() {
        try {
            return new URI("https://github.com/akurdyukov/sensu-client-output");
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    public Version getVersion() {
        return new Version(1, 0, 0, "SNAPSHOT");
    }

    @Override
    public String getDescription() {
        return "Inject messages to Sensu client thru port 3030 on localhost";
    }

    @Override
    public Version getRequiredVersion() {
        return new Version(0, 90, 0);
    }
}
