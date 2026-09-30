package org.ordermatching.handlerdomain;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class EngineRunner implements Runnable {
    private volatile boolean running = false;
    private Thread thread;

    private final BlockingQueue<Command> queue;
    private final MatchingEngine engine;

    public EngineRunner(MatchingEngine engine) {
        this(engine, 10_000);
    }

    public EngineRunner(MatchingEngine engine, int queueCapacity) {
        this.engine = engine;
        this.queue = new ArrayBlockingQueue<>(queueCapacity);
    }

    public boolean offer(Command cmd) {
        return running && queue.offer(cmd);
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        thread = new Thread(this, "matching-engine");
        thread.start();
    }

    public synchronized void stop() throws InterruptedException {
        if (!running) return;
        running = false;
        thread.interrupt();
        thread.join();
    }

    @Override
    public void run() {
        while (running) {
            Command cmd;
            try {
                cmd = queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            handle(cmd);
        }
    }

    private void handle(Command cmd) {
        // any exception goes back to the caller instead of killing the engine thread
        switch (cmd) {
            case NewOrder n -> {
                try { n.reply().complete(engine.submit(n.accountId(), n.request())); }
                catch (Exception e) { n.reply().completeExceptionally(e); }
            }
            case Cancel c -> {
                try { c.reply().complete(engine.cancel(c.accountId(), c.orderId())); }
                catch (Exception e) { c.reply().completeExceptionally(e); }
            }
            case GetOrderStatus g -> {
                try { g.reply().complete(engine.getOrder(g.accountId(), g.orderId())); }
                catch (Exception e) { g.reply().completeExceptionally(e); }
            }
        }
    }
}
