package com.visualbuilder.page.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.visualbuilder.page.entity.PageVersion;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PageVersionMapper extends BaseMapper<PageVersion> {

    /**
     * 仅保留指定页面的最新 {@code keep} 个版本，删除更早的版本。
     *
     * <p>以单条 DELETE 完成「保留最新 N 个」的裁剪，替代 count → select → delete 三步流程，
     * 消除并发保存下版本数超限的竞态；内层派生表用于绕过 MySQL「UPDATE/DELETE 子查询不能
     * 直接引用同表并带 LIMIT」的限制。</p>
     *
     * @return 实际删除的行数
     */
    @Delete("""
            DELETE FROM biz_page_version
            WHERE page_id = #{pageId}
              AND version_no NOT IN (
                SELECT version_no FROM (
                  SELECT version_no FROM biz_page_version
                  WHERE page_id = #{pageId}
                  ORDER BY version_no DESC
                  LIMIT #{keep}
                ) AS keep_versions
              )
            """)
    int deleteExcessVersions(@Param("pageId") Long pageId, @Param("keep") int keep);
}
