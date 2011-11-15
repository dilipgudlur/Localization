AUDIO_SET=$1
FDIR=geometryOut
if [ -d test ]; then
  cd test # in case we're running at the top level
fi

FILES="$FDIR/*.txt"
echo -n "Generating graphs: "
cp ../grid.plt .tmp.plt
cmd=""
for set in $@; do
  cmd="$cmd,\"$set.dat\""
  cp ../$set.dat .
done

pcmd="plot ${cmd#,}"
echo $pcmd >> .tmp.plt
echo Generating $pcmd to $AUDIO_SET.gif
for file in $FILES; do
  cp $file $AUDIO_SET.dat
  (gnuplot < .tmp.plt 2>/dev/null && echo -n .) || echo -n X
  target=${file%.txt}.gif
  mv graph.gif $target
done    
echo
cp $target ../$AUDIO_SET.gif
gnome-open ../$AUDIO_SET.gif
animation=$AUDIO_SET-animation.gif
echo Generating $animation
gifsicle --delay 10 $FDIR/*.gif > ../$animation
