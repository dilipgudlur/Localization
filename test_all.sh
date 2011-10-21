CLASSPATH=$PWD/`find . -name Localization.jar`
echo CLASSPATH is $CLASSPATH
OPTS="-classpath $CLASSPATH"
PACKAGE=edu.cmu.pandaa

cd test/
java $OPTS $PACKAGE.module.FeatureExtractTest sample_input1.wav impulses1.txt
java $OPTS $PACKAGE.module.FeatureExtractTest sample_input2.wav impulses2.txt
java $OPTS $PACKAGE.module.FeatureExtractTest sample_input3.wav impulses3.txt
java $OPTS $PACKAGE.module.TDOAImpulseCorrelationModule impulses1.txt impulses2.txt tdoa12.txt
java $OPTS $PACKAGE.module.TDOAImpulseCorrelationModule impulses2.txt impulses2.txt tdoa23.txt
java $OPTS $PACKAGE.module.TDOAImpulseCorrelationModule impulses1.txt impulses3.txt tdoa13.txt
java $OPTS $PACKAGE.module.ProcessGeometryModule tdoa12.txt tdoa23.txt tdoa13.txt geometry.txt
