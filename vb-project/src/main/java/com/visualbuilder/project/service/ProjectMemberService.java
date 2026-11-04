package com.visualbuilder.project.service;

import com.visualbuilder.common.core.base.BaseService;
import com.visualbuilder.project.dto.BatchResultVO;
import com.visualbuilder.project.dto.ProjectMemberAddDTO;
import com.visualbuilder.project.entity.ProjectMember;

import java.util.List;

public interface ProjectMemberService extends BaseService<ProjectMember> {
    List<ProjectMember> listMembers(Long projectId);
    void addMember(Long projectId, Long userId, String permission);
    BatchResultVO batchAddMembers(Long projectId, List<ProjectMemberAddDTO> members);
    void updatePermission(Long projectId, Long userId, String permission);
    void removeMember(Long projectId, Long userId);
    void transferOwnership(Long projectId, Long newOwnerId);
    boolean isProjectMember(Long projectId, Long userId);
    /** 判断用户是否为项目所有者或项目成员 */
    boolean isProjectOwnerOrMember(Long projectId, Long userId);
    String getMemberPermission(Long projectId, Long userId);
}
