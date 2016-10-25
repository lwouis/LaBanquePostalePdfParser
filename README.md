# Purpose

This is a Java app which takes PDF bank statements from [LaBanquePostale website](https://www.labanquepostale.fr/),
and consolidate all operations into a single CSV file.

It is a workaround for the feature available on their website which only let's you get CSV data for the current year.

# How to install

1. Download/clone the project.
2. Run `./gradlew shadowJar` to build `/build/libs/LaBanquePostalePdfParser-all.jar`

# How to use

1. Put all your PDF files in `/input/`.
2. Either run `./gradlew runShadow` to run this JAR using your local Java installation,
3. Or run `./gradlew launch4j` to build a Windows executable in `/build/launch4j/LaBanquePostalePdfParser.exe`, then 
run it.
4. Collect your result CSV file in `/output/`.
