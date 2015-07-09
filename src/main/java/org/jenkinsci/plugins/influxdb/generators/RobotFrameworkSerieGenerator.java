package org.jenkinsci.plugins.influxdb.generators;

import hudson.model.AbstractBuild;
import hudson.plugins.robot.RobotBuildAction;
import org.influxdb.dto.Point;

import java.util.*;
import java.util.List;
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

//    private final AbstractBuild<?, ?> build;
//    private final Map<String, RobotTagResult> tagResults;

    public RobotFrameworkSerieGenerator(AbstractBuild<?, ?> build) {
        this.build = build;
//        tagResults = new Hashtable<String, RobotTagResult>();
    }

    public boolean hasReport() {
        RobotBuildAction robotBuildAction = build.getAction(RobotBuildAction.class);
        return robotBuildAction != null && robotBuildAction.getResult() != null;
    }

    public Point[] generate(String baseSerieName) {
        this.baseSerieName = baseSerieName;
        RobotBuildAction robotBuildAction = build.getAction(RobotBuildAction.class);

        List<Point> seriesList = new ArrayList<Point>();


        seriesList.add(point(BUILD_DURATION, build.getDuration()));
        seriesList.add(point(BUILD_TIME,  build.getTimeInMillis()));
        seriesList.add(point(BUILD_NUMBER,  build.getNumber()));
        seriesList.add(point(RF_FAILED,  robotBuildAction.getResult().getOverallFailed()));
        seriesList.add(point(RF_PASSED,  robotBuildAction.getResult().getOverallPassed()));
        seriesList.add(point(RF_TOTAL,  robotBuildAction.getResult().getOverallTotal()));

        /*
                .field(RF_PASS_PERCENTAGE, robotBuildAction.getResult().getPassPercentage()).build();
                .field(RF_CRITICAL_FAILED, robotBuildAction.getResult().getCriticalFailed()).build();
                .field(RF_CRITICAL_PASSED, robotBuildAction.getResult().getCriticalPassed()).build();
                .field(RF_CRITICAL_TOTAL, robotBuildAction.getResult().getCriticalTotal()).build();
                .field(RF_CRITICAL_PASS_PERCENTAGE, robotBuildAction.getResult().getPassPercentage(true)).build();
                .field(RF_DURATION, robotBuildAction.getResult().getDuration()).build();
                .field(RF_SUITES, robotBuildAction.getResult().getAllSuites().size()).build();
        seriesList.add(generateOverviewSeries(robotBuildAction));
        */
//        seriesList.addAll(generateSubSeries(robotBuildAction.getResult()));

        return seriesList.toArray(new Point[seriesList.size()]);
    }

    private Point point(String measurement, Object value) {
        return Point.measurement(getSeriePrefix()+"."+measurement)
                .time(build.getStartTimeInMillis() + build.getDuration(), TimeUnit.MILLISECONDS)
                .field("value", value).build();
    }

    /*
    private List<Point> generateSubSeries(RobotResult robotResult) {
        List<Serie> subSeries = new ArrayList<Serie>();
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
        List<String> columns = new ArrayList<String>();
        List<Object> values = new ArrayList<Object>();

        columns.add(RF_CRITICAL_FAILED);
        values.add(caseResult.getCriticalFailed());

        columns.add(RF_CRITICAL_PASSED);
        values.add(caseResult.getCriticalPassed());

        columns.add(RF_FAILED);
        values.add(caseResult.getFailed());

        columns.add(RF_PASSED);
        values.add(caseResult.getPassed());

        columns.add(RF_DURATION);
        values.add(caseResult.getDuration());

        Serie.Builder builder = new Serie.Builder(getCaseSerieName(caseResult));

        for(String tag : caseResult.getTags()) {
            markTagResult(tag, caseResult);
        }

        return builder.columns(columns.toArray(new String[columns.size()])).values(values.toArray()).build();
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
        List<String> columns = new ArrayList<String>();
        List<Object> values = new ArrayList<Object>();

        columns.add(RF_CRITICAL_FAILED);
        values.add(tagResult.criticalFailed);

        columns.add(RF_CRITICAL_PASSED);
        values.add(tagResult.criticalPassed);

        columns.add(RF_CRITICAL_TOTAL);
        values.add(tagResult.criticalPassed + tagResult.criticalFailed);

        columns.add(RF_FAILED);
        values.add(tagResult.failed);

        columns.add(RF_PASSED);
        values.add(tagResult.passed);

        columns.add(RF_TOTAL);
        values.add(tagResult.passed + tagResult.failed);

        columns.add(RF_DURATION);
        values.add(tagResult.duration);

        Serie.Builder builder = new Serie.Builder(getTagSerieName(tagResult));

        return builder.columns(columns.toArray(new String[columns.size()])).values(values.toArray()).build();
    }

    private Point generateSuiteSerie(RobotSuiteResult suiteResult) {
        List<String> columns = new ArrayList<String>();
        List<Object> values = new ArrayList<Object>();

        addJenkinsBuildNumber(build, columns, values);
        addJenkinsProjectName(build, columns, values);

        columns.add(RF_TESTCASES);
        values.add(suiteResult.getAllCases().size());

        columns.add(RF_CRITICAL_FAILED);
        values.add(suiteResult.getCriticalFailed());

        columns.add(RF_CRITICAL_PASSED);
        values.add(suiteResult.getCriticalPassed());

        columns.add(RF_CRITICAL_TOTAL);
        values.add(suiteResult.getCriticalTotal());

        columns.add(RF_FAILED);
        values.add(suiteResult.getFailed());

        columns.add(RF_PASSED);
        values.add(suiteResult.getPassed());

        columns.add(RF_TOTAL);
        values.add(suiteResult.getTotal());

        columns.add(RF_DURATION);
        values.add(suiteResult.getDuration());

        Serie.Builder builder = new Serie.Builder(getSuiteSerieName(suiteResult));
        return builder.columns(columns.toArray(new String[columns.size()])).values(values.toArray()).build();
    }



    private String getTagSerieName(RobotTagResult tagResult) {
        return getTagSeriePrefix()+"."+tagResult.name;
    }

    private String getCaseSerieName(RobotCaseResult caseResult) {
        return getCaseSeriePrefix()+"."+caseResult.getDuplicateSafeName();
    }

    private String getCaseSeriePrefix() {
        return getSeriePrefix()+".testcase";
    }

    private String getSuiteSerieName(RobotSuiteResult suiteResult) {
        return getSuiteSeriePrefix()+"."+suiteResult.getDuplicateSafeName();
    }

    private String getTagSeriePrefix() {
        return getSeriePrefix()+".tag";
    }

    private String getSuiteSeriePrefix() {
        return getSeriePrefix()+".suite";
    }
    */

    private String getSeriePrefix() {
        return getBaseSerieName() + "rf";
    }
}
