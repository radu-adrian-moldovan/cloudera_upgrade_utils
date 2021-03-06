package com.streever.hive.perf;

import com.streever.hive.reporting.ReportingConf;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.TreeMap;

public class CollectStatistics extends Thread {

    private JDBCRecordIterator jri;
    private String header = null;

    private DateFormat dtf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private Long[] windows = {60000l, 180000l, 300000l, 600000l};
    private Map<Long, PerfWindow> perfWindows = new TreeMap<Long, PerfWindow>();

    public JDBCRecordIterator getJri() {
        return jri;
    }

    public Long[] getWindows() {
        return windows;
    }

    public void setWindows(Long[] windows) {
        this.windows = windows;
    }

    public String getHeader() {
        if (header == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("URL        : " + getJri().getJdbcUrl()).append("\n");
            sb.append("Batch Size : " + getJri().getBatchSize()).append("\n");
            sb.append("SQL        : " + getJri().getQuery()).append("\n");
            sb.append("Lite       : " + getJri().getLite());
            header = sb.toString();
        }
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public CollectStatistics(JDBCRecordIterator jri) {
        this.jri = jri;
    }

    private void setUpWindows() {
        for (Long window : windows) {
            PerfWindow perfWindow = new PerfWindow(window);
            perfWindows.put(window, perfWindow);
        }
    }

    private void addToPerfWindows(Statistic statistic) {
        for (Long window : windows) {
            PerfWindow pw = this.perfWindows.get(window);
            pw.pushStat(statistic);
        }
    }

    private void updateStatus() {
        addToPerfWindows(getJri().getStat());
    }

    public void printStatus() {
        StringBuilder sb = new StringBuilder();
        
        sb.append(ReportingConf.CLEAR_CONSOLE);
        sb.append(ReportingConf.ANSI_YELLOW + "========== v.${Implementation-Version} ===========" + ReportingConf.ANSI_RESET).append("\n");
        sb.append(ReportingConf.ANSI_WHITE + getHeader()).append("\n");
        sb.append(ReportingConf.ANSI_YELLOW + "----------------------------" + ReportingConf.ANSI_CYAN).append("\n");
        sb.append(getJri().getConnectionDetails().toString()).append("\n");
        sb.append(ReportingConf.ANSI_YELLOW + "----------------------------" + ReportingConf.ANSI_GREEN).append("\n");
        sb.append("Window Length(ms) | Record Average | Records per/sec | Data Size per/sec"+ReportingConf.ANSI_RESET).append("\n");
        for (Long window : windows) {
            PerfWindow pw = this.perfWindows.get(window);
            sb.append(window + "\t\t" + pw.toString()).append("\n");
        }
        sb.append(getJri().getDelays()).append("\n");
        sb.append(ReportingConf.ANSI_BLUE + "===========================").append("\n");
        sb.append(ReportingConf.ANSI_YELLOW + "Running for: " + (System.currentTimeMillis() -
                getJri().getStart().getTime()) + "ms\t\tStarted: " + dtf.format(getJri().getStart()) +
                "\t\tRecord Count: " + getJri().getCount() + "\t\tData Size: " + getJri().getSize().get() + ReportingConf.ANSI_RESET).append("\n");

        String output = ReportingConf.substituteVariables(sb.toString());
        System.out.println(output);

    }

    @Override
    public void interrupt() {
        super.interrupt();
    }

    @Override
    public void run() {
        setUpWindows();
        try {
            while (true) {
                Thread.sleep(JDBCPerfTest.STATUS_INTERVAL);
                updateStatus();
            }
        } catch (InterruptedException ire) {
            // Done
        }
    }
}
