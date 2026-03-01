package com.oolonghoo.woonpc.action;

import com.oolonghoo.woonpc.npc.Npc;
import org.bukkit.entity.Player;

import java.util.List;

public class ActionExecutionContext {
    private final Npc npc;
    private final Player player;
    private final ActionTrigger trigger;
    private final List<NpcAction.NpcActionData> actions;
    private int currentIndex;
    private boolean terminated;
    private boolean skipRemaining;
    private int jumpToIndex;
    
    public ActionExecutionContext(Npc npc, Player player, ActionTrigger trigger, 
                                   List<NpcAction.NpcActionData> actions) {
        this.npc = npc;
        this.player = player;
        this.trigger = trigger;
        this.actions = actions;
        this.currentIndex = 0;
        this.terminated = false;
        this.skipRemaining = false;
        this.jumpToIndex = -1;
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
        return currentIndex;
    }
    
    public void setCurrentIndex(int index) {
        this.currentIndex = index;
    }
    
    public NpcAction.NpcActionData getCurrentAction() {
        if (currentIndex >= 0 && currentIndex < actions.size()) {
            return actions.get(currentIndex);
        }
        return null;
    }
    
    public boolean hasNextAction() {
        return currentIndex < actions.size() - 1;
    }
    
    public void nextAction() {
        currentIndex++;
    }
    
    public boolean isTerminated() {
        return terminated;
    }
    
    public void terminate() {
        this.terminated = true;
    }
    
    public boolean isSkipRemaining() {
        return skipRemaining;
    }
    
    public void skipRemaining() {
        this.skipRemaining = true;
    }
    
    public int getJumpToIndex() {
        return jumpToIndex;
    }
    
    public void setJumpToIndex(int index) {
        this.jumpToIndex = index;
    }
    
    public void jumpTo(int index) {
        this.jumpToIndex = index;
    }
    
    public void delayNext(long ticks) {
        // Used by WaitAction to schedule continuation
    }
}
