package dev.aegiseye.gateway.demo;

public record DemoTamperRequest(String deviceId, String streamId, long sequenceNumber) {
}
