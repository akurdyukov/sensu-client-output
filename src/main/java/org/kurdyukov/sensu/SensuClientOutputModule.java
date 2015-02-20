package org.kurdyukov.sensu;

import org.graylog2.plugin.PluginModule;

/**
 * Extend the PluginModule abstract class here to add you plugin to the system.
 */
public class SensuClientOutputModule extends PluginModule {
    @Override
    protected void configure() {
        addMessageOutput(SensuClientOutput.class);
    }
}
