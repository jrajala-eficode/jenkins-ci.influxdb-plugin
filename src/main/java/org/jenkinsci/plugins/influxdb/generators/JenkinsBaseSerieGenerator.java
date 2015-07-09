package org.jenkinsci.plugins.influxdb.generators;

import hudson.model.AbstractBuild;
import hudson.tasks.test.AbstractTestResultAction;
import org.influxdb.dto.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by jrajala on 15.5.2015.
 */
public class JenkinsBaseSerieGenerator extends AbstractSerieGenerator {

    public static final String PROJECT_BUILD_HEALTH = "project_build_health";


    public JenkinsBaseSerieGenerator(AbstractBuild<?, ?> build) {
        this.build = build;
    }

    public boolean hasReport() {
        return true;
    }

    public Point[] generate(String baseSerieName) {
        this.baseSerieName = baseSerieName;
        Point.Builder builder = Point.measurement(getSerieName());

        Point point = builder.field(BUILD_DURATION,    build.getDuration())
                .field(BUILD_TIME, build.getTimeInMillis())
                .field(BUILD_NUMBER, build.getNumber())
                .field(PROJECT_BUILD_HEALTH, build.getProject().getBuildHealth().getScore())
                .tag("source", "jenkins")
                .time(build.getStartTimeInMillis() + build.getDuration(), TimeUnit.MILLISECONDS)
                .build();


        return new Point[] { point };

    }

    private String getSerieName() {
        return getBaseSerieName() + "jenkins";
    }

}
