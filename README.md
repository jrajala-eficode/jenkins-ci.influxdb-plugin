# InfluxDB Jenkins Plugin

This plugin allows you to send the build metrics to InfluxDB time series database, to be used for analysis and in radiators. Codebase is forked from Jenkings Graphite Plugin and refactored to suit purpose.

Has been tested with InfluxDB version v0.11 and v0.12. Does not support InfluxDB versions prior v0.9.

Supported metrics:
   - Jenkins base report
   - Cobertura code coverage metrics
   - Robot Framework plugin metrics

## Jenkins base report (*project_name*.jenkins)

Supported metrics:
   - project_name
   - build_number
   - build_duration
   - build_result
   - build_result_ordinal
   - build_status_message
   - project_build_stability
   - project_build_health
   - last_successful_build
   - last_stable_build
   - tests_failed
   - tests_skipped
   - tests_total

## Cobertura code coverage metrics (*project_name*.cobertura)

Requires Jenkins Cobertura plugin. Supported metrics:
   - project_name
   - build_number
   - cobertura_package_coverage_rate
   - cobertura_number_of_packages
   - cobertura_class_coverage_rate
   - cobertura_number_of_classes
   - cobertura_line_coverage_rate
   - cobertura_number_of_lines
   - cobertura_sourcefile_coverage_rate
   - cobertura_number_of_sourcefiles
   - cobertura_condition_coverage_rate
   - cobertura_number_of_conditions
   - cobertura_method_coverage_rate
   - cobertura_number_of_methods

## Robot Framework plugin metrics

Requires Jenkins Robot Framework plugin. Generates four different measurements. 

###Summary data (*project_name*.rf)
   - project_name
   - build_number
   - rf_duration
   - rf_passed
   - rf_failed
   - rf_total
   - rf_pass_percentage
   - rf_critical_passed
   - rf_critical_failed
   - rf_critical_total
   - rf_critical_pass_percentage
   - rf_suites

###Tag (*project_name*.rf.tag)

Per each tag
   - project_name
   - build_number
   - rf_tag
   - rf_duration
   - rf_passed
   - rf_failed
   - rf_total
   - rf_critical_passed
   - rf_critical_failed
   - rf_critical_total

###Suite (*project_name*.rf.suite)

Per each test suite
   - project_name
   - build_number
   - rf_suite
   - rf_duration
   - rf_passed
   - rf_failed
   - rf_total
   - rf_testcases
   - rf_critical_passed
   - rf_critical_failed
   - rf_critical_total

###Testcase (*project_name*.rf.testcase)

Per each test case
   - project_name
   - build_number
   - rf_case_name
   - rf_duration
   - rf_passed
   - rf_failed
   - rf_critical_passed
   - rf_critical_failed
   - rf_suite
   - rf_tag_list

##Future plans:
   - Getting plugin into Jenkins distribution
