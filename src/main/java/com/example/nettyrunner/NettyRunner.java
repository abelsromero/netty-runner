package com.example.nettyrunner;

import java.net.InetSocketAddress;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.DecoderResultProvider;
import reactor.netty.DisposableServer;
import reactor.netty.NettyPipeline;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;

public class NettyRunner {


	public static final int PORT = 8080;

	public static class CustomChannelHandler extends ChannelInboundHandlerAdapter {

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			if (msg instanceof DecoderResultProvider request) {
				DecoderResult decoderResult = request.decoderResult();
				if (decoderResult.isFailure()) {
					Throwable cause = decoderResult.cause();
					System.out.println(cause);
				}
			}
			super.channelRead(ctx, msg);
		}
	}

	public static void main(String[] args) {
		DisposableServer disposableServer = NettyRunner.createHttpServer(PORT)
				.doOnChannelInit((connectionObserver, channel, remoteAddress) -> {
					ChannelPipeline pipeline = channel.pipeline();
					// Using pipeline.addLast(new CustomChannelHandler()); does not work
					pipeline.addAfter(NettyPipeline.HttpCodec, "my-handler", new CustomChannelHandler());
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
