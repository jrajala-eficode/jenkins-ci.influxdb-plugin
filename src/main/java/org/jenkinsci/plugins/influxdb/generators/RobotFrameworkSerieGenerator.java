package org.jenkinsci.plugins.influxdb.generators;

import hudson.model.AbstractBuild;
import hudson.plugins.robot.RobotBuildAction;
import hudson.plugins.robot.model.RobotCaseResult;
import hudson.plugins.robot.model.RobotResult;
import hudson.plugins.robot.model.RobotSuiteResult;
import org.influxdb.dto.Point;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by jrajala on 15.5.2015.
 */
public class RobotFrameworkSerieGenerator extends AbstractSerieGenerator {

    public static final String RF_FAILED = "rf_failed";
    public static final String RF_PASSED = "rf_passed";
    public static final String RF_TOTAL = "rf_total";
    public static final String RF_CRITICAL_FAILED = "rf_critical_failed";
    public static final String RF_CRITICAL_PASSED = "rf_critical_passed";
    public static final String RF_CRITICAL_TOTAL = "rf_critical_total";
    public static final String RF_CRITICAL_PASS_PERCENTAGE = "rf_critical_pass_percentage";
    public static final String RF_PASS_PERCENTAGE = "rf_pass_percentage";
    public static final String RF_DURATION = "rf_duration";
    public static final String RF_SUITES = "rf_suites";
    public static final String RF_TESTCASES = "rf_testcases";
    public static final String RF_CASE_NAME = "rf_case_name";
    public static final String RF_CASE_SUITE = "rf_suite";
    public static final String RF_TAG_NAME = "rf_tag";
    public static final String RF_TAG_LIST = "rf_tag_list";
    public static final String RF_SUITE_NAME = "rf_suite";

    private final Map<String, RobotTagResult> tagResults;

    public RobotFrameworkSerieGenerator(AbstractBuild<?, ?> build, PrintStream logger) {
        super(build, logger);
        tagResults = new Hashtable<String, RobotTagResult>();
    }

    public boolean hasReport() {
        RobotBuildAction robotBuildAction = build.getAction(RobotBuildAction.class);
        return robotBuildAction != null && robotBuildAction.getResult() != null;
    }

    public Point[] generate() {
        RobotBuildAction robotBuildAction = build.getAction(RobotBuildAction.class);

        List<Point> seriesList = new ArrayList<Point>();

        logger.println("Influxdb starting to generate Robot Framework report");

        seriesList.add(generateOverviewSerie(robotBuildAction));
        seriesList.addAll(generateSubSeries(robotBuildAction.getResult()));

        logger.println("Influxdb Robot Framework report generation finished");

        return seriesList.toArray(new Point[seriesList.size()]);
    }

    private Point generateOverviewSerie(RobotBuildAction robotBuildAction) {
        Point.Builder pointBuilder = Point.measurement(getSeriePrefix())
            .time(build.getTimeInMillis(), TimeUnit.MILLISECONDS);

        addJenkinsBaseInfo(pointBuilder);

        pointBuilder.field(RF_FAILED, robotBuildAction.getResult().getOverallFailed());
        pointBuilder.field(RF_PASSED, robotBuildAction.getResult().getOverallPassed());
        pointBuilder.field(RF_TOTAL, robotBuildAction.getResult().getOverallTotal());
        pointBuilder.field(RF_CRITICAL_FAILED, robotBuildAction.getResult().getCriticalFailed());
        pointBuilder.field(RF_CRITICAL_PASSED, robotBuildAction.getResult().getCriticalPassed());
        pointBuilder.field(RF_CRITICAL_TOTAL, robotBuildAction.getResult().getCriticalTotal());
        pointBuilder.field(RF_CRITICAL_PASS_PERCENTAGE, robotBuildAction.getCriticalPassPercentage());
        pointBuilder.field(RF_PASS_PERCENTAGE, robotBuildAction.getOverallPassPercentage());
        pointBuilder.field(RF_DURATION, robotBuildAction.getResult().getDuration());
        pointBuilder.field(RF_SUITES, robotBuildAction.getResult().getAllSuites().size());

        return pointBuilder.build();
    }

    private List<Point> generateSubSeries(RobotResult robotResult) {
        List<Point> subSeries = new ArrayList<Point>();
        for(RobotSuiteResult suiteResult : robotResult.getAllSuites()) {
            subSeries.add(generateSuiteSerie(suiteResult));

            for(RobotCaseResult caseResult : suiteResult.getAllCases()) {
                subSeries.add(generateCaseSerie(caseResult));
            }

        }

        for(Map.Entry<String, RobotTagResult> entry : tagResults.entrySet()) {
            subSeries.add(generateTagSerie(entry.getValue()));
        }
        return subSeries;
    }

