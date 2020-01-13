declare module "@capacitor/core" {
  interface PluginRegistry {
    UdpPlugin: IUdpPlugin;
  }
}

export interface IUdpPlugin {
 create(options?: { properties?: {name?:string, bufferSize?: number} }): Promise<{socketId: number}>;
 update(options: { socketId:number, properties: {name?:string, bufferSize?: number} }): Promise<{}>;
 setPaused(options: {socketId:number, paused:boolean}): Promise<{}>;
 bind(options:{socketId:number,address:string,port:number}):Promise<{}>;
 send(options:{socketId:number,address:string,port:number,buffer:string}):Promise<{}>;
 closeAllSockets():Promise<{}>;
 close(options:{socketId:number}):Promise<{}>;
 getInfo(options:{socketId:number}):Promise<{socketId:number,bufferSize:number,name:string|null, paused:boolean, localAddress?:string, localPort?:number}>;
 getSockets():Promise<{sockets:[{socketId:number,bufferSize:number,name:string|null, paused:boolean, localAddress?:string, localPort?:number}]}>;
 joinGroup(options:{socketId:number,address:string}):Promise<{}>;
 leaveGroup(options:{socketId:number,address:string}):Promise<{}>;
 setMulticastTimeToLive(options:{socketId:number,ttl:number}):Promise<{}>;
 setBroadcast(options:{socketId:number,enabled:boolean}):Promise<{}>;
 setMulticastLoopbackMode(options:{socketId:number,enabled:boolean}):Promise<{}>;
 getJoinedGroups():Promise<{groups:[string]}>;
 addListener(events:"receive", functions: (params:{socketId:number,buffer:string}) => void ) :void;
 addListener(events:"receiveError", functions:(params:string) => void ):void;
}
