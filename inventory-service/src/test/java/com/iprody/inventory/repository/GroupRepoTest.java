package com.iprody.inventory.repository;

import com.iprody.inventory.configuration.PostgresTestConfig;
import com.iprody.inventory.model.Group;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(PostgresTestConfig.class)
@DisplayName("GroupRepo Integration Tests")
class GroupRepoTest {
    @Autowired
    private GroupRepo groupRepo;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        groupRepo.deleteAll();
    }


    @Nested
    @DisplayName("findAllByFilter()")
    class FindAllByFilterTests {

        @Nested
        @DisplayName("Positive")
        class PositiveTests {

            @Test
            @DisplayName("should return all groups when no filters provided")
            void findAllByFilter_noFilters_returnsAllGroups() {
                createGroup(UUID.randomUUID(), 5L, 10L);
                createGroup(UUID.randomUUID(), 10L, 10L);
                createGroup(UUID.randomUUID(), 0L, 5L);

                Page<Group> result = groupRepo.findAllByFilter(null, null, Pageable.unpaged());

                assertThat(result.getTotalElements()).isEqualTo(3);
                assertThat(result.getContent()).hasSize(3);
            }

            @Test
            @DisplayName("should filter by groupRefId when provided")
            void findAllByFilter_byGroupRefId_returnsMatchingGroups() {
                UUID targetRefId = UUID.randomUUID();
                createGroup(targetRefId, 5L, 10L);
                createGroup(targetRefId, 8L, 10L);
                createGroup(UUID.randomUUID(), 3L, 5L);

                Page<Group> result = groupRepo.findAllByFilter(targetRefId, null, Pageable.unpaged());

                assertThat(result.getTotalElements()).isEqualTo(2);
                assertThat(result.getContent())
                        .extracting(Group::getGroupRefId)
                        .containsOnly(targetRefId);
            }

            @Test
            @DisplayName("should return groups with available free places when isAvailFreePlaces=true")
            void findAllByFilter_availFreePlacesTrue_returnsOnlyWithFreePlaces() {
                createGroup(UUID.randomUUID(), 5L, 10L);
                createGroup(UUID.randomUUID(), 10L, 10L);
                createGroup(UUID.randomUUID(), 0L, 5L);

                Page<Group> result = groupRepo.findAllByFilter(null, true, Pageable.unpaged());

                assertThat(result.getTotalElements()).isEqualTo(2);
                assertThat(result.getContent())
                        .allMatch(g -> g.getCurrentCount() < g.getLimit());
            }

            @Test
            @DisplayName("should return groups without free places when isAvailFreePlaces=false")
            void findAllByFilter_availFreePlacesFalse_returnsOnlyFullGroups() {
                createGroup(UUID.randomUUID(), 5L, 10L);
                createGroup(UUID.randomUUID(), 10L, 10L);
                createGroup(UUID.randomUUID(), 7L, 5L);

                Page<Group> result = groupRepo.findAllByFilter(null, false, Pageable.unpaged());

                assertThat(result.getTotalElements()).isEqualTo(2);
                assertThat(result.getContent())
                        .allMatch(g -> g.getCurrentCount() >= g.getLimit());
            }

            @Test
            @DisplayName("should apply pagination and sorting correctly")
            void findAllByFilter_withPagination_returnsCorrectPage() {
                for (int i = 0; i < 10; i++) {
                    createGroup(UUID.randomUUID(), (long) i, 10L);
                }

                Pageable pageable = PageRequest.of(1, 3, Sort.by("currentCount").ascending());
                Page<Group> result = groupRepo.findAllByFilter(null, null, pageable);

                assertThat(result.getTotalElements()).isEqualTo(10);
                assertThat(result.getTotalPages()).isEqualTo(4);
                assertThat(result.getNumber()).isEqualTo(1);
                assertThat(result.getContent()).hasSize(3);
                assertThat(result.getContent())
                        .extracting(Group::getCurrentCount)
                        .isSorted();
            }
        }

        @Nested
        @DisplayName("Negative")
        class NegativeTests {

            @Test
            @DisplayName("should return empty page when no groups match combined filters")
            void findAllByFilter_noMatches_returnsEmptyPage() {
                createGroup(UUID.randomUUID(), 5L, 10L);
                createGroup(UUID.randomUUID(), 0L, 5L);

                Page<Group> result = groupRepo.findAllByFilter(null, false, Pageable.unpaged());

                assertThat(result.isEmpty()).isTrue();
                assertThat(result.getTotalElements()).isZero();
            }

            @Test
            @DisplayName("should return empty page when groupRefId does not exist")
            void findAllByFilter_nonExistentGroupRefId_returnsEmpty() {
                createGroup(UUID.randomUUID(), 5L, 10L);
                UUID nonExistentRefId = UUID.randomUUID();

                Page<Group> result = groupRepo.findAllByFilter(nonExistentRefId, null, Pageable.unpaged());

                assertThat(result.isEmpty()).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("decrementCountByGroupRefIdId()")
    class DecrementCountTests {

        @Nested
        @DisplayName("Positive")
        class PositiveTests {

            @Test
            @DisplayName("should decrement currentCount by 1 and return 1 when count > 0")
            void decrementCount_success_decrementsAndReturnsOne() {
                UUID groupRefId = UUID.randomUUID();
                createGroup(groupRefId, 5L, 10L);

                int updatedRows = groupRepo.decrementCountByGroupRefIdId(groupRefId);
                entityManager.clear();

                assertThat(updatedRows).isEqualTo(1);

                Group updated = groupRepo.findByGroupRefId(groupRefId).orElseThrow();
                assertThat(updated.getCurrentCount()).isEqualTo(4L);
            }

            @Test
            @DisplayName("should decrement multiple times correctly until zero")
            void decrementCount_multipleCalls_decrementsUntilZero() {
                UUID groupRefId = UUID.randomUUID();
                createGroup(groupRefId, 2L, 10L);

                int r2 = groupRepo.decrementCountByGroupRefIdId(groupRefId);
                int r3 = groupRepo.decrementCountByGroupRefIdId(groupRefId);
                entityManager.clear();

                assertThat(r2).isEqualTo(1);
                assertThat(r3).isEqualTo(1);

                Group finalState = groupRepo.findByGroupRefId(groupRefId).orElseThrow();
                assertThat(finalState.getCurrentCount()).isEqualTo(0L);
            }
        }

        @Nested
        @DisplayName("Negative")
        class NegativeTests {

            @Test
            @DisplayName("should return 0 and not decrement when currentCount is 0")
            void decrementCount_zeroCount_noUpdate() {
                UUID groupRefId = UUID.randomUUID();
                createGroup(groupRefId, 0L, 10L);

                int updatedRows = groupRepo.decrementCountByGroupRefIdId(groupRefId);

                assertThat(updatedRows).isEqualTo(0);

                Group unchanged = groupRepo.findByGroupRefId(groupRefId).orElseThrow();
                assertThat(unchanged.getCurrentCount()).isEqualTo(0L);
            }

            @Test
            @DisplayName("should return 0 when groupRefId does not exist")
            void decrementCount_notFound_noUpdate() {
                UUID nonExistentRefId = UUID.randomUUID();

                int updatedRows = groupRepo.decrementCountByGroupRefIdId(nonExistentRefId);

                assertThat(updatedRows).isEqualTo(0);
            }

            @Test
            @DisplayName("should not decrement below zero (atomic guard works)")
            void decrementCount_guardPreventsNegativeCount() {
                UUID groupRefId = UUID.randomUUID();
                createGroup(groupRefId, 1L, 10L);

                groupRepo.decrementCountByGroupRefIdId(groupRefId);
                int secondCall = groupRepo.decrementCountByGroupRefIdId(groupRefId);
                entityManager.clear();

                assertThat(secondCall).isEqualTo(0);

                Group finalState = groupRepo.findByGroupRefId(groupRefId).orElseThrow();
                assertThat(finalState.getCurrentCount()).isEqualTo(0L);
            }
        }
    }

    @Nested
    @DisplayName("findByGroupRefId()")
    class FindByGroupRefIdTests {

        @Nested
        @DisplayName("Positive")
        class PositiveTests {

            @Test
            @DisplayName("should return group when found by groupRefId")
            void findByGroupRefId_found_returnsGroup() {
                UUID groupRefId = UUID.randomUUID();
                Group expected = createGroup(groupRefId, 5L, 10L);

                Optional<Group> result = groupRepo.findByGroupRefId(groupRefId);

                assertThat(result).isPresent();
                assertThat(result.get().getId()).isEqualTo(expected.getId());
                assertThat(result.get().getGroupRefId()).isEqualTo(groupRefId);
            }
        }

        @Nested
        @DisplayName("Negative")
        class NegativeTests {

            @Test
            @DisplayName("should return empty Optional when groupRefId does not exist")
            void findByGroupRefId_notFound_returnsEmpty() {
                UUID nonExistentRefId = UUID.randomUUID();

                Optional<Group> result = groupRepo.findByGroupRefId(nonExistentRefId);

                assertThat(result).isEmpty();
            }

            @Test
            @DisplayName("should return empty Optional when groupRefId is null")
            void findByGroupRefId_nullRefId_returnsEmpty() {
                Optional<Group> result = groupRepo.findByGroupRefId(null);

                assertThat(result).isEmpty();
            }
        }
    }

    private Group createGroup(UUID groupRefId, Long currentCount, Long limit) {
        Group group = new Group();
        group.setGroupRefId(groupRefId);
        group.setCurrentCount(currentCount);
        group.setLimit(limit);
        return groupRepo.save(group);
    }
}
