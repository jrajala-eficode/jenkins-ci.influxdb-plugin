package org.jenkinsci.plugins.influxdb.generators;

import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.tasks.test.AbstractTestResultAction;
import org.influxdb.dto.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.io.PrintStream;

/**
 * Created by jrajala on 15.5.2015.
 */
public class JenkinsBaseSerieGenerator extends AbstractSerieGenerator {

    public static final String BUILD_DURATION = "build_duration";
    public static final String BUILD_RESULT = "build_result";
    public static final String BUILD_RESULT_ORDINAL = "build_result_ordinal";
    public static final String BUILD_STATUS_MESSAGE = "build_status_message";
    public static final String PROJECT_BUILD_STABILITY = "project_build_stability";
    public static final String PROJECT_BUILD_HEALTH = "project_build_health";
    public static final String PROJECT_LAST_SUCCESSFUL = "last_successful_build";
    public static final String PROJECT_LAST_STABLE = "last_stable_build";
    public static final String TESTS_FAILED = "tests_failed";
    public static final String TESTS_SKIPPED = "tests_skipped";
    public static final String TESTS_TOTAL = "tests_total";

    public JenkinsBaseSerieGenerator(AbstractBuild<?, ?> build, PrintStream logger) {
        super(build, logger);
    }

    public boolean hasReport() {
        return true;
    }

    public Point[] generate() {
        Point.Builder pointBuilder = Point.measurement(getSerieName())
            .time(build.getTimeInMillis(), TimeUnit.MILLISECONDS);

        logger.println("Influxdb starting to generate basic report");

        addJenkinsBaseInfo(pointBuilder);

        pointBuilder.field(BUILD_DURATION, build.getDuration());
        pointBuilder.field(BUILD_RESULT, build.getResult().toString());
        pointBuilder.field(BUILD_RESULT_ORDINAL, build.getResult().ordinal);
        pointBuilder.field(BUILD_STATUS_MESSAGE, build.getBuildStatusSummary().message);
        pointBuilder.field(PROJECT_BUILD_STABILITY, build.getProject().getBuildHealth().getScore());
        pointBuilder.field(PROJECT_BUILD_HEALTH, getBuildHealth());
        pointBuilder.field(PROJECT_LAST_SUCCESSFUL, build.getProject().getLastSuccessfulBuild().getNumber());
        pointBuilder.field(PROJECT_LAST_STABLE, build.getProject().getLastStableBuild().getNumber());

        if(hasTestResults()) {
            pointBuilder.field(TESTS_TOTAL, build.getAction(AbstractTestResultAction.class).getTotalCount());
            pointBuilder.field(TESTS_FAILED, build.getAction(AbstractTestResultAction.class).getFailCount());
            pointBuilder.field(TESTS_SKIPPED, build.getAction(AbstractTestResultAction.class).getSkipCount());
        }

        logger.println("Influxdb basic report generation finished");

        return new Point[]{pointBuilder.build()};
    }

    private boolean hasTestResults() {
        return build.getAction(AbstractTestResultAction.class) != null;
    }

    /**
     * Build health weihted by the status of the latest build. Guarranteed
     * to be over 50 if the latest build succeeded, or below 50 if the latest
     * build failed (including test failures).
     **/
    private int getBuildHealth() {
        int projectBuildStability = build.getProject().getBuildHealth().getScore();
        boolean buildFailed = build.getResult().isWorseThan(Result.UNSTABLE);

        if (buildFailed == false && hasTestResults()) {
            if (build.getAction(AbstractTestResultAction.class).getFailCount() > 0)
                buildFailed = true;
        }

        return (buildFailed) ? projectBuildStability / 2 : 50 + projectBuildStability / 2;
    }

    private String getSerieName() {
        return build.getProject().getName()+".jenkins";
    }
}
