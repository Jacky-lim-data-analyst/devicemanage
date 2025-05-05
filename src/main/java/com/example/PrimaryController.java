package com.example;

import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;
// oshi imports
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Sensors;
import oshi.hardware.UsbDevice;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OperatingSystem.OSVersionInfo;
import oshi.util.FormatUtil;

public class PrimaryController implements Initializable{
    // FXML UI components (CPU & memory)
    @FXML
    private ProgressBar cpuProgressBar, memoryProgressBar;
    @FXML
    private Label cpuLabel, memoryLabel, cpuTemperatureLabel, fanSpeedLabel, cpuVoltageLabel;
    // FXML UI components (disk info)
    @FXML
    private TableView<DiskInfo> diskTableView;
    @FXML
    private TableColumn<DiskInfo, String> nameColumn, totalColumn, usedColumn, freeColumn, usageColumn;

    // FXML UI components (usb devices info)
    @FXML
    private TableView<usbDeviceInfo> usbTableView;
    @FXML
    private TableColumn<usbDeviceInfo, String> usbNamesColumn, usbProductIDColumn, usbSerialNumberColumn, usbVendorColumn;
    @FXML
    private TextArea systemInfoTextArea;

    // new FXML components for processes
    @FXML
    private TableView<ProcessInfo> processesTableView;
    @FXML
    private TableColumn<ProcessInfo, Integer> processIDColumn, processPriorityColumn, numThreadsColumn;
    @FXML 
    private TableColumn<ProcessInfo, Double> cpuUsageColumn, processMemoryColumn;
    @FXML
    private TableColumn<ProcessInfo, String> processNamesColumn, pathsColumn, startTimeColumn;
    @FXML
    private Label numProcessesLabel;

    // system monitoring instances (OSHI)
    private SystemInfo si;
    private HardwareAbstractionLayer hal;   
    private CentralProcessor processor;
    private GlobalMemory memory;
    private OperatingSystem os;

    // helper for formatting numbers
    private final DecimalFormat df = new DecimalFormat("0.0");
    // observable list for disk data in the TableView
    private final ObservableList<DiskInfo> diskData = FXCollections.observableArrayList();
    // observable list for usb devices in the TableView
    private final ObservableList<usbDeviceInfo> usbDeviceData = FXCollections.observableArrayList();
    // observable list for processes in the TableView
    private final ObservableList<ProcessInfo> processData = FXCollections.observableArrayList();
    // Timeline for periodic update
    private Timeline updateTimeline;
    // CPU Load calculation
    private long[] prevTicks;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // initialize system monitoring components from OSHI
        si = new SystemInfo();
        hal = si.getHardware();
        processor = hal.getProcessor();
        memory = hal.getMemory();
        os = si.getOperatingSystem();
        // initialize CPU ticks array
        prevTicks = processor.getSystemCpuLoadTicks();

        // setup disk table view
        initializeDiskTableView();

        // setup usb devices table view 
        initializeUsbTableView();

        // setup processes table view
        initializeProcessTableView();

        // initialize UI
        initializeUI();

        // setup periodic updates (1 s)
        updateCpuUsage();
        updateMemoryUsage();
        updateDiskInfo();
        updateSystemInfo();
        updateProcessInfo();

