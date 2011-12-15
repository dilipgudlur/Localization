Localization using PANDAA algorithm

Members: Trevor Pering, Fabian Popa, Dilip Gudlur, Divya Vavili, Qi Zheng

Basic usage:

./build
# and then:
./test_all.sh  
# or
./test_set.sh [setname]

Sample run as of 12/15/11:

peringknife@peringknife-glaptop:~/Localization$ ./build 
peringknife@peringknife-glaptop:~/Localization$ ./test_set.sh 1m_triangle
AudioSynchronization: sync-1.wav from ../audio_src/1m_triangle-1.wav
FeatureStream: impulse1-1.txt sync-1.wav
Consolidate i-1-1-1-23: impulses-1.txt impulse1-1.txt
AudioSynchronization: sync-2.wav from ../audio_src/1m_triangle-2.wav
FeatureStream: impulse1-2.txt sync-2.wav
Consolidate i-1-1-1-23: impulses-2.txt impulse1-2.txt
AudioSynchronization: sync-3.wav from ../audio_src/1m_triangle-3.wav
FeatureStream: impulse1-3.txt sync-3.wav
Consolidate i-1-1-1-23: impulses-3.txt impulse1-3.txt
TDOACorrelation: tdoa1-12.txt impulses-1.txt impulses-2.txt
GeometryMatrix: distance-12.txt tdoa1-12.txt
TDOACorrelation: tdoa1-13.txt impulses-1.txt impulses-3.txt
GeometryMatrix: distance-13.txt tdoa1-13.txt
TDOACorrelation: tdoa1-23.txt impulses-2.txt impulses-3.txt
GeometryMatrix: distance-23.txt tdoa1-23.txt
DistanceMatrix: geometryAll.txt distance-12.txt distance-13.txt distance-23.txt
GeometryMatrix: geometryOut.txt geometryAll.txt
RMS: rmsOut.txt geometryOut.txt ../audio_src/1m_triangle.txt
GeometryMatrix: adjusted-12.txt tdoa1-12.txt
GeometryMatrix: adjusted-13.txt tdoa1-13.txt
GeometryMatrix: adjusted-23.txt tdoa1-23.txt
DistanceMatrix: geometryAll2.txt adjusted-12.txt adjusted-13.txt adjusted-23.txt
GeometryMatrix: geometryOut2.txt geometryAll2.txt
RMS: rmsOut2.txt geometryOut2.txt ../audio_src/1m_triangle.txt
Generating graphs: Generating plot "1m_triangle.dat" with linespoints,"actual.dat" with linespoints to 1m_triangle.gif
X.......................................................................................................................................................................................
Generating 1m_triangle-animation.gif
gifsicle: geometryOut/0000.gif: empty file
183 16.881667892582918 0.0 183 14.280359972505387 0.0
peringknife@peringknife-glaptop:~/Localization$ 

