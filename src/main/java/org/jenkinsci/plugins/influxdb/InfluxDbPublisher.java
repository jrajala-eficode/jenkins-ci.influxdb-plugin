package org.jenkinsci.plugins.influxdb;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.jenkinsci.plugins.influxdb.generators.CoberturaSerieGenerator;
import org.jenkinsci.plugins.influxdb.generators.JenkinsBaseSerieGenerator;
import org.jenkinsci.plugins.influxdb.generators.RobotFrameworkSerieGenerator;
import org.jenkinsci.plugins.influxdb.generators.SerieGenerator;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 *
 * @author jrajala-eficode
 * @author joachimrodrigues
 *
 */
public class InfluxDbPublisher extends Notifier {


    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    private String selectedServer;


    public InfluxDbPublisher() {
    }

    public InfluxDbPublisher(String server) {
        this.selectedServer = server;
        System.out.println("Selected Server: " + server);
    }

    public String getSelectedServer() {
        String ipTemp = selectedServer;
        if (ipTemp == null) {
            Server[] servers = DESCRIPTOR.getServers();
            if (servers.length > 0) {
                ipTemp = servers[0].getHost();
            }
        }
        return ipTemp;
    }

    public void setSelectedServer(String server) {
        this.selectedServer = server;
    }

    public Server getServer() {
        Server[] servers = DESCRIPTOR.getServers();
        if (selectedServer == null && servers.length > 0) {
            return servers[0];
        }
        for (Server server : servers) {
            if (server.getHost().equals(selectedServer)) {
                return server;
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * hudson.tasks.BuildStepCompatibilityLayer#prebuild(hudson.model.AbstractBuild
     * , hudson.model.BuildListener)
     */
    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see hudson.tasks.Publisher#needsToRunAfterFinalized()
     */
    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see hudson.tasks.BuildStep#getRequiredMonitorService()
     */
    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    /*
     * (non-Javadoc)
     *
     * @see hudson.tasks.Notifier#getDescriptor()
     */
    @Override
    public BuildStepDescriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * hudson.tasks.BuildStepCompatibilityLayer#perform(hudson.model.AbstractBuild
     * , hudson.Launcher, hudson.model.BuildListener)
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        PrintStream logger = listener.getLogger();
        Server server = getServer();
        InfluxDB influxDB = openInfluxDb(server);

        logger.println("Influxdb publisher started");

        JenkinsBaseSerieGenerator jGenerator = new JenkinsBaseSerieGenerator(build, logger);
        BatchPoints jenkinsBatchPoints = BatchPoints.database(server.getDatabaseName())
                .retentionPolicy("default")
                .points(jGenerator.generate()).build();
        influxDB.write(jenkinsBatchPoints);

        CoberturaSerieGenerator cbGenerator = new CoberturaSerieGenerator(build, logger);
        if(cbGenerator.hasReport()) {
            BatchPoints coberturaPoints = BatchPoints
                    .database(server.getDatabaseName())
                    .retentionPolicy("default")
                    .points(cbGenerator.generate()).build();
            influxDB.write(coberturaPoints);
        }

        SerieGenerator rfGenerator = new RobotFrameworkSerieGenerator(build, logger);
        if(rfGenerator.hasReport()) {
            BatchPoints robotFrameworkPoints = BatchPoints
                    .database(server.getDatabaseName())
                    .retentionPolicy("default")
                    .points(rfGenerator.generate())
                    .build();
            influxDB.write(robotFrameworkPoints);
        }

        logger.println("Influxdb publisher finished");

        return true;
    }

    private InfluxDB openInfluxDb(Server server) {
        return InfluxDBFactory.connect("http://" + server.getHost() + ":" + server.getPort(), server.getUser(), server.getPassword());
    }
}
