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

java $OPTS $PACKAGE.stream.RawAudioFileStream segmented_audio-1.wav triangle_clap-01.wav
java $OPTS $PACKAGE.stream.RawAudioFileStream segmented_audio-2.wav triangle_clap-02.wav
java $OPTS $PACKAGE.stream.RawAudioFileStream segmented_audio-3.wav triangle_clap-03.wav
java $OPTS $PACKAGE.module.ImpulseStreamModule impulses-1.txt triangle_clap-01.wav
java $OPTS $PACKAGE.module.ImpulseStreamModule impulses-2.txt triangle_clap-02.wav
java $OPTS $PACKAGE.module.ImpulseStreamModule impulses-3.txt triangle_clap-03.wav
java $OPTS $PACKAGE.module.TDOACorrelationModule distance12.txt impulses-1.txt impulses-2.txt 
java $OPTS $PACKAGE.module.TDOACorrelationModule distance13.txt impulses-1.txt impulses-3.txt 
java $OPTS $PACKAGE.module.TDOACorrelationModule distance23.txt impulses-2.txt impulses-3.txt 
java $OPTS $PACKAGE.module.ConstructGeometryModule geometry123.txt distance12.txt distance13.txt distance23.txt
java $OPTS $PACKAGE.module.GeometryMatrixModule geometryOut.txt geometry123.txt
#java $OPTS $PACKAGE.module.ConsolidateModule i 1-1 impulses-c.txt impulses-1.txt 
#java $OPTS $PACKAGE.module.ConsolidateModule d 1-1 distance-c.txt distance-1.txt 

