package uwr.ms.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import uwr.ms.constant.FriendshipStatus;

@Entity
@Table(name = "friendships")
@Data
public class FriendshipEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "requester_username", referencedColumnName = "username", foreignKey = @ForeignKey(name = "fk_friendship_requester"))
    private UserEntity requester;

    @ManyToOne
    @JoinColumn(name = "addressee_username", referencedColumnName = "username", foreignKey = @ForeignKey(name = "fk_friendship_addressee"))
    private UserEntity addressee;

    @Enumerated(EnumType.STRING)
    private FriendshipStatus status;

    @Override
    public String toString() {
        return "FriendshipEntity{" +
                "id=" + id +
                ", requester=" + requester.username +
                ", addressee=" + addressee.username +
                ", status=" + status +
                '}';
    }

//    private LocalDateTime createdAt;
//    private LocalDateTime updatedAt;
}
