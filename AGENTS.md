# CloudMusic-Mod（MC 26.2 Fabric 移植）交接说明

> 给后续接手本仓库的 AI / 开发者的速查手册。**先读本文件，不要重新翻一遍全部源码。**
> 本仓库是把 CloudMusic-Mod（网易云音乐客户端 mod）从旧 MC 版本移植到 Minecraft 26.2 Fabric 的工作分支。

## 1. 现状

- 分支：`codex/mc-26.2`；远端 `origin` = `https://github.com/tomh500/CloudMusic-Mod.git`
- 移植进度：**基本可用**——播放、聊天指令、登录、二维码、HUD 封面/方框均正常。
- 刚修复的最后一个 bug：**HUD 右上角音乐信息面板只显示封面和方框、文字全部不显示**。
  根因：MC 26.2 的 `GuiGraphicsExtractor.text(...)` 在颜色 `ARGB.alpha==0` 时直接 return，而本 mod 所有文字颜色默认值都是 `#00...`（alpha=0）。
  修复：`Configs.java` 默认色改为 `#FF...`；`ConfigUtil.addConfigColor()` 无参默认值改为 `#FFFFFFFF`；`MusicHudRenderer.java` 新增 `forceOpaqueColor()` 兜底（防止用户旧配置文件里存的 `#00...` 值再次导致文字不显示）。
- 接下来：用户测试本修复 → 第一次 commit + push → 用户会另开新对话做优化。

## 2. 构建 / 部署 / 日志（必读）

- 构建：`.\gradlew.bat build --console=plain`（Java 25 / Loom 1.17 / Gradle 9.6.1）。`MusicPlayer.java` 有过时 API 警告、构建日志 `DELETE_CACHED_MAIN` 等警告，均无碍。
- 产物：`build\libs\cloudmusic-mod-26.2-0.6.0.jar`（版本号来自 `gradle.properties`：`mod_version=0.6.0`，`minecraft_version=26.2`）。
- 部署到测试实例：复制 jar 覆盖到 `C:\HMCL\.minecraftersions\Friends-OWN\mods\cloudmusic-mod-26.2-0.6.0.jar`。
- 测试环境：HMCL 实例 `Friends-OWN`（MC 26.2），mods 里有 malilib-fabric-26.2-0.29.3、Fabric API、liquidbounce-0.39.1、Iris/Sodium 等。
- 游戏日志：`C:\HMCL\.minecraftersions\Friends-OWN\logs\latest.log`
  - **编码是 GBK/GB2312**（中文显示为乱码是正常的，别误判为文件损坏）。
  - 游戏可能占用该文件：读的时候用 `FileShare.ReadWrite` 打开流。
  - 日志只显示 INFO 级：调试输出必须用 `LOGGER.info(...)`（代码里已保留 `[CloudMusic][...]` 前缀日志）。
- malilib 配置：`.minecraft\config\cloudmusic.json`。注意：旧配置文件里保存的 `#00...` 颜色值会覆盖代码里的新默认值，所以渲染层必须保留 `forceOpaqueColor()` 兜底。

## 3. MC 26.2 API 映射（移植踩坑记录，最重要）

26.2 使用 Mojang 官方映射（无 yarn），类名全部换过：

