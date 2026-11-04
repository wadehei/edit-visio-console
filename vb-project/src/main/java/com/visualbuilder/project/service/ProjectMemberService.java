/*
 * @Author: songshaohui shaohui.song@hjlysz.com
 * @Date: 2026-10-21 00:39:27
 * @LastEditors: songshaohui shaohui.song@hjlysz.com
 * @LastEditTime: 2026-11-04 14:49:10
 * @FilePath: \edit-visio-console\vb-project\src\main\java\com\visualbuilder\project\service\ProjectMemberService.java
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 */
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
    /** 查询用户作为成员加入的全部项目 ID（不含其作为所有者的项目） */
    List<Long> listProjectIdsByUser(Long userId);
    String getMemberPermission(Long projectId, Long userId);
    /**
     * 校验用户对项目的角色是否达到最低要求（view &lt; edit &lt; admin &lt; owner）。
     * 项目所有者视为 owner；未达到则抛出 FORBIDDEN。
     */
    void assertProjectRole(Long projectId, Long userId, String minRole);
    /** 判断用户对项目的角色是否达到最低要求（不抛异常） */
    boolean hasProjectRole(Long projectId, Long userId, String minRole);
}
