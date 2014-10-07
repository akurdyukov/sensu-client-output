package org.kurdyukov.sensu;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

/**
 * This is the plugin. Your class should implement one of the existing plugin
 * interfaces. (i.e. AlarmCallback, MessageInput, MessageOutput)
 */
public class SensuClientOutput implements MessageOutput {
    private static final Logger LOG = LoggerFactory.getLogger(SensuClientOutput.class);

    private final static String CK_CHECK_NAME = "check_name";
    private final static String CK_NEW_ALERT_EVERY_LINE = "new_alert_every_line";

    private final static int STATUS_OK = 0;
    private final static int STATUS_WARNING = 1;
    private final static int STATUS_CRITICAL = 2;

    private Socket socket;
    private DataOutputStream out;
    private String checkName;
    private boolean needNewAlertEveryLine;

    @Override
    public void initialize(Configuration configuration) throws MessageOutputConfigurationException {
        if (!configuration.stringIsSet(CK_CHECK_NAME)) {
            throw new MessageOutputConfigurationException(CK_CHECK_NAME + " must not be empty");
        }
        checkName = configuration.getString(CK_CHECK_NAME);
        needNewAlertEveryLine = configuration.getBoolean(CK_NEW_ALERT_EVERY_LINE);

        // open TCP socket
        try {
            openSocket();
        } catch (IOException e) {
            throw new MessageOutputConfigurationException("Cannot connect to local Sensu client");
        }
    }

    private void openSocket() throws IOException {
        try {
            socket = new Socket("localhost", 3030);
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            LOG.error("Cannot connect to local Sensu client", e);

            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e1) {
                    // just ignore it
                }
                socket = null;
            }

            throw e;
        }
    }

    private void closeSocket() {
        try {
            out.close();
            socket.close();
        } catch (IOException e) {
            LOG.error("Error stopping Sensu output stream", e);
        } finally {
            out = null;
            socket = null;
        }
    }

    @Override
    public void stop() {
        // close TCP socket
        closeSocket();
    }

    @Override
    public boolean isRunning() {
        return out != null;
    }

    @Override
    public void write(Message message) throws Exception {
        String name = needNewAlertEveryLine ? checkName + message.getId() : checkName;

        int level = message.getFieldAs(Integer.class, "level");
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

        try {
            out.writeUTF(json.toString());
        } catch (IOException e) {
            LOG.warn("Error sending message {}", json.toString(), e);
            // something is wrong with socket - reopen it
            closeSocket();
            openSocket();
        }
    }

    @Override
    public void write(List<Message> messages) throws Exception {
        for (Message msg: messages) {
            write(msg);
        }
    }

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

    @Override
    public String getName() {
        return "SensuClientOutput";
    }

    @Override
    public String getHumanName() {
        return "SensuClientOutput";
    }

    @Override
    public String getLinkToDocs() {
        return "https://github.com/akurdyukov/sensu-client-output";
    }
}
