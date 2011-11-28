IMPULSE_ALGORITHM=1
TDOA_ALGORITHM=1
REPEAT_TIMES=10
DISTANCE_SMOOTH=100

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

AUDIO_SET=1m_triangle
if [ "$1" ]; then
  AUDIO_SET=$1
fi

# Easier to do our work in the test sub-directory
rm -rf test/
mkdir -p test/
cd test/
AUDIO=../audio_src/$AUDIO_SET

FILESET="1 2 3 4 5 6 7 8 9"
# Format for all main options is:
#
# java ... (options) output_file input_file(s)
#

for file in $FILESET; do 
  if [ -f $AUDIO-$file.wav ]; then
    java $OPTS $PACKAGE.module.ImpulseStreamModule impulse1-$file.txt $AUDIO-$file.wav
    java $OPTS $PACKAGE.module.FeatureStreamModule impulse2-$file.txt $AUDIO-$file.wav
    java $OPTS $PACKAGE.module.ConsolidateModule i-1-1-1-$REPEAT_TIMES impulses-$file.txt impulse$IMPULSE_ALGORITHM-$file.txt
  fi
done
for a in $FILESET; do 
 for b in $FILESET; do 
  if [ -f impulses-$a.txt -a -f impulses-$b.txt -a $a -lt $b ]; then
   java $OPTS $PACKAGE.module.TDOACorrelationModule tdoa1-$a$b.txt impulses-$a.txt impulses-$b.txt 
   java $OPTS $PACKAGE.module.TDOACrossModule tdoa2-$a$b.txt impulses-$a.txt impulses-$b.txt 
   java $OPTS $PACKAGE.module.DistanceFilter $DISTANCE_SMOOTH distance-$a$b.txt tdoa$TDOA_ALGORITHM-$a$b.txt
   inputs="$inputs distance-$a$b.txt"
  fi
 done
done
if [ "$inputs" == "" ]; then
  echo no inputs genereated!
  exit
fi
java $OPTS $PACKAGE.module.DistanceMatrixModule geometryAll.txt $inputs
java $OPTS $PACKAGE.module.GeometryMatrixModule geometryOut.txt geometryAll.txt

if [ "$*" != "" ]; then
  shift
  if [ "$*" != "" ]; then
    ../grid_multi.sh $AUDIO_SET "$@"
  else
    ../grid_anim.sh $AUDIO_SET
  fi
fi
