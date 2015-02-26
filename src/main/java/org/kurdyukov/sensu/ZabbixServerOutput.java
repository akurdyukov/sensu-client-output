package org.kurdyukov.sensu;

import com.google.inject.assistedinject.Assisted;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Zabbix local sender
 */
public class ZabbixServerOutput implements MessageOutput {
    private static final Logger LOG = LoggerFactory.getLogger(ZabbixServerOutput.class);

    private final static String CK_ZABBIX_HOST = "zabbix_host";
    private final static String CK_ZABBIX_PORT = "zabbix_port";
    private final static String CK_KEY_PATTERN = "zabbix_key_pattern";
    private final static String CK_VALUE_PATTERN = "zabbix_value_pattern";

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Inject
    public ZabbixServerOutput(@Assisted Configuration configuration) throws MessageOutputConfigurationException {
        LOG.info("Initializing");
        // TODO: open socket
        isRunning.set(true);
    }

    @Override
    public void stop() {
        // TODO: close socket
        isRunning.set(false);
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public void write(Message message) throws Exception {
        // TODO: implement writing
    }

    @Override
    public void write(List<Message> messages) throws Exception {
        for (Message msg : messages) {
            write(msg);
        }
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("ZabbixServerOutput", false, "https://github.com/akurdyukov/sensu-client-output", "Output to Zabbix server");
        }
    }

    public interface Factory extends MessageOutput.Factory<ZabbixServerOutput> {
        @Override
        ZabbixServerOutput create(Stream stream, Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Config extends MessageOutput.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            ConfigurationRequest c = new ConfigurationRequest();

            c.addField(new TextField(
                    CK_ZABBIX_HOST,
                    "Zabbix host",
                    "localhost",
                    "Zabbix host name or IP address",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));
            c.addField(new NumberField(
                    CK_ZABBIX_PORT,
                    "Zabbix port",
                    10051,
                    "Zabbix active port",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            c.addField(new TextField(
                    CK_KEY_PATTERN,
                    "Trapper key pattern",
                    "",
                    "Trapper key pattern",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            c.addField(new TextField(
                    CK_VALUE_PATTERN,
                    "Trapper value pattern",
                    "",
                    "Trapper value pattern",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            return c;
        }
    }
}
