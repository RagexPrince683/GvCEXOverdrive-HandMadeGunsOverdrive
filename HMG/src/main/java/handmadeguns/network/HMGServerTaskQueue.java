package handmadeguns.network;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/** 1.7.10 SimpleImpl does not provide a server scheduler, so network work is handed to this tick queue. */
public final class HMGServerTaskQueue {
    public static final HMGServerTaskQueue INSTANCE = new HMGServerTaskQueue();
    private static final Queue<Runnable> TASKS = new ConcurrentLinkedQueue<Runnable>();

    private HMGServerTaskQueue() { }

    public static void enqueue(Runnable task) { TASKS.add(task); }

    @SubscribeEvent public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Runnable task;
        while ((task = TASKS.poll()) != null) {
            try {
                task.run();
            } catch (RuntimeException exception) {
                exception.printStackTrace();
            }
        }
    }
}
