package com.aick.mmp.edge.service;

import com.aick.ai.FrameAnalysisGrpc;
import com.aick.ai.FrameOuterClass;
import com.aick.mmp.edge.config.AiServiceConfig;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class FrameAnalysisGrpcClient {
    private static final Logger log = LoggerFactory.getLogger(FrameAnalysisGrpcClient.class);

    private final AiServiceConfig config;
    private ManagedChannel channel;
    private FrameAnalysisGrpc.FrameAnalysisStub asyncStub;
    private StreamObserver<FrameOuterClass.FrameRequest> requestObserver;

    public FrameAnalysisGrpcClient(AiServiceConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        channel = NettyChannelBuilder.forTarget(config.getTargetAddress())
            .usePlaintext()
            .maxInboundMessageSize(10 * 1024 * 1024)
            .build();
        asyncStub = FrameAnalysisGrpc.newStub(channel);
    }

    public void sendFrame(String cameraId, byte[] jpegData, long timestampMs,
                          java.util.List<String> analysisTypes) {
        FrameOuterClass.FrameRequest request = FrameOuterClass.FrameRequest.newBuilder()
            .setCameraId(cameraId)
            .setEdgeNodeId("edge-" + cameraId)
            .setFrameData(com.google.protobuf.ByteString.copyFrom(jpegData))
            .setTimestamp(timestampMs)
            .addAllAnalysisTypes(analysisTypes)
            .build();

        if (requestObserver == null) {
            initStream();
        }

        try {
            requestObserver.onNext(request);
        } catch (Exception e) {
            log.error("gRPC send failed, reconnecting...", e);
            requestObserver = null;
        }
    }

    private void initStream() {
        requestObserver = asyncStub.analyzeFrame(new StreamObserver<>() {
            @Override
            public void onNext(FrameOuterClass.AnalysisResult value) {
                log.debug("Analysis result for camera {}: {}",
                    value.getCameraId(), value.getPassengerCase());
            }

            @Override
            public void onError(Throwable t) {
                log.error("gRPC stream error", t);
                requestObserver = null;
            }

            @Override
            public void onCompleted() {
                log.info("gRPC stream completed");
                requestObserver = null;
            }
        });
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        if (requestObserver != null) {
            requestObserver.onCompleted();
        }
        if (channel != null) {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
