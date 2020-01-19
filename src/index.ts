export * from './definitions';
export * from './web';

export const UdpPluginUtils = {
    bufferToString: function (buffer: ArrayBuffer): string {
        const charcodes = new Uint8Array(buffer);
        return btoa(String.fromCharCode.apply(null, charcodes));
    },
    stringToBuffer: function (base64String: string): ArrayBuffer {
        const str = atob(base64String);
        let buf = new ArrayBuffer(str.length);
        let bufView = new Uint8Array(buf);
        for (var i = 0, strLen = str.length; i < strLen; i++) {
            bufView[i] = str.charCodeAt(i);
        }
        return buf;
    }
}

