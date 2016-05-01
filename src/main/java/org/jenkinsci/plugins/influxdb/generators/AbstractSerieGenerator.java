package org.jenkinsci.plugins.influxdb.generators;

import hudson.model.AbstractBuild;
import org.influxdb.dto.Point;

import java.util.HashMap;
import java.util.List;
import java.io.PrintStream;


/**
 * Created by jrajala on 15.5.2015.
 */
public abstract class AbstractSerieGenerator implements SerieGenerator {

    public static final String PROJECT_NAME = "project_name";
    public static final String BUILD_NUMBER = "build_number";

    protected final AbstractBuild<?, ?> build;
    protected final PrintStream logger;

    public AbstractSerieGenerator(AbstractBuild<?, ?> build, PrintStream logger) {
        this.build = build;
        this.logger = logger;
    }

    protected void addJenkinsBaseInfo(Point.Builder pointBuilder) {
        pointBuilder.field(PROJECT_NAME, build.getProject().getName());
        pointBuilder.field(BUILD_NUMBER, build.getNumber());
    }
}
