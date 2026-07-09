package org.pytenix.packets.impl;

import com.google.common.io.ByteStreams;
import com.google.protobuf.ByteString;
import org.pytenix.packets.AbstractPacketMapper;
import org.pytenix.proto.generated.NetworkPackets;

import java.util.UUID;

public class ProfileMapper extends AbstractPacketMapper<NetworkPackets.ProfilePacket, ProfileMapper.ProfileData> {


    public ProfileMapper() {
        super(NetworkPackets.ProfilePacket.class, ProfileData.class);
    }

    @Override
    public NetworkPackets.ProfilePacket to(ProfileData packet) {
        return NetworkPackets.ProfilePacket.newBuilder()
                .setLicense(packet.license())
                .setConsentType(packet.consentType() == null ? NetworkPackets.ProfilePacket.ConsentType.UNKNOWN : packet.consentType())
                .setRequestIdMostSig(packet.requestId().getMostSignificantBits())
                .setRequestIdLeastSig(packet.requestId().getLeastSignificantBits())
                .setAnalyticsId(ByteString.copyFrom(packet.analyticId()))
                .build();
    }

    @Override
    public ProfileData from(NetworkPackets.ProfilePacket packet) {
        return new ProfileData(
                packet.getLicense(),
                packet.getAnalyticsId().toByteArray(),
                new UUID(packet.getRequestIdMostSig(), packet.getRequestIdLeastSig()),
                packet.getConsentType()
        );
    }


    public record ProfileData(
            String license,
            byte[] analyticId,
            UUID requestId,
            NetworkPackets.ProfilePacket.ConsentType consentType
    ) {
        public static ProfileData createDefault(String license, byte[] analyticId, UUID requestId) {
            return new ProfileData(
                    license,
                    analyticId,
                    requestId,
                    NetworkPackets.ProfilePacket.ConsentType.UNKNOWN
            );
        }

        public ProfileData withRequestId(UUID newRequestId) {
            return new ProfileData(
                    this.license,
                    this.analyticId,
                    newRequestId,
                    this.consentType
            );
        }


        public ProfileData withConsentType(NetworkPackets.ProfilePacket.ConsentType newConsent) {
            return new ProfileData(
                    this.license,
                    this.analyticId,
                    this.requestId,
                    newConsent
            );
        }

    }
}
