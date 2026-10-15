package com.visualbuilder.component.service;

import com.visualbuilder.common.core.base.BaseService;
import com.visualbuilder.component.dto.ComponentVO;
import com.visualbuilder.component.entity.Component;
import java.util.List;

public interface ComponentService extends BaseService<Component> {
    List<ComponentVO> listComponents(Long categoryId, String keyword);
    ComponentVO getComponentDetail(Long componentId);
}
