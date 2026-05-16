# GPTSmartHome

可视化智能语音交互控制系统。项目采用 JavaFX 桌面端 + Python FastAPI 后端，所有设备均为虚拟设备，支持语音、手势、LLM 自然语言理解、TTS 语音回复、设备自定义音效和电视视频播放。

## 功能

- JavaFX 智能家居可视化界面：客厅、卧室、玄关、设备卡片、状态动画。
- 手势识别预览：实时显示当前手势、识别区域框和手指关键点连线。
- 摄像头画面预览：启动手势后显示笔记本摄像头实时画面，并在画面上叠加识别框和手部骨架。
- 语音输入可视化：启动监听后显示麦克风状态、实时音量、语音波形和频谱图。
- FastAPI 后端：统一设备状态中心、REST API、WebSocket 实时推送。
- 语音模块：麦克风录音、带通滤波、FFT 频谱、中文语音识别、异常容错。
- 手势模块：摄像头 + MediaPipe，支持手掌、拳头、比耶、举手手势。
- LLM 管家：可接入 OpenAI 兼容 API，将模糊自然语言解析为安全白名单指令。
- TTS 回复：默认 `edge-tts`，可切换 `pyttsx3` 或关闭。
- 用户资源：设备开关音效放入 `assets/sounds/`，电视视频放入 `assets/videos/`。
- 扩展功能：声纹校验、定时任务、观影/睡眠/回家/离家场景模式。

## 环境

本项目已按要求创建 Conda 环境：

```powershell
conda activate gpt_smarthome
```

如需重新创建：

```powershell
conda env create -f environment.yml
```

## 配置

复制 `.env.example` 为 `.env`，然后按需填写：

```powershell
Copy-Item .env.example .env
```

LLM API 使用 OpenAI 兼容格式：

```env
LLM_ENABLED=true
LLM_API_KEY=your_api_key
LLM_BASE_URL=https://your-provider.example/v1
LLM_MODEL=your-model-name
LLM_AUTH_HEADER=authorization
```

如果使用 `xiaomiTokenPlan` 的 OpenAI 兼容协议，可以配置为：

```env
LLM_ENABLED=true
LLM_API_KEY=你的_tp_xxxxx_TokenPlan_Key
LLM_BASE_URL=https://token-plan-cn.xiaomimimo.com/v1
LLM_AUTH_HEADER=api-key
LLM_MODEL=mimo-v2-flash
```

Token Plan 官方文档还列出新加坡和欧洲集群：

```env
LLM_BASE_URL=https://token-plan-sgp.xiaomimimo.com/v1
LLM_BASE_URL=https://token-plan-ams.xiaomimimo.com/v1
```

注意：Token Plan 的 Key 通常是 `tp-xxxxx`，按量付费 Key 通常是 `sk-xxxxx`，两者不能混用。模型名请以控制台显示为准；官方文档列出的常见聊天模型包括 `mimo-v2-flash`、`mimo-v2.5`、`mimo-v2.5-pro`。

不要把 `.env` 提交到 Git。

## 自定义音效

把自己的音频放到：

```text
assets/sounds/
```

默认文件名在 `config/media-map.json` 中，例如：

```text
light_on.mp3
tv_on.mp3
curtain_open.mp3
movie_mode.mp3
```

如果文件不存在，前端只会在日志中提示，不会中断系统。

## 自定义电视视频

把自己的视频放到：

```text
assets/videos/
```

默认电视开机视频：

```text
tv_demo.mp4
```

推荐格式：MP4 + H.264 + AAC。

## 启动后端

PowerShell 推荐方式：

```powershell
conda activate gpt_smarthome
python -m uvicorn backend.app.main:app --host 127.0.0.1 --port 8000 --reload
```

或运行：

```powershell
.\scripts\run_backend.ps1
```

如果你使用的是 CMD，可以运行：

```cmd
scripts\run_backend.cmd
```

健康检查：

```text
http://127.0.0.1:8000/health
```

API 文档：

```text
http://127.0.0.1:8000/docs
```

## 启动前端

另开一个终端：

PowerShell 推荐方式：

```powershell
conda activate gpt_smarthome
cd frontend-java
mvn javafx:run
```

或运行：

```powershell
.\scripts\run_frontend.ps1
```

如果你使用的是 CMD，可以运行：

```cmd
scripts\run_frontend.cmd
```

不要在 `scripts` 目录里直接粘贴 `mvn -f frontend-java/pom.xml ...`，因为这个相对路径是按项目根目录计算的。脚本本身已经会自动定位项目根目录。

## 设备诊断

如果摄像头或麦克风没有反应，先点击前端的 `检测设备`，或直接访问：

```text
http://127.0.0.1:8000/api/system/diagnostics
```

诊断会列出可打开的摄像头索引、麦克风输入设备、OpenCV 版本和 MediaPipe 版本。手势识别依赖 `mediapipe==0.10.21`，这个版本包含 `mp.solutions.hands`。

## 演示指令

可以在 JavaFX 输入框中输入：

```text
打开客厅灯
关闭风扇
打开电视
进入观影模式
我要睡觉了
我有点热
太暗了
我回来了
```

## 语音指令清单

本地关键词解析支持以下固定说法。开启 LLM 后，可以使用更自然的表达，系统会尽量转换成同样的设备动作。

设备别名：

