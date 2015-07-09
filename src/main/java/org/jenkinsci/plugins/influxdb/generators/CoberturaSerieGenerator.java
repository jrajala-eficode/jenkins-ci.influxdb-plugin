package org.jenkinsci.plugins.influxdb.generators;

import hudson.model.AbstractBuild;
import net.sourceforge.cobertura.coveragedata.ClassData;
import net.sourceforge.cobertura.coveragedata.CoverageDataFileHandler;
import net.sourceforge.cobertura.coveragedata.PackageData;
import net.sourceforge.cobertura.coveragedata.ProjectData;
import org.influxdb.dto.Point;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by jrajala on 15.5.2015.
 */
public class CoberturaSerieGenerator extends AbstractSerieGenerator {

    public static final String COBERTURA_PACKAGE_COVERAGE_RATE = "cobertura_package_coverage_rate";
    public static final String COBERTURA_CLASS_COVERAGE_RATE = "cobertura_class_coverage_rate";
    public static final String COBERTURA_LINE_COVERAGE_RATE = "cobertura_line_coverage_rate";
    public static final String COBERTURA_BRANCH_COVERAGE_RATE = "cobertura_branch_coverage_rate";
    public static final String COBERTURA_NUMBER_OF_PACKAGES = "cobertura_number_of_packages";
    public static final String COBERTURA_NUMBER_OF_SOURCEFILES = "cobertura_number_of_sourcefiles";
    public static final String COBERTURA_NUMBER_OF_CLASSES = "cobertura_number_of_classes";
    private static final String COBERTURA_REPORT_FILE = "/target/cobertura/cobertura.ser";

//    private final AbstractBuild<?, ?> build;
    private ProjectData coberturaProjectData;
    private final File coberturaFile;

    public CoberturaSerieGenerator(AbstractBuild<?, ?> build) {
        this.build = build;
        coberturaFile = new File(build.getWorkspace() + COBERTURA_REPORT_FILE);
    }

    public boolean hasReport() {
        return (coberturaFile != null && coberturaFile.exists() && coberturaFile.canRead());
    }

    public Point[] generate(String baseSerieName) {
        this.baseSerieName = baseSerieName;
        coberturaProjectData = CoverageDataFileHandler.loadCoverageData(coberturaFile);

        Point.Builder builder = Point.measurement(getSerieName());

        Point point = builder
                .field(BUILD_DURATION,    build.getDuration())
                .field(BUILD_TIME, build.getTimestamp())
                .field(BUILD_NUMBER, build.getNumber())
                .field(COBERTURA_NUMBER_OF_PACKAGES, coberturaProjectData.getPackages().size())
                .field(COBERTURA_NUMBER_OF_SOURCEFILES, coberturaProjectData.getNumberOfSourceFiles())
                .field(COBERTURA_NUMBER_OF_CLASSES, coberturaProjectData.getNumberOfClasses())
                .field(COBERTURA_BRANCH_COVERAGE_RATE, coberturaProjectData.getBranchCoverageRate()*100d)
                .field(COBERTURA_LINE_COVERAGE_RATE, coberturaProjectData.getLineCoverageRate()*100d)
                .field(COBERTURA_PACKAGE_COVERAGE_RATE, calcPackageCoverageRate(coberturaProjectData))
                .field(COBERTURA_CLASS_COVERAGE_RATE, calcClassCoverageRate(coberturaProjectData))
                .tag("source","cobertura")
                .time(build.getStartTimeInMillis() + build.getDuration(), TimeUnit.MILLISECONDS)
                .build();

        return new Point[] {point};

    }

    private double calcPackageCoverageRate(ProjectData projectData) {
        double totalPacakges = projectData.getPackages().size();
        double packagesCovered = 0;
        for(Object nextPackage : projectData.getPackages()) {
            PackageData packageData = (PackageData) nextPackage;
            if(packageData.getLineCoverageRate() > 0)
                packagesCovered++;
        }
        double packageCoverage = packagesCovered / totalPacakges;
        return packageCoverage*100d;
    }

    private double calcClassCoverageRate(ProjectData projectData) {
        double totalClasses = projectData.getNumberOfClasses();
        double classesCovered = 0;
        for(Object nextClass : projectData.getClasses()) {
            ClassData classData = (ClassData) nextClass;
            if(classData.getLineCoverageRate() > 0)
                classesCovered++;
        }
        double classCoverage = classesCovered / totalClasses;

        return classCoverage*100d;
    }

    private String getSerieName() {
        return getBaseSerieName() +"cobertura";
    }


}
