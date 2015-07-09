package org.jenkinsci.plugins.influxdb.generators;

import hudson.model.AbstractBuild;
import org.influxdb.dto.Point;

import java.util.List;

/**
 * Created by jrajala on 15.5.2015.
 */
public abstract class AbstractSerieGenerator implements  SerieGenerator {

    protected AbstractBuild<?, ?> build;
    public static final String BUILD_NUMBER = "build_number";
    public static final String BUILD_TIME = "build_time";
    public static final String BUILD_DURATION = "build_duration";
    protected String baseSerieName = "";

    protected String getProjectSerieName() {
       return build.getProject().getFullName().replaceAll("/",".");
    }

    protected String getBaseSerieName() {
        StringBuilder serieName = new StringBuilder();

        if(baseSerieName!=null)
            serieName.append(baseSerieName.trim()+".");

        serieName.append(getProjectSerieName()+".");
        return serieName.toString();
    }
}
