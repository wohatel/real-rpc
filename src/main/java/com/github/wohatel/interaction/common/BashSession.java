package com.github.wohatel.interaction.common;

import com.github.wohatel.util.DefaultVirtualThreadPool;
import com.github.wohatel.util.RunnerUtil;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;


@Slf4j
public class BashSession {
    @Getter
    private final Process process; // Process object representing the bash session
    @Getter
    private final long bashSessionId; // Unique identifier for the bash session
    @Getter
    private final BlockingQueue<String> commandQueue = new LinkedBlockingQueue<>(); // Queue to store commands
    @Getter
    private final BlockingQueue<String> outputQueue; // Queue to store output lines
    @Getter
    private AtomicBoolean stoped = new AtomicBoolean(false); // Flag to indicate if session is stopped
    private final AtomicLong lastOperateTime = new AtomicLong(System.currentTimeMillis()); // Timestamp of last operation
    private Future<?> future;
    private EliminateModel eliminateModel;



    public BashSession() {
        this(500_000); // Default constructor with outline limit of 500_000
    }

    public BashSession(int outlineLimit) {
        this("bash", outlineLimit, null); // Constructor with bash environment and outline limit
    }

    /**
     * Constructor to create a bash session with specified environment
     * @param bashEnv  bashEnv
     *                 "bash" linux or mac ... support
     *                 "zsh"
     *                 "ash"  Alpine Linux default shell
     *                 ....
     *                 "bash" is default
     */
    @SneakyThrows
    public BashSession(String bashEnv, int outlineLimit, EliminateModel eliminateModel) {
        ProcessBuilder builder = new ProcessBuilder(bashEnv);
        builder.redirectErrorStream(true); // stderr 合并到 stdout
        process = builder.start();
        this.bashSessionId = process.pid();
        this.eliminateModel = eliminateModel;
        if (this.eliminateModel == null) {
            this.eliminateModel = EliminateModel.DISCARD;
        }
        this.outputQueue = new LinkedBlockingQueue<>(outlineLimit);
        DefaultVirtualThreadPool.execute(() -> readStream(process.inputReader()));


    }

    /**
     * Method to read from a BufferedReader and add lines to a queue
     * Uses @SneakyThrows to handle exceptions in a sneaky way
     *
     * @param reader The BufferedReader to read from
     */
    @SneakyThrows
    private void readStream(BufferedReader reader) {
        try {
            String line;
            while (!stoped.get() && (line = reader.readLine()) != null) {
                boolean offer = outputQueue.offer(line);
                if (this.eliminateModel == EliminateModel.CLOSE && !offer) {
                    log.warn("outputQueue满了，主动关闭 BashSession");
                    this.close();
                    break;
                }
            }
        } catch (IOException e) {
            if (!stoped.get()) {
                log.error("读取Bash输出异常", e);
            }
        }
    }

    /**
     * Sends a command with default parameters
     * This is a convenience method that calls the main sendCommand method with default parameter values
     *
     * @param cmd The command string to be sent
     */
    public void sendCommand(String cmd) {
        // Call the overloaded sendCommand method with default parameter for async
        this.sendCommand(cmd, false);
    }

    /**
     * Sends a command to the process with optional logging to output queue
     *
     * @param cmd              The command string to be sent
     * @param addToOutputQueue Flag indicating whether to add the command to output queue
     */
    @SneakyThrows
    public void sendCommand(String cmd, boolean addToOutputQueue) {
        // Check if the process is still running
        if (!stoped.get()) {
            // Update the last operation timestamp
            lastOperateTime.set(System.currentTimeMillis());
            // Get the writer to process input
            BufferedWriter inputWriter = process.outputWriter();
            // Write the command to process input
            inputWriter.write(cmd);
            inputWriter.write("\n");
            inputWriter.flush();
            // Optionally add to output queue for tracking
            if (addToOutputQueue) {
                outputQueue.offer(cmd);
            }
            // 默认存500个,如果超出就删除10%
            commandQueue.offer(cmd);
            if (commandQueue.size() > 500) {
                commandQueue.drainTo(new ArrayList<>(50), 50);
            }
        }
    }

    /**
     * Returns the command history as a single string with each command separated by a newline character.
     * This method is useful for displaying the sequence of commands that have been executed.
     *
     * @return A string containing all commands from the command queue, each separated by a newline character
     */
    public String history() {
        // Join all commands in the queue with newline character as delimiter
        return String.join("\n", commandQueue);
    }


    /**
     * Sends a Ctrl+C signal to the running process to interrupt its current operation.
     * This method updates the last operation time and writes the interrupt character to the process input.
     *
     * @throws IOException if an I/O error occurs while writing to the process input stream
     */
    public void sendCtrlC() throws IOException {
        // Update the timestamp of the last operation
        lastOperateTime.set(System.currentTimeMillis());
        // Get the writer for the process standard input
        BufferedWriter inputWriter = process.outputWriter();
        // Write the ASCII character 3 (Ctrl+C) to interrupt the process
        inputWriter.write("\u0003"); // Ctrl+C
        // Ensure the data is immediately sent to the process
        inputWriter.flush();
    }

