import { WebPlugin } from '@capacitor/core';
import { UdpPluginPlugin } from './definitions';
export declare class UdpPluginWeb extends WebPlugin implements UdpPluginPlugin {
    constructor();
    echo(options: {
        value: string;
    }): Promise<{
        value: string;
    }>;
}
declare const UdpPlugin: UdpPluginWeb;
export { UdpPlugin };
