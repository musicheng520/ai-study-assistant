package com.msc.springai.controller;

import com.msc.springai.dto.workflow.revision.GenerateRevisionPackRequest;
import com.msc.springai.dto.workflow.revision.RevisionPackResponse;
import com.msc.springai.security.CurrentUserUtil;
import com.msc.springai.service.RevisionPackService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RevisionPackController {

    private final RevisionPackService revisionPackService;

    @PostMapping("/courses/{courseId}/revision-pack/generate")
    public RevisionPackResponse generateRevisionPack(
            @PathVariable Long courseId,
            @RequestBody(required = false) GenerateRevisionPackRequest request
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return revisionPackService.generateRevisionPack(
                currentUserId,
                courseId,
                request
        );
    }

    @GetMapping("/courses/{courseId}/revision-packs")
    public List<RevisionPackResponse> getCourseRevisionPacks(
            @PathVariable Long courseId
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return revisionPackService.getCourseRevisionPacks(
                currentUserId,
                courseId
        );
    }

    @GetMapping("/revision-packs/{packId}")
    public RevisionPackResponse getRevisionPackDetail(
            @PathVariable Long packId
    ) {
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        return revisionPackService.getRevisionPackDetail(
                currentUserId,
                packId
        );
    }
}