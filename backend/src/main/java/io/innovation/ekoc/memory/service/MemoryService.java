package io.innovation.ekoc.memory.service;

import com.pgvector.PGvector;
import io.innovation.ekoc.memory.domain.MemoryEntry;
import io.innovation.ekoc.memory.domain.MemoryType;
import io.innovation.ekoc.memory.dto.MemoryEntryDTO;
import io.innovation.ekoc.memory.dto.StoreMemoryRequest;
import io.innovation.ekoc.memory.repository.MemoryEntryRepository;
import io.innovation.ekoc.shared.exception.ResourceNotFoundException;
import io.innovation.ekoc.users.domain.User;
import io.innovation.ekoc.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryService {

    private static final int SHORT_TERM_TTL_HOURS = 24;
    private static final int DEFAULT_FETCH_LIMIT = 20;

    private final MemoryEntryRepository memoryRepository;
    private final MemoryIndexService memoryIndexService;
    private final UserService userService;

    @Transactional
    public MemoryEntryDTO store(StoreMemoryRequest request, String username) {
        User user = userService.findByUsername(username);

        MemoryEntry entry = MemoryEntry.builder()
                .user(user)
                .type(request.getType())
                .content(request.getContent())
                .metadata(request.getMetadata())
                .build();
        entry = memoryRepository.save(entry);

        // Embed and persist the vector
        float[] embedding = memoryIndexService.indexMemory(entry);
        entry.setEmbedding(new PGvector(embedding));
        entry = memoryRepository.save(entry);

        log.debug("Stored {} memory for user {}", request.getType(), username);
        return toDTO(entry);
    }

    @Transactional(readOnly = true)
    public List<MemoryEntryDTO> getRecent(String username, MemoryType type, int limit) {
        User user = userService.findByUsername(username);
        int effectiveLimit = limit > 0 ? limit : DEFAULT_FETCH_LIMIT;
        PageRequest page = PageRequest.of(0, effectiveLimit);

        List<MemoryEntry> entries = type != null
                ? memoryRepository.findByUserIdAndTypeAndActiveTrueOrderByCreatedAtDesc(user.getId(), type, page)
                : memoryRepository.findByUserIdAndActiveTrueOrderByCreatedAtDesc(user.getId(), page);

        return entries.stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<MemoryEntryDTO> searchRelevant(String query, String username, MemoryType type, int topK) {
        User user = userService.findByUsername(username);
        return memoryIndexService.searchSimilar(query, user.getId(), type, topK > 0 ? topK : 5);
    }

    @Transactional
    public void deactivate(UUID memoryId, String username) {
        User user = userService.findByUsername(username);
        MemoryEntry entry = memoryRepository.findById(memoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Memory entry", "id", memoryId));
        if (!entry.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Memory entry", "id", memoryId);
        }
        entry.setActive(false);
        memoryRepository.save(entry);
    }

    @Transactional
    @Scheduled(cron = "0 0 2 * * *") // daily at 2am
    public void cleanupExpiredShortTermMemories() {
        Instant cutoff = Instant.now().minus(SHORT_TERM_TTL_HOURS, ChronoUnit.HOURS);
        // Deactivate across all users
        memoryRepository.findAll().stream()
                .filter(m -> m.getType() == MemoryType.SHORT_TERM
                        && m.isActive()
                        && m.getCreatedAt().isBefore(cutoff))
                .forEach(m -> {
                    m.setActive(false);
                    memoryRepository.save(m);
                });
        log.info("Short-term memory cleanup complete (cutoff={})", cutoff);
    }

    public String buildContextString(String username, String currentQuery) {
        List<MemoryEntryDTO> relevant = searchRelevant(currentQuery, username, MemoryType.LONG_TERM, 3);
        if (relevant.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("User context from memory:\n");
        relevant.forEach(m -> sb.append("- ").append(m.getContent()).append("\n"));
        return sb.toString().trim();
    }

    private MemoryEntryDTO toDTO(MemoryEntry entry) {
        return MemoryEntryDTO.builder()
                .id(entry.getId())
                .type(entry.getType())
                .content(entry.getContent())
                .metadata(entry.getMetadata())
                .relevanceScore(entry.getRelevanceScore())
                .active(entry.isActive())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
