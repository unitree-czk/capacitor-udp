UDP Plugin for Capacitor for from cordova-plugin-chrome-apps-sockets-udp

## Install

```bash
$ npm install capacitor-udp
```


## Usage

```js
import { Plugins } from "@capacitor/core";
const { UdpPlugin } = Plugins;
import {UdpPluginUtils} from "capacitor-udp"; // if you want support for converting between ArrayBuffer and String
```

## API Reference
somehow similar to https://developer.chrome.com/apps/sockets_udp, but with the taste of capacitor

```js
UdpPlugin.create({properties: { name: "yourSocketName", bufferSize: 2048 }} )
```
properties is optional

```js
UdpPlugin.update( {socketId: yourSocketId, properties: { name: "socketname", bufferSize: 2048 }} )
```
yourSocketId is Int

```js
UdpPlugin.bind({ socketId: yourSocketId, port: 5000})
```

```js
UdpPlugin.send({ socketId: yourSocketId, address: targetAddress, port: 6000, buffer: bufferString}) // bufferString is of type string
UdpPlugin.send({ socketId: yourSocketId, address: targetAddress, port: 6000, buffer: UdpPluginUtils.bufferToString(data)}) // data is of type ArrayBuffer
```
support both ipv4 for ipv6 target address, please make sure your router support ipv6

```js
UdpPlugin.close({ socketId: yourSocketId }) // close one socket
UdpPlugin.closeAllSockets() // close all sockets
```

```js
UdpPlugin.setBroadcast({socketId: yourSocketId,enabled: enableBroadcastOrNot}) 
```
after enable it , you can send data with target Address 255.255.255.255

```js
UdpPlugin.getSockets()
UdpPlugin.getJoinedGroups({ socketId: yourSocketId }) // multicast group
```

```js
UdpPlugin.joinGroup({socketId: yourSocketId, address: multicastAddress})
UdpPlugin.leaveGroup({socketId: yourSocketId, address: multicastAddress})
```
ipv4 like "238.12.12.12"
ipv6 like "ff02::08"

naturally support ff02::01, but no longer available for IOS if you add other group

```js
UdpPlugin.setPaused({socketId: yourSocketId,paused: pauseOrNot})
```
pause receiving data

```js
UdpPlugin.setMulticastLoopbackMode({socketId: yourSocketId, enabled: enabledOrNot})
```

For android, the native function multicastSocket.setLoopbackMode(!enabled); doesn't seem to work.

If you know anything about it, please do help me!

