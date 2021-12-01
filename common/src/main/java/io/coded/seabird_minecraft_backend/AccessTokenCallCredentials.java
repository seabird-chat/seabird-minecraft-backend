package io.coded.seabird_minecraft_backend;

import io.grpc.CallCredentials;
import io.grpc.Metadata;

import java.util.concurrent.Executor;

public class AccessTokenCallCredentials extends CallCredentials {
    String token;

    AccessTokenCallCredentials(String token) {
        this.token = token;
    }

    @Override
    public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor, MetadataApplier applier) {
        Metadata headers = new Metadata();
        headers.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer " + this.token);
        applier.apply(headers);
    }

    @Override
    public void thisUsesUnstableApi() {
    }
}
