# Workbench 上传故障对照（2026-10-13）

用户要求定位失败并修复。不能把重复重连或“动态文件控件不兼容”的推测当成已修复结果。

## 已执行的对照

本机无敏感内容测试文件 `release/upload-diagnostic/probe.txt` 为 65 字节。测试页只显示文件名及大小，不上传内容；页面源码为忽略目录 `release/upload-diagnostic/index.html`。

| 测试 | 结果 |
| --- | --- |
| 普通页面上固定挂载的 input[type=file] | setFiles 成功，页面显示 probe.txt (65 bytes) |
| 普通页面通过 document.createElement 创建、未挂载的临时 input | setFiles 成功，页面显示 probe.txt (65 bytes) |
| Workbench 旧版 File Navigator /srv 上传同一个 probe.txt | filechooser 产生，但 setFiles 报 No node found for given backend id |
| Workbench 新旧版上传约 37 MB 发布包 | 同样错误，没有成功上传证据 |

结论：不是本地文件访问全面失效，也不能归因于所有未挂载动态 input 不受支持；Workbench 特定选择器与控制工具的衔接失败。小文件也复现，不能认为缩小包体一定能解决。当前不足以确定是 Workbench 控件生命周期还是工具处理方式，不能宣称根因或修复成功。

## 其他已核对边界

- Workbench 当前的隐藏 DOM input 挂载于 display:none 的 form 中，带 multiple；按钮产生的 filechooser 报 isMultiple=false。两者不是可直接等同的控件。这只是诊断线索，没有读取网页 JS 内部对象、修改第三方页面或修改控制工具源码。
- 尝试访问 chrome://extensions 被浏览器工具 URL 安全策略禁止，未绕过；用户之前已表示开启文件访问权限。无需再依据猜测让用户改权限。
- 官方文档搜索工具本轮返回错误；直接读取已知 OpenAI Chrome 扩展说明 URL 返回 403。未将未取到的文档作为根因依据。
- 本机诊断服务最初在受限环境监听但实际 Chrome 无法访问；随后在授权环境以回环地址 15180 启动，Chrome 成功读取诊断页。没有开放公网端口。
- setFiles 调用实际耗时明显超过所传 timeout：固定控件约 158 秒、临时控件约 357 秒、Workbench 小文件约 324 秒。不能把传入 timeout 当成实际硬截止时间，避免重复发起长时阻塞调用。

## 当前停点

代码及候选包仍未变更，生产未执行 prepare/candidate/publish。已请求用户明确指定使用 Windows 原生文件选择窗口；用户回答要求进一步解释和修复，尚未明确选择原生控制。遵守浏览器工具要求，不擅自切换其他控制技术。

后续可行路径：由用户明确要求 Windows 原生文件窗口后，用官方 computer-use 技能控制本地文件选择；或用户手动选择发布包。之后必须校验服务器文件哈希，按 records-progress-release-20261013.md 完成备份、隔离 MySQL 8.4 验证与发布。不要再次盲重试同一 Workbench setFiles。
