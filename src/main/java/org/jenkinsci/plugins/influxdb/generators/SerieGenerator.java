package org.jenkinsci.plugins.influxdb.generators;

import org.influxdb.dto.Point;

/**
 * Created by jrajala on 15.5.2015.
 */
public interface SerieGenerator {

    boolean hasReport();

    Point[] generate();

}
