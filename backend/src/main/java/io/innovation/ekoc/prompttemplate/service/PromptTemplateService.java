package io.innovation.ekoc.prompttemplate.service;

import io.innovation.ekoc.prompttemplate.domain.PromptTemplate;
import io.innovation.ekoc.prompttemplate.dto.CreatePromptTemplateRequest;
import io.innovation.ekoc.prompttemplate.dto.PromptTemplateDTO;
import io.innovation.ekoc.prompttemplate.repository.PromptTemplateRepository;
import io.innovation.ekoc.shared.exception.BusinessException;
import io.innovation.ekoc.shared.exception.ResourceNotFoundException;
import io.innovation.ekoc.teams.domain.Team;
import io.innovation.ekoc.teams.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptTemplateService {

    private static final String DEFAULT_TEMPLATE_NAME = "Default";

    private final PromptTemplateRepository templateRepository;
    private final TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public Page<PromptTemplateDTO> listActive(Pageable pageable) {
        return templateRepository.findByActiveTrueOrderByNameAsc(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public PromptTemplateDTO getById(UUID id) {
        return toDTO(find(id));
    }

    @Transactional
    public PromptTemplateDTO create(CreatePromptTemplateRequest req) {
        if (templateRepository.findTopByNameAndActiveTrue(req.getName()).isPresent()) {
            throw new BusinessException("A template named '" + req.getName() + "' already exists");
        }
        Team team = req.getTeamId() != null
                ? teamRepository.findById(req.getTeamId())
                        .orElseThrow(() -> new ResourceNotFoundException("Team", "id", req.getTeamId()))
                : null;

        PromptTemplate template = PromptTemplate.builder()
                .name(req.getName())
                .description(req.getDescription())
                .systemPrompt(req.getSystemPrompt())
                .roleType(req.getRoleType())
                .team(team)
                .build();
        return toDTO(templateRepository.save(template));
    }

    @Transactional
    public PromptTemplateDTO update(UUID id, CreatePromptTemplateRequest req) {
        PromptTemplate template = find(id);
        template.setName(req.getName());
        template.setDescription(req.getDescription());
        template.setSystemPrompt(req.getSystemPrompt());
        template.setRoleType(req.getRoleType());
        if (req.getTeamId() != null) {
            Team team = teamRepository.findById(req.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team", "id", req.getTeamId()));
            template.setTeam(team);
        } else {
            template.setTeam(null);
        }
        return toDTO(templateRepository.save(template));
    }

    @Transactional
    public void deactivate(UUID id) {
        PromptTemplate template = find(id);
        if (DEFAULT_TEMPLATE_NAME.equals(template.getName())) {
            throw new BusinessException("The default template cannot be deactivated");
        }
        template.setActive(false);
        templateRepository.save(template);
    }

    /**
     * Resolve the best system prompt for a user context.
     * Priority: team-scoped > role-scoped > default.
     */
    @Transactional(readOnly = true)
    public String resolveSystemPrompt(UUID teamId, String roleType) {
        if (teamId != null) {
            var teamTemplate = templateRepository.findTopByTeamIdAndActiveTrueOrderByUpdatedAtDesc(teamId);
            if (teamTemplate.isPresent()) {
                log.debug("Using team-scoped prompt template for teamId={}", teamId);
                return teamTemplate.get().getSystemPrompt();
            }
        }
        if (roleType != null) {
            var roleTemplate = templateRepository
                    .findTopByRoleTypeAndTeamIdIsNullAndActiveTrueOrderByUpdatedAtDesc(roleType);
            if (roleTemplate.isPresent()) {
                log.debug("Using role-scoped prompt template for role={}", roleType);
                return roleTemplate.get().getSystemPrompt();
            }
        }
        return templateRepository.findTopByNameAndActiveTrue(DEFAULT_TEMPLATE_NAME)
                .map(PromptTemplate::getSystemPrompt)
                .orElse(null); // AnswerComposerImpl falls back to its hardcoded default
    }

    private PromptTemplate find(UUID id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PromptTemplate", "id", id));
    }

    private PromptTemplateDTO toDTO(PromptTemplate t) {
        return PromptTemplateDTO.builder()
                .id(t.getId())
                .name(t.getName())
                .description(t.getDescription())
                .systemPrompt(t.getSystemPrompt())
                .roleType(t.getRoleType())
                .teamId(t.getTeam() != null ? t.getTeam().getId() : null)
                .teamName(t.getTeam() != null ? t.getTeam().getName() : null)
                .active(t.isActive())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
