package com.oolonghoo.woonpc.npc;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Predicate;

/**
 * 虚拟同步器
 * <p>
 * 用于创建虚拟实体（如坐骑）时替代默认的 ServerEntity.Synchronizer。
 * 这个类不实际发送任何数据包，因为我们手动控制数据包的发送。
 * </p>
 *
 * <h3>使用场景</h3>
 * <ul>
 *   <li>创建 NPC 坐骑实体</li>
 *   <li>创建虚拟显示实体</li>
 *   <li>需要手动控制数据包发送的场景</li>
 * </ul>
 *
 * <h3>版本支持</h3>
 * <ul>
 *   <li>1.21.9+ 版本需要使用此类</li>
 *   <li>1.21.11 版本使用新的 ServerEntity 构造函数</li>
 * </ul>
 *
 * @author oolongho
 * @since 1.0.0
 * @see ServerEntity.Synchronizer
 */
public class FakeSynchronizer implements ServerEntity.Synchronizer {

    /**
     * 单例实例
     */
    public static final FakeSynchronizer INSTANCE = new FakeSynchronizer();

    /**
     * 私有构造函数，防止外部实例化
     */
    private FakeSynchronizer() {
    }

    /**
     * 发送数据包给所有追踪玩家
     * <p>
     * 此实现为空操作，因为我们手动控制数据包发送。
     * </p>
     *
     * @param packet 要发送的数据包
     */
    @Override
    public void sendToTrackingPlayers(Packet<? super ClientGamePacketListener> packet) {
        // 空实现 - 手动控制数据包发送
    }

    /**
     * 发送数据包给所有追踪玩家和自身
     * <p>
     * 此实现为空操作，因为我们手动控制数据包发送。
     * </p>
     *
     * @param packet 要发送的数据包
     */
    @Override
    public void sendToTrackingPlayersAndSelf(Packet<? super ClientGamePacketListener> packet) {
        // 空实现 - 手动控制数据包发送
    }

    /**
     * 发送过滤后的数据包给追踪玩家
     * <p>
     * 此实现为空操作，因为我们手动控制数据包发送。
     * </p>
     *
     * @param packet    要发送的数据包
     * @param predicate 过滤谓词
     */
    @Override
    public void sendToTrackingPlayersFiltered(Packet<? super ClientGamePacketListener> packet, Predicate<ServerPlayer> predicate) {
        // 空实现 - 手动控制数据包发送
    }
}
