package org.jenkinsci.plugins.influxdb.generators;

import hudson.model.AbstractBuild;
import net.sourceforge.cobertura.coveragedata.ClassData;
import net.sourceforge.cobertura.coveragedata.CoverageDataFileHandler;
import net.sourceforge.cobertura.coveragedata.PackageData;
import net.sourceforge.cobertura.coveragedata.ProjectData;
import org.influxdb.dto.Point;
import hudson.plugins.cobertura.Ratio;
import hudson.plugins.cobertura.targets.CoverageMetric;
import hudson.plugins.cobertura.targets.CoverageResult;
import hudson.plugins.cobertura.CoberturaBuildAction;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by jrajala on 15.5.2015.
 */

public class CoberturaSerieGenerator extends AbstractSerieGenerator {

    public static final String COBERTURA_PACKAGE_COVERAGE_RATE = "cobertura_package_coverage_rate";
    public static final String COBERTURA_NUMBER_OF_PACKAGES = "cobertura_number_of_packages";
    public static final String COBERTURA_CLASS_COVERAGE_RATE = "cobertura_class_coverage_rate";
    public static final String COBERTURA_NUMBER_OF_CLASSES = "cobertura_number_of_classes";
    public static final String COBERTURA_LINE_COVERAGE_RATE = "cobertura_line_coverage_rate";
    public static final String COBERTURA_NUMBER_OF_LINES = "cobertura_number_of_lines";
    public static final String COBERTURA_SOURCEFILE_COVERAGE_RATE = "cobertura_sourcefile_coverage_rate"; 
    public static final String COBERTURA_NUMBER_OF_SOURCEFILES = "cobertura_number_of_sourcefiles";
    public static final String COBERTURA_CONDITION_COVERAGE_RATE = "cobertura_condition_coverage_rate";
    public static final String COBERTURA_NUMBER_OF_CONDITIONS = "cobertura_number_of_conditions";
    public static final String COBERTURA_METHOD_COVERAGE_RATE = "cobertura_method_coverage_rate";
    public static final String COBERTURA_NUMBER_OF_METHODS = "cobertura_number_of_methods";
    public static final String COBERTURA_BRANCH_COVERAGE_RATE = "cobertura_branch_coverage_rate";

    private static final String COBERTURA_REPORT_FILE = "/target/cobertura/cobertura.ser";

    coberturaJenkinsPluginGenerator jenkinsGenerator = new coberturaJenkinsPluginGenerator();
    coberturaSerFileGenerator serFileGenerator = new coberturaSerFileGenerator();

    public CoberturaSerieGenerator(AbstractBuild<?, ?> build, PrintStream logger) {
        super(build, logger);
    }

    public boolean hasReport() {
        return jenkinsGenerator.hasReport() || serFileGenerator.hasReport();
    }

    public Point[] generate() {
        Point.Builder pointBuilder = Point.measurement(getSerieName())
            .time(build.getTimeInMillis(), TimeUnit.MILLISECONDS);

        logger.println("Influxdb starting to generate Cobertura report");

        addJenkinsBaseInfo(pointBuilder);

        if (jenkinsGenerator.hasReport()) {
            jenkinsGenerator.generate(pointBuilder);
        }
        else if (serFileGenerator.hasReport()) {
            serFileGenerator.generate(pointBuilder);
        }

        logger.println("Influxdb Cobertura report generation finished");

        return new Point[]{pointBuilder.build()};
    }

    private class coberturaJenkinsPluginGenerator {
        private CoverageResult coberturaCoverageResult = null;

        protected coberturaJenkinsPluginGenerator() {
            CoberturaBuildAction coberturaAction = build.getAction(CoberturaBuildAction.class);
            if (coberturaAction != null) {
                coberturaCoverageResult = coberturaAction.getResult();
            }
        }

        private boolean hasReport() {
            return coberturaCoverageResult != null &&
                coberturaCoverageResult.getCoverage(CoverageMetric.LINE) != null;
        }

