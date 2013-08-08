/*
 * Copyright 2013 Rob Fletcher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.freeside.betamax.proxy.netty

import java.util.logging.Logger
import co.freeside.betamax.handler.*
import co.freeside.betamax.message.Response
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.handler.codec.http.*
import io.netty.util.CharsetUtil
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1
import static java.util.logging.Level.SEVERE

@ChannelHandler.Sharable
public class BetamaxChannelHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

	private HttpHandler handlerChain

	private static final Logger log = Logger.getLogger(BetamaxChannelHandler.class.name)

	@Override
	protected void channelRead0(ChannelHandlerContext context, FullHttpRequest request) {
		def betamaxRequest = new NettyRequestAdapter(request)
		def betamaxResponse = handlerChain.handle(betamaxRequest)
		sendSuccess(context, betamaxResponse)
	}

	@Override
	void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
		if (cause instanceof HandlerException) {
			log.log SEVERE, "${cause.getClass().simpleName} in proxy processing", cause.message
			sendError context, new HttpResponseStatus(cause.httpStatus, cause.message), cause.message
		} else {
			log.log SEVERE, "error recording HTTP exchange", cause
			sendError context, INTERNAL_SERVER_ERROR, cause.message
		}
	}

	HttpHandler leftShift(HttpHandler httpHandler) {
		handlerChain = httpHandler
		handlerChain
	}

	private void sendSuccess(ChannelHandlerContext context, Response betamaxResponse) {
		def status = HttpResponseStatus.valueOf(betamaxResponse.status)
		def content = betamaxResponse.hasBody() ? Unpooled.copiedBuffer(betamaxResponse.bodyAsBinary.bytes) : Unpooled.EMPTY_BUFFER
		FullHttpResponse response = betamaxResponse.hasBody() ? new DefaultFullHttpResponse(HTTP_1_1, status, content) : new DefaultFullHttpResponse(HTTP_1_1, status)
		for (Map.Entry<String, String> header : betamaxResponse.headers) {
			response.headers().set(header.key, header.value.split(/,\s*/).toList())
		}
		sendResponse(context, response)
	}

	private void sendError(ChannelHandlerContext context, HttpResponseStatus status, String message) {
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, Unpooled.copiedBuffer(message ?: "", CharsetUtil.UTF_8))
		response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8")
		sendResponse(context, response)
	}

	private void sendResponse(ChannelHandlerContext context, DefaultFullHttpResponse response) {
		context.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
	}

}
