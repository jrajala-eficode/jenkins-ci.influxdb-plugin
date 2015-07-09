package org.jenkinsci.plugins.influxdb.generators;

import hudson.model.AbstractBuild;
import org.influxdb.dto.Point;

/**
 * Created by jrajala on 15.5.2015.
 */
public interface SerieGenerator {

    public boolean hasReport();

    public Point[] generate(String baseSerieName);

}
