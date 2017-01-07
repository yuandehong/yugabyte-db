// Copyright (c) YugaByte, Inc.

package org.yb.loadtester.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.yb.loadtester.Workload;

public class Configuration {
  private static final Logger LOG = Logger.getLogger(Configuration.class);

  // The types of workloads currently registered.
  public static enum WorkLoadType {
    RedisSimpleReadWrite,
  }

  // The class type of the workload.
  private Class<? extends Workload> workloadClass;
  public List<Node> nodes = new ArrayList<>();
  Random random = new Random();
  int numReaderThreads;
  int numWriterThreads;

  public Configuration(WorkLoadType workloadType, List<String> hostPortList)
      throws ClassNotFoundException {
    // Get the workload class.
    workloadClass = Class.forName("org.yb.loadtester.workload." + workloadType.toString())
                         .asSubclass(Workload.class);
    LOG.info("Workload: " + workloadClass.getSimpleName());

    for (String hostPort : hostPortList) {
      LOG.info("Adding node: " + hostPort);
      this.nodes.add(Node.fromHostPort(hostPort));
    }
  }

  public Workload getWorkloadInstance() {
    Workload workload = null;
    try {
      // Create a new workload object.
      workload = workloadClass.newInstance();
      // Initialize the workload.
      workload.initialize(this, null);
    } catch (Exception e) {
      LOG.error("Could not create instance of " + workloadClass.getName(), e);
    }
    return workload;
  }

  public Node getRandomNode() {
    int nodeId = random.nextInt(nodes.size());
    LOG.info("Returning random node id " + nodeId);
    return nodes.get(nodeId);
  }

  public int getNumReaderThreads() {
    return numReaderThreads;
  }

  public int getNumWriterThreads() {
    return numWriterThreads;
  }

  public void initializeThreadCount(String numThreadsStr) {
    // Check if there are a fixed number of threads or variable.
    if (Workload.workloadConfig.readIOPSPercentage == -1) {
      numReaderThreads = Workload.workloadConfig.numReaderThreads;
      numWriterThreads = Workload.workloadConfig.numWriterThreads;
    } else {
      int numThreads = 0;
      if (numThreadsStr != null) {
          numThreads = Integer.parseInt(numThreadsStr);
      } else {
        // Default to 8 * num-cores
        numThreads = 8 * Runtime.getRuntime().availableProcessors();
      }
      numReaderThreads =
          (int) Math.round(1.0 * numThreads * Workload.workloadConfig.readIOPSPercentage / 100);
      numWriterThreads = numThreads - numReaderThreads;
    }

    LOG.info("Num reader threads: " + numReaderThreads +
             ", num writer threads: " + numWriterThreads);
  }

  public static Configuration createFromArgs(String[] args) throws Exception {
    Options options = new Options();
    options.addOption("h", "help", false, "show help.");

    Option appType = OptionBuilder.create("workload");
    appType.setDescription("The workload to run.");
    appType.setRequired(true);
    appType.setLongOpt("workload");
    appType.setArgs(1);

    Option proxyAddrs = OptionBuilder.create("nodes");
    proxyAddrs.setDescription("Comma separated proxies, host1:port1,....,hostN:portN");
    proxyAddrs.setRequired(true);
    proxyAddrs.setLongOpt("nodes");
    proxyAddrs.setArgs(1);

    Option numThreads = OptionBuilder.create("num_threads");
    appType.setDescription("The total number of threads.");
    appType.setRequired(false);
    appType.setLongOpt("num_threads");
    appType.setArgs(1);

    Option logtostderr = OptionBuilder.create("logtostderr");
    logtostderr.setDescription("Log to console.");
    logtostderr.setRequired(false);
    logtostderr.setLongOpt("logtostderr");
    logtostderr.setArgs(0);

    options.addOption(appType);
    options.addOption(proxyAddrs);
    options.addOption(numThreads);
    options.addOption(logtostderr);

    CommandLineParser parser = new BasicParser();
    CommandLine cmd = null;

    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      printUsage(options, e.getMessage());
      System.exit(0);
    }

    // Enable console logging.
    if (cmd.hasOption("logtostderr")) {
      // Add in console logging so its easier to see logs.
      ConsoleAppender console = new ConsoleAppender();
      String PATTERN = "%d [%p|%c|%C{1}] %m%n";
      console.setLayout(new PatternLayout(PATTERN));
      console.setThreshold(Level.INFO);
      console.activateOptions();
      Logger.getRootLogger().addAppender(console);
    }

    if (cmd.hasOption("h")) {
      printUsage(options, "Usage:");
      System.exit(0);
    }

    // Get the workload.
    WorkLoadType workloadType = WorkLoadType.valueOf(cmd.getOptionValue("workload"));
    // Get the proxy nodes.
    List<String> hostPortList = Arrays.asList(cmd.getOptionValue("nodes").split(","));
    // Create the configuration.
    Configuration configuration = new Configuration(workloadType, hostPortList);
    // Set the number of threads.
    configuration.initializeThreadCount(cmd.getOptionValue("num_threads"));

    return configuration;
  }

  private static void printUsage(Options options, String header) {
    StringBuilder footer = new StringBuilder();
    footer.append("Valid values for 'workload' are: ");
    for (WorkLoadType workloadType : WorkLoadType.values()) {
      footer.append(workloadType.toString() + " ");
    }
    footer.append("\n");

    HelpFormatter formater = new HelpFormatter();
    formater.printHelp("LoadTester", header, options, footer.toString());
    System.exit(0);
  }

  public static class Node {
    String host;
    int port;

    public Node(String host, int port) {
      this.host = host;
      this.port = port;
    }

    public String getHost() {
      return host;
    }

    public int getPort() {
      return port;
    }

    public static Node fromHostPort(String hostPort) {
      String[] parts = hostPort.split(":");
      String host = parts[0];
      int port = Integer.parseInt(parts[1]);
      return new Node(host, port);
    }
  }
}