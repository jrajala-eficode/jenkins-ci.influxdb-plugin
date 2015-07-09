package org.jenkinsci.plugins.influxdb;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by jrajala on 1.5.2015.
 */
public class ServerTest {
    private Server server;

    @Before
    public void setupTestcase() {
        server = new Server();
    }

    @Test
    public void shouldAllowSettingHost() {
        server.setHost("hostname");
        Assert.assertEquals("hostname", server.getHost());
    }

    @Test
    public void shouldAllowSettingPort() {
        server.setPort("1");
        Assert.assertEquals("1", server.getPort());
    }

    @Test
    public void shouldAllowSettingDescription() {
        server.setDescription("description");
        Assert.assertEquals("description", server.getDescription());
    }

    @Test
    public void shouldAllowSettingDatabaseName() {
        server.setDatabaseName("database");
        Assert.assertEquals("database", server.getDatabaseName());
    }

    @Test
    public void shouldAllowSettingUser() {
        server.setUser("user");
        Assert.assertEquals("user", server.getUser());
    }

    @Test
    public void shouldAllowSettingPassword() {
        server.setPassword("pass");
        Assert.assertEquals("pass", server.getPassword());
    }

    @Test
    public void shouldAllowSettingSerieBaseName() {
        server.setSerieBaseName("serie.foo.bar");
        Assert.assertEquals("serie.foo.bar", server.getSerieBaseName());
    }
}
