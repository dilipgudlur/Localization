CLASSPATH=$PWD/`find . -name Localization.jar`
echo CLASSPATH is $CLASSPATH
OPTS="-classpath $CLASSPATH"
PACKAGE=edu.cmu.pandaa

cd test/

# Format for all main options is:
#
# java ... (options) output_file input_file(s)
#

java $OPTS $PACKAGE.module.FeatureExtractTest impulses-1.txt sample_input-1.wav
java $OPTS $PACKAGE.module.FeatureExtractTest impulses-2.txt sample_input-2.wav
java $OPTS $PACKAGE.module.FeatureExtractTest impulses-3.txt sample_input-3.wav
java $OPTS $PACKAGE.module.TDOAImpulseCorrelationModule tdoa-12.txt impulses-1.txt impulses-2.txt
java $OPTS $PACKAGE.module.TDOAImpulseCorrelationModule tdoa-13.txt impulses-1.txt impulses-3.txt
java $OPTS $PACKAGE.module.TDOAImpulseCorrelationModule tdoa-23.txt impulses-2.txt impulses-3.txt
java $OPTS $PACKAGE.module.ProcessGeometryModule geometry.txt tdoa-12.txt tdoa-23.txt tdoa-13.txt
