package org.kurdyukov.sensu;

import com.google.inject.assistedinject.Assisted;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.streams.Stream;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
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
    private final static String REQUEST_FIELD_VALUE = "ZBXD\\x01";
    private final static String ZABBIX_HEADER = "sender data";

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private Socket socket;
    private Configuration configuration;
    private OutputStream stream;
    @Inject
    public ZabbixServerOutput(@Assisted Configuration configuration) throws Exception {
        this.configuration = configuration;
        LOG.info("Initializing");

        initializeSocket();
        isRunning.set(true);
    }
    private void initializeSocket() throws Exception {

        if(socket != null && socket.isConnected()){
            socket.close();
        }

        socket = new Socket(configuration.getString(CK_ZABBIX_HOST), configuration.getInt(CK_ZABBIX_PORT));
        LOG.info("connecting to zabbix server...");
        socket.connect(socket.getRemoteSocketAddress(), 5000);

        if(socket.isConnected()){
            LOG.info("connection to zabbix server has been established");
           stream = socket.getOutputStream();
        }else{
            throw new Exception("failed to connect to zabbix server");
        }
    }
    @Override
    public void stop() {
        try {
            socket.close();
        } catch (IOException e) {
            LOG.error("Problems stopping socket", e);
        }
        isRunning.set(false);
        LOG.info("Stopped");
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public void write(Message message) throws Exception {

        JSONObject jsonData = new JSONObject();
        jsonData.put("host",configuration.getString(CK_ZABBIX_HOST));


        String keyString = GetFormattedResult(message, configuration.getString(CK_KEY_PATTERN));
        String valueString = GetFormattedResult(message, configuration.getString(CK_VALUE_PATTERN));

        jsonData.put("key", keyString);
        jsonData.put("value", valueString);

        JSONObject json = new JSONObject();
        json.put("request", REQUEST_FIELD_VALUE);
        json.put("data", Arrays.asList(jsonData.toString()));

        byte[] data = json.toString().getBytes();
        byte[] header = REQUEST_FIELD_VALUE.getBytes();

        ByteBuffer buffer = ByteBuffer.allocate(header.length + 4 + data.length);

        int currentOffset = 0;

        buffer.put(header, 0, header.length);
        currentOffset += header.length;

        buffer.putInt(currentOffset, data.length);
        currentOffset += 4;

        buffer.put(data, currentOffset, data.length);

        stream.write(buffer.array());
    }

    private String GetFormattedResult(Message message, String pattern) {
        String result = new String(pattern);

        for (String fieldName : message.getFieldNames()){
            result = result.replace(String.format("%({0})", fieldName), message.getField(fieldName).toString());
        }

        return result;
    }

    @Override
    public void write(List<Message> messages) throws Exception {

        if(socket == null || !socket.isConnected()){
            initializeSocket();
        }

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

            return c;
        }
    }
}