        private void generate(Point.Builder pointBuilder) {
            logger.println("Influxdb Generating Cobertura report using cobertura plugin");

            pointBuilder.field(COBERTURA_PACKAGE_COVERAGE_RATE,
                               getCoveragePercentage(coberturaCoverageResult, CoverageMetric.PACKAGES));
            pointBuilder.field(COBERTURA_NUMBER_OF_PACKAGES,
                               getCoverageValue(coberturaCoverageResult, CoverageMetric.PACKAGES));

            pointBuilder.field(COBERTURA_CLASS_COVERAGE_RATE,
                               getCoveragePercentage(coberturaCoverageResult, CoverageMetric.CLASSES));
            pointBuilder.field(COBERTURA_NUMBER_OF_CLASSES,
                               getCoverageValue(coberturaCoverageResult, CoverageMetric.CLASSES));

            pointBuilder.field(COBERTURA_LINE_COVERAGE_RATE,
                               getCoveragePercentage(coberturaCoverageResult, CoverageMetric.LINE));
            pointBuilder.field(COBERTURA_NUMBER_OF_LINES,
                               getCoverageValue(coberturaCoverageResult, CoverageMetric.LINE));

            pointBuilder.field(COBERTURA_SOURCEFILE_COVERAGE_RATE,
                               getCoveragePercentage(coberturaCoverageResult, CoverageMetric.FILES));
            pointBuilder.field(COBERTURA_NUMBER_OF_SOURCEFILES,
                               getCoverageValue(coberturaCoverageResult, CoverageMetric.FILES));

            pointBuilder.field(COBERTURA_CONDITION_COVERAGE_RATE,
                               getCoveragePercentage(coberturaCoverageResult, CoverageMetric.CONDITIONAL));
            pointBuilder.field(COBERTURA_NUMBER_OF_CONDITIONS,
                               getCoverageValue(coberturaCoverageResult, CoverageMetric.CONDITIONAL));

            pointBuilder.field(COBERTURA_METHOD_COVERAGE_RATE,
                               getCoveragePercentage(coberturaCoverageResult, CoverageMetric.METHOD));
            pointBuilder.field(COBERTURA_NUMBER_OF_METHODS,
                               getCoverageValue(coberturaCoverageResult, CoverageMetric.METHOD));

        }

        private float getCoveragePercentage(CoverageResult result, CoverageMetric metric) {
            Ratio ratio = result.getCoverage(metric);
            if (ratio == null) {
                return 0.0f;
            }
            return ratio.getPercentageFloat();
        }

        private int getCoverageValue(CoverageResult result, CoverageMetric metric) {
            Ratio ratio = result.getCoverage(metric);
            if (ratio == null) {
                return 0;
            }
            return Math.round(ratio.denominator);
        }
    }

    private class coberturaSerFileGenerator {
        private File coberturaFile = null;

        protected coberturaSerFileGenerator() {
            coberturaFile = new File(build.getWorkspace() + COBERTURA_REPORT_FILE);
        }

        private boolean hasReport() {
            return (coberturaFile != null && coberturaFile.exists() && coberturaFile.canRead());
        }

        private void generate(Point.Builder pointBuilder) {
            ProjectData coberturaData = CoverageDataFileHandler.loadCoverageData(coberturaFile);

            logger.println("Influxdb Generating Cobertura report using cobertura file " +
                           build.getWorkspace() + COBERTURA_REPORT_FILE);

            pointBuilder.field(COBERTURA_NUMBER_OF_PACKAGES, coberturaData.getPackages().size());
            pointBuilder.field(COBERTURA_NUMBER_OF_SOURCEFILES, coberturaData.getNumberOfSourceFiles());
            pointBuilder.field(COBERTURA_NUMBER_OF_CLASSES, coberturaData.getNumberOfClasses());
            pointBuilder.field(COBERTURA_BRANCH_COVERAGE_RATE, coberturaData.getBranchCoverageRate()*100d);
            pointBuilder.field(COBERTURA_LINE_COVERAGE_RATE, coberturaData.getLineCoverageRate()*100d);

            addPackageCoverage(coberturaData, pointBuilder);
            addClassCoverage(coberturaData, pointBuilder);
        }

        private void addPackageCoverage(ProjectData coberturaData, Point.Builder pointBuilder) {
            double totalPacakges = coberturaData.getPackages().size();
            double packagesCovered = 0;
            for(Object nextPackage : coberturaData.getPackages()) {
                PackageData packageData = (PackageData) nextPackage;
                if(packageData.getLineCoverageRate() > 0)
                    packagesCovered++;
            }
            double packageCoverage = packagesCovered / totalPacakges;

            pointBuilder.field(COBERTURA_PACKAGE_COVERAGE_RATE, packageCoverage*100d);
        }

        private void addClassCoverage(ProjectData coberturaData, Point.Builder pointBuilder) {
            double totalClasses = coberturaData.getNumberOfClasses();
            double classesCovered = 0;
            for(Object nextClass : coberturaData.getClasses()) {
                ClassData classData = (ClassData) nextClass;
                if(classData.getLineCoverageRate() > 0)
                    classesCovered++;
            }
            double classCoverage = classesCovered / totalClasses;

            pointBuilder.field(COBERTURA_CLASS_COVERAGE_RATE, classCoverage*100d);
        }
    }

    private String getSerieName() {
        return build.getProject().getName()+".cobertura";
    }
}
