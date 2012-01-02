if [ "$IMPULSE_ALGORITHM" == "" ]; then
  IMPULSE_ALGORITHM=2
fi

if [ "$TDOA_ALGORITHM" == "" ]; then
  TDOA_ALGORITHM=3
fi
TARGET_SAMPLES=50000
DISTANCE_SMOOTH=25
GRAPH=yes

JAR=Localization.jar
CLASSPATH=$PWD/`find . -name $JAR`

if [ ! -f $CLASSPATH ]; then
  echo Could not find $JAR
  exit
fi

CLASSPATH=$CLASSPATH:$PWD/lib/mdsj.jar
#echo CLASSPATH is $CLASSPATH
OPTS="-classpath $CLASSPATH" #use with Linux
#OPTS="-classpath `cygpath -wp $CLASSPATH`" #use with Cygwin ons Windows
PACKAGE=edu.cmu.pandaa

if [ "$1" == "nograph" ]; then
  GRAPH=no
  shift
fi

INPUT_SET=1m_triangle
if [ "$1" ]; then
  INPUT_SET=$1
fi

# Easier to do our work in the test sub-directory
rm -rf test/
mkdir -p test/
cd test/
INPUT=../audio_src/$INPUT_SET

FILESET="1 2 3 4 5 6 7 8 9"
# Format for all main options is:
#
# java ... (options) output_file input_file(s)
#

for file in $FILESET; do 
  if [ -f $INPUT-$file.wav ]; then
    #java $OPTS $PACKAGE.module.AudioSynchronizationModule sync-$file.wav $INPUT-$file.wav
    cp $INPUT-$file.wav sync-$file.wav
    if [ $IMPULSE_ALGORITHM == 1 ]; then
      java $OPTS $PACKAGE.module.ImpulseStreamModule impulse1-$file.txt sync-$file.wav
    elif [ $IMPULSE_ALGORITHM == 2 ]; then 
      java $OPTS $PACKAGE.module.FeatureStreamModule impulse2-$file.txt sync-$file.wav
    elif [ $IMPULSE_ALGORITHM == 3 ]; then 
      java $OPTS $PACKAGE.module.DbImpulseStreamModule impulse3-$file.txt sync-$file.wav
    fi
    ifile=impulse$IMPULSE_ALGORITHM-$file.txt
    size=`cat $ifile | wc -l`
    repeat=$(($TARGET_SAMPLES/$size+1))
    java $OPTS $PACKAGE.module.ConsolidateModule i-1-1-1-$repeat impulses-$file.txt $ifile
  fi
done

if [ $TDOA_ALGORITHM == 3 ]; then
 for a in $FILESET; do 
  for b in $FILESET; do 
   if [ -f impulses-$a.txt -a -f impulses-$b.txt -a $a -lt $b ]; then
     java $OPTS $PACKAGE.module.TDOACrossModule tdoa3-$a$b.raw impulses-$a.txt impulses-$b.txt 
   fi
  done
 done
fi

for a in $FILESET; do 
 for b in $FILESET; do 
  if [ -f impulses-$a.txt -a -f impulses-$b.txt -a $a -lt $b ]; then
   if [ $TDOA_ALGORITHM == 1 ]; then
     java $OPTS $PACKAGE.module.TDOACorrelationModule tdoa1-$a$b.txt impulses-$a.txt impulses-$b.txt 
   elif [ $TDOA_ALGORITHM == 2 ]; then
     java $OPTS $PACKAGE.module.TDOACrossModule tdoa2-$a$b.txt impulses-$a.txt impulses-$b.txt 
   elif [ $TDOA_ALGORITHM == 3 ]; then
     java $OPTS $PACKAGE.module.TDOACrossModule -c tdoa3-$a$b.txt impulses-$a.txt impulses-$b.txt 
   fi
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
java $OPTS $PACKAGE.module.RMSModule rmsOut.txt geometryOut.txt $INPUT.txt

inputs=""
for a in $FILESET; do 
 for b in $FILESET; do 
  if [ -f impulses-$a.txt -a -f impulses-$b.txt -a $a -lt $b ]; then
   java $OPTS $PACKAGE.module.DistanceFilter $DISTANCE_SMOOTH adjusted-$a$b.txt tdoa$TDOA_ALGORITHM-$a$b.txt geometryOut.txt
   inputs="$inputs adjusted-$a$b.txt"
  fi
 done
done

java $OPTS $PACKAGE.module.DistanceMatrixModule geometryAll2.txt $inputs
java $OPTS $PACKAGE.module.GeometryMatrixModule geometryOut2.txt geometryAll2.txt
java $OPTS $PACKAGE.module.RMSModule rmsOut2.txt geometryOut2.txt $INPUT.txt

if [ "$*" != "" -a "$GRAPH" == "yes" ]; then
  shift
  if [ "$*" != "" ]; then
    ../grid_multi.sh $INPUT_SET "$@"
  else
    cp rmsOut-actual/0000.txt actual.dat
    ../grid_anim.sh $INPUT_SET actual.dat
  fi
fi

rms1=`tail -1 rmsOut.txt`
rms2=`tail -1 rmsOut2.txt`
echo "$rms1 $rms2"

