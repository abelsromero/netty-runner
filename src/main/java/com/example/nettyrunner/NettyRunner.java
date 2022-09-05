package com.example.nettyrunner;

import java.net.InetSocketAddress;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import reactor.netty.DisposableServer;
import reactor.netty.NettyPipeline;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;

public class NettyRunner {

	public static final int PORT = 8080;

	private HttpServer httpServer;
	private volatile DisposableServer disposableServer;

	public static class CustomChannelHandler implements ChannelHandler {
		@Override
		public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
			System.out.println("handlerAdded: " + ctx);
		}

		@Override
		public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
			System.out.println("handlerRemoved: " + ctx);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			System.out.println("exceptionCaught: " + ctx + " " + cause.getMessage());
		}
	}

	public static void main(String[] args) {
		DisposableServer disposableServer = NettyRunner.createHttpServer(PORT)
				.doOnChannelInit((connectionObserver, channel, remoteAddress) -> {
					ChannelPipeline pipeline = channel.pipeline();
					pipeline.addAfter(NettyPipeline.HttpCodec, "my-handler", new CustomChannelHandler());
					pipeline.addLast(new CustomChannelHandler());
				}).handle((request, httpServerResponse) -> null)
				.bindNow();

		startDaemonAwaitThread(disposableServer);
	}

	public static HttpServer createHttpServer(int port) {
		return HttpServer.create()
				.bindAddress(() -> new InetSocketAddress(port))
				.protocol(listProtocols()).forwarded(false);
	}

	private static HttpProtocol[] listProtocols() {
		return new HttpProtocol[] {HttpProtocol.HTTP11};
	}

	private static void startDaemonAwaitThread(DisposableServer disposableServer) {
		Thread awaitThread = new Thread("server") {

			@Override
			public void run() {
				disposableServer.onDispose().block();
			}

		};
		awaitThread.setContextClassLoader(disposableServer.getClass().getClassLoader());
		awaitThread.setDaemon(false);
		awaitThread.start();
	}
}
