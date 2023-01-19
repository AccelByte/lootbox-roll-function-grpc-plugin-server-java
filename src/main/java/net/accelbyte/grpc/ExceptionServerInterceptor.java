package net.accelbyte.grpc;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;
import org.lognet.springboot.grpc.GRpcGlobalInterceptor;
import org.springframework.core.annotation.Order;

@Slf4j
@GRpcGlobalInterceptor
@Order(10)
public class ExceptionServerInterceptor implements ServerInterceptor {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        ServerCall.Listener<ReqT> listener = next.startCall(call, headers);
        return new ExceptionHandlingServerCallListener<>(listener, call, headers);
    }

    private class ExceptionHandlingServerCallListener<ReqT, RespT> extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {
        private ServerCall<ReqT, RespT> serverCall;
        private Metadata metadata;

        ExceptionHandlingServerCallListener(ServerCall.Listener<ReqT> listener, ServerCall<ReqT, RespT> serverCall,
                                            Metadata metadata) {
            super(listener);
            this.serverCall = serverCall;
            this.metadata = metadata;
        }

        @Override
        public void onHalfClose() {
            try {
                super.onHalfClose();
            } catch (RuntimeException ex) {
                handleException(ex, serverCall, metadata);
                throw ex;
            }
        }

        @Override
        public void onReady() {
            try {
                super.onReady();
            } catch (RuntimeException ex) {
                handleException(ex, serverCall, metadata);
                throw ex;
            }
        }

        private void handleException(RuntimeException exception, ServerCall<ReqT, RespT> serverCall, Metadata metadata) {
            log.error("Exception caught", exception);
            if (exception instanceof IllegalArgumentException) {
                serverCall.close(Status.INVALID_ARGUMENT.withDescription(exception.getMessage()), metadata);
            } else {
                serverCall.close(Status.UNKNOWN, metadata);
            }
        }
    }
}
