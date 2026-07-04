# ZrLog-Base

`zrlog-base` 是 ZrLog 多仓库体系里的共享基础库，提供通用响应结构、异常、工具、数据模型、公共服务、模板对象、插件宿主侧协议和后台 token 基础能力。

面向 AI Agent 和跨仓库开发的入口见 [AGENTS.md](AGENTS.md)。修改 public API、DTO、model、插件宿主协议或 native/Gson 注册时，先确认下游消费者，再运行对应验证。

## 模块

- data
- service
- common
- admin-token
- template