| 设备 | 可说的名称 |
|---|---|
| 客厅主灯 | 客厅灯、主灯、灯光、灯 |
| 卧室灯 | 卧室灯、床头灯 |
| 空调 | 空调、冷气、暖气 |
| 窗帘 | 窗帘、帘子 |
| 电视 | 电视、电视机、屏幕 |
| 风扇 | 风扇、电扇 |
| 加湿器 | 加湿器、加湿 |
| 门锁 | 门锁、锁 |
| 扫地机器人 | 扫地机器人、机器人、扫地 |

基础动作：

| 动作 | 可说的关键词 | 示例 |
|---|---|---|
| 打开 | 打开、开启、启动、开一下、解锁 | 打开客厅灯、启动风扇、解锁门锁 |
| 关闭 | 关闭、关掉、停止、关上、锁门、上锁 | 关闭电视、停止扫地机器人、锁门 |
| 切换 | 切换、反转 | 切换窗帘、切换电视 |
| 调温 | 温度、调到、调成、度 | 空调调到 24 度、把空调温度调成 26 度 |
| 调档/亮度 | 亮度、档位、风速、调亮、调暗 | 客厅灯亮度 80、风扇档位 60 |

场景模式：

| 说法 | 效果 |
|---|---|
| 进入观影模式、看电影、电影模式、影院 | 调暗客厅灯、关闭窗帘、打开电视、打开风扇低档 |
| 我要睡觉了、晚安、睡眠、休息 | 关闭客厅灯和电视，卧室灯低亮，空调调到 26 度 |
| 我回来了、回家、到家 | 解锁门锁、打开客厅灯、打开窗帘、空调调到 24 度 |
| 我要出门、离家、不在家 | 关闭主要设备并上锁 |

模糊语义：

| 说法 | 效果 |
|---|---|
| 太暗了、好暗、看不清 | 打开客厅灯 |
| 有点热、太热、好热 | 打开空调并调到 24 度 |
| 有点冷、太冷、好冷 | 空调调到 27 度 |

## 手势映射

启动手势后，JavaFX 右侧会显示“手势识别预览”：

- 摄像头实时画面会显示在预览框中。
- 当前手势名称会实时刷新。
- 虚线框表示摄像头识别区域。
- 青色实线框表示当前检测到的手部范围。
- 青色线条和绿色点用于勾勒手指与手掌关键点。

- 手掌：打开客厅灯。
- 拳头：关闭客厅灯。
- 比耶：切换电视。
- 举手/单指：切换窗帘。

## 卡顿排查

如果启动语音或手势后明显卡顿，优先检查这些配置：

```env
GESTURE_FRAME_WIDTH=424
GESTURE_FRAME_HEIGHT=240
GESTURE_PROCESS_INTERVAL_SECONDS=0.12
GESTURE_PREVIEW_INTERVAL_SECONDS=0.20
GESTURE_USE_OPENCL=true
GESTURE_VIDEO_PREVIEW_ENABLED=true
GESTURE_VIDEO_INTERVAL_SECONDS=0.30
GESTURE_VIDEO_JPEG_QUALITY=55
```

含义：

- `GESTURE_FRAME_WIDTH` / `GESTURE_FRAME_HEIGHT`：摄像头处理分辨率，越高越吃 CPU。
- `GESTURE_PROCESS_INTERVAL_SECONDS`：后端手势识别间隔，`0.12` 约等于每秒 8 帧。
- `GESTURE_PREVIEW_INTERVAL_SECONDS`：前端手势预览刷新间隔，`0.20` 约等于每秒 5 帧。
- `GESTURE_USE_OPENCL`：启用 OpenCV OpenCL 做摄像头帧缩放、翻转、颜色转换；MediaPipe Python 仍主要使用 CPU。
- `GESTURE_VIDEO_PREVIEW_ENABLED`：是否向前端发送摄像头预览画面。
- `GESTURE_VIDEO_INTERVAL_SECONDS`：摄像头画面刷新间隔，越小越流畅但更占网络和 CPU。
- `GESTURE_VIDEO_JPEG_QUALITY`：摄像头预览 JPEG 质量，越高越清晰但更占带宽。

如果电脑性能较弱，可以改成：

```env
GESTURE_FRAME_WIDTH=320
GESTURE_FRAME_HEIGHT=180
GESTURE_PROCESS_INTERVAL_SECONDS=0.18
GESTURE_PREVIEW_INTERVAL_SECONDS=0.30
GESTURE_USE_OPENCL=true
GESTURE_VIDEO_PREVIEW_ENABLED=true
GESTURE_VIDEO_INTERVAL_SECONDS=0.35
GESTURE_VIDEO_JPEG_QUALITY=50
```

如果 GPU 已被游戏、录屏或浏览器大量占用，OpenCL 不一定更快；这时可以改成 `GESTURE_USE_OPENCL=false`。

前端脚本会自动给 JavaFX 设置 Direct3D 渲染参数：

```text
-Dprism.order=d3d -Dprism.vsync=true
```

这会优先使用 Windows 上的 GPU 图形管线渲染界面动画。

如果只是想演示普通设备控制，先不要点击 `启动手势` 和 `开始监听`，手动输入指令会最流畅。

## 目录结构

```text
backend/             Python FastAPI 后端
frontend-java/       JavaFX 前端
assets/sounds/       用户自定义设备音效
assets/videos/       用户自定义电视视频
assets/tts/          后端生成的 AI 管家语音
config/              音效/视频映射配置
docs/                题目文档、设计报告、实验报告
scripts/             启动脚本
```

## GitHub

远程仓库已绑定：

```text
https://github.com/RefrainDai/GPTSmartHome.git
```

首次提交后，如需推送：

```powershell
git push -u origin main
```
