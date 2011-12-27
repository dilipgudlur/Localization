PREFIX=audio_src/
POSTFIX=.txt
RESULTS=results.txt

date > $RESULTS

sets=`ls ${PREFIX}*${POSTFIX}`
for set in $sets; do
 for impulse in 2; do
  for tdoa in 2 3; do
   setname=${set#${PREFIX}}
   setname=${setname%${POSTFIX}}
   export IMPULSE_ALGORITHM=$impulse
   export TDOA_ALGORITHM=$tdoa
   ./test_set.sh nograph $setname
   result=`tail -1 test/rmsOut.txt`
   echo $setname-$impulse-$tdoa $result >> $RESULTS
  done
 done
done
echo
cat $RESULTS

