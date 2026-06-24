package org.pytenix.packets.impl;

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
                .setAction(packet.action())
                .setConsentType(packet.consentType() == null ? NetworkPackets.ProfilePacket.ConsentType.UNKNOWN : packet.consentType())
                .setRequestIdMostSig(packet.requestId().getMostSignificantBits())
                .setRequestIdLeastSig(packet.requestId().getLeastSignificantBits())
                .setUserIdMostSig(packet.playerId().getMostSignificantBits())
                .setUserIdLeastSig(packet.playerId().getLeastSignificantBits())
                .build();
    }

    @Override
    public ProfileData from(NetworkPackets.ProfilePacket packet) {
        return new ProfileData(
                packet.getLicense(),
                packet.getAction(),
                new UUID(packet.getUserIdMostSig(), packet.getUserIdLeastSig()),
                new UUID(packet.getRequestIdMostSig(), packet.getRequestIdLeastSig()),
                packet.getConsentType()
        );
    }


    public record ProfileData(
            String license,
            NetworkPackets.ProfilePacket.Action action,
            UUID playerId,
            UUID requestId,
            NetworkPackets.ProfilePacket.ConsentType consentType
    ) {
        public static ProfileData createDefault(String license, UUID playerId, UUID requestId) {
            return new ProfileData(
                    license,
                    NetworkPackets.ProfilePacket.Action.RESPONSE,
                    playerId,
                    requestId,
                    NetworkPackets.ProfilePacket.ConsentType.UNKNOWN
            );
        }

        public ProfileData withRequestId(UUID newRequestId) {
            return new ProfileData(
                    this.license,
                    this.action,
                    this.playerId,
                    newRequestId,
                    this.consentType
            );
        }

        public ProfileData withAction(NetworkPackets.ProfilePacket.Action newAction) {
            return new ProfileData(
                    this.license,
                    newAction,
                    this.playerId,
                    this.requestId,
                    this.consentType
            );
        }

        public ProfileData withConsentType(NetworkPackets.ProfilePacket.ConsentType newConsent) {
            return new ProfileData(
                    this.license,
                    this.action,
                    this.playerId,
                    this.requestId,
                    newConsent
            );
        }

    }
}
