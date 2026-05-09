package com.iprody.inventory.service;

import com.iprody.common.PageUtils;
import com.iprody.common.Pagination;
import com.iprody.common.ResultCode;
import com.iprody.common.ResultList;
import com.iprody.common.exception.AppException;
import com.iprody.inventory.mapper.GroupMapper;
import com.iprody.inventory.model.Group;
import com.iprody.inventory.model.GroupData;
import com.iprody.inventory.model.GroupUpdateData;
import com.iprody.inventory.repository.GroupRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupService {
    private final GroupRepo groupRepo;

    @Transactional(readOnly = true)
    public Group findByGroupRefId(UUID id) {
        return groupRepo.findByGroupRefId(id).orElseThrow(() -> new AppException(ResultCode.NOT_FOUND, id));
    }

    @Transactional
    public Group save(GroupData data) {
        return groupRepo.save(GroupMapper.INSTANCE.fromData(data));
    }

    @Transactional
    public Group update(UUID id, GroupUpdateData data) {
        return groupRepo.save(
                GroupMapper.INSTANCE.update(
                        findByGroupRefId(id),
                        data
                )
        );
    }

    @Transactional(readOnly = true)
    public ResultList<Group> findAllByFilter(GroupFilter filter, Pagination pagination) {
        return ResultList.from(groupRepo.findAllByFilter(
                filter.getGroupRefId(),
                filter.getIsAvailFreePlaces(),
                PageUtils.of(pagination))
        );
    }

    @Transactional(readOnly = true)
    public void checkFindByGroupRefId(UUID id) {
        findByGroupRefId(id);
    }

    @Transactional
    public boolean cancellingReservation(UUID groupRefId) {
        int updatedRows = groupRepo.decrementCountByGroupRefIdId(groupRefId);
        return updatedRows > 0;
    }

    @Transactional(readOnly = true)
    public boolean availFreeSeats(UUID groupRefId, Long count) {
        Group group = findByGroupRefId(groupRefId);
        return group.getCurrentCount() + count <= group.getLimit();
    }
}
