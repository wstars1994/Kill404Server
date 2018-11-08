package com.boomzz;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class Kill404SocketHandler implements Runnable {
	
	private SocketChannel sChannel;
	private Selector selector;
	
	public Kill404SocketHandler(SocketChannel sChannel, Selector selector) throws IOException {
		this.sChannel = sChannel;
		this.selector = selector;
		String remoteName = sChannel.getRemoteAddress().toString();
        System.out.println("客户:" + remoteName + " 连接成功!");
	}
	
	@Override
	public void run() {
		
	}

}
