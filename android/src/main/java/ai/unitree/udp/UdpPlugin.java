package ai.unitree.udp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Base64;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import java.net.*;
import java.util.*;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import org.json.JSONException;

@NativePlugin()
public class UdpPlugin extends Plugin {
    private static final String LOG_TAG = "CapacitorUDP";
    private Map<Integer, UdpSocket> sockets = new ConcurrentHashMap<Integer, UdpSocket>();
    private BlockingQueue<SelectorMessage> selectorMessages = new LinkedBlockingQueue<SelectorMessage>();
    private int nextSocket = 0;
    private Selector selector;
    private SelectorThread selectorThread;


    private BroadcastReceiver dataForwardReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int socketId = intent.getIntExtra("socketId", -1);
            String address = intent.getStringExtra("address");
            int port = intent.getIntExtra("port", -1);
            byte[] data = intent.getByteArrayExtra("data");
            try {
                UdpSocket socket = obtainSocket(socketId);
                if (!socket.isBound) throw new Exception("Not bound yet");
                socket.addSendPacket(address, port, data, null);
                addSelectorMessage(socket, SelectorMessageType.SO_ADD_WRITE_INTEREST, null);
            } catch (Exception e) {
            }

        }
    };

    @Override
    protected void handleOnStart() {
        startSelectorThread();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(dataForwardReceiver, new IntentFilter("capacitor-udp-forward"));
    }

    @Override
    protected void handleOnStop() {
        Log.i("lifecycle", "stop");
        stopSelectorThread();
    }

    @Override
    protected void handleOnRestart() {
        Log.i("lifecycle", "restart");
        startSelectorThread();
    }


    @PluginMethod()
    public void create(PluginCall call) {
        try {
            JSObject properties = call.getObject("properties");
            UdpSocket socket = new UdpSocket(nextSocket++, properties);
            sockets.put(Integer.valueOf(socket.getSocketId()), socket);
            JSObject ret = new JSObject();
            ret.put("socketId", socket.getSocketId());
            ret.put("ipv4", socket.ipv4Address.getHostAddress());
            String ipv6 = socket.ipv6Address.getHostAddress();
            int ip6InterfaceIndex = ipv6.indexOf("%");
            ret.put("ipv6", ipv6.substring(0, ip6InterfaceIndex));

            call.success(ret);

        } catch (Exception e) {
            call.error("create error");
        }
    }

    private UdpSocket obtainSocket(int socketId) throws Exception {
        UdpSocket socket = sockets.get(Integer.valueOf(socketId));
        if (socket == null) {
            throw new Exception("No socket with socketId " + socketId);
        }
        return socket;
    }

    @PluginMethod()
    public void update(PluginCall call) {
        try {
            int socketId = call.getInt("socketId");
            JSObject properties = call.getObject("properties");
            UdpSocket socket = obtainSocket(socketId);
            socket.setProperties(properties);
            call.success();
        } catch (Exception e) {
            call.error(e.getMessage());
        }
    }

    @PluginMethod()
    public void setPaused(PluginCall call) {
        int socketId = call.getInt("socketId");
        boolean paused = call.getBoolean("paused");
        try {
            UdpSocket socket = obtainSocket(socketId);
            socket.setPaused(paused);
            if (paused) {
                // Read interest will be removed when socket is readable on selector thread.
                call.success();
            } else {
                addSelectorMessage(socket, SelectorMessageType.SO_ADD_READ_INTEREST, call);
            }
        } catch (Exception e) {
            call.error(e.getMessage());
        }
    }

    @PluginMethod()
    public void bind(PluginCall call) {
        int socketId = call.getInt("socketId");
        String address = call.getString("address");
        int port = call.getInt("port");
        try {
            UdpSocket socket = obtainSocket(socketId);
            socket.bind(address, port);
            addSelectorMessage(socket, SelectorMessageType.SO_BIND, call);
        } catch (Exception e) {
            call.error(e.getMessage());
        }
    }

    @PluginMethod()
    public void send(PluginCall call) {
        try {
            int socketId = call.getInt("socketId");
            String address = call.getString("address");
            int port = call.getInt("port");
            String bufferString = call.getString("buffer");
            byte[] data = Base64.decode(bufferString, Base64.DEFAULT);
            UdpSocket socket = obtainSocket(socketId);
            if (!socket.isBound) throw new Exception("Not bound yet");
            socket.addSendPacket(address, port, data, call);
            addSelectorMessage(socket, SelectorMessageType.SO_ADD_WRITE_INTEREST, null);
        } catch (Exception e) {
            call.error(e.getMessage());
        }
    }

    @PluginMethod()
    public void closeAllSockets(PluginCall call) {
        try {
            for (UdpSocket socket : sockets.values()) {
                addSelectorMessage(socket, SelectorMessageType.SO_CLOSE, null);
            }
            call.success();
        } catch (Exception e) {
            call.error(e.getMessage());
        }

    }

    @PluginMethod()
    public void close(PluginCall call) {
        try {
            int socketId = call.getInt("socketId");
            UdpSocket socket = obtainSocket(socketId);
            addSelectorMessage(socket, SelectorMessageType.SO_CLOSE, call);
        } catch (Exception e) {
            call.error(e.getMessage());
        }
    }

    @PluginMethod()
    public void getInfo(PluginCall call) {
        try {
            int socketId = call.getInt("socketId");
            UdpSocket socket = obtainSocket(socketId);
            call.success(socket.getInfo());
        } catch (Exception e) {
            call.error(e.getMessage());
        }
    }

    @PluginMethod()
    public void getSockets(PluginCall call) {
        try {
            JSArray results = new JSArray();
            for (UdpSocket socket : sockets.values()) {
                results.put(socket.getInfo());
            }
            JSObject ret = new JSObject();
            ret.put("sockets", results);
            call.success(ret);
        } catch (Exception e) {
            call.error(e.getMessage());
        }
    }

    @PluginMethod()
    public void joinGroup(PluginCall call) {
        try {
            int socketId = call.getInt("socketId");
            String address = call.getString("address");
            UdpSocket socket = obtainSocket(socketId);
            socket.joinGroup(address);
            call.success();
        } catch (Exception e) {
            call.error(e.getMessage());
        }
    }

    @PluginMethod()
    public void leaveGroup(PluginCall call) {
        try {
            int socketId = call.getInt("socketId");
            String address = call.getString("address");
            UdpSocket socket = obtainSocket(socketId);
            socket.leaveGroup(address);
            call.success();
        } catch (Exception e) {
            call.error(e.getMessage());
        }
    }

    @PluginMethod()
    public void setMulticastTimeToLive(PluginCall call) {
        try {
            int socketId = call.getInt("socketId");
            int ttl = call.getInt("ttl");
            UdpSocket socket = obtainSocket(socketId);
            socket.setMulticastTimeToLive(ttl);
            call.success();
        } catch (Exception e) {
            call.error(e.getMessage());
        }
    }

    @PluginMethod()
    public void setBroadcast(PluginCall call) {
        try {
            int socketId = call.getInt("socketId");
            boolean enabled = call.getBoolean("enabled");
            UdpSocket socket = obtainSocket(socketId);
            socket.setBroadcast(enabled);
            call.success();
        } catch (Exception e) {
            call.error(e.getMessage());
        }
    }

    @PluginMethod()
    public void setMulticastLoopbackMode(PluginCall call) {
        try {
            int socketId = call.getInt("socketId");
            boolean enabled = call.getBoolean("enabled");
            UdpSocket socket = obtainSocket(socketId);
            socket.setMulticastLoopbackMode(enabled, call);
        } catch (Exception e) {
            call.error(e.getMessage());
        }
    }

    @PluginMethod()
    public void getJoinedGroups(PluginCall call) {
        try {
            int socketId = call.getInt("socketId");
            UdpSocket socket = obtainSocket(socketId);

            JSArray results = new JSArray(socket.getJoinedGroups());
            JSObject ret = new JSObject();
            ret.put("groups", results);
            call.success(ret);
        } catch (Exception e) {
            call.error(e.getMessage());
        }
    }


    private void sendReceiveErrorEvent(int code, String message) {
        JSObject error = new JSObject();
        try {
            error.put("message", message);
            error.put("resultCode", code);
            notifyListeners("receiveError", error, false);
        } catch (Exception e) {
        }
    }

    // This is a synchronized method because regular read and multicast read on
    // different threads, and we need to send data and metadata in serial in order
    // to decode the receive event correctly. Alternatively, we can send Multipart
    // messages.
    private synchronized void sendReceiveEvent(byte[] data, int socketId, String address, int port) {
        JSObject ret = new JSObject();
        try {
            ret.put("socketId", socketId);
            int ip6InterfaceIndex = address.indexOf("%");
            if (ip6InterfaceIndex > 0) {
                ret.put("remoteAddress", address.substring(0, ip6InterfaceIndex));
            } else {
                ret.put("remoteAddress", address);
            }
            ret.put("remotePort", port);
            String bufferString = new String(Base64.encode(data, Base64.DEFAULT));
            ret.put("buffer", bufferString);
            notifyListeners("receive", ret, false);
        } catch (Exception e) {
        }
    }


    private void startSelectorThread() {
        if (selectorThread != null) return;
        selectorThread = new SelectorThread(selectorMessages, sockets);
        selectorThread.start();
    }

    private void stopSelectorThread() {
        if (selectorThread == null) return;
        addSelectorMessage(null, SelectorMessageType.T_STOP, null);
        try {
            selectorThread.join();
            selectorThread = null;
        } catch (InterruptedException e) {
        }
    }

    private void addSelectorMessage(
            UdpSocket socket, SelectorMessageType type, PluginCall call) {
        try {
            selectorMessages.put(new SelectorMessage(socket, type, call));
            if (selector != null) selector.wakeup();
        } catch (InterruptedException e) {
        }
    }

    private enum SelectorMessageType {
        SO_BIND,
        SO_CLOSE,
        SO_ADD_READ_INTEREST,
        SO_ADD_WRITE_INTEREST,
        T_STOP;
    }

    private NetworkInterface getNetworkInterface() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                if (addrs.size() < 2) continue;
                if (addrs.get(0).isLoopbackAddress()) continue;
                return intf;
            }
        } catch (Exception ignored) {
        } // for now eat exceptions
        return null;
    }

    private InetAddress getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                if (addrs.size() < 2) continue;
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':') < 0;
                        if (useIPv4) {
                            if (isIPv4)
                                return addr;
                            //return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return addr;
                                //return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        } // for now eat exceptions
        return InetAddress.getLoopbackAddress();
    }


    private class SelectorMessage {
        final UdpSocket socket;
        final SelectorMessageType type;
        final PluginCall call;

        SelectorMessage(
                UdpSocket socket, SelectorMessageType type, PluginCall call) {
            this.socket = socket;
            this.type = type;
            this.call = call;
        }
    }

    private class SelectorThread extends Thread {

        private BlockingQueue<SelectorMessage> selectorMessages;
        private Map<Integer, UdpSocket> sockets;
        private boolean running = true;

        SelectorThread(BlockingQueue<SelectorMessage> selectorMessages, Map<Integer, UdpSocket> sockets) {
            this.selectorMessages = selectorMessages;
            this.sockets = sockets;
        }

        private void processPendingMessages() {

            while (selectorMessages.peek() != null) {
                SelectorMessage msg = null;
                try {
                    msg = selectorMessages.take();
                    switch (msg.type) {
                        case SO_BIND:
                            msg.socket.register(selector, SelectionKey.OP_READ);
                            msg.socket.isBound = true;
                            break;
                        case SO_CLOSE:
                            msg.socket.close();
                            sockets.remove(Integer.valueOf(msg.socket.getSocketId()));
                            break;
                        case SO_ADD_READ_INTEREST:
                            msg.socket.addInterestSet(SelectionKey.OP_READ);
                            break;
                        case SO_ADD_WRITE_INTEREST:
                            msg.socket.addInterestSet(SelectionKey.OP_WRITE);
                            break;
                        case T_STOP:
                            running = false;
                            break;
                    }

                    if (msg.call != null)
                        msg.call.success();

                } catch (InterruptedException e) {
                } catch (IOException e) {
                    if (msg.call != null) {
                        msg.call.error(e.getMessage());
                    }
                }
            }

        }

        public void run() {

            try {
                if (selector == null) selector = Selector.open();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // process possible messages that send during openning the selector
            // before select.
            processPendingMessages();

            Iterator<SelectionKey> it;

            while (running) {

                try {
                    selector.select();
                } catch (IOException e) {
                    continue;
                }

                it = selector.selectedKeys().iterator();

                while (it.hasNext()) {

                    SelectionKey key = it.next();
                    it.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    UdpSocket socket = (UdpSocket) key.attachment();

                    if (key.isReadable()) {
                        socket.read();
                    }

                    if (key.isWritable()) {
                        socket.dequeueSend();
                    }
                } // while next

                processPendingMessages();
            }
        }
    }


    private class UdpSocket {
        private final int socketId;
        private final DatagramChannel channel;

        private MulticastSocket multicastSocket;

        private BlockingQueue<UdpSendPacket> sendPackets = new LinkedBlockingQueue<UdpSendPacket>();
        private Set<String> multicastGroups = new HashSet<String>();
        private SelectionKey key;
        private boolean isBound;


        private boolean paused;
        private DatagramPacket pausedMulticastPacket;

        private String name;
        private int bufferSize;

        private MulticastReadThread multicastReadThread;
        private boolean multicastLoopback;
        private InetAddress ipv4Address;
        private InetAddress ipv6Address;
        private NetworkInterface networkInterface;


        UdpSocket(int socketId, JSObject properties) throws JSONException, IOException {
            this.socketId = socketId;
            this.ipv4Address = getIPAddress(true);
            this.ipv6Address = getIPAddress(false);
            this.networkInterface = getNetworkInterface();
            channel = DatagramChannel.open();
            channel.configureBlocking(false);
            channel.setOption(StandardSocketOptions.IP_MULTICAST_IF, this.networkInterface);
            multicastSocket = null;

            // set socket default options
            paused = false;
            bufferSize = 4096;
            name = "";

            multicastReadThread = null;
            multicastLoopback = true;

            isBound = false;

            setProperties(properties);
            setBufferSize();
        }

        // Only call this method on selector thread
        void addInterestSet(int interestSet) {
            if (key != null && key.isValid()) {
                key.interestOps(key.interestOps() | interestSet);
                key.selector().wakeup();
            }
        }

        // Only call this method on selector thread
        void removeInterestSet(int interestSet) {
            if (key != null && key.isValid()) {
                key.interestOps(key.interestOps() & ~interestSet);
                key.selector().wakeup();
            }
        }


        int getSocketId() {
            return socketId;
        }

        void register(Selector selector, int interestSets) throws IOException {
            key = channel.register(selector, interestSets, this);
        }

        void setProperties(JSObject properties) throws JSONException, SocketException {

            if (!properties.isNull("name"))
                name = properties.getString("name");

            if (!properties.isNull("bufferSize")) {
                bufferSize = properties.getInt("bufferSize");
                setBufferSize();
            }
        }

        void setBufferSize() throws SocketException {
            channel.socket().setSendBufferSize(bufferSize);
            channel.socket().setReceiveBufferSize(bufferSize);
        }

        private void sendMulticastPacket(DatagramPacket packet) {
            byte[] out = packet.getData();

            // Truncate the buffer if the message was shorter than it.
            if (packet.getLength() != out.length) {
                byte[] temp = new byte[packet.getLength()];
                for (int i = 0; i < packet.getLength(); i++) {
                    temp[i] = out[i];
                }
                out = temp;
            }

            sendReceiveEvent(out, socketId, packet.getAddress().getHostAddress(), packet.getPort());
        }

        private void bindMulticastSocket() throws SocketException {
            multicastSocket.bind(new InetSocketAddress(channel.socket().getLocalPort()));

            if (!paused) {
                multicastReadThread = new MulticastReadThread(multicastSocket);
                multicastReadThread.start();
            }
        }

        // Upgrade the normal datagram socket to multicast socket. All incoming
        // packet will be received on the multicast read thread. There is no way to
        // downgrade the same socket back to a normal datagram socket.
        private void upgradeToMulticastSocket() throws IOException {
            if (multicastSocket == null) {
                multicastSocket = new MulticastSocket(null);
                multicastSocket.setReuseAddress(true);
                multicastSocket.setLoopbackMode(false);


                if (channel.socket().isBound()) {
                    bindMulticastSocket();
                }
            }
        }

        private void resumeMulticastSocket() {
            if (pausedMulticastPacket != null) {
                sendMulticastPacket(pausedMulticastPacket);
                pausedMulticastPacket = null;
            }

            if (multicastSocket != null && multicastReadThread == null) {
                multicastReadThread = new MulticastReadThread(multicastSocket);
                multicastReadThread.start();
            }
        }

        void setPaused(boolean paused) {
            this.paused = paused;
            if (!this.paused) {
                resumeMulticastSocket();
            }
        }

        void addSendPacket(String address, int port, byte[] data, PluginCall call) {
            UdpSendPacket sendPacket = new UdpSendPacket(address, port, data, call);
            try {
                sendPackets.put(sendPacket);
            } catch (Exception e) {
                call.error(e.getMessage());
            }
        }

        void bind(String address, int port) throws SocketException {
            channel.socket().setReuseAddress(true);
            channel.socket().bind(new InetSocketAddress(port));

            if (multicastSocket != null) {
                bindMulticastSocket();
            }
        }

        // This method can be only called by selector thread.
        void dequeueSend() {
            if (sendPackets.peek() == null) {
                removeInterestSet(SelectionKey.OP_WRITE);
                return;
            }

            UdpSendPacket sendPacket = null;
            try {
                sendPacket = sendPackets.take();
                JSObject ret = new JSObject();
                int bytesSent = channel.send(sendPacket.data, sendPacket.address);
                ret.put("bytesSent", bytesSent);
                if (sendPacket.call != null) sendPacket.call.success(ret);
            } catch (InterruptedException e) {
            } catch (IOException e) {
                if (sendPacket.call != null) sendPacket.call.error(e.getMessage());
            }
        }


        void close() throws IOException {

            if (key != null && channel.isRegistered())
                key.cancel();

            channel.close();

            if (multicastSocket != null) {
                multicastSocket.close();
                multicastSocket = null;
            }

            if (multicastReadThread != null) {
                multicastReadThread.cancel();
                multicastReadThread = null;
            }
        }

        JSObject getInfo() throws JSONException {
            JSObject info = new JSObject();
            info.put("socketId", socketId);
            info.put("bufferSize", bufferSize);
            info.put("name", name);
            info.put("paused", paused);
            if (channel.socket().getLocalAddress() != null) {
                info.put("localAddress", channel.socket().getLocalAddress().getHostAddress());
                info.put("localPort", channel.socket().getLocalPort());
            }
            return info;
        }

        void joinGroup(String address) throws IOException {

            upgradeToMulticastSocket();

            if (multicastGroups.contains(address)) {
                Log.e(LOG_TAG, "Attempted to join an already joined multicast group.");
                return;
            }

            multicastGroups.add(address);
            multicastSocket.joinGroup(new InetSocketAddress(InetAddress.getByName(address), channel.socket().getLocalPort()), networkInterface);

        }

        void leaveGroup(String address) throws UnknownHostException, IOException {
            if (multicastGroups.contains(address)) {
                multicastGroups.remove(address);
                multicastSocket.leaveGroup(InetAddress.getByName(address));
            }
        }

        void setMulticastTimeToLive(int ttl) throws IOException {
            upgradeToMulticastSocket();
            multicastSocket.setTimeToLive(ttl);
        }

        void setMulticastLoopbackMode(boolean enabled, PluginCall call) throws IOException {
            upgradeToMulticastSocket();
            multicastSocket.setLoopbackMode(!enabled);
            multicastLoopback = enabled;
            JSObject ret = new JSObject();
            ret.put("enabled", !multicastSocket.getLoopbackMode());
            call.success(ret);
        }

        void setBroadcast(boolean enabled) throws IOException {
            channel.socket().setBroadcast(enabled);
        }

        public Collection<String> getJoinedGroups() {
            return multicastGroups;
        }

        // This method can be only called by selector thread.
        void read() {

            if (paused) {
                // Remove read interests to avoid seletor wakeup when readable.
                removeInterestSet(SelectionKey.OP_READ);
                return;
            }

            ByteBuffer recvBuffer = ByteBuffer.allocate(bufferSize);
            recvBuffer.clear();

            try {
                InetSocketAddress address = (InetSocketAddress) channel.receive(recvBuffer);


                recvBuffer.flip();
                byte[] recvBytes = new byte[recvBuffer.limit()];
                recvBuffer.get(recvBytes);
                if (address.getAddress().getHostAddress().contains(":") && multicastSocket != null) {
                    return;
                }
                sendReceiveEvent(recvBytes, socketId, address.getAddress().getHostAddress(), address.getPort());
            } catch (IOException e) {
                sendReceiveErrorEvent(-2, e.getMessage());
            }
        }

        private class MulticastReadThread extends Thread {

            private final MulticastSocket socket;

            MulticastReadThread(MulticastSocket socket) {
                this.socket = socket;
            }

            public void run() {
                while (!Thread.currentThread().isInterrupted()) {

                    if (paused) {
                        // Terminate the thread if the socket is paused
                        multicastReadThread = null;
                        return;
                    }
                    try {
                        byte[] out = new byte[socket.getReceiveBufferSize()];
                        DatagramPacket packet = new DatagramPacket(out, out.length);
                        socket.receive(packet);
                        if (!multicastLoopback) {
                            String fromAddress = packet.getAddress().getHostAddress();
                            String ip4 = ipv4Address.getHostAddress();
                            String ip6 = ipv6Address.getHostAddress();

                            if (fromAddress.equalsIgnoreCase(ip4) || fromAddress.equalsIgnoreCase(ip6)) {
                                continue;
                            }
                        }
                        if (paused) {
                            pausedMulticastPacket = packet;
                        } else {
                            sendMulticastPacket(packet);
                        }

                    } catch (IOException e) {
                        sendReceiveErrorEvent(-2, e.getMessage());
                    }
                }
            }

            public void cancel() {
                interrupt();
            }
        }

        private class UdpSendPacket {
            final SocketAddress address;
            final PluginCall call;
            final ByteBuffer data;

            UdpSendPacket(String address, int port, byte[] data, PluginCall call) {
                this.address = new InetSocketAddress(address, port);
                this.data = ByteBuffer.wrap(data);
                this.call = call;
            }
        }
    }
}
