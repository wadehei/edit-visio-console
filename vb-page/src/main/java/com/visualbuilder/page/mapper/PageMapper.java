package com.visualbuilder.page.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.visualbuilder.page.entity.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

@Mapper
public interface PageMapper extends BaseMapper<Page> {

    /** 查询包括已删除的页面（绕过逻辑删除过滤） */
    @Select("SELECT * FROM biz_page WHERE id = #{id}")
    Page selectByIdIncludeDeleted(@Param("id") Long id);

    /** 查询回收站列表（deleted=1） */
    @Select("SELECT * FROM biz_page WHERE project_id = #{projectId} AND user_id = #{userId} AND deleted = 1 ORDER BY updated_time DESC")
    IPage<Page> selectTrashPage(com.baomidou.mybatisplus.extension.plugins.pagination.Page<Page> page, @Param("projectId") Long projectId, @Param("userId") Long userId);

    /** 恢复已删除的页面（deleted: 1→0） */
    @Update("UPDATE biz_page SET deleted = 0, updated_time = NOW() WHERE id = #{id} AND deleted = 1")
    int restoreById(@Param("id") Long id);

    /** 永久删除页面 */
    @Delete("DELETE FROM biz_page WHERE id = #{id} AND deleted = 1")
    int permanentDeleteById(@Param("id") Long id);
}
