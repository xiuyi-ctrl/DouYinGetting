# 重构 3.0 · 界面稿

两块内容：

1. **首页**：完全保留现有布局（212dp 主视觉卡 + 双列 178dp 功能卡 + 96dp 底部导航与悬浮首页按钮），只替换插画与图标素材。
2. **读取页 / 扫描记录**：工具型重构稿（配色收敛、描边代替阴影、素材优先），作为后续可选的改版方向。

## 首页：只换美术资源

| 资源 | 源文件 | 导出尺寸 | 替换到 |
|---|---|---|---|
| `hero_art.png` | `assets/hero.svg` | 700×840 | `android/app/src/main/res/drawable-nodpi/` |
| `douyin_feature.png` | `assets/douyin-feature.svg` | 304×304 | 同上 |
| `xhs_feature.png` | `assets/xhs-feature.svg` | 304×304 | 同上 |
| `douyin_nav.png` | `assets/douyin-nav.svg` | 160×160 | 同上 |
| `xhs_nav.png` | `assets/xhs-nav.svg` | 160×160 | 同上 |
| `home_nav.png` | `assets/home-nav.svg` | 288×288 | 同上 |

可直接取用的文件在 `android/`，文件名与 `res/drawable-nodpi` 里的现有文件一一对应，覆盖即可，代码不用改。

原尺寸参考（替换前的值）：`hero_art` 425×420、`douyin_feature` / `xhs_feature` 159×152、`douyin_nav` / `xhs_nav` 80×80、`home_nav` 160×160。新文件按 2~4 倍出图，显示尺寸仍由布局里写死的 dp 决定，不会变形。

新素材的取向：低饱和渐变 + 单一白色图形，去掉原来的高饱和拼贴感；首页配色、字号、间距、圆角、卡片底色全部沿用原稿。

## 文件

| 文件 | 说明 |
|---|---|
| `home.html` | 首页：原布局 + 新素材 |
| `reader-scan.html` | 读取页（扫描中） |
| `reader-results.html` | 读取页（结果） |
| `records.html` | 扫描记录 |
| `tokens.css` | 读取页 / 记录页的设计令牌 |
| `assets/*.svg` | 素材源文件（可编辑） |
| `assets/render-*.html` | 出图用的包装页，由脚本生成 |
| `png/` | 全部渲染结果（`home.png` 等，2 倍图） |
| `android/` | 可直接覆盖到 `res/drawable-nodpi/` 的素材 |
| `probe.js` | 版式探针：溢出量、越界元素、图片加载、区块高度 |
| `render.ps1` | 渲染四屏 + 总览 |
| `render-assets.ps1` | 把 `assets/*.svg` 导出成各尺寸 PNG |
| `preview.html` | 四屏并排总览 |

## 出图

```powershell
powershell -File design\redesign-3.0\render-assets.ps1     # 素材
powershell -File design\redesign-3.0\render.ps1            # 四屏 + 总览
powershell -File design\redesign-3.0\render.ps1 -Only home # 只重出首页
```

`render.ps1` 每屏先跑探针（`--dump-dom` 读 `<title>`），输出
`overflow=0 wide=0 imgs=5 broken=0 | section.hero=212 h2.section-title=26 ...`，
不用看图就能发现版面溢出、元素越界和图片加载失败。

注意：这台机器上的 headless Chrome 会把窗口宽度钳制到约 500px，所以
`render-assets.ps1` 采用「固定像素摆放 + 精确裁剪」的方式出图，避免缩放误差。

## 读取页改了什么（可选方向）

- 一个主色 `#14806B`，平台色降为图标底色 12% 透明。
- 字号 28/20/16/14/12 五档；卡片白底 + 1px 描边，去掉阴影。
- 缩略图铺满、去掉粉蓝衬底与「图片」角标；筛选改 segmented control。
- 扫描卡由浅绿整块改为白底 + 主色竖条 + 细进度条。

## 已接入代码

四个界面已落到 `MainActivity.java`，构建并在模拟器（Android 16 / 1080×2400）上验证通过，四屏截图见 `png/device-*.png`。

- **设计令牌**：`SURFACE / LINE / INK / INK2 / INK3 / ACCENT / ACCENT_SOFT / TRACK / DANGER`，配套 `card(radius)`、`outlined(radius, color)`、`segment(text, type, parent)` 三个通用构造。
- **首页**：布局、配色、字号全部不变，只替换 6 个 drawable。
- **读取页**：54dp 顶栏（方角返回 + 记录胶囊）、白卡描边输入区、扫描状态卡（主色竖条 + 3dp 进度 + 模式提示）、网页区白卡、segmented 筛选、铺满的三列网格（仅动图 / 视频有角标）、带 1px 分隔线的保存条。
- **扫描记录**：顶栏 + 条数胶囊、白卡信息行（平台图标 + 链接 + 进度 + 状态）、分隔线下的三个操作。
- **行为未改**：扫描、翻页重试、记录持久化、保存、预览、小红书注入逻辑均未改动。
- **已知差异**：设计稿里输入框的两个图标按钮用「粘贴 / 复制」文字按钮实现，没有引入矢量图标资源。
- **修掉的问题**：扫描卡竖条最初用 `MATCH_PARENT`，在 `ScrollView` 的 `AT_MOST` 约束下把卡片撑到 1194px，导致下方内容被挤出屏幕；改为固定高度竖条 + 横向 `LinearLayout`。

## 落地建议

1. 首页：只替换 `res/drawable-nodpi` 里的 6 个 PNG，先看真机效果。
2. 读取页：抽 `Ui.java` 集中颜色/字号/圆角/间距，再分批替换 `MainActivity` 里的 `Color.rgb(...)` 与魔法数。
3. 图标统一用 VectorDrawable（现在 `⚙`、`↑`、`‹` 是文字字形，与 PNG 图标不是一套语言）。