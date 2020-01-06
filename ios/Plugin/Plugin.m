#import <Foundation/Foundation.h>
#import <Capacitor/Capacitor.h>

// Define the plugin using the CAP_PLUGIN Macro, and
// each method the plugin supports using the CAP_PLUGIN_METHOD macro.
CAP_PLUGIN(UdpPlugin, "UdpPlugin",
           CAP_PLUGIN_METHOD(create, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(update, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(setPaused, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(bind, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(close, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(getInfo, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(getSockets, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(send, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(setBroadcast, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(joinGroup, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(leaveGroup, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(getJoinedGroups, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(closeAllSockets, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(setMulticastTimeToLive, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(setMulticastLoopbackMode, CAPPluginReturnPromise);
)
