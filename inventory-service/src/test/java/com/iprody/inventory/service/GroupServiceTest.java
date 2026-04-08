package com.iprody.inventory.service;

import com.iprody.common.Pagination;
import com.iprody.common.ResultCode;
import com.iprody.common.ResultList;
import com.iprody.common.exception.AppException;
import com.iprody.inventory.model.Group;
import com.iprody.inventory.model.GroupData;
import com.iprody.inventory.model.GroupUpdateData;
import com.iprody.inventory.repository.GroupRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepo groupRepo;

    @InjectMocks
    private GroupService groupService;

    @Test
    void findById_Success() {
        UUID id = UUID.randomUUID();
        Group group = new Group();
        when(groupRepo.findById(id)).thenReturn(Optional.of(group));

        Group result = groupService.findById(id);

        assertNotNull(result);
        verify(groupRepo).findById(id);
    }

    @Test
    void findById_NotFound_ThrowsException() {
        UUID id = UUID.randomUUID();
        when(groupRepo.findById(id)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> groupService.findById(id));
        assertEquals(ResultCode.NOT_FOUND, exception.getCode());
    }

    @Test
    void save_Success() {
        GroupData data = new GroupData();
        Group group = new Group();
        when(groupRepo.save(any(Group.class))).thenReturn(group);

        Group result = groupService.save(data);

        assertNotNull(result);
        verify(groupRepo).save(any(Group.class));
    }

    @Test
    void update_Success() {
        UUID id = UUID.randomUUID();
        GroupUpdateData updateData = new GroupUpdateData();
        Group existingGroup = new Group();

        when(groupRepo.findById(id)).thenReturn(Optional.of(existingGroup));
        when(groupRepo.save(any(Group.class))).thenAnswer(i -> i.getArguments()[0]);

        Group result = groupService.update(id, updateData);

        assertNotNull(result);
        verify(groupRepo).findById(id);
        verify(groupRepo).save(any(Group.class));
    }

    @Test
    void findAllByFilter_Success() {
        UUID refId = UUID.randomUUID();
        GroupFilter filter = new GroupFilter();
        filter.setGroupRefId(refId);
        filter.setIsAvailFreePlaces(true);

        Pagination pagination = new Pagination(0, 10);

        Group group = new Group();
        Page<Group> mockPage = new PageImpl<>(List.of(group), PageRequest.of(0, 10), 1);

        when(groupRepo.findAllByFilter(eq(refId), eq(true), any()))
                .thenReturn(mockPage);

        ResultList<Group> result = groupService.findAllByFilter(filter, pagination);

        assertNotNull(result);
        assertEquals(1, result.getElements().size());

        verify(groupRepo).findAllByFilter(eq(refId), eq(true), any());
    }

    @Test
    void findAllByFilter_EmptyResult() {
        GroupFilter filter = new GroupFilter();
        Pagination pagination = new Pagination(0,1);

        when(groupRepo.findAllByFilter(any(), any(), any()))
                .thenReturn(Page.empty());

        ResultList<Group> result = groupService.findAllByFilter(filter, pagination);

        assertTrue(result.getElements().isEmpty());
        verify(groupRepo, times(1)).findAllByFilter(any(), any(), any());
    }
}
