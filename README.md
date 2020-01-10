UDP Plugin for Capacitor inspired from cordova-plugin-chrome-apps-sockets-udp!
Support both IPv6 and IPv4, multicast and broadcast!

With capacitor, it is possible to write following code:
```js
async function process (){
    try {
        await UdpPlugin.closeAllSockets();
        let info = await UdpPlugin.create();
        await UdpPlugin.bind({ socketId: info.socketId, port: 5500})
        await UdpPlugin.send({ socketId: info.socketId, address: targetAddress, port: 6000, buffer: UdpPluginUtils.bufferToString(data)}) })
    } catch {
        //........
    }
}

```
Isn't it amazing!


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

The api is to some extent similar to [Chrome UDP API](https://developer.chrome.com/apps/sockets_udp) , but with the taste of capacitor!

- [create](#create)
- [update](#update)
- [bind](#bind)
- [send](#send)
- [close](#close)
- [closeAllSockets](#close-all-sockets)
- [setBroadcast](#set-broadcast)
- [getSockets](#get-sockets)
- [joinGroup](#join-group)
- [leaveGroup](#leave-group)
- [getJoinedGroups](#get-joined-Groups)
- [setPaused](#set-paused)
- [setMulticastLoopbackMode](#set-multicast-loopback-mode)

Events:
- [receive](#receive-event)
- [receiveError](#receive-error-event)

## Create ##
Create a socket for udp, and you can create more than one differentiated by the socket id. 
```js
UdpPlugin.create({properties: { name: "yourSocketName", bufferSize: 2048 }} ).then(res=>{socketId = res.socketId});
```

## Update ##
Update the socket info including socket name and buffer size.
```js
UdpPlugin.update( {socketId: yourSocketId, properties: { name: "socketname", bufferSize: 2048 }} )
```

## Bind ##
You need to bind a socket before sending and receiving data.
```js
UdpPlugin.bind({ socketId: yourSocketId, port: 5000})
```

## Send ##
Capacitor doesn't support Arraybuffer for now, so I need to convert ArrayBuffer to base64 string. 
I have provided a util function to help you achieve that!
```js
UdpPlugin.send({ socketId: yourSocketId, address: targetAddress, port: 6000, buffer: bufferString}) // bufferString is of type string
UdpPlugin.send({ socketId: yourSocketId, address: targetAddress, port: 6000, buffer: UdpPluginUtils.bufferToString(data)}) // data is of type ArrayBuffer
```

## Close ##
Close one socket
```js
UdpPlugin.close({ socketId: yourSocketId }) 
```

## Close All Sockets ##
```js
UdpPlugin.closeAllSockets() 
```

## Set Broadcast ##
After enabling broadcasting, you can send data with target address 255.255.255.255.
```js
UdpPlugin.setBroadcast({socketId: yourSocketId,enabled: enableBroadcastOrNot}) 
```

## Get Sockets ##
Obtain all the sockets available.

```js
UdpPlugin.getSockets().then(res=>{
    //res contains sockets...
})
```

## Join Group ##
Join a particular group address.
For IPv4, it's like "238.12.12.12".
For IPv6, it's like "ff02::08".
```js
UdpPlugin.joinGroup({socketId: yourSocketId, address: multicastAddress})
```

## Leave Group ##
```js
UdpPlugin.leaveGroup({socketId: yourSocketId, address: multicastAddress})
```

## Get Joined Groups ##
```js
UdpPlugin.getJoinedGroups({ socketId: yourSocketId }).then(res=>{
    // res contains your group addresses
})
```

## Set Paused ##
Pause receiving data.
```js
UdpPlugin.setPaused({socketId: yourSocketId,paused: pauseOrNot})
```

## Set Multicast Loopback Mode ##
```js
UdpPlugin.setMulticastLoopbackMode({socketId: yourSocketId, enabled: enabledOrNot})
```

## Receive Event ##
```js
UdpPlugin.addListener("receive", data => {
    yourArrayBuffer = UdpPluginUtils.stringToBuffer(data)
}});
```
For understanding ArrayBuffer, you can refer to [Typed Arrays](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Typed_arrays)

## Receive Error Event ##
```js
UdpPlugin.addListener("receiveError", error => {console.log(error)});
```
