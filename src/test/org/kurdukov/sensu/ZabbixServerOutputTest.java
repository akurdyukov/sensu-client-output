package org.kurdukov.sensu;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kurdyukov.sensu.ZabbixServerOutput;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ZabbixServerOutputTest {

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void ZabbixSendTest() throws Exception {

        Thread thr = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket socket = new ServerSocket (10051);
                    Socket client = socket.accept();

                    InputStream inputStream = client.getInputStream();
                    byte[] buff = new byte[1024];
                    int i = inputStream.read(buff);
                    if(i >=0){
                        OutputStream outputStream = client.getOutputStream();
                        String message = "processed: 1; failed: 0";
                        ByteBuffer buffer = ByteBuffer.allocate(13 + message.length());
                        buffer.position(13);
                        buffer.put(message.getBytes(), 0, message.length());
                        outputStream.write(buffer.array());
                        client.close();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thr.start();

        Thread.sleep(100);

        ZabbixServerOutput.Config config = new ZabbixServerOutput.Config();
        ConfigurationRequest request = config.getRequestedConfiguration();

        Map<String, Object> configMap = new HashMap<>();

        for (ConfigurationField conf : request.getFields().values()) {
            configMap.put(conf.getName(), conf.getDefaultValue());
        }
        Configuration configuration = new Configuration(configMap);


        ZabbixServerOutput plugin = new ZabbixServerOutput(configuration);

        ArrayList<Message> mess = new ArrayList<>();
        Map<String, Object> keyValueMap = new HashMap<>();
        keyValueMap.put("server", "test");
        keyValueMap.put("message", "test");

        keyValueMap.put("level", "fatal");
        keyValueMap.put("exception", "test");
        keyValueMap.put("_id", "id");
        mess.add(new Message(keyValueMap));

        plugin.write(new ArrayList<>(mess));
    }

    @After
    public void tearDown() throws Exception {

    }
}