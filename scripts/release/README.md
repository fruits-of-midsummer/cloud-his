# 云医院 HIS 运行包

你现在打开的是已经构建好的运行包。只需 **Java 17 或更新版本**；启动所需文件已包含在本目录。

## 启动

将 ZIP 完整解压到可写的文件夹，双击 **启动.cmd**（或 `run.cmd`），等待服务就绪后，浏览器会自动打开：

http://127.0.0.1:8080

在 PowerShell 中也可以从本目录执行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Start.ps1
```

首次登录账号：`admin`，开发初始密码：`His@2026!!`。也可使用下表中的岗位账号，初始密码相同；已修改过的密码不会因重启而重置。

| 账号 | 岗位 |
| --- | --- |
| admin | 系统管理员 |
| cashier | 挂号收费 |
| doctor / doctor2 | 门诊医生 |
| check / lab / treat | 检查、检验、处置 |
| pharmacy | 药房药师 |

## 停止与数据

双击 **停止.cmd**，或在 PowerShell 执行 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Stop.ps1`。

患者、病历和费用记录保存在本目录的 `data/` 中。重启会保留数据；备份前先停止服务，再复制整个 `data/`。日志位于 `.runtime/server.out.log` 和 `.runtime/server.err.log`。

## 其他运行方式

如果希望在当前终端直接运行，可以执行：

```powershell
java -jar .\cloud-his-1.0.0.jar
```

等待启动后自行打开浏览器，按 Ctrl+C 停止。若已通过“启动.cmd”启动，应先通过“停止.cmd”结束该实例，再使用此方式。

## 运行包与源码工程

本包的文件包括 `cloud-his-1.0.0.jar`、`Start.ps1`、`Stop.ps1`、“启动.cmd”、“停止.cmd”、`run.cmd` 及说明文档。

`Build.ps1` 是源码工程的构建脚本，位于源码 `cloud-his` 根目录，运行本包不需要执行它。若要修改代码或重新构建，请打开含 `backend/`、`frontend/` 和 `Build.ps1` 的源码目录。

业务流程与范围见 `docs/需求实现对照.md`，数据库部署见 `docs/部署说明.md`，已完成的开发环境验证见 `docs/验证记录.md`。
