package com.iprody.inquiry.service;

import com.iprody.common.Pagination;
import com.iprody.common.ResultCode;
import com.iprody.common.ResultList;
import com.iprody.common.Sorting;
import com.iprody.common.exception.AppException;
import com.iprody.inquiry.model.*;
import com.iprody.inquiry.repository.InquiryRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {
    @Mock
    private InquiryRepo inquiryRepo;

    @InjectMocks
    private InquiryService inquiryService;

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("should successfully save new inquiry with NEW status")
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
        @DisplayName("should propagate repository exception")
        void save_RepoThrowsException_propagates() {
            InquiryData data = new InquiryData();
            when(inquiryRepo.save(any())).thenThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> inquiryService.save(data))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("DB error");
            verify(inquiryRepo).save(any());
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("should return inquiry when exists")
        void findById_Exists_returnsEntity() {
            UUID id = UUID.randomUUID();
            Inquiry inquiry = new Inquiry();
            inquiry.setId(id);
            when(inquiryRepo.findById(id)).thenReturn(Optional.of(inquiry));

            Inquiry result = inquiryService.findById(id);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(id);
            verify(inquiryRepo).findById(id);
        }

        @Test
        @DisplayName("should return null when inquiry not found")
        void findById_NotFound_returnsNull() {
            UUID id = UUID.randomUUID();
            when(inquiryRepo.findById(id)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> inquiryService.findById(id))
                    .isInstanceOf(AppException.class);

            verify(inquiryRepo).findById(id);
        }

        @Test
        @DisplayName("should throw AppException with NOT_FOUND when id is null")
        void findById_NullId_throwsException() {
            assertThatThrownBy(() -> inquiryService.findById(null))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ResultCode.NOT_FOUND.getDefaultMessage());

            verify(inquiryRepo).findById(null);
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
    }

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("should update existing inquiry successfully")
        void update_Success() {
            UUID id = UUID.randomUUID();
            InquiryUpdateData updateData = new InquiryUpdateData();
            Inquiry existingInquiry = new Inquiry();
            existingInquiry.setId(id);

            when(inquiryRepo.findById(id)).thenReturn(Optional.of(existingInquiry));
            when(inquiryRepo.save(any(Inquiry.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Inquiry result = inquiryService.update(id, updateData);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(id);
            verify(inquiryRepo).save(any(Inquiry.class));
        }

        @Test
        @DisplayName("should throw NPE when inquiry not found")
        void update_NotFound_throwsNPE() {
            UUID id = UUID.randomUUID();
            when(inquiryRepo.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> inquiryService.update(id, new InquiryUpdateData()))
                    .isInstanceOf(AppException.class);

            verify(inquiryRepo).findById(id);
            verify(inquiryRepo, never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when id is null")
        void update_NullId_throwsException() {
            assertThatThrownBy(() -> inquiryService.update(null, new InquiryUpdateData()))
                    .isInstanceOf(AppException.class);

            verify(inquiryRepo).findById(null);
        }
    }

    @Nested
    @DisplayName("findAllByFilter()")
    class FindAllByFilterTests {

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

        @Test
        @DisplayName("should throw NPE when filter is null")
        void findAllByFilter_NullFilter_throwsException() {
            assertThatThrownBy(() -> inquiryService.findAllByFilter(
                    null,
                    new Pagination(0, 10),
                    new Sorting(InquirySortField.STATUS, Sort.Direction.DESC)))
                    .isInstanceOf(NullPointerException.class);
            verifyNoInteractions(inquiryRepo);
        }

        @Test
        @DisplayName("should throw NPE when pagination is null")
        void findAllByFilter_NullPagination_throwsException() {
            assertThatThrownBy(() -> inquiryService.findAllByFilter(
                    new InquiryFilter(),
                    null,
                    new Sorting(InquirySortField.STATUS, Sort.Direction.DESC)))
                    .isInstanceOf(NullPointerException.class);
            verifyNoInteractions(inquiryRepo);
        }

        @Test
        @DisplayName("should throw NPE when sorting is null")
        void findAllByFilter_NullSorting_throwsException() {
            assertThatThrownBy(() -> inquiryService.findAllByFilter(new InquiryFilter(), new Pagination(0, 10), null))
                    .isInstanceOf(NullPointerException.class);
            verifyNoInteractions(inquiryRepo);
        }

        @Test
        @DisplayName("should return empty ResultList when repository returns empty page")
        void findAllByFilter_EmptyRepo_returnsEmptyList() {
            InquiryFilter filter = new InquiryFilter();
            filter.setStatus(InquiryStatus.NEW);
            Page<Inquiry> emptyPage = new PageImpl<>(List.of());
            when(inquiryRepo.findAllByFilter(anyString(), any(), any(), any())).thenReturn(emptyPage);

            ResultList<Inquiry> result = inquiryService.findAllByFilter(
                    filter, new
                            Pagination(0, 10),
                    new Sorting(InquirySortField.STATUS, Sort.Direction.DESC));

            assertThat(result.getElements()).isEmpty();
            verify(inquiryRepo).findAllByFilter(anyString(), any(), any(), any());
        }
    }
}
