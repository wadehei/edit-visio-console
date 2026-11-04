package com.visualbuilder.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.visualbuilder.project.entity.ProjectMember;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProjectMemberMapper extends BaseMapper<ProjectMember> {

    /**
     * 一次查询解析用户在项目中的有效角色。
     *
     * <p>返回 {@code owner} 表示项目所有者；返回空串表示项目存在但用户非成员；
     * 返回 {@code null} 表示项目不存在。项目所有者优先于成员表记录。
     */
    @Select("SELECT CASE WHEN p.user_id = #{userId} THEN 'owner' "
            + "ELSE COALESCE((SELECT pm.permission FROM biz_project_member pm "
            + "WHERE pm.project_id = p.id AND pm.user_id = #{userId} LIMIT 1), '') END "
            + "FROM biz_project p WHERE p.id = #{projectId} AND p.deleted = 0")
    String selectEffectiveRole(@Param("projectId") Long projectId, @Param("userId") Long userId);

    /**
     * 批量插入项目成员 — 单条语句完成，减少数据库交互次数。
     */
    @Insert("<script>"
            + "INSERT INTO biz_project_member (project_id, user_id, permission, created_time) VALUES "
            + "<foreach collection='list' item='item' separator=','>"
            + "(#{item.projectId}, #{item.userId}, #{item.permission}, #{item.createdTime})"
            + "</foreach>"
            + "</script>")
    int batchInsert(@Param("list") List<ProjectMember> list);
}