    /**
     * Method to send a SIGINT signal (interrupt) to a process with the specified PID.
     * This is equivalent to pressing Ctrl+C in the terminal.
     *
     * @param pid The process ID of the target process to be interrupted
     */
    public void kill2Pid(Long pid) {
        try {
            // Record the last operation time for tracking purposes
            lastOperateTime.set(System.currentTimeMillis());
            // Build and start a process to execute the kill command with SIGINT (-2) signal
            Process p = new ProcessBuilder("kill", "-2", String.valueOf(pid)).start();
            // Close input and error streams to prevent resource leaks
            p.getInputStream().close();
            p.getErrorStream().close();
            p.waitFor(500, TimeUnit.MILLISECONDS); // 最多等500ms
        } catch (IOException e) {
            log.error("exec kill -2", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    /**
     * Forcefully terminates a process with the given PID using the 'kill -9' command.
     * This method attempts to kill the process and waits for at most 500ms for the operation to complete.
     *
     * @param pid The process ID of the process to be terminated
     */
    public void kill9Pid(Long pid) {
        try {
            // Record the last operation time
            lastOperateTime.set(System.currentTimeMillis());
            // Build and start the process to execute 'kill -9' command
            Process p = new ProcessBuilder("kill", "-9", String.valueOf(pid)).start();
            // Close input and error streams to prevent resource leaks
            p.getInputStream().close();
            p.getErrorStream().close();
            p.waitFor(500, TimeUnit.MILLISECONDS); // 最多等500ms
        } catch (IOException e) {
            log.error("exec kill -9 exception", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    /**
     * This method sets up a consumer for output processing with thread synchronization.
     * It creates a virtual thread pool to handle batch processing of output messages.
     *
     * @param consumer The consumer function that processes the output strings
     */
    public synchronized void onOutPut(Consumer<List<String>> consumer) {
        // Execute the processing in a default virtual thread pool
        if (this.future == null) {
            this.future = DefaultVirtualThreadPool.submit(() -> {
                while (!stoped.get() && !Thread.currentThread().isInterrupted()) {
                    try {
                        List<String> batch = new ArrayList<>(128);
                        String take = outputQueue.take();
                        batch.add(take);
                        // Drain up to 99 messages from the output queue to the batch
                        outputQueue.drainTo(batch, 99);
                        // 注意消费线程
                        consumer.accept(batch);
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        log.error("abnormal consumption", e);
                    }
                }
            });
        }
    }


    /**
     * This method checks if the current application is running in interactive mode.
     * Interactive mode is determined by checking if there is a foreground process.
     *
     * @return true if there is no foreground process (indicating interactive mode),
     *         false otherwise
     */
    public boolean isInteractive() {
        // Find the foreground process ID
        Long foregroundProcess = findForegroundProcess();
        // Return true if no foreground process is found (null), false otherwise
        return foregroundProcess == null;
    }

    /**
     * Finds the foreground process among child processes.
     * This method sneaky throws any exceptions that might occur during process handling.
     *
     * @return The PID (Process ID) of the foreground process if found, otherwise null
     */
    @SneakyThrows
    public Long findForegroundProcess() {
        // Get all child processes and collect their PIDs into a list
        List<Long> collect = process.children().map(ProcessHandle::pid).toList();
        // If there are no child processes, return null
        if (collect.isEmpty()) {
            return null;
        }
        // Return the first PID from the collected list of child processes
        return collect.getFirst();
    }

    /**
     * Closes the resource using the default timeout of 2 seconds.
     * This method is annotated with @SneakyThrows to automatically throw checked exceptions.
     *
     * @throws Exception if an error occurs during the closing process
     */
    @SneakyThrows
    public void close() {
        // Call the overloaded close method with a default timeout of 2 seconds
        this.close(2);
    }

    /**
     * Closes the process with a specified timeout period.
     * This method attempts to gracefully shut down the process,
     * and if necessary, forcefully terminates it.
     *
     * @param timeoutSeconds The maximum time in seconds to wait for the process to terminate
     */
    public void close(int timeoutSeconds) {
        // 直接尝试中断
        future.cancel(true);
        // Check if the process hasn't been stopped yet, and atomically update the flag
        if (stoped.compareAndSet(false, true)) {
            try {
                this.sendCommand("exit"); // 请求shell自行退出
                RunnerUtil.execSilent(() -> process.inputReader().close());
                RunnerUtil.execSilent(() -> process.errorReader().close());
                RunnerUtil.execSilent(() -> process.outputWriter().close());
                if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                    process.destroy(); // 软杀
                    if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                        process.destroyForcibly(); // 强杀
                    }
                }
            } catch (Exception e) {
                log.error("abnormal consumption", e);
                process.destroyForcibly(); // 保底强杀
            }
        }
    }

    /**
     * Method to check if the process or operation has been stopped.
     * This method returns the current status of the stop flag.
     *
     * @return boolean value indicating whether the process is stopped (true) or running (false)
     */
    public boolean isStoped() {
        return stoped.get();  // Returns the current value of the atomic boolean stop flag
    }

    public enum EliminateModel {
        DISCARD, // 丢弃
        CLOSE // 关停
    }
}