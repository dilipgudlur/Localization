JAR=Localization.jar
CLASSPATH=$PWD/`find . -name $JAR`

if [ ! -f $CLASSPATH ]; then
  echo Could not find $JAR
  exit
fi

CLASSPATH=$CLASSPATH:$PWD/lib/mdsj.jar
echo CLASSPATH is $CLASSPATH
OPTS="-classpath $CLASSPATH" #use with Linux
#OPTS="-classpath `cygpath -wp $CLASSPATH`" #use with Cygwin ons Windows
PACKAGE=edu.cmu.pandaa

cd test/

# Format for all main options is:
#
# java ... (options) output_file input_file(s)
#

java $OPTS $PACKAGE.stream.RawAudioFileStream mangled_audio.wav sample_input-1.wav
java $OPTS $PACKAGE.module.ProcessGeometryModule geometryOut.txt geometryIn.txt
java $OPTS $PACKAGE.module.ConsolidateModule 1-1 consolidated-1.txt impulses-1.txt 
java $OPTS $PACKAGE.module.TDOACorrelationModule distances.txt impulses-1.txt impulses-2.txt 

exit

java $OPTS $PACKAGE.module.FeatureExtractTest impulses-1.txt sample_input-1.wav 
java $OPTS $PACKAGE.module.FeatureExtractTest impulses-2.txt sample_input-2.wav 
java $OPTS $PACKAGE.module.FeatureExtractTest impulses-3.txt sample_input-3.wav 
java $OPTS $PACKAGE.module.TDOAImpulseCorrelationModule tdoa-12.txt impulses-1.txt impulses-2.txt 
java $OPTS $PACKAGE.module.TDOAImpulseCorrelationModule tdoa-13.txt impulses-1.txt impulses-3.txt 
java $OPTS $PACKAGE.module.TDOAImpulseCorrelationModule tdoa-23.txt impulses-2.txt impulses-3.txt 


