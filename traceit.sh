CLASSPATH=Localization.jar
CLASSPATH=$CLASSPATH:$PWD/lib/mdsj.jar

if [ "$*" == "" ]; then
  files=capture_*
else
  files="$@"
fi

for file in $files; do
  TARGET=trace-$file
  rm -rf trace $TARGET
  java -cp $CLASSPATH edu.cmu.pandaa.framework.App $file
  mv trace $TARGET
done

