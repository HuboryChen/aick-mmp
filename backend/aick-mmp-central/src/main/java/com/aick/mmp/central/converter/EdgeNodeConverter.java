package com.aick.mmp.central.converter;

import com.aick.mmp.shared.model.EdgeNode;
import com.aick.mmp.shared.model.Region;
import com.aick.mmp.central.dto.EdgeNodeDTO;
import com.aick.mmp.central.repository.RegionRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * EdgeNode实体与EdgeNodeDTO之间的转换器
 */
@Component
public class EdgeNodeConverter {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private RegionRepository regionRepository;

    /**
     * 将EdgeNode实体转换为EdgeNodeDTO
     *
     * @param edgeNode EdgeNode实体
     * @return EdgeNodeDTO对象
     */
    public EdgeNodeDTO convertToDTO(EdgeNode edgeNode) {
        if (edgeNode == null) {
            return null;
        }
        EdgeNodeDTO dto = modelMapper.map(edgeNode, EdgeNodeDTO.class);
        // 设置区域名称
        if (edgeNode.getRegionId() != null) {
            regionRepository.findById(edgeNode.getRegionId())
                .ifPresent(region -> dto.setRegionName(region.getName()));
        }
        return dto;
    }

    /**
     * 将EdgeNodeDTO转换为EdgeNode实体
     *
     * @param edgeNodeDTO EdgeNodeDTO对象
     * @return EdgeNode实体
     */
    public EdgeNode convertToEntity(EdgeNodeDTO edgeNodeDTO) {
        if (edgeNodeDTO == null) {
            return null;
        }
        return modelMapper.map(edgeNodeDTO, EdgeNode.class);
    }
    
    /**
     * 将源EdgeNodeDTO的属性更新到目标EdgeNode实体中
     * 
     * @param source 源EdgeNodeDTO
     * @param target 目标EdgeNode实体
     */
    public void updateEntityFromDTO(EdgeNodeDTO source, EdgeNode target) {
        if (source == null || target == null) {
            return;
        }
        modelMapper.map(source, target);
    }
}