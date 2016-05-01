package org.jenkinsci.plugins.influxdb;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * Created by jrajala on 17.5.2015.
 */
public class InfluxDbPublisherTest {

    public static final String SERVER = "server";
    public static final String SERVER_2 = "server2";
    InfluxDbPublisher publisher;

    @Before
    public void setup() {
        publisher = new InfluxDbPublisher(SERVER);
    }

    @Test
    public void shouldSaveSelectedServerInConstructor() {
        Assert.assertEquals(SERVER, publisher.getSelectedServer());
    }


    @Test
    public void shouldSaveSelectedServerWithAccessor() {
        publisher.setSelectedServer(SERVER_2);
        Assert.assertEquals(SERVER_2, publisher.getSelectedServer());
    }

}

