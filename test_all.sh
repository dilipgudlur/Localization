
PREFIX=audio_src/
POSTFIX=.txt
sets=`ls ${PREFIX}*${POSTFIX}`
for set in $sets; do
  setname=${set#${PREFIX}}
  setname=${setname%${POSTFIX}}
  ./test_set.sh $setname
  result=
done
