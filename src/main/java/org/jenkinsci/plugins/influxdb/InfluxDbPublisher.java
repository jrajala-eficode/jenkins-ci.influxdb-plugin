package org.jenkinsci.plugins.influxdb;

import org.apache.log4j.Logger;
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
import org.jenkinsci.plugins.influxdb.generators.CoberturaSerieGenerator;
import org.jenkinsci.plugins.influxdb.generators.JenkinsBaseSerieGenerator;
import org.jenkinsci.plugins.influxdb.generators.RobotFrameworkSerieGenerator;
import org.jenkinsci.plugins.influxdb.generators.SerieGenerator;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


/**
 *
 * @author jrajala-eficode
 * @author joachimrodrigues
 * 
 */
public class InfluxDbPublisher extends Notifier {

    Logger log = Logger.getLogger(InfluxDbPublisher.class);

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    private String selectedServer;


    public InfluxDbPublisher() {
    }

    public InfluxDbPublisher(String server) {
        this.selectedServer = server;
    }

    public String getSelectedServer() {
        String tempSelectedServer = selectedServer;
        if (tempSelectedServer == null) {
            Server[] servers = DESCRIPTOR.getServers();
            if (servers.length > 0) {
                tempSelectedServer = servers[0].getHost();
            }
        }
        return tempSelectedServer;
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

        log.info("Performing InfluxDb PostBuildAction");

        Server server = getServer();
        InfluxDB influxDB = openInfluxDb(server);

        JenkinsBaseSerieGenerator jGenerator = new JenkinsBaseSerieGenerator(build);
        BatchPoints jenkinsPoints = BatchPoints.database( server.getDatabaseName() ).points(jGenerator.generate(server.getSerieBaseName())).build();
        influxDB.write(jenkinsPoints);

        CoberturaSerieGenerator cbGenerator = new CoberturaSerieGenerator(build);
        if(cbGenerator.hasReport()) {
            BatchPoints coberturaPoints = BatchPoints.database( server.getDatabaseName() ).points(cbGenerator.generate(server.getSerieBaseName())).build();
            influxDB.write(coberturaPoints);
        }

        SerieGenerator rfGenerator = new RobotFrameworkSerieGenerator(build);
        if(rfGenerator.hasReport()) {
            BatchPoints coberturaPoints = BatchPoints.database( server.getDatabaseName() ).points(rfGenerator.generate(server.getSerieBaseName())).build();
            influxDB.write(coberturaPoints);
        }

        log.info("Done with InfluxDb PostBuildAction");
        return true;
    }

    private InfluxDB openInfluxDb(Server server) {
        System.out.println("!!!! http://" + server.getHost() + ":" + server.getPort());
        return InfluxDBFactory.connect("http://" + server.getHost() + ":" + server.getPort(), server.getUser(), server.getPassword());
    }



}
