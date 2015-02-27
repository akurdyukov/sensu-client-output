package org.kurdukov.sensu;

import junit.framework.TestCase;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kurdyukov.sensu.ZabbixServerOutput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ZabbizServerOutputTest  {

    @Before
    public void setUp() throws Exception {

    }
    @Test
    public  void ZabbixSendTest() throws Exception {

        ZabbixServerOutput.Config config = new ZabbixServerOutput.Config();
        ConfigurationRequest request = config.getRequestedConfiguration();

        Map<String,Object> configMap = new HashMap<>();

        for (ConfigurationField conf : request.getFields().values()){
            configMap.put(conf.getName(), conf.getDefaultValue());
        }
        Configuration configuration = new Configuration(configMap);


        ZabbixServerOutput plugin = new ZabbixServerOutput(configuration);

        ArrayList<Message> mess = new ArrayList<>();
        Map<String,Object> keyValueMap = new HashMap<>();
        keyValueMap.put("server","test");
        keyValueMap.put("message","test");

        keyValueMap.put("level","fatal");
        keyValueMap.put("exception","test");
        keyValueMap.put("_id", "id");
        mess.add(new Message(keyValueMap));

        plugin.write(new ArrayList<>(mess));
    }

    @After
    public void tearDown() throws Exception {

    }
}