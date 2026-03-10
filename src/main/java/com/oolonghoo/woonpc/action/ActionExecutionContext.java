package com.oolonghoo.woonpc.action;

import com.oolonghoo.woonpc.npc.Npc;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class ActionExecutionContext {
    private final Npc npc;
    private final Player player;
    private final ActionTrigger trigger;
    private final List<NpcAction.NpcActionData> actions;
    private final AtomicInteger currentIndex = new AtomicInteger(0);
    private final AtomicBoolean terminated = new AtomicBoolean(false);
    private final AtomicBoolean skipRemaining = new AtomicBoolean(false);
    private final AtomicInteger jumpToIndex = new AtomicInteger(-1);
    private final ReentrantLock lock = new ReentrantLock();
    
    public ActionExecutionContext(Npc npc, Player player, ActionTrigger trigger, 
                                   List<NpcAction.NpcActionData> actions) {
        this.npc = npc;
        this.player = player;
        this.trigger = trigger;
        this.actions = actions;
    }
    
    public Npc getNpc() {
        return npc;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public ActionTrigger getTrigger() {
        return trigger;
    }
    
    public List<NpcAction.NpcActionData> getActions() {
        return actions;
    }
    
    public int getCurrentIndex() {
        return currentIndex.get();
    }
    
    public void setCurrentIndex(int index) {
        this.currentIndex.set(index);
    }
    
    public NpcAction.NpcActionData getCurrentAction() {
        int idx = currentIndex.get();
        if (idx >= 0 && idx < actions.size()) {
            return actions.get(idx);
        }
        return null;
    }
    
    public boolean hasNextAction() {
        return currentIndex.get() < actions.size() - 1;
    }
    
    public void nextAction() {
        currentIndex.incrementAndGet();
    }
    
    public boolean isTerminated() {
        return terminated.get();
    }
    
    public void terminate() {
        this.terminated.set(true);
    }
    
    public boolean isSkipRemaining() {
        return skipRemaining.get();
    }
    
    public void skipRemaining() {
        this.skipRemaining.set(true);
    }
    
    public void resetSkipRemaining() {
        this.skipRemaining.set(false);
    }
    
    public int getJumpToIndex() {
        return jumpToIndex.get();
    }
    
    public void setJumpToIndex(int index) {
        this.jumpToIndex.set(index);
    }
    
    public void jumpTo(int index) {
        this.jumpToIndex.set(index);
    }
    
    public void delayNext(long ticks) {
    }
    
    /**
     * 获取锁用于复合操作
     */
    public ReentrantLock getLock() {
        return lock;
    }
    
    /**
     * 在锁保护下执行操作
     */
    public void withLock(Runnable action) {
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 原子地执行条件检查和跳转
     * @return 是否成功跳转
     */
    public boolean atomicJumpIf(int expectedIndex, int newIndex) {
        return currentIndex.compareAndSet(expectedIndex, newIndex);
    }
}
