import { WebPlugin } from '@capacitor/core';
import { UdpPluginPlugin } from './definitions';

export class UdpPluginWeb extends WebPlugin implements UdpPluginPlugin {
  constructor() {
    super({
      name: 'UdpPlugin',
      platforms: ['web']
    });
  }

  async echo(options: { value: string }): Promise<{value: string}> {
    console.log('ECHO', options);
    return options;
  }
}

const UdpPlugin = new UdpPluginWeb();

export { UdpPlugin };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(UdpPlugin);
