import { WebPlugin } from '@capacitor/core';
export class UdpPluginWeb extends WebPlugin {
    constructor() {
        super({
            name: 'UdpPlugin',
            platforms: ['web']
        });
    }
}
const UdpPlugin = new UdpPluginWeb();
export { UdpPlugin };
import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(UdpPlugin);
//# sourceMappingURL=web.js.map