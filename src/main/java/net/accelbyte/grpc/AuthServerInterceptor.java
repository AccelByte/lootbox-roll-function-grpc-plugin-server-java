package net.accelbyte.grpc;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;
import net.accelbyte.sdk.core.AccelByteSDK;
import net.accelbyte.sdk.core.AccelByteConfig;
import net.accelbyte.sdk.core.client.OkhttpClient;
import net.accelbyte.sdk.core.repository.DefaultConfigRepository;
import net.accelbyte.sdk.core.repository.DefaultTokenRepository;

import org.lognet.springboot.grpc.GRpcGlobalInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;

@Slf4j
@GRpcGlobalInterceptor
@Order(20)
public class AuthServerInterceptor implements ServerInterceptor {

    @Value("${plugin.grpc.server.interceptor.auth.enabled:true}")
    private boolean enabled;
    private AccelByteSDK sdk;
    private String namespace;
    private String resource;

    @Autowired
    public AuthServerInterceptor(
            @Value("${plugin.grpc.config.resource_name}") String resource,
            @Value("${plugin.grpc.config.namespace}") String namespace) {

        final AccelByteConfig config = new AccelByteConfig(
                new OkhttpClient(),
                new DefaultTokenRepository(),
                new DefaultConfigRepository());
        this.sdk = new AccelByteSDK(config);

        this.namespace = namespace;
        this.resource = resource;

        log.info("AuthServerInterceptor initialized");
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        if (!enabled) {
            return next.startCall(call, headers);
        }

        log.info("AuthServerInterceptor::interceptCall start");
        try {
            final String authHeader = headers.get(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER));

            if (authHeader == null) {
                throw new Exception("Auth header is null");
            }

            final String[] authTypeToken = authHeader.split(" ");

            if (authTypeToken.length != 2) {
                throw new Exception("Invalid auth header format");
            }

            final String authToken = authTypeToken[1];
            final String qPermission = String.format("NAMESPACE:%s:%s", this.namespace,  this.resource);
            log.info("AUTH PERMISSION: {}",qPermission);
            log.info("TOKEN: {}",authToken);

            if (!sdk.validateToken(authToken, qPermission, 2)) {    // Action 2 - read only
                throw new Exception("Auth token validation failed");
            }
        } catch (Exception e) {
            log.info("Authorization error: " + e.getMessage());
            log.error("Authorization error", e);
            unAuthorizedCall(call, headers);
        }

        return next.startCall(call, headers);
    }

    private <ReqT, RespT> void unAuthorizedCall(ServerCall<ReqT, RespT> call, Metadata headers) {
        call.close(Status.UNAUTHENTICATED.withDescription("Call not authorized"), headers);
    }
}
