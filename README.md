# WooNPC

🍵一款现代化的 Minecraft NPC 插件

## 特色

### 🎭 多版本支持
- **1.21 ~ 1.21.11**：完整支持所有版本，自动适配 API 差异
- **单 Jar 多版本**：无需下载不同版本，一个 Jar 搞定所有

### 🎨 皮肤系统
- **多 API 支持**：Mojang、Ashcon、MineTools、SkinsRestorer
- **镜像皮肤**：NPC 显示查看者的皮肤
- **皮肤缓存**：24 小时缓存，减少 API 请求
- **@none 清除**：一键清除皮肤恢复默认

### ⚡ 动作系统
- **多种触发器**：左键、右键、任意点击
- **丰富动作类型**：消息、命令、控制台命令、音效
- **PlaceholderAPI**：支持 PAPI 变量
- **冷却时间**：防止刷屏

### 🖱️ 交互功能
- **自动面向玩家**：NPC 自动转向附近玩家
- **全息图支持**：在 NPC 头顶显示文字
- **Tab 列表显示**：可选显示在 Tab 列表

### ⚙️ 配置灵活
- **可见距离**：自定义 NPC 可见距离
- **转向距离**：自定义转向玩家的距离
- **API 优先级**：自定义皮肤 API 调用顺序

## 环境

- Minecraft 1.21+
- Java 21+
- PlaceholderAPI（可选）
- SkinsRestorer（可选，用于皮肤缓存）

## 命令

| 命令 | 描述 | 权限 |
|------|------|------|
| `/npc create <名称>` | 创建 NPC | `woonpc.create` |
| `/npc delete <名称>` | 删除 NPC | `woonpc.delete` |
| `/npc list` | 列出所有 NPC | `woonpc.use` |
| `/npc info <名称>` | 查看 NPC 详情 | `woonpc.use` |
| `/npc movehere <名称>` | 移动到当前位置 | `woonpc.move` |
| `/npc moveto <名称> <x> <y> <z>` | 移动到指定坐标 | `woonpc.move` |
| `/npc teleport <名称> [玩家]` | 传送到 NPC 位置 | `woonpc.teleport` |
| `/npc copy <源> <新名称>` | 复制 NPC | `woonpc.copy` |
| `/npc skin <名称> <皮肤>` | 设置皮肤 | `woonpc.skin` |
| `/npc action <名称> ...` | 管理动作 | `woonpc.action` |
| `/npc hologram <名称> ...` | 管理全息图 | `woonpc.hologram` |
| `/npc glowing <名称> <true/false> [颜色]` | 设置发光效果 | `woonpc.glowing` |
| `/npc turn <名称> <true/false>` | 设置自动转向 | `woonpc.turn` |
| `/npc reload` | 重载配置 | `woonpc.admin` |

## 皮肤命令

| 参数 | 描述 |
|------|------|
| `@mirror` | 镜像皮肤（显示查看者的皮肤） |
| `@none` | 清除皮肤（恢复默认） |
| `<玩家名>` | 使用正版玩家皮肤 |
| `<UUID>` | 使用指定 UUID 的皮肤 |

## 动作命令

| 命令 | 描述 |
|------|------|
| `/npc action <名称> <触发器> add <类型> <值>` | 添加动作 |
| `/npc action <名称> <触发器> delete <索引>` | 删除动作 |
| `/npc action <名称> <触发器> list` | 列出动作 |
| `/npc action <名称> <触发器> clear` | 清除动作 |

### 触发器类型

| 触发器 | 描述 |
|------|------|
| `left_click` | 左键点击 |
| `right_click` | 右键点击 |
| `any_click` | 任意点击 |

### 动作类型

| 类型 | 描述 | 示例 |
|------|------|------|
| `message` | 发送消息 | `message &a你好！` |
| `command` | 玩家执行命令 | `command spawn` |
| `console_command` | 控制台执行命令 | `console_command give %player% diamond 1` |
| `sound` | 播放音效 | `sound ENTITY_EXPERIENCE_ORB_PICKUP 1.0 1.0` |


## 全息图命令

| 命令 | 描述 |
|------|------|
| `/npc hologram <名称> add <内容>` | 添加全息图行 |
| `/npc hologram <名称> set <行号> <内容>` | 设置行内容 |
| `/npc hologram <名称> delete <行号>` | 删除行 |
| `/npc hologram <名称> clear` | 清除所有行 |
| `/npc hologram <名称> list` | 列出所有行 |

## 权限

| 权限 | 描述 | 默认 |
|------|------|------|
| `woonpc.use` | 基础使用 | true |
| `woonpc.create` | 创建 NPC | op |
| `woonpc.delete` | 删除 NPC | op |
| `woonpc.move` | 移动 NPC | op |
| `woonpc.teleport` | 传送 | op |
| `woonpc.copy` | 复制 NPC | op |
| `woonpc.skin` | 设置皮肤 | op |
| `woonpc.action` | 管理动作 | op |
| `woonpc.hologram` | 管理全息图 | op |
| `woonpc.glowing` | 设置发光 | op |
| `woonpc.turn` | 设置转向 | op |
| `woonpc.admin` | 管理员权限 | op |

## API 使用示例

```java
WooNPCAPI api = WooNPCAPI.getInstance();

// 创建 NPC
Npc npc = api.createNpc("test", player.getLocation());

// 设置皮肤
npc.getData().setSkin("Notch", skinValue, skinSignature);

// 添加动作
npc.getData().addAction(ActionTrigger.RIGHT_CLICK, 
    new NpcAction.NpcActionData(0, new MessageAction(), "&a你好！"));

// 显示给玩家
npc.spawnForAll();

// 监听点击事件
@EventHandler
public void onNpcClick(NpcClickEvent event) {
    Player player = event.getPlayer();
    Npc npc = event.getNpc();
    // 你的逻辑
}
```

---

❤️ 主包是开发新手，如果有做得不好的地方，欢迎指正。希望能和大家一起交流！

⭐ 觉得有用请给个 Star 爱你哟
