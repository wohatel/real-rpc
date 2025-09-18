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
    private final BlockingQueue<String> commandQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<String> outputQueue = new LinkedBlockingQueue<>();
    private volatile boolean stop;
    private final AtomicLong lastOperateTime = new AtomicLong(System.currentTimeMillis());


    public BashSession(Consumer<String> consumer) {
        this("bash", consumer);
    }

    /**
     *
     * @param bashEnv  bash环境
     *                 "bash" 基本上类linux系统都支持
     *                 "zsh"  很多机器都支持
     *                 "ash"  Alpine Linux 默认 shell（BusyBox 的一部分）
     *                 ....
     *                 默认构建的是"bash"环境
     * @param consumer 构建消费者, 处理日志
     */
    @SneakyThrows
    public BashSession(String bashEnv, Consumer<String> consumer) {
        ProcessBuilder builder = new ProcessBuilder(bashEnv);
        builder.redirectErrorStream(true); // stderr 合并到 stdout
        process = builder.start();
        this.bashSessionId = process.pid();
        VirtualThreadPool.execute(() -> readStream(process.inputReader()));
        VirtualThreadPool.execute(() -> readStream(process.errorReader()));
        consumeMsg(consumer);
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
        lastOperateTime.set(System.currentTimeMillis());
        commandQueue.add(cmd);
        BufferedWriter inputWriter = process.outputWriter();
        inputWriter.write(cmd);
        inputWriter.write("\n");
        inputWriter.flush();
        // 默认存500个,如果超出就删除10%
        if (commandQueue.size() > 500) {
            commandQueue.drainTo(new ArrayList<>(50), 50);
        }
    }

    /**
     * 查询历史
     */
    public String history() {
        return String.join("\n", commandQueue);
    }

    /**
     * 执行ctrl+C
     *
     * @throws IOException 异常
     */
    public void sendCtrlC() throws IOException {
        lastOperateTime.set(System.currentTimeMillis());
        BufferedWriter inputWriter = process.outputWriter();
        inputWriter.write("\u0003"); // Ctrl+C
        inputWriter.flush();
    }

    /**
     * 优雅杀死进程
     *
     * @param pid 进程id
     */
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

    /**
     * 直接杀死进程
     *
     * @param pid
     */
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
     * 单线程消费消息
     * 注意consumer不要阻塞线程消费
     *
     * @param consumer 输出消费
     */
    private void consumeMsg(Consumer<String> consumer) {
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
     * 判断是否可交互
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