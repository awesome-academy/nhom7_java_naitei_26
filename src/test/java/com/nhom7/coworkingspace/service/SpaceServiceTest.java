package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.request.SpaceSearchRequest;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.SpaceResponse;
import com.nhom7.coworkingspace.entity.Space;
import com.nhom7.coworkingspace.entity.Venue;
import com.nhom7.coworkingspace.mapper.SpaceMapper;
import com.nhom7.coworkingspace.repository.SpaceRepository;
import com.nhom7.coworkingspace.service.impl.SpaceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpaceService - Unit Tests")
class SpaceServiceTest {

        @Mock
        private SpaceRepository spaceRepository;

        @Mock
        private SpaceMapper spaceMapper;

        @InjectMocks
        private SpaceServiceImpl spaceService;

        private Space mockSpace;
        private SpaceResponse mockResponse;

        @BeforeEach
        void setUp() {
                Venue venue = Venue.builder()
                                .id(1L)
                                .name("Innovation Hub")
                                .city("Hanoi")
                                .street("Kim Ma")
                                .address("123 Kim Ma, Ba Dinh, Hanoi")
                                .build();

                mockSpace = Space.builder()
                                .id(10L)
                                .venue(venue)
                                .name("Private Office A")
                                .type("private office")
                                .price(new BigDecimal("5000000.00"))
                                .priceUnit("month")
                                .openTime(LocalTime.of(8, 0))
                                .closeTime(LocalTime.of(22, 0))
                                .capacity(4)
                                .build();

                mockResponse = SpaceResponse.builder()
                                .id(10L)
                                .venueId(1L)
                                .venueName("Innovation Hub")
                                .name("Private Office A")
                                .type("private office")
                                .price(new BigDecimal("5000000.00"))
                                .priceUnit("month")
                                .openTime(LocalTime.of(8, 0))
                                .closeTime(LocalTime.of(22, 0))
                                .capacity(4)
                                .build();
        }

        @Test
        @DisplayName("Should return paginated space search results")
        void givenSearchRequest_whenSearchSpaces_thenReturnPageResponse() {
                SpaceSearchRequest request = SpaceSearchRequest.builder()
                                .name("Private Office")
                                .city("Hanoi")
                                .type("private office")
                                .page(0)
                                .size(10)
                                .build();

                Page<Space> page = new PageImpl<>(List.of(mockSpace));

                given(spaceRepository.findAll(
                                ArgumentMatchers.<Specification<Space>>any(),
                                any(Pageable.class))).willReturn(page);
                given(spaceMapper.toSpaceResponse(mockSpace)).willReturn(mockResponse);

                PageResponse<SpaceResponse> response = spaceService.searchSpaces(request);

                assertThat(response).isNotNull();
                assertThat(response.getContent()).hasSize(1);
                assertThat(response.getContent().get(0).getName()).isEqualTo("Private Office A");
                assertThat(response.getTotalElements()).isEqualTo(1);

                verify(spaceRepository).findAll(
                                ArgumentMatchers.<Specification<Space>>any(),
                                any(Pageable.class));
        }

        @Test
        @DisplayName("Should search spaces with available time filter (operating hours & booking slot)")
        void givenAvailableTimeFilter_whenSearchSpaces_thenReturnMatchingSpaces() {
                SpaceSearchRequest request = SpaceSearchRequest.builder()
                                .openTime(LocalTime.of(9, 0))
                                .closeTime(LocalTime.of(18, 0))
                                .bookingStart(LocalDateTime.of(2026, 8, 25, 9, 0))
                                .bookingEnd(LocalDateTime.of(2026, 8, 25, 17, 0))
                                .page(0)
                                .size(10)
                                .build();

                Page<Space> page = new PageImpl<>(List.of(mockSpace));

                given(spaceRepository.findAll(
                                ArgumentMatchers.<Specification<Space>>any(),
                                any(Pageable.class))).willReturn(page);
                given(spaceMapper.toSpaceResponse(mockSpace)).willReturn(mockResponse);

                PageResponse<SpaceResponse> response = spaceService.searchSpaces(request);

                assertThat(response).isNotNull();
                assertThat(response.getContent()).hasSize(1);
                assertThat(response.getContent().get(0).getOpenTime()).isEqualTo(LocalTime.of(8, 0));
                assertThat(response.getContent().get(0).getCloseTime()).isEqualTo(LocalTime.of(22, 0));

                verify(spaceRepository).findAll(
                                ArgumentMatchers.<Specification<Space>>any(),
                                any(Pageable.class));
        }
}
