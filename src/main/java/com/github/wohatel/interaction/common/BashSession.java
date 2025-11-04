package com.github.wohatel.interaction.common;

import com.github.wohatel.util.RunnerUtil;
import com.github.wohatel.util.VirtualThreadPool;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;


@Data
@Slf4j
public class BashSession {
    private final Process process;
    private long bashSessionId;
    private volatile Consumer<String> consumer;
    private final BlockingQueue<String> commandQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<String> outputQueue;
    private volatile boolean stop;
    private final AtomicLong lastOperateTime = new AtomicLong(System.currentTimeMillis());

    public BashSession() {
        this(10000);
    }

    public BashSession(int outlineLimit) {
        this("bash", outlineLimit);
    }

    /**     
     *
     * @param bashEnv  bashEnv
     *                 "bash" linux or mac ... support
     *                 "zsh"
     *                 "ash"  Alpine Linux default shell
     *                 ....
     *                 "bash" is default
     */
    @SneakyThrows
    public BashSession(String bashEnv, int outlineLimit) {
        ProcessBuilder builder = new ProcessBuilder(bashEnv);
        builder.redirectErrorStream(true); // stderr 合并到 stdout
        process = builder.start();
        this.bashSessionId = process.pid();
        VirtualThreadPool.execute(() -> readStream(process.inputReader()));
        this.outputQueue = new LinkedBlockingQueue<>(outlineLimit);
    }

    @SneakyThrows
    private void readStream(BufferedReader reader) {
        String line;
        while (!stop && (line = reader.readLine()) != null) {
            outputQueue.offer(line); // 有界队列自己限制大小
        }
    }

    @SneakyThrows
    public void sendCommand(String cmd) {
        if (!stop) {
            lastOperateTime.set(System.currentTimeMillis());
            BufferedWriter inputWriter = process.outputWriter();
            inputWriter.write(cmd);
            inputWriter.write("\n");
            inputWriter.flush();
            // 默认存500个,如果超出就删除10%
            if (commandQueue.size() > 500) {
                commandQueue.drainTo(new ArrayList<>(50), 50);
            }
        }
    }

    public String history() {
        return String.join("\n", commandQueue);
    }

    /**
     * exec ctrl+C
     *
     */
    public void sendCtrlC() throws IOException {
        lastOperateTime.set(System.currentTimeMillis());
        BufferedWriter inputWriter = process.outputWriter();
        inputWriter.write("\u0003"); // Ctrl+C
        inputWriter.flush();
    }

    public void kill2Pid(Long pid) {
        try {
            lastOperateTime.set(System.currentTimeMillis());
            Process p = new ProcessBuilder("kill", "-2", String.valueOf(pid)).start();
            p.getInputStream().close();
            p.getErrorStream().close();
            p.waitFor(500, TimeUnit.MILLISECONDS); // 最多等500ms
        } catch (IOException | InterruptedException e) {
            log.error("exec kill -2", e);
        }
    }


    public void kill9Pid(Long pid) {
        try {
            lastOperateTime.set(System.currentTimeMillis());
            Process p = new ProcessBuilder("kill", "-9", String.valueOf(pid)).start();
            p.getInputStream().close();
            p.getErrorStream().close();
            p.waitFor(500, TimeUnit.MILLISECONDS); // 最多等500ms
        } catch (IOException | InterruptedException e) {
            log.error("exec kill -9 exception", e);
        }
    }

    /**
     * 处理标准输出或错误输出
     *
     * @param consumer 消费每一行输出信息
     */
    public void onPrintOut(Consumer<String> consumer) {
        if (this.consumer != null) {
            return;
        }
        this.consumer = consumer;
        VirtualThreadPool.execute(() -> {
            List<String> batch = new ArrayList<>(100);
            while (!stop) {
                try {
                    batch.clear();
                    outputQueue.drainTo(batch, 99);
                    if (batch.isEmpty()) {
                        String poll = outputQueue.poll(5, TimeUnit.MILLISECONDS);
                        if (poll != null) {
                            batch.add(poll);
                        } else {
                            continue;
                        }
                    }
                    // 注意消费线程
                    consumer.accept(String.join("\n", batch));
                } catch (Exception e) {
                    log.error("abnormal consumption", e);
                }
            }
        });
    }


    /**
     * 是否是交互式
     *
     * @return boolean
     */
    public boolean isInteractive() {
        Long foregroundProcess = findForegroundProcess();
        return foregroundProcess == null;
    }

    @SneakyThrows
    public Long findForegroundProcess() {
        List<Long> collect = process.children().map(ProcessHandle::pid).toList();
        if (collect.isEmpty()) {
            return null;
        }
        return collect.getFirst();
    }

    @SneakyThrows
    public void close() {
        this.close(2);
    }

    public void close(int timeoutSeconds) {
        try {
            this.setStop(true);
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
            process.destroyForcibly(); // 保底强杀
        }
    }

}