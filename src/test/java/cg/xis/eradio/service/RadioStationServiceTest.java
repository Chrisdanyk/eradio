package cg.xis.eradio.service;

import cg.xis.eradio.domain.entity.RadioStation;
import cg.xis.eradio.domain.repository.RadioStationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class RadioStationServiceTest {

    @Mock
    private RadioStationRepository radioStationRepository;

    @Mock
    private cg.xis.eradio.domain.repository.FavoriteRepository favoriteRepository;

    @Mock
    private cg.xis.eradio.infrastructure.client.RadioBrowserApiClient radioBrowserApiClient;

    @Mock
    private cg.xis.eradio.infrastructure.mapper.RadioStationMapper radioStationMapper;

    @InjectMocks
    private RadioStationService radioStationService;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(radioStationRepository, favoriteRepository, radioBrowserApiClient, radioStationMapper);
    }

    @Test
    void findByStationUuidInBatched_WhenNull_ThrowsIllegalArgumentException() {
        // Given
        Collection<String> nullUuids = null;

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> radioStationService.findByStationUuidInBatched(nullUuids)
        );

        assertEquals("Station UUIDs collection cannot be null", exception.getMessage());
        verify(radioStationRepository, never()).findByStationUuidIn(anyList());
    }

    @Test
    void findByStationUuidInBatched_WhenEmpty_ReturnsEmptyList() {
        // Given
        Collection<String> emptyUuids = Collections.emptyList();

        // When
        List<RadioStation> result = radioStationService.findByStationUuidInBatched(emptyUuids);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(radioStationRepository, never()).findByStationUuidIn(anyList());
    }

    @Test
    void findByStationUuidInBatched_WhenSmallCollection_CallsRepositoryOnce() {
        // Given
        List<String> uuids = List.of("uuid1", "uuid2", "uuid3");
        List<RadioStation> expectedStations = createTestStations(uuids);

        when(radioStationRepository.findByStationUuidIn(uuids)).thenReturn(expectedStations);

        // When
        List<RadioStation> result = radioStationService.findByStationUuidInBatched(uuids);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(expectedStations, result);
        verify(radioStationRepository, times(1)).findByStationUuidIn(uuids);
    }

    @Test
    void findByStationUuidInBatched_WhenExceedsBatchSize_SplitsIntoMultipleBatches() {
        // Given - Create 1200 UUIDs (exceeds MAX_BATCH_SIZE of 500)
        List<String> uuids = new ArrayList<>();
        for (int i = 1; i <= 1200; i++) {
            uuids.add("uuid" + i);
        }

        // Mock repository to return stations for each batch
        when(radioStationRepository.findByStationUuidIn(anyList())).thenAnswer(invocation -> {
            List<String> batch = invocation.getArgument(0);
            return createTestStations(batch);
        });

        // When
        List<RadioStation> result = radioStationService.findByStationUuidInBatched(uuids);

        // Then
        assertNotNull(result);
        assertEquals(1200, result.size());

        // Verify repository was called 3 times (500 + 500 + 200)
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(radioStationRepository, times(3)).findByStationUuidIn(captor.capture());

        // Verify batch sizes
        List<List<String>> allBatches = captor.getAllValues();
        assertEquals(3, allBatches.size());
        assertEquals(500, allBatches.get(0).size());
        assertEquals(500, allBatches.get(1).size());
        assertEquals(200, allBatches.get(2).size());
    }

    @Test
    void findByStationUuidInBatched_WhenExactlyBatchSize_CallsRepositoryOnce() {
        // Given - Create exactly 500 UUIDs (MAX_BATCH_SIZE)
        List<String> uuids = new ArrayList<>();
        for (int i = 1; i <= 500; i++) {
            uuids.add("uuid" + i);
        }

        List<RadioStation> expectedStations = createTestStations(uuids);
        when(radioStationRepository.findByStationUuidIn(uuids)).thenReturn(expectedStations);

        // When
        List<RadioStation> result = radioStationService.findByStationUuidInBatched(uuids);

        // Then
        assertNotNull(result);
        assertEquals(500, result.size());
        verify(radioStationRepository, times(1)).findByStationUuidIn(uuids);
    }

    @Test
    void findByStationUuidInBatched_WhenMultipleBatches_AggregatesResultsCorrectly() {
        // Given - Create 750 UUIDs (1.5 batches)
        List<String> uuids = new ArrayList<>();
        for (int i = 1; i <= 750; i++) {
            uuids.add("uuid" + i);
        }

        // Mock repository to return different stations for each batch
        when(radioStationRepository.findByStationUuidIn(anyList())).thenAnswer(invocation -> {
            List<String> batch = invocation.getArgument(0);
            return createTestStations(batch);
        });

        // When
        List<RadioStation> result = radioStationService.findByStationUuidInBatched(uuids);

        // Then
        assertNotNull(result);
        assertEquals(750, result.size());

        // Verify all UUIDs are present in results
        Set<String> resultUuids = result.stream()
                .map(RadioStation::getStationUuid)
                .collect(java.util.stream.Collectors.toSet());

        assertEquals(750, resultUuids.size());
        assertTrue(resultUuids.containsAll(uuids));

        // Verify repository was called 2 times (500 + 250)
        verify(radioStationRepository, times(2)).findByStationUuidIn(anyList());
    }

    @Test
    void findByStationUuidInBatched_WhenRepositoryReturnsEmpty_AggregatesEmptyResults() {
        // Given
        List<String> uuids = List.of("uuid1", "uuid2", "uuid3");
        when(radioStationRepository.findByStationUuidIn(anyList())).thenReturn(Collections.emptyList());

        // When
        List<RadioStation> result = radioStationService.findByStationUuidInBatched(uuids);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(radioStationRepository, times(1)).findByStationUuidIn(uuids);
    }

    @Test
    void findByStationUuidInBatched_WhenSetProvided_ProcessesCorrectly() {
        // Given - Test with Set instead of List
        Set<String> uuidSet = Set.of("uuid1", "uuid2", "uuid3");
        List<RadioStation> expectedStations = createTestStations(new ArrayList<>(uuidSet));

        when(radioStationRepository.findByStationUuidIn(anyList())).thenReturn(expectedStations);

        // When
        List<RadioStation> result = radioStationService.findByStationUuidInBatched(uuidSet);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(radioStationRepository, times(1)).findByStationUuidIn(anyList());
    }

    /**
     * Helper method to create test RadioStation entities from UUIDs
     */
    private List<RadioStation> createTestStations(List<String> uuids) {
        List<RadioStation> stations = new ArrayList<>();
        for (int i = 0; i < uuids.size(); i++) {
            String uuid = uuids.get(i);
            RadioStation station = RadioStation.builder()
                    .id((long) (i + 1))
                    .stationUuid(uuid)
                    .name("Station " + uuid)
                    .url("http://example.com/stream" + i)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            stations.add(station);
        }
        return stations;
    }
}

