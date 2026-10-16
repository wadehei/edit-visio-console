package com.visualbuilder.project.service;

import com.visualbuilder.common.core.base.BaseService;
import com.visualbuilder.project.entity.ProjectMember;

import java.util.List;
import java.util.Map;

public interface ProjectMemberService extends BaseService<ProjectMember> {
    List<ProjectMember> listMembers(Long projectId);
    void addMember(Long projectId, Long userId, String permission);
    Map<String, Object> batchAddMembers(Long projectId, List<Map<String, Object>> members);
    void updatePermission(Long projectId, Long userId, String permission);
    void removeMember(Long projectId, Long userId);
    void transferOwnership(Long projectId, Long newOwnerId);
    boolean isProjectMember(Long projectId, Long userId);
    String getMemberPermission(Long projectId, Long userId);
}
