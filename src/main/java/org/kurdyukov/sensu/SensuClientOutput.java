package org.kurdyukov.sensu;

import com.google.inject.assistedinject.Assisted;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Stream;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sends message to Sensu local agent
 */
public class SensuClientOutput implements MessageOutput {
    private static final Logger LOG = LoggerFactory.getLogger(SensuClientOutput.class);

    private final static String CK_CHECK_NAME = "check_name";
    private final static String CK_NEW_ALERT_EVERY_LINE = "new_alert_every_line";

    private final static int STATUS_OK = 0;
    private final static int STATUS_WARNING = 1;
    private final static int STATUS_CRITICAL = 2;

    private DatagramSocket socket;
    private InetAddress ipAddress;
    private String checkName;
    private boolean needNewAlertEveryLine;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Inject
    public SensuClientOutput(@Assisted Configuration configuration) throws MessageOutputConfigurationException {
        LOG.info("Initializing");

        if (!configuration.stringIsSet(CK_CHECK_NAME)) {
            throw new MessageOutputConfigurationException(CK_CHECK_NAME + " must not be empty");
        }
        checkName = configuration.getString(CK_CHECK_NAME);
        needNewAlertEveryLine = configuration.getBoolean(CK_NEW_ALERT_EVERY_LINE);
        LOG.info("Starting Sensu output check name '{}' new alerts {}", checkName, needNewAlertEveryLine);

        try {
            socket = new DatagramSocket();
            ipAddress = InetAddress.getByName("localhost");
        } catch (IOException e) {
            LOG.error("Problems starting output", e);
            throw new MessageOutputConfigurationException("Cannot start Sensu output");
        }
        isRunning.set(true);
    }

    @Override
    public void stop() {
        if (socket != null) {
            socket.close();
        }

        isRunning.set(false);
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public void write(Message message) throws Exception {
        String name = needNewAlertEveryLine ? checkName + message.getId() : checkName;

        long level = message.getFieldAs(Long.class, "level");
        int status = STATUS_OK;
        if (level < 4)
            status = STATUS_WARNING;
        if (level < 2)
            status = STATUS_CRITICAL;

        String output = message.getMessage();

        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("output", output);
        json.put("status", status);

        byte[] data = json.toString().getBytes("UTF-8");
        DatagramPacket packet = new DatagramPacket(data, data.length, ipAddress, 3030);
        socket.send(packet);
    }

    @Override
    public void write(List<Message> messages) throws Exception {
        for (Message msg : messages) {
            write(msg);
        }
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("SensuClientOutput", false, "https://github.com/akurdyukov/sensu-client-output", "Output to local sensu agent");
        }
    }

    public interface Factory extends MessageOutput.Factory<SensuClientOutput> {
        @Override
        SensuClientOutput create(Stream stream, Configuration configuration);

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
                    CK_CHECK_NAME,
                    "Sensu check name",
                    "log_error",
                    "Check name reported to Sensu",
                    ConfigurationField.Optional.NOT_OPTIONAL));

            c.addField(new BooleanField(
                    CK_NEW_ALERT_EVERY_LINE,
                    "Create new event for every line",
                    false,
                    "Create new error event for every new line in stream"));

            return c;
        }
    }
}
