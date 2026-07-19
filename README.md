# 简约影视 (TvBox-like Android TV App)

一个类 TVBox 的 Android TV 影视客户端，**UI 简约**、**兼容 TVBox 配置（config.json）**、
播放使用 **Media3 ExoPlayer**。支持标准苹果 CMS（`type` 1~3）HTTP JSON 源，以及
通过 **Rhino** 运行时执行 drpy / spider（`type` 0 / 4，或 `.js` 后缀）JS 源。

## 功能

- 浏览首页：最近更新 + 各分类横向卡片行（D-pad 导航）
- **翻页加载**：横向列表滚到末尾自动加载下一页（CMS 与 JS 源均支持）
- 详情页：海报 / 简介 / 多播放线路（线路切换）/ 选集
- 搜索：按片名检索，结果同样支持**翻页加载**
- 设置：粘贴 TVBox 配置地址加载、切换默认影视源（配置本地持久化）
- 播放：ExoPlayer 直接播放 m3u8 / mp4 直链，遥控器返回退出
- **JS 源**：内置 Rhino 引擎运行 drpy/spider 脚本，覆盖 `request/base64/pdfh/pdfa` 等常用 API

## 在本地生成 APK

> 本仓库已附带 Gradle Wrapper（`gradlew` / `gradlew.bat` / `gradle-wrapper.jar`），
> 只需装好 JDK 与 Android SDK，无需单独安装 Gradle。

### 前置条件
- **JDK 17**（推荐 Android Studio 自带）
- **Android SDK**（装 Android Studio 即包含），并在 SDK Manager 中安装：
  - Android SDK Platform 34
  - Android SDK Build-Tools 34.x
  - Android SDK Platform-Tools
- 配置环境变量（或在 Android Studio 的 Terminal 中操作）：
  ```bat
  set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
  set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
  ```

### 生成调试版 APK（可直接安装，无需签名）
在项目根目录 `TvBoxApp` 执行：
```bat
gradlew.bat assembleDebug
```
产物：`app\build\outputs\apk\debug\app-debug.apk`
安装到已开启 USB 调试的 TV/盒子：
```bat
%ANDROID_HOME%\platform-tools\adb install app\build\outputs\apk\debug\app-debug.apk
```

### 生成发布版 APK（需签名）
1. 生成签名密钥库（仅需一次）：
   ```bat
   %JAVA_HOME%\bin\keytool -genkey -v -keystore release.keystore ^
     -alias tvapp -keyalg RSA -keysize 2048 -validity 10000
   ```
2. 在 `app/build.gradle.kts` 的 `android { }` 内加入（切勿提交真实密码）：
   ```kotlin
   signingConfigs {
       create("release") {
           storeFile = file("../release.keystore")
           storePassword = "你的密钥库密码"
           keyAlias = "tvapp"
           keyPassword = "你的密钥密码"
       }
   }
   buildTypes {
       release {
           isMinifyEnabled = false
           signingConfig = signingConfigs.getByName("release")
       }
   }
   ```
3. 构建：
   ```bat
   gradlew.bat assembleRelease
   ```
   产物：`app\build\outputs\apk\release\app-release.apk`

> 也可直接用 Android Studio：菜单 **Build ▸ Generate Signed Bundle / APK**，按向导选 APK 与密钥库即可。

### 通过 GitHub Actions 云端构建（无需本地装环境）
工程已包含 `.github/workflows/build.yml`。把代码推到 GitHub 后：
1. 仓库 **Actions** 页会显示 **Build APK** 工作流；也可在 **Actions ▸ Build APK ▸ Run workflow** 手动触发。
2. 工作流自动安装 JDK 17 + Android SDK（platform 34 / build-tools 34.0.0），运行 `assembleRelease`，
   再用 `apksigner` 以 CI 生成的密钥库对 APK 签名。
3. 运行完成后在 **Artifacts** 里下载 `tvbox-app-release`（即 `app-release.apk`），可直接安装到 TV/盒子。

> 说明：CI 每次运行用新密钥库签名，因此升级安装前需先卸载旧版本；如需稳定签名，
> 可在 `build.yml` 里改用仓库 Secrets 中固定的密钥库（读取为环境变量传入 `apksigner`）。

## 使用步骤

1. 打开 App → 右上角「设置」→ 粘贴一个 TVBox `config.json` 的网址 → 点「加载」。
2. 加载成功后从「选择默认影视源」里选一个源（CMS 或 JS 源均可）→ 点「保存并进入」。
3. 回到首页即可浏览；横向列表滚到末尾会自动翻页加载；选片子看详情、选集播放；右上「搜索」可搜片名。

## 支持的配置格式（节选）

```json
{
  "sites": [
    {
      "key": "cms_demo",
      "name": "CMS 演示源",
      "type": 1,
      "api": "https://你的影视站/api.php",
      "searchable": 1
    },
    {
      "key": "js_demo",
      "name": "JS/drpy 演示源",
      "type": 3,
      "api": "https://你的js源/spider.js",
      "searchable": 1
    }
  ],
  "rules": { "ua": "Mozilla/5.0", "host": [], "cookies": {} }
}
```

- **CMS 源判定**：`type` 为数字 1~3，或字符串 `cms/json/苹果cms/maccms`。
- **JS 源判定**：`type` 为数字 0 / 4，或字符串 `js/drpy/spider/csp/python`，或 `api` 以 `.js` 结尾。

### CMS 接口约定（苹果 CMS 标准）
- 分类：`{api}?ac=list`
- 列表：`{api}?ac=videolist&t={分类id}&pg={页}`
- 详情：`{api}?ac=detail&ids={片id}`
- 搜索：`{api}?wd={关键词}&pg={页}`

播放地址从 `vod_play_from` / `vod_play_url` 解析（以 `$$$` 分线路、`#` 分集、`$` 分「名称|地址」）。

### JS/drpy 源
通过 Rhino 运行脚本中的 `homeContent / categoryContent / detailContent / searchContent`，
并注入 `request / base64Encode / base64Decode / pdfh / pdfa` 等桥接 API。
对以 JSON 接口为主的 drpy 源兼容性较好；重度依赖 HTML 选择器的 spider 可能不完全兼容，
失败时 UI 会提示「该源暂未完全支持」。

## 目录结构

```
app/src/main/
  java/com/example/tvapp/
    App.kt
    data/
      ConfigManager.kt    # 配置加载 / 持久化 / Spider 选择
      Spider.kt           # 影视源统一接口（含翻页返回值）
      model/              # Config / Vod / 领域模型
      remote/
        CmsSpider.kt      # 苹果 CMS 源实现
        JsSpider.kt       # drpy/spider JS 源实现（Rhino）
        RetrofitClient.kt # 全局 Retrofit 单例
    ui/                   # MainActivity / Details / Search / Settings / Player
    ui/adapter/           # 卡片 / 行(翻页) / 选集 适配器
  res/                    # 布局 / 主题 / 字符串 / banner
```

## 已知限制 / 后续可扩展

- 部分源返回的是「解析页」而非直链，需接入 `parses` 解析（未实现，直链会直接交给 ExoPlayer）。
- JS 源为尽力而为的 Rhino 运行时，极个别依赖特殊语法的 spider 可能不兼容。
- 未做收藏、历史记录、清晰度切换。
- 焦点滚动在超长列表边缘可能需微调。
