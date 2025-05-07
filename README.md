# JavaFX System Monitor
A comprehensive desktop system monitoring application built with JavaFX and the [OSHI](https://github.com/oshi/oshi) (Operating System & Hardware Information) library. 

## Features
1. Real-time hardware monitoring
    * CPU usage
    * Memory usage
    * CPU temperature
    * Fan speeds and CPU voltage (when available)
2. Disk info
    * Storage device details
    * Used / free space monitoring
    * Usage percentage visualization
3. Process management
    * List of running processes
    * Process CPU and memory usage
    * Process details (PID, priority, threads, start time)
    * Sorting by CPU usage
4. System info
    * OS details and version
    * System architecture
    * Boot time and uptime
    * User sessions
5. USB device monitoring
    * Connected USB devices
    * Device details (name, product ID, serial number, vendor)
6. Installed apps:
    * List of installed software
    * App details (name, install time, vendor, version)

## Technology stack
- Java 
- JavaFX
- OSHI
- CSS

## Requirements
- Java Development Kit (JDK) 11 or higher
- JavaFX SDK 13 or higher
- OSHI library (and its dependencies)

## Installation
1. Clone this repo <br>
`git clone https://github.com/Jacky-lim-data-analyst/devicemanage.git`
2. Navigate to the project directory <br>
`cd devicemanage`
3. Build the project with Maven: <br>
`mvn clean install`
4. Run the application: <br>
`mvn javafx:run`

## License
[MIT LICENSE](./LICENSE)