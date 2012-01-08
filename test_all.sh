PREFIX=audio_src/
POSTFIX=.txt
RESULTS=results.txt

date >> $RESULTS

#   for cal in 0 1 2 3 4 5 6 7 8 9; do
    export CAL_FACTOR=$cal

sets=`ls ${PREFIX}*${POSTFIX}`
for set in $sets; do
 for impulse in 2; do
  for tdoa in 3; do
    setname=${set#${PREFIX}}
    setname=${setname%${POSTFIX}}
    export IMPULSE_ALGORITHM=$impulse
    export TDOA_ALGORITHM=$tdoa
    echo Beginning test for set $setname, IMPULSE=$impulse, TDOA=$tdoa, CAL=$cal
    ./test_set.sh nograph $setname
    result=`tail -1 test/rmsOut.txt`
    echo $setname/$impulse/$tdoa/$cal $result >> $RESULTS
  done
 done
done
echo
cat $RESULTS

