# JustDSD

DSD audio format decoder and PCM encoder.
It supports .DSF (Sony) and .DIFF (Philips), and SACD containers including a DST decompressor.
ID3V2 metadata support provided by <a href="https://github.com/drogatkin/tiny-codec/tree/master/tools/ID3V2">tiny-codec</a>

There is Android client for extracting dff <a href="https://play.google.com/store/apps/details?id=rogatkin.mobile.app.dsdboss" target="_blank">DSD Boss</a> 

## Build and run

Make sure you have 7Bee installed, visit [7Bee](https://github.com/drogatkin/7Bee). Type:
```
bee
```

in the project root directory and make sure that the build runs and you get welcome message as:
```
dmitriy@dmitriy-desktop:~/projects/JustDSD$ bee
Compiling...
warning: [options] bootstrap class path not set in conjunction with -source 7
Note: Some input files use or override a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
Note: /home/dmitriy/projects/JustDSD/src/java/org/justcodecs/dsd/Utils.java uses unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
1 warning
Jarring...
WARNING: Illegal reflective access by org.bee.processor.Task (file:/usr/local/bee/lib/bee.jar) to method sun.tools.jar.Main.main(java.lang.String[])
Running...
Java DSD player for PCM DAC  (c) 2015-2022 D. Rogatkin
Please use with at least one .dsf, .dff, or [SACD].iso file argument
```
The program uses one dependency: [ID3V2](https://github.com/drogatkin/tiny-codec/tree/master/tools/ID3V2),
you can get pre-built version of the jar from the release page of the tiny-codec project.
Modify [env.xml](https://github.com/drogatkin/JustDSD/blob/4a597ecacdb69813c3131dce9a64b5947a90e749/env.xml#L45) to
specify a correct jar location.

If you plan to use DST decompressor, then use bee target **dff**
```
bee dff
```
For example:
```
james@proba-desktop:~/projects/JustDSD$ bee dff -- -3 "/media/dsd/Music/Robin Trower - Bridge Of Sighs (1974, RE2014, Germany, DST128)/A1 - Day Of The Eagle.dff"
```
If you plan to ship the tool to somebody and do not want to deal with dependencies, then **jdsd-nodep.jar** gets generated in lib directory.
Use:
```
java -jar jdsd-nodep.jar ..parameters
```

## Acknowledgment
Special thanks to Peter McQuillan without his valuable information and testing efforts this project wasn't possible
