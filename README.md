## 简介

在搭建家庭 NAS 的过程中，Nginx 通常被用作反向代理，为所有后端服务提供统一入口。但在实际部署中，我遇到了一些问题：

1. 某些服务使用了较为特殊的认证协议，必须为 Netty 集成相应的认证插件才能正常访问；
2. 虽然运营商提供了公网 IP，但封锁了常用的 443 和 8443 端口。每次访问 NAS 服务都需要手动指定端口，使用不便。虽然可以借助 Cloudflare 解决端口限制，但其转发过程会显著影响访问速度。

为了解决上述问题，我开发了一个基于 Netty 的轻量代理工具，作为 Nginx 的替代方案。该工具通过在客户端与后端服务之间建立 TCP 隧道，绕过了协议兼容性限制（解决问题 1），同时支持 Shadowsocks 协议，实现访问流量的加密，保障用户隐私安全。此外，工具内置了域名映射功能，可将自定义域名请求转发至指定后端服务，从而避开端口封锁问题（解决问题 2）。

该工具具备以下特性：

* 入站协议支持 HTTP、SOCKS 和 Shadowsocks，出站支持智能域名解析，并可判断流量是否指向国内；
* 支持通过 WOL 协议远程唤醒离线设备；
* 提供基础的监控与统计功能，覆盖硬盘健康状态、内存占用和流量信息。

下图为我自己部署的一个大致架构：

![image](https://github.com/user-attachments/assets/8a7a57c9-1857-429f-aa15-7bb70dbe2088)

## 代理配置说明
config.json配置文件包含两个核心部分：

* `inbounds`: 定义服务端监听的入口（如 Socks、HTTP、Shadowsocks）
* `outbounds`: 定义请求出去的方式（如直连、通过 SOCKS 代理等）
### 📥 `inbounds` - 入站配置（监听本地端口，接收代理请求）

#### 1. SOCKS 代理

```json
{
  "tag": "socks",
  "enable": false,
  "port": 36172,
  "listen": "0.0.0.0",
  "protocol": "socks",
  "settings": {
    "password": "xxxx"
  },
  "user": "xxxx"
}
```

* **tag**: 唯一标识该入站配置。
* **enable**: 是否启用该入站服务（此处为 false，即未启用）。
* **port**: 监听的端口号（36172）。
* **listen**: 监听地址，`0.0.0.0` 表示所有网卡地址。
* **protocol**: 使用 SOCKS 协议。
* **settings.password**: 设置密码认证（提高安全性）。
* **user**: 标记该服务对应的用户。

#### 2. HTTP 代理

```json
{
  "tag": "http",
  "enable": false,
  "port": 36171,
  "listen": "0.0.0.0",
  "protocol": "http",
  "settings": {},
  "user": "xxxx"
}
```

* 与上面的 SOCKS 类似，只是协议为 HTTP，未启用。

#### 3. Shadowsocks

```json
{
  "tag": "shadow_socks",
  "enable": true,
  "port": 36170,
  "listen": "0.0.0.0",
  "protocol": "shadow_socks",
  "settings": {
    "method": "chacha20_poly1305",
    "password": "user_1_password"
  },
  "user": "user_1"
}
```

* **protocol**: 使用 `shadow_socks` 协议（轻量代理，支持加密）。
* **method**: 加密算法 `chacha20_poly1305`（高安全性与性能），同时支持aes_256_gcm,aes_128_gcm算法。
* **password**: 密码。
* **user**: 归属用户标识，需要保证唯一。


### 📤 `outbounds` - 出站配置（转发出站流量的方式）

#### 1. 直连出口（本地转发）

```json
{
  "tag": "direct",
  "port": 36196,
  "protocol": "direct",
  "settings": {
    "targetAddress": "127.0.0.1",
    "domain": "s-s-admin.cn"
  }
}
```
该配置为默认配置，表示将s-s-admin.cn域名的请求转发到127.0.0.1的36196端口。
* **protocol**: `direct` 表示不经过任何代理，直接连接目标。
* **targetAddress**: 转发目标地址。
* **domain**: 可能用于路由匹配条件或记录。

#### 2. SOCKS 出口（转发到本地代理）

```json
{
  "tag": "socks",
  "port": 10808,
  "protocol": "socks",
  "settings": {
    "targetAddress": "127.0.0.1",
    "domain": "geosite:!china"
  }
}
```

* **protocol**: 出站使用 SOCKS 协议。
* **domain**: `geosite:!china` 表示非中国站点走该出站，可配合其他代理服务使用。

---

## 🌐 WOL 配置列表

该字段是一个数组，可以配置多个设备。

### 示例设备配置：

```json
{
  "name": "cc1",
  "ipAddress": "192.168.10.21",
  "subNetMask": "255.255.255.255",
  "macAddress": "D8-BB-C1-D4-58-CF",
  "wolPort": 9
}
```

### 字段解释：

* **`name`**:
  唯一标识该目标设备的名称。

* **`ipAddress`**:
  设备的 IP 地址，用于探测目标服务器是否在线。

* **`subNetMask`**:
udp包发送的广播地址，通常都为`255.255.255.255` （本地网络的广播）

* **`macAddress`**:
  目标设备的 **MAC 地址**（必须是支持 WOL 功能的网卡），格式为 `XX-XX-XX-XX-XX-XX` 或 `XX:XX:XX:XX:XX:XX`。

* **`wolPort`**:
  发送魔术包的目标端口，WOL 通常使用 UDP 的 **端口 9** 或 7。

---


## 使用说明

前提条件
- Java 17
- 部署再linux的宿主机上
- 安全硬盘健康检测工具：apt install smartmontools
- 配置好相关配置
下载解压压缩包，通过`./proxy.sh start` 开启服务，`./proxy.sh stop` 关闭服务。

## 后台界面
后台界面启动在127.0.0.1的36196端口，由于监听的127.0.0.1，所以只允许通过代理方式访问，连上代理后，通过浏览器访问：http://s-s-admin.cn/：
![image](https://github.com/user-attachments/assets/f51d3bc2-71b2-4046-b412-4fd19345dee8)
![image](https://github.com/user-attachments/assets/90569b73-2027-45e2-8013-78a25b6f4824)



