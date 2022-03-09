# QuickGet
Quick Getting of Files

Small application that serves a single file over HTTP and creates
a QR code to scan the resulting URL for download.

Made to quickly transfer files to Smartphones in the same network without
having to type in the URL manually.

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

### Interface
The interface is divided into settings, the QR code and a simple log.

```
-QuickGet---------------------X-
|External URL [http://myhost  ]
|(Start)  Port [80   ]
|(Select File) <filename.ext>
|         Name [served_name]
|         MIME [image/png]
-Code---------------------------
|         ##_##_###_##
|         #_#QRCode###
|         #__###_##__#|
-^_-----------------------------
| Log
--------------------------------
| Status
--------------------------------
```

Whatever you enter into the URL field will be used
as the start of the QR code content.

*Typically you would set this field to point to the IP address or DNS name
of the machine you are serving the file from.*

The port setting defined the local port where the HTTP server
will listen for requests.

*The server can only be started once a file has been selected.
After starting, the button will turn into a 'Stop' button to 
shut down the server.*

A file is selected via the 'Select File' button. This opens a
standard file chooser.

*Once a file is selected, the name field is populated with the file name.
This value can be changed to make the content available under a different name.*

Select a MIME type via the drop-down box. You can also set a custom MIME type here.

**Note:** The MIME-type will determine how the browser/Smartphone will handle the content.
Select *'application/octet-stream'* for generic downloads.

The code section will automatically update to reflect your changes and will also
change size to fill the available space.

You can change all settings except port without restarting the server.

The content of the code will be the value of the external URL field
plus a '/' (if not present) followed by the served name, e.g. if you set 
external URL to 'http://example' and the name to 'myimage.png' the
code will read 'http://example/myimage.png'.

The log section (can be changed in size) shows some general information.
On application start it will also show a list of all know IP addresses for
the machine it is running on (IPv4 and IPv6).

The status line shows the last entry of the log.

## Building

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