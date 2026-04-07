package com.iprody.inventory.mapper;

import com.iprody.inventory.model.Group;
import com.iprody.inventory.model.GroupData;
import com.iprody.inventory.model.GroupUpdateData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface GroupMapper {
    GroupMapper INSTANCE = Mappers.getMapper(GroupMapper.class);

    Group fromData(GroupData data);

    @Mapping(target = "currentCount", source = "data.currentCount")
    Group update(Group group, GroupUpdateData data);
}
