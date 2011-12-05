PREFIX=audio_src/
POSTFIX=.txt
RESULTS=results.txt

date > $RESULTS

sets=`ls ${PREFIX}*${POSTFIX}`
for set in $sets; do
  setname=${set#${PREFIX}}
  setname=${setname%${POSTFIX}}
  ./test_set.sh nograph $setname
  result=`tail -1 test/RMSOut.txt`
  echo $set $result >> $RESULTS
done
echo
cat $RESULTS

