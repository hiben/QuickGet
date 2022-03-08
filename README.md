# QuickGet
Quick Getting of Files

Small application that serves a single file over HTTP and creates
a QR code to scan.

Made to quickly transfer files to Smartphones in the same network.

## Running

This application has a GUI but can use following settings from 
VM arguments (*-Doption=value*) or environment variables.

All arguments are optional, e.g. you can run the Jar file directly without a terminal.

- *url / QG_URL*: Base URL in generated code
- *port / QG_PORT*: HTTP server port
- *file / QG_FILE*: File to serve
- *name / QG_NAME*: Name to serve file as
- *mime / QG_MIME*: MIME for content
- *extraMimes / QG_EXTRA_MIMES*: Comma separated list of MIME types
- *start / QG_START* : Start server if set to '*true*'

### Example
```
java -jar quickget.jar -Durl=http://192.168.111.12 -Dfile=/home/test/myimage.gif -Dname=image.gif -Dmime=image/gif -Dstart=true
```

All settings can be changed in the GUI while serving the content (but port changes will restart the server).

## Build

This application uses gradle for building and the shadow jar plugin
to create a runnable jar file.

```
gradlew shadowJar
```

The runnable jar file can be found at **build/libs/quickget.jar**

Uses QR code generation from:

```
 QR Code generator library (Java)

 Copyright (c) Project Nayuki. (MIT License)
 https://www.nayuki.io/page/qr-code-generator-library
```