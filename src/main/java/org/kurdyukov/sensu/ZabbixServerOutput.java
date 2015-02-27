package org.kurdyukov.sensu;

import com.google.inject.assistedinject.Assisted;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Stream;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Zabbix local sender
 */
public class ZabbixServerOutput implements MessageOutput {
    private static final Logger LOG = LoggerFactory.getLogger(ZabbixServerOutput.class);

    private final static String CK_ZABBIX_HOST = "zabbix_host";
    private final static String CK_REPORTER_HOST = "reporter_host";
    private final static String CK_ZABBIX_PORT = "zabbix_port";
    private final static String CK_KEY_PATTERN = "zabbix_key_pattern";
    private final static String CK_VALUE_PATTERN = "zabbix_value_pattern";
    private final static String REQUEST_FIELD_VALUE = "sender data";
    private final static String ZABBIX_HEADER = "ZBXD\1";

    private final static int LONG_NUMBER_BYTE_LENGTH = 8;
    private final static int NON_DATA_PART_OF_MESSAGE = ZABBIX_HEADER.length() + LONG_NUMBER_BYTE_LENGTH;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private Configuration configuration;

    @Inject
    public ZabbixServerOutput(@Assisted Configuration configuration) throws MessageOutputConfigurationException {
        this.configuration = configuration;
        LOG.info("Initializing");
        isRunning.set(true);
    }

    @Override
    public void stop() {
        isRunning.set(false);
        LOG.info("Stopped");
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public void write(Message message) throws Exception {

        LOG.info("connecting to zabbix server...");
        Socket socket = new Socket(configuration.getString(CK_ZABBIX_HOST), configuration.getInt(CK_ZABBIX_PORT));

        if (!socket.isConnected()) {
            throw new Exception("failed to connect to zabbix server");
        }
        LOG.info("connection to zabbix server has been established");

        buildMessageAndSend(message, socket);
        readAndHandleResponse(socket);
    }

    private void buildMessageAndSend(Message message, Socket socket) throws IOException {
        String str = buildMessage(message);
        ByteBuffer buffer = getMessageByteBuffer(str);

        OutputStream outputStream = socket.getOutputStream();
        outputStream.write(buffer.array());
    }

    private void readAndHandleResponse(Socket socket) throws IOException {
        String response = new String();
        InputStream inputStream = socket.getInputStream();
        while (isRunning()) {
            byte[] buff = new byte[1024];
            int i = inputStream.read(buff);

            if (i == -1) {
                break;
            }

            String dataPart = new String(buff, 0, i, "UTF-8");

            response += dataPart;
        }

        if (response.length() <= NON_DATA_PART_OF_MESSAGE) {
            LOG.error("got strange response from server " + response);
            return;
        }
        response = response.substring(NON_DATA_PART_OF_MESSAGE);
        if (!response.contains("processed: 1") || response.contains("failed: 1")) {
            LOG.error(response);
        }
    }

    private String buildMessage(Message message) {
        JSONObject jsonData = new JSONObject();
        jsonData.put("host", configuration.getString(CK_REPORTER_HOST));

        String keyString = getFormattedResult(message, configuration.getString(CK_KEY_PATTERN));
        String valueString = getFormattedResult(message, configuration.getString(CK_VALUE_PATTERN));

        jsonData.put("key", keyString);
        jsonData.put("value", valueString);

        JSONObject json = new JSONObject();
        json.put("request", REQUEST_FIELD_VALUE);
        json.put("data", Arrays.asList(jsonData));

        return json.toString().replaceAll("\\\\", "");
    }

    private ByteBuffer getMessageByteBuffer(String str) {
        byte[] data = str.getBytes();
        byte[] header = ZABBIX_HEADER.getBytes();

        ByteBuffer buffer = ByteBuffer.allocate(header.length + LONG_NUMBER_BYTE_LENGTH + data.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        int currentOffset = 0;
        buffer.put(header, 0, header.length);
        currentOffset += header.length;

        buffer.putInt(currentOffset, data.length);
        currentOffset += LONG_NUMBER_BYTE_LENGTH;

        buffer.position(currentOffset);
        buffer.put(data, 0, data.length);
        return buffer;
    }

    private String getFormattedResult(Message message, String pattern) {
        String result = new String(pattern);

        for (String fieldName : message.getFieldNames()) {
            String replacement = message.getField(fieldName).toString();
            String whatToReplace = String.format("%%(%s)", fieldName);
            result = result.replace(whatToReplace, replacement);
        }

        return result;
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
                    "%(server).%(level)",
                    "Trapper key pattern",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            c.addField(new TextField(
                    CK_VALUE_PATTERN,
                    "Trapper value pattern",
                    "%(message).%(exception)",
                    "Trapper value pattern",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            c.addField(new TextField(
                    CK_REPORTER_HOST,
                    "Reporter server host",
                    "localhost",
                    "reporter host name or IP address",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            return c;
        }
    }
}
