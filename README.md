# QuickNII-service

This is the atlas slicer component residing next to QuickNII in its `Serv` folder. QuickNII will automatically launch it using the Java runtime it finds in its `Java` folder.

At the time of writing the exact command issued on Windows is

    Java\bin\java.exe -Xmx3G -cp Serv Serv something.pack
    
where `something.pack` is read from `pack.txt` or overridden by any command-line argument supplied to `QuickNII.exe`.

Initial communication is done via standard output, among diagnostic data, the service component produces the following lines:

    Catcher: 50570
    XPort: 50575
    YPort: 50576
    ZPort: 50577
    FPort: 50578
    Server ready.

These are ports where the service component waits for TCP connections on the loopback interface.

* `Catcher` starts waiting first, and has a timeout of 10 seconds. If the GUI fails to connect during that period, the service component assumes that QuickNII crashed or exited, and closes itself. Otherwise supplies atlas information for the GUI and then kept open for detecting when QuickNII terminates. The protocol is binary and will be documented later
* `X/Y/Z/FPort`: ports for producing X/Y/Z slices for navigation and the "free" slice for the main view. The protocol is binary and will be documented later

The service component itself reads and parses `cutlas` files briefly documented in `Serv.java`, something is available on NITRC: https://www.nitrc.org/plugins/mwiki/index.php/quicknii:Cutlas_file_format, and an example packer is provided in QuickNII-extras (https://github.com/Tevemadar/QuickNII-extras/blob/master/Java/PackWHSRatV2Demo.java)  
QuickNII itself connects to the catcher port as soon as it can (after reading the first line-break after the `Catcher:` line), and connection to the rest of the ports is triggered by reading the `Server ready.` string.

On Linux, the component works in a different way, as it is used for starting everything, and then uses 

    wine QuickNII.exe something.pack cport xport yport zport fport

(and there is even a code path for doing it without `wine`, so I can test it on Windows - it is triggered by supplying any second command-line argument for the service)

# Acknowledgements
QuickNII is developed by the Neural Systems Laboratory at the Institute of Basic Medical Sciences, University of Oslo, Norway. QuickNII was developed with support from the EBRAINS infrastructure, and funding from the European Union’s Horizon 2020 Framework Programme for Research and Innovation under the Framework Partnership Agreement No. 650003 (HBP FPA).

# Documentation
https://quicknii.readthedocs.io

# Developper
Gergely Csucs

# Authors
 Maja A Puchades, Jan G Bjaalie. 

# Licence
- Main component: Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International
- Source code: MIT licence

# Contact us
Report issues here on github or email: support@ebrains.eu