- `Text` → `Component`；`MutableText` → `MutableComponent`；`Formatting` → `ChatFormatting`；`MinecraftClient` → `Minecraft`；`ClientCommandManager` → `ClientCommands`。
- `ClickEvent` / `HoverEvent` 现在是接口：用 `new ClickEvent.SuggestCommand(String)`、`new ClickEvent.RunCommand(...)`、`new HoverEvent.ShowText(Component)`；`Style.withUnderlined(Boolean)`。
- 资源 ID：`Identifier.fromNamespaceAndPath(...)`（没有 `of` 静态方法）。
- HUD 渲染：用 Fabric API 的 `HudElement` + `HudElementRegistry.addLast(id, ...)`，回调签名 `extractRenderState(GuiGraphicsExtractor, DeltaTracker)`。旧版 `HudRenderCallback` / `InGameHudMixin` 已删除（见 `src/main/java/fengliu/cloudmusic/render/MusicHudRenderer.java`，这是 26.2 新增文件）。
- `GuiGraphicsExtractor.blit(id, x0, y0, x1, y1, u0, u1, v0, v1)`：**前 4 个 int 是两个对角点 (x0,y0)-(x1,y1)，不是 x/y/w/h**；UV 是 0..1 归一化。参数传错会导致图片乱跑/变形（二维码之前就是这么挂的）。
- `GuiGraphicsExtractor.text(...)`：**颜色 alpha==0 时直接 return，不渲染任何文字**。所有传给 `text()` 的颜色必须保证 alpha != 0。
- `DynamicTexture` 必须在渲染线程创建：包进 `Minecraft.getInstance().execute(...)`，否则报 `Render system called from wrong thread`（见 `MusicIconTexture.java`）。
- `SoundManager.play(...)` 返回 `SoundEngine.PlayResult`：mixin 必须用 `CallbackInfoReturnable<SoundEngine.PlayResult>`，取消时 `cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED)`（不能只 `cancel()`，否则崩）。
- malilib 26.2：`FileUtils` 返回 `Path`；`JsonUtils` 在 `fi.dy.masa.malilib.util.data.json`。
- 依赖仓库：`masa.dy.fi/maven/sakura-ryoko`（malilib）、`maven.fallenbreath.me/releases`（conditional-mixin-fabric）、`maven.terraformersmc.com`（modmenu）、jitpack。
- 版本：`minecraft=26.2`、`loader=0.19.3`、`fabric_api=0.153.0+26.2`（mods 目录里是 0.156.0+26.2）、`malilib=0.29.3`、`modmenu=20.0.0-beta.4`、Java release 25。

## 4. 代码结构速览

- `CloudMusicClient.java`：入口，注册指令、HUD（`MusicHudRenderer.register()`）、hotkey、输入。
- `command/MusicCommand.java`：全部 `/cloudmusic` 聊天指令；`runCommand` 在 `CloudMusic Thread` 异步线程执行，**跨线程发聊天消息必须用 `Minecraft.getInstance().execute(...)` 桥接**。
- `render/MusicHudRenderer.java`（新增）：HUD 三块——歌曲信息面板（封面+文字+进度条）、歌词、登录二维码。
- `render/MusicIconTexture.java`：封面 / 二维码 → `DynamicTexture` 注册（注意渲染线程）。
- `mixin/` 共 4 个：`ChatHudMixin`、`ClientPlayerEntityMixin`（附近怪物降音量）、`MinecraftClientMixin`（断线/退出停音乐）、`SoundSystemMixin`（播歌时屏蔽 MC 音乐）。
- `music163/`：网易云 API 封装（登录、搜索、歌单、评论等）；`LoginMusic163.java` 负责账号/二维码登录。

## 5. 已知问题与排查结论（别重复踩）

- 网易登录 `502` = 密码错误 / 风控，**不是代码 bug**；优先推荐二维码登录。
- 密码参数已改用 `StringArgumentType.greedyString()`（原来用 `string` 会把含空格/`?` 的密码截断）。
- 已修复的历史 bug：SoundSystemMixin 返回值导致崩溃；二维码 blit 参数顺序导致渲染乱跑；动态纹理线程问题；HUD 文字 alpha=0 不显示。

## 6. 本环境注意事项（给 AI 代理）

- **`apply_patch` 在本环境不可用（Access denied）**：改文件用 PowerShell + Python 管道（`@'...'@ | python -`）。
- 通过 PowerShell 管道传中文给 Python 前，必须先设 `$OutputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()`，否则中文会变成 `?`。
- 读写文件统一 UTF-8、无 BOM、LF 行尾；源码含中文注释。
- `build/`、`.gradle/` 已在 `.gitignore`，提交时不要包含它们。
- 本文件给下一个接手者省时间：接手新任务时先跑 `git status` + 读本文件，不要全文重读源码。
