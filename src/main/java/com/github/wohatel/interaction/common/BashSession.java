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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;


@Slf4j
public class BashSession {
    @Getter
    private final Process process;
    @Getter
    private final long bashSessionId;
    private Consumer<String> consumer;
    @Getter
    private final BlockingQueue<String> commandQueue = new LinkedBlockingQueue<>();
    @Getter
    private final BlockingQueue<String> outputQueue;
    @Getter
    private AtomicBoolean stoped = new AtomicBoolean(false);
    private final AtomicLong lastOperateTime = new AtomicLong(System.currentTimeMillis());


    public BashSession() {
        this(50000);
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
        DefaultVirtualThreadPool.execute(() -> readStream(process.inputReader()));
        this.outputQueue = new LinkedBlockingQueue<>(outlineLimit);
    }

    @SneakyThrows
    private void readStream(BufferedReader reader) {
        String line;
        while (!stoped.get() && (line = reader.readLine()) != null) {
            outputQueue.offer(line); // 有界队列自己限制大小
        }
    }

    public void sendCommand(String cmd) {
        this.sendCommand(cmd, false);
    }

    @SneakyThrows
    public void sendCommand(String cmd, boolean addToOutputQueue) {
        if (!stoped.get()) {
            lastOperateTime.set(System.currentTimeMillis());
            BufferedWriter inputWriter = process.outputWriter();
            inputWriter.write(cmd);
            inputWriter.write("\n");
            inputWriter.flush();
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
        } catch (IOException e) {
            log.error("exec kill -2", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    public void kill9Pid(Long pid) {
        try {
            lastOperateTime.set(System.currentTimeMillis());
            Process p = new ProcessBuilder("kill", "-9", String.valueOf(pid)).start();
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
     * 处理标准输出或错误输出
     *
     * @param consumer 消费每一行输出信息
     */
    public synchronized void onOutPut(Consumer<String> consumer) {
        if (this.consumer != null) {
            return;
        }
        this.consumer = consumer;
        DefaultVirtualThreadPool.execute(() -> {
            List<String> batch = new ArrayList<>(100);
            while (!stoped.get()) {
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
                process.destroyForcibly(); // 保底强杀
            }
        }
    }

    /**
     * 是否停止
     */
    public boolean isStoped() {
        return stoped.get();
    }

}