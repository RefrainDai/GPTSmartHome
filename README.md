# GPTSmartHome

可视化智能语音交互控制系统。项目采用 JavaFX 桌面端 + Python FastAPI 后端，所有设备均为虚拟设备，支持语音、手势、LLM 自然语言理解、TTS 语音回复、设备自定义音效和电视视频播放。

## 功能

- JavaFX 智能家居可视化界面：客厅、卧室、玄关、设备卡片、状态动画。
- 手势识别预览：实时显示当前手势、识别区域框和手指关键点连线。
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

## 手势映射

启动手势后，JavaFX 右侧会显示“手势识别预览”：

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
```

含义：

- `GESTURE_FRAME_WIDTH` / `GESTURE_FRAME_HEIGHT`：摄像头处理分辨率，越高越吃 CPU。
- `GESTURE_PROCESS_INTERVAL_SECONDS`：后端手势识别间隔，`0.12` 约等于每秒 8 帧。
- `GESTURE_PREVIEW_INTERVAL_SECONDS`：前端手势预览刷新间隔，`0.20` 约等于每秒 5 帧。

如果电脑性能较弱，可以改成：

```env
GESTURE_FRAME_WIDTH=320
GESTURE_FRAME_HEIGHT=180
GESTURE_PROCESS_INTERVAL_SECONDS=0.18
GESTURE_PREVIEW_INTERVAL_SECONDS=0.30
```

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
