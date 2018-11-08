package com.boomzz;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;

public class NIOServer {

	private Selector selector = null;
	private ServerSocketChannel ssChannel;
	private int port = 1080; 
	
	public static HashMap<SocketChannel, Socket> channelStatus = new HashMap<>();
	
	public NIOServer() {
		init();
	}
	
	public NIOServer(int port) {
		this.port = port;
		init();
	}
	
	private void init(){
		try {
			ssChannel = ServerSocketChannel.open();
			ssChannel.configureBlocking(false);
			ssChannel.socket().bind(new InetSocketAddress(port));
			this.selector = Selector.open();
			ssChannel.register(selector, SelectionKey.OP_ACCEPT);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void start() {
		while (true) {
			try {
				selector.select();
				Iterator<?> it = this.selector.selectedKeys().iterator();
				while (it.hasNext()) {
					
					SelectionKey key = (SelectionKey)it.next();
					if (key.isAcceptable()) {
	                    ServerSocketChannel server = (ServerSocketChannel) key.channel();
	                    SocketChannel channel = server.accept();
                    	channel.configureBlocking(false);
                    	channel.register(this.selector, SelectionKey.OP_READ);
	                } else if (key.isReadable()) {
	                    readData((SocketChannel) key.channel());
	                }
					it.remove();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

    private void readData(SocketChannel channel) throws Exception {
        ByteBuffer requestBuffer = ByteBuffer.allocate(2048);
        int read=channel.read(requestBuffer);
        byte[] data = requestBuffer.array();
        byte protocol = data[0];
        byte code = data[1];
        byte auth = data[2];
        byte reqAddrType = data[3];
        //socks 5 && CONNECT 请求
        for(int i=0;i<read;i++) {
        	System.out.print(data[i]+" ");
        }
        System.out.println();
        Socket channelsocket = channelStatus.get(channel);
        if(channelsocket!=null) {
        	System.out.println(new String(data));
        	channelsocket = new Socket(channelsocket.getInetAddress(),channelsocket.getPort());
        	OutputStream outputStream = channelsocket.getOutputStream();
        	outputStream.write(data);
        	outputStream.flush();
        	InputStream inputStream = channelsocket.getInputStream();
        	byte revData[] = new byte[1024];
        	int len=0;
        	ByteBuffer sendByteBuffer = ByteBuffer.allocate(1024);
        	while ((len=inputStream.read(revData))!=-1) {
        		int  s = sendByteBuffer.limit()-sendByteBuffer.position();
        		if(s<len) {
        			ByteBuffer expansionByteBuffer = ByteBuffer.allocate(sendByteBuffer.limit()+len);
        			expansionByteBuffer.put(sendByteBuffer.array());
        			sendByteBuffer = expansionByteBuffer;
        		}
        		sendByteBuffer.put(revData,0,len);
			}
        	sendByteBuffer.flip();
        	channel.write(sendByteBuffer);
        	channel.close();
        	channelStatus.remove(channel);
        }
        //验证阶段 无验证请求
        if(protocol==0x05&&code==0x01&&auth==0x00&&reqAddrType==0x00) {
        	ByteBuffer resBuffer = ByteBuffer.allocate(10);
        	byte []resByte = {0x05,0x00};
        	resBuffer.put(resByte);
        	resBuffer.flip();
        	channel.write(resBuffer);
        }
        //请求数据 - IP地址
        if(protocol==0x05&&code==0x01&&auth==0x00&&reqAddrType==0x01) {
        	int ip1 = data[4]&0xff;
        	int ip2 = data[5]&0xff;
        	int ip3 = data[6]&0xff;
        	int ip4 = data[7]&0xff;
        	int port1 = data[9]&0xff | ((data[8]&0xff)<<8);
        	String ip=ip1+"."+ip2+"."+ip3+"."+ip4;
        	Socket socket = new Socket(ip, port1);
        	InetAddress localAddress = socket.getLocalAddress();
        	byte localIp[] = localAddress.getAddress();
        	int localPort = socket.getLocalPort();
        	byte localPoat[] = {(byte) ((localPort>>8)&0xff),(byte) (localPort&0xff)};
        	ByteBuffer resBuffer = ByteBuffer.allocate(10);
        	byte resByte[]= {0x05,0x00,0x00,0x01,0x00,0x00,0x00,0x00,0x00,0x00};
        	resBuffer.put(resByte);
//        	resBuffer.put(localIp);
//        	resBuffer.put(localPoat);
        	resBuffer.flip();
        	channel.write(resBuffer);
        	channelStatus.put(channel, socket);
        	socket.close();
        }
        //请求数据 - 域名
        if(protocol==0x05&&code==0x01&&auth==0x00&&reqAddrType==0x03) {
        }
//        requestBuffer.flip();
    }
}
