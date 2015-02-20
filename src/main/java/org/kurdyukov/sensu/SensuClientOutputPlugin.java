package org.kurdyukov.sensu;

import java.util.Arrays;
import java.util.Collection;
import com.google.common.collect.Lists;
import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;

/**
 * Implement the Plugin interface here.
 */
public class SensuClientOutputPlugin implements Plugin {
    @Override
    public PluginMetaData metadata() {
        return new SensuClientOutputMetadata();
    }

    @Override
    public Collection<PluginModule> modules () {
        return Arrays.<PluginModule>asList(new SensuClientOutputModule());
    }
}
