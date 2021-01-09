package com.imengyu.vr720.core.utils;

import com.imengyu.vr720.core.natives.NativeVR720Renderer;

import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameUpdateThread {

    private final NativeVR720Renderer renderer;

    public GameUpdateThread(NativeVR720Renderer renderer) {
        this.renderer = renderer;
    }

    /**
     * 更新任务
     */
    private final TimerTask task = new TimerTask() {
        @Override
        public void run() {
            Thread.currentThread().setName("GameUpdate");
            renderer.onMainThread();
        }
    };
    private ScheduledExecutorService pool = null;

    public void startUpdateThread() {
        if(pool == null) {
            pool = Executors.newScheduledThreadPool(1);
            pool.scheduleAtFixedRate(task, 0, 150, TimeUnit.MILLISECONDS);
        }
    }
    public void stopUpdateThread() {
        if(pool != null) {
            pool.shutdown();
            try {
                if(!pool.awaitTermination(1000, TimeUnit.MILLISECONDS))
                    pool.shutdownNow();
            } catch (InterruptedException e) {
                e.printStackTrace();
                pool.shutdownNow();
            }
            pool = null;
        }
    }

}
