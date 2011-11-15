AUDIO_SET=$1
if [ -d test ]; then
  cd test #in case we're at the top level
fi

echo Generating graph...
cp `ls geometryOut/*.txt | tail -1` ../$AUDIO_SET.dat
cp ../grid.plt .tmp.plt
cmd=""
for set in $@; do
  cmd="$cmd,\"$set.dat\""
  cp ../$set.dat .
done
pcmd="plot ${cmd#,}"
echo $pcmd >> .tmp.plt
echo Generating $pcmd to $AUDIO_SET.gif
gnuplot < .tmp.plt
mv graph.gif ../$AUDIO_SET.gif
gnome-open ../$AUDIO_SET.gif
