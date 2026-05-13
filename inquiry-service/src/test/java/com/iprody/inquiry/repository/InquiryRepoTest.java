package com.iprody.inquiry.repository;

import com.iprody.inquiry.model.Inquiry;
import com.iprody.inquiry.model.InquirySortField;
import com.iprody.inquiry.model.InquiryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Sql(scripts = {"/sql/init-schema.sql"})
class InquiryRepoTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withInitScript("sql/init-schema.sql");

    @Autowired
    private InquiryRepo inquiryRepo;

    @Autowired
    private TestEntityManager entityManager;

    private UUID testCustomerId;
    private UUID testManagerId;
    private UUID testGroupId;

    @BeforeEach
    void setUp() {
        inquiryRepo.deleteAll();
        entityManager.flush();
        entityManager.clear();

        testCustomerId = UUID.randomUUID();
        testManagerId = UUID.randomUUID();
        testGroupId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("should save inquiry with all required fields and generate ID/timestamps")
        void save_withRequiredFields_generatesIdAndTimestamps() {
            Inquiry inquiry = createValidInquiry();

            Inquiry saved = inquiryRepo.save(inquiry);
            entityManager.flush();
            entityManager.clear();

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getCreatedAt()).isBeforeOrEqualTo(Timestamp.valueOf(LocalDateTime.now()));
            assertThat(saved.getStatus()).isEqualTo(InquiryStatus.NEW);
            assertThat(saved.getCustomerRefId()).isEqualTo(testCustomerId);
            assertThat(saved.getManagerRefId()).isEqualTo(testManagerId);
            assertThat(saved.getSource()).isEqualTo("WEB");
        }

        @Test
        @DisplayName("should save inquiry with optional fields (groupRefId, comment, note)")
        void save_withOptionalFields_persistsCorrectly() {
            Inquiry inquiry = createValidInquiry();
            inquiry.setGroupRefId(testGroupId);
            inquiry.setComment("Test comment");
            inquiry.setNote("Manager note");

            Inquiry saved = inquiryRepo.save(inquiry);
            entityManager.flush();
            entityManager.clear();

            assertThat(saved.getGroupRefId()).isEqualTo(testGroupId);
            assertThat(saved.getComment()).isEqualTo("Test comment");
            assertThat(saved.getNote()).isEqualTo("Manager note");
        }

        @Test
        @DisplayName("should save multiple inquiries and retrieve them all")
        void save_multipleInquiries_canRetrieveAll() {
            Inquiry i1 = createValidInquiry();
            Inquiry i2 = createValidInquiry();
            i2.setCustomerRefId(UUID.randomUUID());
            Inquiry i3 = createValidInquiry();
            i3.setStatus(InquiryStatus.IN_PROGRESS);

            inquiryRepo.saveAll(List.of(i1, i2, i3));
            entityManager.flush();
            entityManager.clear();

            assertThat(inquiryRepo.count()).isEqualTo(3);
        }

        @Test
        @DisplayName("should fail when groupRefId is null (NOT NULL constraint)")
        void save_nullProductRefId_throwsException() {
            Inquiry inquiry = createValidInquiry();
            inquiry.setGroupRefId(null);

            assertThatThrownBy(() -> inquiryRepo.saveAndFlush(inquiry))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("null value in column \"group_ref_id\"");
        }

        @Test
        @DisplayName("should fail when customerRefId is null (NOT NULL constraint)")
        void save_nullCustomerRefId_throwsException() {
            Inquiry inquiry = createValidInquiry();
            inquiry.setCustomerRefId(null);

            assertThatThrownBy(() -> inquiryRepo.saveAndFlush(inquiry))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("null value in column \"customer_ref_id\"");
        }

        @Test
        @DisplayName("should fail when managerRefId is null (NOT NULL constraint)")
        void save_nullManagerRefId_throwsException() {
            Inquiry inquiry = createValidInquiry();
            inquiry.setManagerRefId(null);

            assertThatThrownBy(() -> inquiryRepo.saveAndFlush(inquiry))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("null value in column \"manager_ref_id\"");
        }

        @Test
        @DisplayName("should fail when source is null (NOT NULL constraint)")
        void save_nullSource_throwsException() {
            Inquiry inquiry = createValidInquiry();
            inquiry.setSource(null);

            assertThatThrownBy(() -> inquiryRepo.saveAndFlush(inquiry))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("null value in column \"source\"");
        }

        @Test
        @DisplayName("should fail when source exceeds max length (100 chars)")
        void save_sourceTooLong_throwsException() {
            Inquiry inquiry = createValidInquiry();
            inquiry.setSource("A".repeat(101));

            assertThatThrownBy(() -> inquiryRepo.saveAndFlush(inquiry))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("value too long for type character varying(100)");
        }

        @Test
        @DisplayName("should fail when status is null (NOT NULL constraint)")
        void save_nullStatus_throwsException() {
            Inquiry inquiry = createValidInquiry();
            inquiry.setStatus(null);

            assertThatThrownBy(() -> inquiryRepo.saveAndFlush(inquiry))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class)
                    .hasMessageContaining("null value in column \"status\"");
        }
    }

    @Nested
    @DisplayName("findAllByFilter()")
    class FindAllByFilterTests {

        @BeforeEach
        void seedTestData() {
            inquiryRepo.save(createInquiryWith(InquiryStatus.NEW, testCustomerId, testManagerId));
            inquiryRepo.save(createInquiryWith(InquiryStatus.IN_PROGRESS, testCustomerId, testManagerId));
            inquiryRepo.save(createInquiryWith(InquiryStatus.NEW, UUID.randomUUID(), testManagerId));
            inquiryRepo.save(createInquiryWith(InquiryStatus.REJECTED, testCustomerId, UUID.randomUUID()));
            entityManager.flush();
            entityManager.clear();
        }

        @Test
        @DisplayName("should return all inquiries when all filters are null")
        void findAllByFilter_noFilters_returnsAll() {
            Page<Inquiry> result = inquiryRepo.findAllByFilter(
                    null, null, null,
                    PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(4);
            assertThat(result.getTotalElements()).isEqualTo(4);
        }

        @Test
        @DisplayName("should filter by status only")
        void findAllByFilter_byStatus_filtersCorrectly() {
            Page<Inquiry> result = inquiryRepo.findAllByFilter(
                    InquiryStatus.NEW, null, null,
                    PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).allMatch(i -> i.getStatus() == InquiryStatus.NEW);
        }

        @Test
        @DisplayName("should filter by customerRefId only")
        void findAllByFilter_byCustomerRefId_filtersCorrectly() {
            Page<Inquiry> result = inquiryRepo.findAllByFilter(
                    null, testCustomerId, null,
                    PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getContent()).allMatch(i -> i.getCustomerRefId().equals(testCustomerId));
        }

        @Test
        @DisplayName("should filter by managerRefId only")
        void findAllByFilter_byManagerRefId_filtersCorrectly() {
            Page<Inquiry> result = inquiryRepo.findAllByFilter(
                    null, null, testManagerId,
                    PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getContent()).allMatch(i -> i.getManagerRefId().equals(testManagerId));
        }

        @Test
        @DisplayName("should filter by multiple criteria (AND logic)")
        void findAllByFilter_multipleFilters_appliesAndLogic() {
            Page<Inquiry> result = inquiryRepo.findAllByFilter(
                    InquiryStatus.NEW, testCustomerId, null,
                    PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent()).allMatch(i ->
                    i.getStatus() == InquiryStatus.NEW && i.getCustomerRefId().equals(testCustomerId));
        }

        @Test
        @DisplayName("should apply pagination correctly")
        void findAllByFilter_withPagination_appliesLimitAndOffset() {
            Page<Inquiry> page0 = inquiryRepo.findAllByFilter(
                    null, null, null,
                    PageRequest.of(0, 2, Sort.by(InquirySortField.CREATED_AT.getSortableAttribute()).descending()));

            assertThat(page0.getContent()).hasSize(2);
            assertThat(page0.getNumber()).isZero();
            assertThat(page0.getTotalPages()).isEqualTo(2);

            Page<Inquiry> page1 = inquiryRepo.findAllByFilter(
                    null, null, null,
                    PageRequest.of(1, 2, Sort.by(InquirySortField.CREATED_AT.getSortableAttribute()).descending()));

            assertThat(page1.getContent()).hasSize(2);
            assertThat(page1.getNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("should apply sorting correctly")
        void findAllByFilter_withSorting_sortsCorrectly() {
            Page<Inquiry> result = inquiryRepo.findAllByFilter(
                    null, null, null,
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, InquirySortField.STATUS.getSortableAttribute())));

            assertThat(result.getContent()).isSortedAccordingTo(Comparator.comparing(i -> i.getStatus().name()));
        }

        @Test
        @DisplayName("should handle null pageable with default behavior")
        void findAllByFilter_nullPageable_usesDefaults() {
            Page<Inquiry> result = inquiryRepo.findAllByFilter(null, null, null, null);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isNotEmpty();
        }
    }

    private Inquiry createValidInquiry() {
        Inquiry inquiry = new Inquiry();
        inquiry.setGroupRefId(testGroupId);
        inquiry.setCustomerRefId(testCustomerId);
        inquiry.setManagerRefId(testManagerId);
        inquiry.setSource("WEB");
        inquiry.setStatus(InquiryStatus.NEW);
        return inquiry;
    }

    private Inquiry createInquiryWith(InquiryStatus status, UUID customerId, UUID managerId) {
        Inquiry inquiry = createValidInquiry();
        inquiry.setStatus(status);
        inquiry.setCustomerRefId(customerId);
        inquiry.setManagerRefId(managerId);
        return inquiry;
    }
}
