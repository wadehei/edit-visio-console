package com.visualbuilder.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visualbuilder.project.entity.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

@Mapper
public interface ProjectMapper extends BaseMapper<Project> {

    /** 查询包括已删除的项目（绕过逻辑删除过滤） */
    @Select("SELECT * FROM biz_project WHERE id = #{id}")
    Project selectByIdIncludeDeleted(@Param("id") Long id);

    /** 查询回收站列表（deleted=1） */
    @Select("SELECT * FROM biz_project WHERE user_id = #{userId} AND deleted = 1 ORDER BY updated_time DESC")
    IPage<Project> selectTrashPage(Page<Project> page, @Param("userId") Long userId);

    /** 恢复已删除的项目（deleted: 1→0） */
    @Update("UPDATE biz_project SET deleted = 0, updated_time = NOW() WHERE id = #{id} AND deleted = 1")
    int restoreById(@Param("id") Long id);

    /** 永久删除项目 */
    @Delete("DELETE FROM biz_project WHERE id = #{id} AND deleted = 1")
    int permanentDeleteById(@Param("id") Long id);
}
