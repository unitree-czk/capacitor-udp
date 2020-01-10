export * from './web';
const textEncoder = ("TextEncoder" in window) ? new TextEncoder() : null;
export const UdpPluginUtils = {
    bufferToString: function (buffer) {
        const charcodes = new Uint8Array(buffer);
        return btoa(String.fromCharCode.apply(null, charcodes));
    },
    stringToBuffer: function (base64String) {
        const str = atob(base64String);
        if (textEncoder) {
            return textEncoder.encode(str).buffer;
        }
        else {
            let buf = new ArrayBuffer(str.length);
            let bufView = new Uint8Array(buf);
            for (var i = 0, strLen = str.length; i < strLen; i++) {
                bufView[i] = str.charCodeAt(i);
            }
            return buf;
        }
    }
};
//# sourceMappingURL=index.js.map