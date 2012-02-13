if [ -d capture ]; then
  echo Capture directory already exists, either remove or rename before capuring again.
  exit
fi
mkdir -p capture
(cd capture; java -cp ../Localization.jar edu.cmu.pandaa.desktop.LiveAudioStream)
