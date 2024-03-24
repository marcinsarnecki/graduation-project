package uwr.ms.model.entity;

import uwr.ms.constant.LoginProvider;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "authorities")
@Data
public class AuthorityEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "authority_id_generator")
    @SequenceGenerator(name = "authority_id_generator", sequenceName = "authorities_seq", allocationSize = 1)
    Long id;

    String name;

    @Enumerated(value = EnumType.STRING)
    LoginProvider provider;

    @OneToMany(mappedBy = "authority", fetch = FetchType.EAGER)
    List<UserAuthorityEntity> assignedTo = new ArrayList<>();

    public AuthorityEntity(String name, LoginProvider provider) {
        this.name = name;
        this.provider = provider;
    }

    public AuthorityEntity() {}
}
