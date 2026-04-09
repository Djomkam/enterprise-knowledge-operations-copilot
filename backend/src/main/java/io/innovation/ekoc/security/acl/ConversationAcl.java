package io.innovation.ekoc.security.acl;

import io.innovation.ekoc.chat.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * ABAC component for conversation resource checks.
 *
 * <pre>
 *   &#64;PreAuthorize("@conversationAcl.canAccess(authentication, #conversationId)")
 * </pre>
 */
@Component("conversationAcl")
@RequiredArgsConstructor
public class ConversationAcl {

    private final ConversationRepository conversationRepository;

    public boolean canAccess(Authentication auth, UUID conversationId) {
        if (isAdmin(auth)) return true;
        return conversationRepository.findById(conversationId)
                .map(conv -> conv.getUser().getUsername().equals(auth.getName()))
                .orElse(false);
    }

    public boolean canDelete(Authentication auth, UUID conversationId) {
        return canAccess(auth, conversationId);
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
