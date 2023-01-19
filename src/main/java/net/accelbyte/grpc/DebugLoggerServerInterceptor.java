package net.accelbyte.grpc;

import io.grpc.ForwardingServerCall.SimpleForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.lognet.springboot.grpc.GRpcGlobalInterceptor;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@GRpcGlobalInterceptor
public class DebugLoggerServerInterceptor implements ServerInterceptor {
    @Value("${plugin.grpc.server.interceptor.debug-logger.enabled:false}")
    private boolean enabled;

    public DebugLoggerServerInterceptor() {
        log.info("DebugLoggerServerInterceptor initialized");
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        if (enabled) {
            log.info("Request path: {}", call.getMethodDescriptor().getFullMethodName());
            log.info("Request headers: {}}", headers);
        }

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                next.startCall(new SimpleForwardingServerCall<ReqT, RespT>(call) {

                    @Override
                    public void sendHeaders(Metadata responseHeaders) {
                        if (enabled) {
                            log.info("Response path: {}", call.getMethodDescriptor().getFullMethodName());
                            log.info("Response headers: {}}", responseHeaders);
                        }
                        super.sendHeaders(responseHeaders);
                    }

                    @Override
                    public void sendMessage(RespT message) {
                        if (enabled) {
                            log.info("Response message: {}", message);
                        }
                        super.sendMessage(message);
                    }
                }, headers)) {

            @Override
            public void onMessage(ReqT message) {
                if (enabled) {
                    log.info("Request message: {}", message);
                }
                super.onMessage(message);
            }

            @Override
            public void onCancel() {
                if (enabled) {
                    log.info("Call cancelled: ", call.getMethodDescriptor().getFullMethodName());
                }
                super.onCancel();
            }
        };
    }
}
