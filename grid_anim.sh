AUDIO_SET=$1
TRUTH=$2
FDIR=geometryOut
if [ -d test ]; then
  cd test # in case we're running at the top level
fi

FILES="$FDIR/*.txt"
echo -n "Generating graphs: "
cp ../grid.plt .tmp.plt
pcmd="plot \"$AUDIO_SET.dat\" with linespoints"
shift
if [ "$TRUTH" ]; then
  pcmd="$pcmd,\"$TRUTH\" with linespoints"
fi

echo $pcmd >> .tmp.plt
echo Generating $pcmd to $AUDIO_SET.gif
for file in $FILES; do
  tail --lines +2 $file > $AUDIO_SET.dat
  (gnuplot < .tmp.plt 2>/dev/null && echo -n .) || echo -n X
  target=${file%.txt}.gif
  mv graph.gif $target
done    
echo
cp $target ../$AUDIO_SET.gif

animation=$AUDIO_SET-animation.gif
echo Generating $animation
gifsicle --delay 10 $FDIR/*.gif > ../$animation

gnome-open ../$animation