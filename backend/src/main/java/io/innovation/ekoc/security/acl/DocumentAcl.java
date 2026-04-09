package io.innovation.ekoc.security.acl;

import io.innovation.ekoc.documents.repository.DocumentRepository;
import io.innovation.ekoc.teams.repository.TeamMemberRepository;
import io.innovation.ekoc.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * ABAC (attribute-based access control) component for document resource checks.
 *
 * <p>Used via SpEL in {@code @PreAuthorize} annotations:
 * <pre>
 *   &#64;PreAuthorize("@documentAcl.canRead(authentication, #documentId)")
 * </pre>
 *
 * Works with both Keycloak JWTs (principal name = preferred_username) and the
 * local JWT filter (principal name = username from UserDetails), so the test
 * profile continues to function unchanged.
 */
@Component("documentAcl")
@RequiredArgsConstructor
public class DocumentAcl {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;

    public boolean canRead(Authentication auth, UUID documentId) {
        return hasAccess(auth, documentId);
    }

    public boolean canWrite(Authentication auth, UUID documentId) {
        return hasAccess(auth, documentId);
    }

    public boolean canDelete(Authentication auth, UUID documentId) {
        return isOwnerOrAdmin(auth, documentId);
    }

    // -------------------------------------------------------------------------

    private boolean hasAccess(Authentication auth, UUID documentId) {
        if (isAdmin(auth)) return true;

        return documentRepository.findById(documentId).map(doc -> {
            String username = auth.getName();
            return userRepository.findByUsername(username).map(user -> {
                // Owner always has access
                if (doc.getOwner().getId().equals(user.getId())) return true;
                // Team member has read access
                if (doc.getTeam() != null) {
                    List<UUID> teamIds = teamMemberRepository
                            .findByUserIdAndActiveTrue(user.getId()).stream()
                            .map(tm -> tm.getTeam().getId())
                            .toList();
                    return teamIds.contains(doc.getTeam().getId());
                }
                return false;
            }).orElse(false);
        }).orElse(false);
    }

    private boolean isOwnerOrAdmin(Authentication auth, UUID documentId) {
        if (isAdmin(auth)) return true;
        return documentRepository.findById(documentId).map(doc -> {
            String username = auth.getName();
            return userRepository.findByUsername(username)
                    .map(user -> doc.getOwner().getId().equals(user.getId()))
                    .orElse(false);
        }).orElse(false);
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