        // setup usb devices and sensors
        updateUSBInfo();
        updateSensorsData();
        // updates in a constant intervals
        setupPeriodicUpdates();
    }

    /**
     * Initializes progress bars and labels to their default states
     */
    private void initializeUI() {
        // initializes progress bars with minimum values
        cpuProgressBar.setProgress(0);
        memoryProgressBar.setProgress(0);

        // labels initialization
        cpuLabel.setText("0.0%");
        memoryLabel.setText("0.0%");
        numProcessesLabel.setText("0");
    }

    /**
     * Configures the tableview columns to bind to DiskInfo properties
     */
    private void initializeDiskTableView() {
        // setup table columns
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
        usedColumn.setCellValueFactory(new PropertyValueFactory<>("used"));
        freeColumn.setCellValueFactory(new PropertyValueFactory<>("free"));
        usageColumn.setCellValueFactory(new PropertyValueFactory<>("usage"));

        // set data source for table 
        diskTableView.setItems(diskData);
    }

    /**
     * Configures the tableview columns to bind to usbDeviceInfo properties
     */
    private void initializeUsbTableView() {
        // setup usb devices table columns
        usbNamesColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        usbProductIDColumn.setCellValueFactory(new PropertyValueFactory<>("productID"));
        usbSerialNumberColumn.setCellValueFactory(new PropertyValueFactory<>("serialNumber"));
        usbVendorColumn.setCellValueFactory(new PropertyValueFactory<>("vendor"));

        usbTableView.setItems(usbDeviceData);
    }

    /**
     * Configures the tableview columns to bind to ProcessInfo properties
     */
    private void initializeProcessTableView() {
        // setup process table columns
        processIDColumn.setCellValueFactory(new PropertyValueFactory<>("processId"));
        processNamesColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        pathsColumn.setCellValueFactory(new PropertyValueFactory<>("path"));
        processPriorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        cpuUsageColumn.setCellValueFactory(new PropertyValueFactory<>("cpuUsageFormatted"));
        startTimeColumn.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        numThreadsColumn.setCellValueFactory(new PropertyValueFactory<>("threadCount"));
        processMemoryColumn.setCellValueFactory(new PropertyValueFactory<>("memorySizeFormatted"));

        // make columns resizable
        processesTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // set data source for table
        processesTableView.setItems(processData);
    }

    /**
     * Setup and starts a TimeLine to periodically call the update methods
     */
    private void setupPeriodicUpdates() {
        updateTimeline = new Timeline(
            // for 3 updates: CPU, memory, and disk info every second
            new KeyFrame(Duration.seconds(1), event -> {
                updateCpuUsage();
                updateMemoryUsage();
                updateDiskInfo();
                updateUSBInfo();
                updateProcessInfo();
            })
        );

        updateTimeline.setCycleCount(Timeline.INDEFINITE);
        updateTimeline.play();
    }

    /**
     * Updates the CPU usage progress bar and label
     * Uses OSHI to get the system CPU load between ticks
     */
    private void updateCpuUsage() {
        // calculate CPU load percentage between previous and current tick counts
        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;

        // update the tick counts for the next calculation
        prevTicks = processor.getSystemCpuLoadTicks();

        // ensure UI updates run on the JavaFx application thread
        cpuProgressBar.setProgress(cpuLoad / 100.0);
        cpuLabel.setText(df.format(cpuLoad) + "%");
    }

    /**
     * Updates the memory usage progress bar and label
     * Uses OSHI to get total and available memory
     */
    private void updateMemoryUsage() {
        long totalMemory = memory.getTotal();
        long availableMemory = memory.getAvailable();
        // used memory: total - available
        long usedMemory = totalMemory - availableMemory;
        // calculate memory usage percentage
        double memoryUsagePercent = (totalMemory == 0) ? 0 : (double) usedMemory / totalMemory * 100;

        memoryProgressBar.setProgress((totalMemory == 0) ? 0 : (double) usedMemory / totalMemory);
        memoryLabel.setText(
            FormatUtil.formatBytes(usedMemory) + " / " +
            FormatUtil.formatBytes(totalMemory) + 
            " (" + df.format(memoryUsagePercent) + "%)"
        );
    }

    /**
     * Updates disk info in the TableView. Uses OSHI to get file store details
     */
    private void updateDiskInfo() {
        // get file system object
        FileSystem fileSystem = os.getFileSystem();
        // get a list of all file stores (disks / partitions)
        List<OSFileStore> fileStores = fileSystem.getFileStores();

        // clear existing data
        diskData.clear();

        // iterate over each file store
        for (OSFileStore fs : fileStores) {
            long totalSpace = fs.getTotalSpace();
            // long usableSpace = fs.getUsableSpace();   // can be used by this user
            long freeSpace = fs.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;

            // usage percentage
            double usagePercent = (totalSpace == 0) ? 0 : (double) usedSpace / totalSpace * 100;

            // create a diskinfo object with formatted strings
            DiskInfo info = new DiskInfo(
                fs.getName(), 
                FormatUtil.formatBytes(totalSpace), 
                FormatUtil.formatBytes(usedSpace), 
                FormatUtil.formatBytes(freeSpace), 
                df.format(usagePercent) + "%"
            );

            diskData.add(info);
        }
    }

    /**
     * Update USB devices in the Table View. Uses OSHI to get USB device details
     */
    private void updateUSBInfo() {
        // get a list of connected usb devices
        List<UsbDevice> usbDevices = hal.getUsbDevices(true);

        // clear the existing data before adding updated info
        usbDeviceData.clear();

        // iterate over each USB device
        for (UsbDevice usb : usbDevices) {
            // create a new usbdevice info
            usbDeviceInfo info = new usbDeviceInfo(
                usb.getName(), 
                usb.getProductId(), 
                usb.getSerialNumber(), 
                usb.getVendor());

            // add the new info object into the observable list
            usbDeviceData.add(info);
        }
    }

    /**
     * Updates the sensor data labels (CPU temperature, fan speed, CPU voltage)
     * Sensor availability accuracy depends on OS, hardware, drivers and permissions
     */
    private void updateSensorsData() {
        // Get the sensors object
        Sensors sensors = hal.getSensors();

        // ---- Update CPU temperature ---
        double temp = sensors.getCpuTemperature();
        // check if valid temperature (based on the API docs)
        if (temp > 0.0 && !Double.isNaN(temp)) {
            cpuTemperatureLabel.setText(df.format(temp) + " °C");
        } else {
            cpuTemperatureLabel.setText("N/A");
        }

        // ---- update fan speeds ----
        int[] fanSpeeds = sensors.getFanSpeeds();
        // check if fan speed data is available
        if (fanSpeeds != null && fanSpeeds.length > 0) {
            // display all fans
            StringBuilder sb = new StringBuilder();
            for (int speed : fanSpeeds) {
                sb.append(speed).append(" RPM  ");
            }
            fanSpeedLabel.setText(sb.toString().trim());
        } else {
            fanSpeedLabel.setText("N/A");
        }

        // --- update CPU voltages
        double voltage = sensors.getCpuVoltage();
        if (voltage > 0.0) {
            cpuVoltageLabel.setText(df.format(voltage) + "V");
        } else {
            cpuVoltageLabel.setText("N/A");
        }
    }

    /**
     * Updates process information in the TableView. Uses OSHI to get process details.
     * Running processes are shown at the top
     */
    private void updateProcessInfo() {
        // get current processes
        List<OSProcess> processes = os.getProcesses();

        // create a temporary list to store process info
        List<ProcessInfo> updatedProcesses = new ArrayList<>();

        // process count
        int runningCount = 0;

        // Date format
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // get number of logical processors for CPU load calculation
        int logicalProcessorCount = processor.getLogicalProcessorCount();
        if (logicalProcessorCount <= 0) {
            logicalProcessorCount = 1;
        }

        for (OSProcess process : processes) {
            // skip processes that can't be properly accessed
            if (process.getName().isEmpty()) {
                continue;
            }

            // CPU usage (Calculate CPU usage per logical processor and convert to percent)
            // sum ticks across cores, divided by #cores
            double cpuLoadTotal = process.getProcessCpuLoadBetweenTicks(process);
            double cpuUsagePercent = (cpuLoadTotal / logicalProcessorCount) * 100.0;
            // String cpuUsageString = df.format(cpuUsage);

            // start time
            String startTime = dateFormat.format(new Date(process.getStartTime()));

            // priority
            int priority = process.getPriority();

            // virtual memory size (MB)
            double memorySizeMB = process.getVirtualSize() / (1024.0 * 1024.0);

            // create process info object
            ProcessInfo info = new ProcessInfo(
                process.getProcessID(), 
                process.getName(), 
                process.getPath(), 
                priority, 
                cpuUsagePercent, 
                startTime, 
                process.getThreadCount(), 
                memorySizeMB
            );

            // count running processes (those using CPU)
            if (process.getState() == OSProcess.State.RUNNING || cpuUsagePercent > 1.0) {
                runningCount++;
            }

            updatedProcesses.add(info);
        }

        // sort processes - running process first
        updatedProcesses.sort(Comparator.<ProcessInfo, Double>comparing(p -> p.getCpuUsage(), Comparator.reverseOrder())
            .thenComparing(ProcessInfo::getName));

        // update UI with new process data
        processData.clear();
        processData.addAll(updatedProcesses);

        // update running process count
        numProcessesLabel.setText(String.valueOf(runningCount));
    }

    /**
     * Updates system information text area with OS details and runtime information
     * Uses OSHI to gather system details
     */
    private void updateSystemInfo() {
        StringBuilder sb = new StringBuilder();

        // 1. OS version info
        OSVersionInfo versionInfo = os.getVersionInfo();
        sb.append("Operating System: ").append(os.toString()).append("\n");
        sb.append("Version: ").append(versionInfo.getVersion()).append("\n");
        sb.append("Build number: ").append(versionInfo.getBuildNumber()).append("\n\n");

        // 2. Bitness (32-bit or 64-bit)
        sb.append("Architecture: ").append((processor.getProcessorIdentifier().isCpu64bit()) ? "64-bit\n\n" : "32-bit\n\n");

        // 3. Number of physical cores
        sb.append("Number of physical cores: ").append(processor.getPhysicalProcessorCount()).append("\n\n");

        // 4. OS family
        sb.append("OS Family: ").append(os.getFamily()).append("\n");

        // 5. OS manufacturer
        sb.append("Manufacturer: ").append(os.getManufacturer()).append("\n\n");

        // 6. Currently logged in user
        sb.append("Current users:\n");
        os.getSessions().forEach(user -> sb.append("- ").append(user.getUserName()).append(" (Login: ").append(
            user.getLoginTime()
        ).append(")\n"));
        sb.append("\n");

        // 7. Unix Time of Boot converts to current date time
        long bootTime = os.getSystemBootTime();
        Date bootDate = new Date(bootTime * 1000L);   // convert to ms
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sb.append("System Boot Time: ").append(dateFormat.format(bootDate)).append("\n\n");

        // 8. Time since boot
        long upTime = os.getSystemUptime();
        java.time.Duration uptimeDuration = java.time.Duration.ofSeconds(upTime);
        long hours = uptimeDuration.toHours();
        long minutes = uptimeDuration.toMinutesPart();
        // long minutes = uptimeDuration.toMinutes() % 60;
        long seconds = uptimeDuration.toSecondsPart();
        // long seconds = uptimeDuration.getSeconds() % 60;
        sb.append(String.format("System Uptime: %02d:%02d:%02d\n\n", hours, minutes, seconds));

        // 9. Number of threads running
        sb.append("Running threads: ").append(os.getThreadCount()).append("\n");

        // update text area
        systemInfoTextArea.setText(sb.toString());
    }

    // disk info data class
    public static class DiskInfo {
        private final String name;
        private final String total;
        private final String used;
        private final String free;
        private final String usage;

        public DiskInfo(String name, String total, String used, String free, String usage) {
            this.name = name;
            this.total = total;
            this.used = used;
            this.free = free;
            this.usage = usage;
        }

        // getters
        public String getName() { return name; }
        public String getTotal() { return total; }
        public String getUsed() { return used; }
        public String getFree() { return free; }
        public String getUsage() { return usage; }
    }

    // usb devices info data class
    public static class usbDeviceInfo {
        private final String name;
        private final String productID;
        private final String serialNumber;
        private final String vendor;

        public usbDeviceInfo(String name, String productID, String serialNumber, String vendor) {
            this.name = name;
            this.productID = productID;
            this.serialNumber = serialNumber;
            this.vendor = vendor;
        }

        // getters
        public String getName() { return name; }
        public String getProductID() { return productID; }
        public String getSerialNumber() { return serialNumber; }
        public String getVendor() { return vendor; }
    }

    // process info data class
    public static class ProcessInfo {
        private final int processId;
        private final String name;
        private final String path;
        private final int priority;
        private final double cpuUsage;   // raw percent
        private final String startTime;
        private final int threadCount;
        private final double memorySize;   // memory size in MB

        public ProcessInfo(int processId, String name, String path, int priority,
            double cpuUsage, String startTime, int threadCount, double memorySize) {
                this.processId = processId;
                this.name = name;
                this.path = path;
                this.priority = priority;
                this.cpuUsage = cpuUsage;
                this.startTime = startTime;
                this.threadCount = threadCount;
                this.memorySize = memorySize;
            }

        // getters
        public int getProcessId() { return processId; }
        public String getName() { return name; }
        public String getPath() { return path; }
        public int getPriority() { return priority; }
        public double getCpuUsage() { return cpuUsage; }
        public String getStartTime() { return startTime; }
        public int getThreadCount() { return threadCount; }
        public double getMemorySize() { return memorySize; }

        // formatted getters for display
        public String getCpuUsageFormatted() { return String.format("%.2f%%", cpuUsage); }
        public String getMemorySizeFormatted() { return String.format("%.1f", memorySize); }
    }
}