    private Point generateCaseSerie(RobotCaseResult caseResult) {
        Point.Builder pointBuilder = Point.measurement(getCaseSerieName(caseResult))
            .time(build.getTimeInMillis(), TimeUnit.MILLISECONDS);

        addJenkinsBaseInfo(pointBuilder);

        pointBuilder.field(RF_CRITICAL_FAILED, caseResult.getCriticalFailed());
        pointBuilder.field(RF_CRITICAL_PASSED, caseResult.getCriticalPassed());
        pointBuilder.field(RF_FAILED, caseResult.getFailed());
        pointBuilder.field(RF_PASSED, caseResult.getPassed());
        pointBuilder.field(RF_DURATION, caseResult.getDuration());

        // build comicolon separated tag list
        StringBuilder tagListBuilder = new StringBuilder();
        for(String tag : caseResult.getTags()) {
            markTagResult(tag, caseResult);
            tagListBuilder.append(tag);
            tagListBuilder.append(";");
        }
        // remove training comma
        String tagList = tagListBuilder.length() > 0 ? tagListBuilder.substring(0, tagListBuilder.length() - 1): "";

        if (tagList.length() > 0) {
            pointBuilder.tag(RF_TAG_LIST, tagList);
        }

        pointBuilder.tag(RF_CASE_NAME, caseResult.getName());
        pointBuilder.tag(RF_CASE_SUITE, caseResult.getParent().getName());

        return pointBuilder.build();
    }

    private final class RobotTagResult {
        protected final String name;
        protected RobotTagResult(String name) {
            this.name = name;
        }
        protected final List<String> testCases = new ArrayList<String>();
        protected int failed = 0;
        protected int passed = 0;
        protected int criticalFailed = 0;
        protected int criticalPassed = 0;
        protected long duration = 0;
    }

    private void markTagResult(String tag, RobotCaseResult caseResult) {
        if(tagResults.get(tag) == null)
            tagResults.put(tag, new RobotTagResult(tag));

        RobotTagResult tagResult = tagResults.get(tag);
        if(!tagResult.testCases.contains(caseResult.getDuplicateSafeName())) {
            tagResult.failed += caseResult.getFailed();
            tagResult.passed += caseResult.getPassed();
            tagResult.criticalFailed += caseResult.getCriticalFailed();
            tagResult.criticalPassed += caseResult.getCriticalPassed();
            tagResult.duration += caseResult.getDuration();
            tagResult.testCases.add(caseResult.getDuplicateSafeName());
        }
    }

    private Point generateTagSerie(RobotTagResult tagResult) {

        Point.Builder pointBuilder = Point.measurement(getTagSerieName(tagResult))
            .time(build.getTimeInMillis(), TimeUnit.MILLISECONDS);

        addJenkinsBaseInfo(pointBuilder);

        pointBuilder.field(RF_CRITICAL_FAILED, tagResult.criticalFailed);
        pointBuilder.field(RF_CRITICAL_PASSED, tagResult.criticalPassed);
        pointBuilder.field(RF_CRITICAL_TOTAL, tagResult.criticalPassed + tagResult.criticalFailed);
        pointBuilder.field(RF_FAILED, tagResult.failed);
        pointBuilder.field(RF_PASSED, tagResult.passed);
        pointBuilder.field(RF_TOTAL, tagResult.passed + tagResult.failed);
        pointBuilder.field(RF_DURATION, tagResult.duration);
        pointBuilder.tag(RF_TAG_NAME, tagResult.name);

        return pointBuilder.build();
    }

    private Point generateSuiteSerie(RobotSuiteResult suiteResult) {
        Point.Builder pointBuilder = Point.measurement(getSuiteSeriePrefix())
            .time(build.getTimeInMillis(), TimeUnit.MILLISECONDS);

        addJenkinsBaseInfo(pointBuilder);

        pointBuilder.field(RF_TESTCASES, suiteResult.getAllCases().size());
        pointBuilder.field(RF_CRITICAL_FAILED, suiteResult.getCriticalFailed());
        pointBuilder.field(RF_CRITICAL_PASSED, suiteResult.getCriticalPassed());
        pointBuilder.field(RF_CRITICAL_TOTAL, suiteResult.getCriticalTotal());
        pointBuilder.field(RF_FAILED, suiteResult.getFailed());
        pointBuilder.field(RF_PASSED, suiteResult.getPassed());
        pointBuilder.field(RF_TOTAL, suiteResult.getTotal());
        pointBuilder.field(RF_DURATION, suiteResult.getDuration());
        pointBuilder.tag(RF_SUITE_NAME, suiteResult.getName());

        return pointBuilder.build();
    }


    private String getTagSerieName(RobotTagResult tagResult) {
        return getTagSeriePrefix();
    }

    private String getCaseSerieName(RobotCaseResult caseResult) {
        return getCaseSeriePrefix();
    }

    private String getCaseSeriePrefix() {
        return getSeriePrefix()+".testcase";
    }

    private String getTagSeriePrefix() {
        return getSeriePrefix()+".tag";
    }

    private String getSuiteSeriePrefix() {
        return getSeriePrefix()+".suite";
    }

    private String getSeriePrefix() {
        return build.getProject().getName()+".rf";
    }
}
