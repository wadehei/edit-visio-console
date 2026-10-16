package com.visualbuilder.common.core.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("状态常量验证")
class StatusConstTest {

    @Test
    @DisplayName("用户状态值与 SQL DDL 一致")
    void userStatus() {
        assertEquals(0, StatusConst.User.DISABLED);
        assertEquals(1, StatusConst.User.NORMAL);
        assertEquals(2, StatusConst.User.LOCKED);
    }

    @Test
    @DisplayName("项目状态值与 SQL DDL 一致")
    void projectStatus() {
        assertEquals(0, StatusConst.Project.DRAFT);
        assertEquals(1, StatusConst.Project.PUBLISHED);
        assertEquals(2, StatusConst.Project.ARCHIVED);
    }

    @Test
    @DisplayName("页面状态值与 SQL DDL 一致")
    void pageStatus() {
        assertEquals(0, StatusConst.Page.EDITING);
        assertEquals(1, StatusConst.Page.PUBLISHED);
    }

    @Test
    @DisplayName("发布状态值与 SQL DDL 一致")
    void publishStatus() {
        assertEquals(0, StatusConst.Publish.PENDING);
        assertEquals(1, StatusConst.Publish.SUCCESS);
        assertEquals(2, StatusConst.Publish.FAILED);
    }

    @Test
    @DisplayName("模板状态值与 SQL DDL 一致")
    void templateStatus() {
        assertEquals(0, StatusConst.Template.PENDING_REVIEW);
        assertEquals(1, StatusConst.Template.ON_SHELF);
        assertEquals(2, StatusConst.Template.OFF_SHELF);
    }

    @Test
    @DisplayName("启用/禁用常量")
    void enableStatus() {
        assertEquals(0, StatusConst.Enable.DISABLED);
        assertEquals(1, StatusConst.Enable.ENABLED);
    }

    @Test
    @DisplayName("是/否常量")
    void yesNo() {
        assertEquals(0, StatusConst.YesNo.NO);
        assertEquals(1, StatusConst.YesNo.YES);
    }

    @Test
    @DisplayName("同一类中常量值互不相同")
    void uniqueValues() {
        assertNotEquals(StatusConst.User.DISABLED, StatusConst.User.NORMAL);
        assertNotEquals(StatusConst.User.NORMAL, StatusConst.User.LOCKED);
        assertNotEquals(StatusConst.Publish.PENDING, StatusConst.Publish.SUCCESS);
        assertNotEquals(StatusConst.Publish.SUCCESS, StatusConst.Publish.FAILED);
    }
}
