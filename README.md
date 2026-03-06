# WooNPC

🧑一款现代化的 Minecraft NPC 插件

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
- **丰富动作类型**：消息、命令、控制台命令、音效、等待、随机执行、权限检查、OP命令
- **PlaceholderAPI**：支持 PAPI 变量
- **冷却时间**：防止刷屏

### 🖱️ 交互功能
- **自动视角跟随**：NPC 自动转向附近玩家
- **WooHolograms Hook**：自动检测并使用 WooHolograms 显示全息图
- **Tab 列表显示**：可选显示在 Tab 列表

### ⚙️ 配置灵活
- **可见距离**：自定义 NPC 可见距离
- **转向距离**：自定义转向玩家的距离
- **API 优先级**：自定义皮肤 API 调用顺序

### 🎭 实体效果
- **着火效果**：NPC 显示着火状态
- **隐形效果**：NPC 隐形
- **冷颤效果**：NPC 显示冷颤状态
- **静音效果**：NPC 不发出声音

### 📏 缩放系统
- **自定义缩放**：支持 0.1 ~ 10.0 倍缩放

## 环境

- Minecraft 1.21+
- Java 21+
- PlaceholderAPI（可选）
- SkinsRestorer（可选，用于皮肤缓存）
- WooHolograms（可选，用于全息图显示）

## 命令

| 命令 | 描述 | 权限 |
|------|------|------|
| `/npc create <id> [--type <类型>]` | 创建 NPC（id 为唯一标识符） | `woonpc.create` |
| `/npc delete <id>` | 删除 NPC | `woonpc.delete` |
| `/npc list` | 列出所有 NPC | `woonpc.use` |
| `/npc info <id>` | 查看 NPC 详情 | `woonpc.use` |
| `/npc displayname <id> <显示名称>` | 设置显示名称（支持颜色代码） | `woonpc.edit` |
| `/npc movehere <id>` | 移动到当前位置 | `woonpc.edit` |
| `/npc moveto <id> <x> <y> <z>` | 移动到指定坐标 | `woonpc.edit` |
| `/npc teleport <id>` | 传送到 NPC 位置 | `woonpc.use` |
| `/npc copy <源id> <新id>` | 复制 NPC | `woonpc.create` |
| `/npc skin <id> <皮肤>` | 设置皮肤 | `woonpc.skin` |
| `/npc action <id> ...` | 管理动作 | `woonpc.action` |
| `/npc hologram <id> ...` | 管理全息图 | `woonpc.hologram` |
| `/npc glowing <id> <true/false> [颜色]` | 设置发光效果 | `woonpc.glowing` |
| `/npc look <id> <true/false>` | 设置自动转向 | `woonpc.edit` |
| `/npc pose <id> <姿势>` | 设置姿势 | `woonpc.pose` |
| `/npc scale <id> <比例>` | 设置缩放 | `woonpc.scale` |
| `/npc effect <id> <效果> <true/false>` | 设置效果 | `woonpc.effect` |
| `/npc equip <id> <槽位> [@hand]` | 设置装备 | `woonpc.equipment` |
| `/npc reload` | 重载配置 | `woonpc.reload` |

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
| `/npc action <id> <触发器> add <类型> <值>` | 添加动作 |
| `/npc action <id> <触发器> delete <索引>` | 删除动作 |
| `/npc action <id> <触发器> list` | 列出动作 |
| `/npc action <id> <触发器> clear` | 清除动作 |

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
| `player_command` | 玩家执行命令 | `player_command spawn` |
| `console_command` | 控制台执行命令 | `console_command give %player% diamond 1` |
| `play_sound` | 播放音效 | `play_sound ENTITY_EXPERIENCE_ORB_PICKUP 1.0 1.0` |
| `wait` | 等待指定时间 | `wait 20` (等待 20 tick = 1秒) |
| `execute_random_action` | 随机执行一个后续动作 | `execute_random_action` |
| `need_permission` | 权限检查 | `need_permission woonpc.vip` |
| `player_command_as_op` | 以 OP 权限执行命令 | `player_command_as_op gamemode creative` |


## 全息图命令

| 命令 | 描述 |
|------|------|
| `/npc hologram <id> add <内容>` | 添加全息图行 |
| `/npc hologram <id> set <行号> <内容>` | 设置行内容 |
| `/npc hologram <id> delete <行号>` | 删除行 |
| `/npc hologram <id> clear` | 清除所有行 |
| `/npc hologram <id> list` | 列出所有行 |

## 效果命令

| 效果 | 描述 |
|------|------|
| `on_fire` | 着火效果 |
| `invisible` | 隐形效果 |
| `shaking` | 冷颤效果 |
| `silent` | 静音效果 |

## 姿势命令

| 姿势 | 描述 |
|------|------|
| `standing` | 站立 |
| `sitting` | 坐下 |
| `sleeping` | 睡觉 |
| `swimming` | 游泳 |
| `fall_flying` | 飞行 |

## 权限

| 权限 | 描述 | 默认 |
|------|------|------|
| `woonpc.use` | 基础使用 | true |
| `woonpc.create` | 创建 NPC | op |
| `woonpc.remove` | 删除 NPC | op |
| `woonpc.edit` | 编辑 NPC（移动、显示名称等） | op |
| `woonpc.skin` | 设置皮肤 | op |
| `woonpc.action` | 管理动作 | op |
| `woonpc.hologram` | 管理全息图 | op |
| `woonpc.glowing` | 设置发光 | op |
| `woonpc.equipment` | 设置装备 | op |
| `woonpc.pose` | 设置姿势 | op |
| `woonpc.scale` | 设置缩放 | op |
| `woonpc.effect` | 设置效果 | op |
| `woonpc.reload` | 重载配置 | op |

## API 使用示例

```java
WooNPCAPI api = WooNPCAPI.getInstance();

// 创建 NPC
Npc npc = api.createNpc("test", player.getLocation());

// 设置显示名称（支持颜色代码）
npc.getData().setDisplayName("&a测试NPC");

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
