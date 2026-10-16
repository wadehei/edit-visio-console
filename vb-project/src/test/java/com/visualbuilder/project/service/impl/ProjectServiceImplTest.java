package com.visualbuilder.project.service.impl;

import com.visualbuilder.common.core.exception.BusinessException;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.project.dto.ProjectVO;
import com.visualbuilder.project.entity.Project;
import com.visualbuilder.project.mapper.ProjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ProjectServiceImpl 单元测试 — 聚焦批量删除和核心业务逻辑。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectServiceImpl 项目管理")
class ProjectServiceImplTest {

    @Mock
    private ProjectMapper projectMapper;

    private ProjectServiceImpl projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectServiceImpl();
        ReflectionTestUtils.setField(projectService, "baseMapper", projectMapper);
    }

    @Test
    @DisplayName("批量删除 - 空列表抛出 PARAM_ERROR")
    void batchDeleteProjects_emptyList() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> projectService.batchDeleteProjects(Collections.emptyList()));
        assertEquals(ErrorCode.PARAM_ERROR, ex.getCode());
    }

    @Test
    @DisplayName("批量删除 - null 列表抛出 PARAM_ERROR")
    void batchDeleteProjects_nullList() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> projectService.batchDeleteProjects(null));
        assertEquals(ErrorCode.PARAM_ERROR, ex.getCode());
    }

    @Test
    @DisplayName("批量删除 - 超过50条抛出 PARAM_ERROR")
    void batchDeleteProjects_tooManyItems() {
        Long[] bigArray = new Long[51];
        for (int i = 0; i < 51; i++) {
            bigArray[i] = (long) i;
        }
        List<Long> bigList = Arrays.asList(bigArray);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> projectService.batchDeleteProjects(bigList));
        assertEquals(ErrorCode.PARAM_ERROR, ex.getCode());
        assertTrue(ex.getMessage().contains("50"));
    }

    @Test
    @DisplayName("项目不存在时抛出 PROJECT_NOT_FOUND")
    void getProjectDetail_notFound() {
        when(projectMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> projectService.getProjectDetail(999L));
        assertEquals(ErrorCode.PROJECT_NOT_FOUND, ex.getCode());
    }

    @Test
    @DisplayName("获取项目详情成功")
    void getProjectDetail_success() {
        Project project = createMockProject(1L, "测试项目");
        when(projectMapper.selectById(1L)).thenReturn(project);

        ProjectVO result = projectService.getProjectDetail(1L);

        assertNotNull(result);
        assertEquals("测试项目", result.getName());
    }

    @Test
    @DisplayName("删除不存在的项目抛出异常")
    void deleteProject_notFound() {
        when(projectMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> projectService.deleteProject(999L));
        assertEquals(ErrorCode.PROJECT_NOT_FOUND, ex.getCode());
    }

    private Project createMockProject(Long projectId, String name) {
        Project project = new Project();
        project.setId(projectId);
        project.setName(name);
        project.setDescription("测试描述");
        project.setStatus(1);
        project.setUserId(1L);
        project.setCreatedTime(LocalDateTime.now());
        project.setUpdatedTime(LocalDateTime.now());
        return project;
    }
}
