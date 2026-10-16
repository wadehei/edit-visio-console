package com.visualbuilder.resource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.visualbuilder.resource.entity.Resource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

@Mapper
public interface ResourceMapper extends BaseMapper<Resource> {

    /** 查询包括已删除的资源（绕过逻辑删除过滤） */
    @Select("SELECT * FROM res_file WHERE id = #{id}")
    Resource selectByIdIncludeDeleted(@Param("id") Long id);

    /** 查询回收站列表（deleted=1） */
    @Select("SELECT * FROM res_file WHERE user_id = #{userId} AND deleted = 1 ORDER BY created_time DESC")
    IPage<Resource> selectTrashPage(Page<Resource> page, @Param("userId") Long userId);

    /** 恢复已删除的资源（deleted: 1→0） */
    @Update("UPDATE res_file SET deleted = 0 WHERE id = #{id} AND deleted = 1")
    int restoreById(@Param("id") Long id);

    /** 永久删除资源 */
    @Delete("DELETE FROM res_file WHERE id = #{id} AND deleted = 1")
    int permanentDeleteById(@Param("id") Long id);
}
