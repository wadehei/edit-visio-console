package com.visualbuilder.component.service;

import com.visualbuilder.common.core.base.BaseService;
import com.visualbuilder.component.dto.ComponentCreateDTO;
import com.visualbuilder.component.dto.ComponentUpdateDTO;
import com.visualbuilder.component.dto.ComponentVO;
import com.visualbuilder.component.entity.Component;
import java.util.List;

public interface ComponentService extends BaseService<Component> {
    List<ComponentVO> listComponents(Long categoryId, String keyword);
    ComponentVO getComponentDetail(Long componentId);
    void createComponent(ComponentCreateDTO dto);
    void updateComponent(Long componentId, ComponentUpdateDTO dto);
    void deleteComponent(Long componentId);
}
