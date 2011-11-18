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

AUDIO_SET=triangle_clap
if [ "$1" ]; then
  AUDIO_SET=$1
fi

# Easier to do our work in the test sub-directory
rm -rf test/
mkdir -p test/
cd test/
AUDIO=../audio_src/$AUDIO_SET

FILESET="1 2 3 4 5 6 7 8 9"
POWSET="3 4 5"
# Format for all main options is:
#
# java ... (options) output_file input_file(s)
#

for file in $FILESET; do 
  if [ -f $AUDIO-$file.wav ]; then
    for pow in $POWSET; do
      val=`echo 2^$pow | bc`
      java $OPTS $PACKAGE.stream.RawAudioFileStream -$val-1 ${AUDIO_SET}_$pow-$file.wav $AUDIO-$file.wav
    done
    java $OPTS $PACKAGE.module.ImpulseStreamModule impulses-$file.txt $AUDIO-$file.wav
  fi
done
for a in $FILESET; do 
 for b in $FILESET; do 
  if [ -f impulses-$a.txt -a -f impulses-$b.txt -a $a -lt $b ]; then
   java $OPTS $PACKAGE.module.TDOACorrelationModule distance-$a$b.txt impulses-$a.txt impulses-$b.txt 
   inputs="$inputs distance-$a$b.txt"
  fi
 done
done
if [ "$inputs" == "" ]; then
  echo no inputs genereated!
  exit
fi
java $OPTS $PACKAGE.module.DistanceMatrixModule geometryAll.txt $inputs
java $OPTS $PACKAGE.module.ConsolidateModule m-1-1-100-30 geometrySmooth.txt geometryAll.txt
java $OPTS $PACKAGE.module.GeometryMatrixModule geometryOut.txt geometrySmooth.txt

if [ "$*" != "" ]; then
  shift
  if [ "$*" != "" ]; then
    ../grid_multi.sh $AUDIO_SET "$@"
  else
    ../grid_anim.sh $AUDIO_SET
  fi
fi
