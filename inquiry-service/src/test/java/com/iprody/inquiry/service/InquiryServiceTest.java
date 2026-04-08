package com.iprody.inquiry.service;

import com.iprody.common.Pagination;
import com.iprody.common.ResultList;
import com.iprody.common.Sorting;
import com.iprody.inquiry.model.*;
import com.iprody.inquiry.repository.InquiryRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {
    @Mock
    private InquiryRepo inquiryRepo;

    @InjectMocks
    private InquiryService inquiryService;

    @Test
    @DisplayName("Should successfully save new inquiry with NEW status")
    void save_Success() {
        InquiryData data = new InquiryData();
        Inquiry savedInquiry = new Inquiry();
        savedInquiry.setStatus(InquiryStatus.NEW);

        when(inquiryRepo.save(any(Inquiry.class))).thenReturn(savedInquiry);

        Inquiry result = inquiryService.save(data);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(InquiryStatus.NEW);
        verify(inquiryRepo).save(any(Inquiry.class));
    }

    @Test
    @DisplayName("Should find inquiry by id or return null")
    void findById_ShouldReturnEntity() {
        UUID id = UUID.randomUUID();
        Inquiry inquiry = new Inquiry();
        inquiry.setId(id);

        when(inquiryRepo.findById(id)).thenReturn(Optional.of(inquiry));

        Inquiry result = inquiryService.findById(id);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
    }

    @Test
    @DisplayName("Should update existing inquiry")
    void update_Success() {
        UUID id = UUID.randomUUID();
        InquiryUpdateData updateData = new InquiryUpdateData();
        Inquiry existingInquiry = new Inquiry();
        existingInquiry.setId(id);

        when(inquiryRepo.findById(id)).thenReturn(Optional.of(existingInquiry));
        when(inquiryRepo.save(any(Inquiry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Inquiry result = inquiryService.update(id, updateData);

        assertThat(result).isNotNull();
        verify(inquiryRepo).save(any(Inquiry.class));
    }

    @Test
    @DisplayName("Should return paginated result by filter")
    void findAllByFilter_Success() {
        InquiryFilter filter = new InquiryFilter();
        filter.setStatus(InquiryStatus.NEW);

        Page<Inquiry> mockPage = new PageImpl<>(List.of(new Inquiry()));
        when(inquiryRepo.findAllByFilter(anyString(), any(), any(), any())).thenReturn(mockPage);

        ResultList<Inquiry> result = inquiryService.findAllByFilter(
                filter,
                new Pagination(0, 10),
                new Sorting(InquirySortField.STATUS, Sort.Direction.DESC));

        assertThat(result.getElements()).hasSize(1);
        verify(inquiryRepo).findAllByFilter(eq("NEW"), any(), any(), any());
    }
}
