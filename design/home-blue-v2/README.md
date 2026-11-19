# 首页组件稿 B / 02

本次只生成设计与素材，未修改 Android 运行代码。设计基准 390 × 844；尺寸 dp，文字 sp。

- components.png：完整首页与组件尺寸、字体规范。
- home.png：首页单独预览。
- index.html：可重复渲染的排版源文件。
- assets/hero-rabbit.png：内置 imagegen 生成的透明兔子叠卡插画，原图直接用于后续接入。
- assets/*.svg：设置、继续扫描、入口箭头及底栏图标源文件。
- assets/Manrope.ttf 与 NotoSansSC.ttf：开源字体，许可见同目录 OFL 文件。

## 规格

| 元素 | 规格 |
|---|---|
| 页面边距 | 16 |
| 顶栏 | 高 68；设置视觉 25，触摸范围 44 |
| 品牌页标题 | Noto Sans SC 26 / 800 |
| Hero | 高 176，圆角 16，独立页面网格 |
| 英文标题 | Manrope 35 / 800；行高 38.15；两行；字距 -1.3 |
| Hero 副标题 | Noto Sans SC 14 / 650 |
| 兔子插画 | 154 × 154，等比显示 |
| 平台入口 | 高 86；圆角 15；内边距 16；间距 12 |
| 平台图标 | 官方原色 52 × 52，不使用主题着色 |
| 平台标题 | Noto Sans SC 20 / 750，行高 27 |
| 平台说明 | Noto Sans SC 14 / 400，行高 20 |
| 继续扫描 | 高 72；时钟图标 28；标题 17 / 700；说明 13 / 400 |
| 底栏 | 高 68＋系统手势区域；图标 24；文字 12；选中浅蓝背景＋实心蓝图标 |

品牌主色 #456BFF，标题与导航强调色 #315AFF，主文字 #101728，次文字 #596274，背景 #F5F7FC，边框 #D6DCE8。

## 官方品牌图标来源

- 抖音：App Store 官方应用元数据 https://itunes.apple.com/lookup?id=1142110895&country=cn 的 artworkUrl512。
- 小红书：官网 https://www.xiaohongshu.com/ 的 apple-touch-icon：https://picasso-static.xiaohongshu.com/fe-platform/f43dc4a8baf03678996c62d8db6ebc01a82256ff.png
- douyin-official.ico/png 为官网 favicon 备用素材，设计稿实际使用更清晰的 douyin-official-app.png。
- 原图标保留原色及比例，不使用 AI 生成品牌图标。

## 插画生成

模式：内置 image_gen；参考：上一版 B 方向左侧首页的兔子叠卡。

Prompt:
Create ONE isolated production illustration asset for Xiuyi extraction Android HOME hero, using the blue rabbit sticker stack in the LEFT HOME SCREEN of reference as the strict design reference. Transparent background, no text no page no grid. Three slightly rotated overlapping physical sticker cards, front card tilted clockwise ~9deg, pale cream white thick rounded border, thin midnight navy outline, vivid electric blue #456BFF inner fill. A cheerful expressive WHITE RABBIT bust inside front card, long ears with pink inner ears, black hand drawn playful facial lines, eyes closed in a happy laughing expression, little open pink mouth, tiny rosy cheeks, two raised front paws, cute sticker style. Back two cards offset right-down pale periwinkle and blue with white outlines. Small 3 blue excitement strokes top left outside cards. Clean polished flat sticker vector aesthetic with subtle soft shadow, NOT a bland geometric bunny head, NOT 3D. Preserve reference silhouette and hand drawn personality. Full artwork fits centered with 8% transparent margin. Square 1024px.

## 检查

已通过本地 Chrome 渲染并检查首页及组件板，品牌图标、字体文件和透明插画均为本地素材。截图中评论数量为示例。后续接入须使用以上原始素材与字体，不再次以几何绘制替代插画。

## 4.0.1 接入

已将透明兔子原图、官方品牌图标、Manrope 与 Noto Sans SC 字体接入首页。Android 通过 wght 轴应用可变字体字重；首页标题 800、入口标题 750、继续扫描 700。同步调整卡片尺寸、设置/继续扫描/箭头及选中首页图标。已构建 debug APK 并验证签名，使用模拟器验证首页、两平台入口和小屏显示。实际截图位于 android/app/build/qa/home-4.0.1-*.png。
