JAR=Localization.jar
CLASSPATH=$PWD/`find . -name $JAR`

if [ ! -f $CLASSPATH ]; then
  echo Could not find $JAR
  exit
fi

echo CLASSPATH is $CLASSPATH
OPTS="-classpath $CLASSPATH"
PACKAGE=edu.cmu.pandaa

cd test/

# Format for all main options is:
#
# java ... (options) output_file input_file(s)
#

java $OPTS $PACKAGE.module.ConsolidateModule 1-1 impulses-1.txt consolidated-1.txt

exit

java $OPTS $PACKAGE.module.FeatureExtractTest sample_input-1.wav impulses-1.txt
java $OPTS $PACKAGE.module.FeatureExtractTest sample_input-2.wav impulses-2.txt
java $OPTS $PACKAGE.module.FeatureExtractTest sample_input-3.wav impulses-3.txt
java $OPTS $PACKAGE.module.TDOAImpulseCorrelationModule impulses-1.txt impulses-2.txt tdoa-12.txt
java $OPTS $PACKAGE.module.TDOAImpulseCorrelationModule impulses-1.txt impulses-3.txt tdoa-13.txt
java $OPTS $PACKAGE.module.TDOAImpulseCorrelationModule impulses-2.txt impulses-3.txt tdoa-23.txt
java $OPTS $PACKAGE.module.ProcessGeometryModule geometryIn.txt geometryOut.txt

