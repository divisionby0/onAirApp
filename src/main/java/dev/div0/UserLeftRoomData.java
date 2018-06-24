package dev.div0;


import org.kurento.room.api.pojo.UserParticipant;

import java.util.Set;

public class UserLeftRoomData {
    private Set<UserParticipant> remainingParticipants;
    private boolean isOwner;

    public UserLeftRoomData(Set<UserParticipant> remainingParticipants, boolean isOwner) {
        this.remainingParticipants = remainingParticipants;
        this.isOwner = isOwner;
    }

    public Set<UserParticipant> getRemainingParticipants() {
        return remainingParticipants;
    }

    public void setRemainingParticipants(Set<UserParticipant> remainingParticipants) {
        this.remainingParticipants = remainingParticipants;
    }

    public boolean isOwner() {
        return isOwner;
    }

    public void setOwner(boolean owner) {
        isOwner = owner;
    }
}
