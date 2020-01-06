declare module "@capacitor/core" {
  interface PluginRegistry {
    UdpPlugin: UdpPluginPlugin;
  }
}

export interface UdpPluginPlugin {
  echo(options: { value: string }): Promise<{value: string}>;
}
