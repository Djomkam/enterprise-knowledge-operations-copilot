package io.innovation.ekoc.security;

import io.innovation.ekoc.documents.domain.Document;
import io.innovation.ekoc.documents.repository.DocumentRepository;
import io.innovation.ekoc.security.acl.DocumentAcl;
import io.innovation.ekoc.teams.domain.Team;
import io.innovation.ekoc.teams.domain.TeamMember;
import io.innovation.ekoc.teams.repository.TeamMemberRepository;
import io.innovation.ekoc.users.domain.User;
import io.innovation.ekoc.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentAclTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamMemberRepository teamMemberRepository;

    private DocumentAcl acl;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID strangerUserId = UUID.randomUUID();
    private final UUID documentId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();

    private User owner;
    private User stranger;
    private Document document;
    private Team team;

    @BeforeEach
    void setUp() {
        acl = new DocumentAcl(documentRepository, userRepository, teamMemberRepository);

        owner = buildUser(ownerId, "owner");
        stranger = buildUser(strangerUserId, "stranger");

        team = new Team();
        team.setId(teamId);

        document = new Document();
        document.setOwner(owner);
    }

    @Test
    void canRead_owner_returnsTrue() {
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));

        assertThat(acl.canRead(auth("owner"), documentId)).isTrue();
    }

    @Test
    void canRead_stranger_returnsFalse() {
        // Document belongs to a team but stranger is not in that team
        document.setTeam(team);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(userRepository.findByUsername("stranger")).thenReturn(Optional.of(stranger));
        when(teamMemberRepository.findByUserIdAndActiveTrue(strangerUserId)).thenReturn(List.of());

        assertThat(acl.canRead(auth("stranger"), documentId)).isFalse();
    }

    @Test
    void canRead_teamMember_returnsTrue() {
        document.setTeam(team);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(userRepository.findByUsername("stranger")).thenReturn(Optional.of(stranger));

        TeamMember tm = new TeamMember();
        tm.setTeam(team);
        tm.setUser(stranger);
        tm.setRole("MEMBER");
        when(teamMemberRepository.findByUserIdAndActiveTrue(strangerUserId)).thenReturn(List.of(tm));

        assertThat(acl.canRead(auth("stranger"), documentId)).isTrue();
    }

    @Test
    void canRead_admin_alwaysReturnsTrue() {
        // Admin should not even reach the repository checks
        assertThat(acl.canRead(adminAuth("admin"), documentId)).isTrue();
    }

    @Test
    void canDelete_owner_returnsTrue() {
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));

        assertThat(acl.canDelete(auth("owner"), documentId)).isTrue();
    }

    @Test
    void canDelete_teamMemberNotOwner_returnsFalse() {
        document.setTeam(team);

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(userRepository.findByUsername("stranger")).thenReturn(Optional.of(stranger));

        // Team member can READ but not DELETE (only owner/admin can delete)
        assertThat(acl.canDelete(auth("stranger"), documentId)).isFalse();
    }

    @Test
    void canRead_documentNotFound_returnsFalse() {
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());
        assertThat(acl.canRead(auth("owner"), documentId)).isFalse();
    }

    // -------------------------------------------------------------------------

    private Authentication auth(String username) {
        return new UsernamePasswordAuthenticationToken(
                username, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private Authentication adminAuth(String username) {
        return new UsernamePasswordAuthenticationToken(
                username, null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private User buildUser(UUID id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        return u;
    }
}
