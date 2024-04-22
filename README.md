An example of using offline instrumentation with Kover.

To generate an HTML report, need to run the `:app:koverHtmlReport` task.
This will cause the instrumentation of classes using Kover, the launch of connected tests, and the generation of HTML report.

First, task `jacocoDebug` is performed, instrumentation takes place using JaCoCo, but after that Kover replace these classes.
