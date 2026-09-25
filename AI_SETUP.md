# EduMix AI 配置

AI 页面只保留两个能力：平台资源/案例推荐、教学案例对话整合。未配置密钥时，后端仍可正常启动，AI 页面会显示“尚未配置”。

## 在 VS Code PowerShell 终端中启动

```powershell
$env:AI_CHAT_PROVIDER = "openai"
$env:AI_API_KEY = "在这里填写你的服务端 API Key"
$env:AI_BASE_URL = "https://api.openai.com"
$env:AI_MODEL = "gpt-4o-mini"
Set-Location "E:\38-bishe\ruanjian\mixin-back\edumix"
mvn spring-boot:run
```

API Key 只应配置在后端环境变量中，不能写入 Vue 前端，也不要提交到 Git。

## OpenAI 兼容服务

如果使用其他兼容 OpenAI Chat Completions 的服务，修改 `AI_BASE_URL` 和 `AI_MODEL` 即可；接口路径不是 `/v1/chat/completions` 时，再设置：

```powershell
$env:AI_CHAT_PATH = "/服务商提供的接口路径"
```

这些环境变量在 `mixin-back/edumix/src/main/resources/application.yaml` 中读取。修改后需要重启 Spring Boot。

## 接口

- `GET /api/ai/status`：检查是否已配置。
- `POST /api/ai/recommend`：从当前账号有权访问的资源和案例中推荐。
- `POST /api/ai/chat`：围绕所选资料对话，并可生成案例草稿。

两个生成接口仅允许教师和管理员访问。推荐结果返回前会再次校验资源/案例 ID，模型不能把不存在的内容带入结果。
