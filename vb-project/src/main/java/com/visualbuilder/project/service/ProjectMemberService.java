package com.visualbuilder.project.service;

import com.visualbuilder.common.core.base.BaseService;
import com.visualbuilder.project.entity.ProjectMember;

import java.util.List;

public interface ProjectMemberService extends BaseService<ProjectMember> {
    List<ProjectMember> listMembers(Long projectId);
    void addMember(Long projectId, Long userId, String permission);
    void updatePermission(Long projectId, Long userId, String permission);
    void removeMember(Long projectId, Long userId);
}
