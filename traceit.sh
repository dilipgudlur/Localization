CLASSPATH=Localization.jar
libs=$PWD/lib/*.jar
for lib in $libs; do
  CLASSPATH=$CLASSPATH:$lib
done

if [ "$*" == "" ]; then
  files=capture_*
else
  files="$@"
fi

for file in $files; do
  TARGET=trace-$file
  rm -rf trace $TARGET
  echo java -cp $CLASSPATH edu.cmu.pandaa.framework.App $file
  java -cp $CLASSPATH edu.cmu.pandaa.framework.App $file
  mv trace $TARGET
done

