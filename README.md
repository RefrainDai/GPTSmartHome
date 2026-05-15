# GPTSmartHome

可视化智能语音交互控制系统。项目采用 JavaFX 桌面端 + Python FastAPI 后端，所有设备均为虚拟设备，支持语音、手势、LLM 自然语言理解、TTS 语音回复、设备自定义音效和电视视频播放。

## 功能

- JavaFX 智能家居可视化界面：客厅、卧室、玄关、设备卡片、状态动画。
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
```

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

```powershell
conda activate gpt_smarthome
python -m uvicorn backend.app.main:app --host 127.0.0.1 --port 8000 --reload
```

或运行：

```powershell
.\scripts\run_backend.ps1
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

```powershell
conda activate gpt_smarthome
cd frontend-java
mvn javafx:run
```

或运行：

```powershell
.\scripts\run_frontend.ps1
```

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

- 手掌：打开客厅灯。
- 拳头：关闭客厅灯。
- 比耶：切换电视。
- 举手/单指：切换窗帘。

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
