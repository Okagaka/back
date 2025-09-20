package com.okagaka.OkaGaka.domain.location.service;

import com.okagaka.OkaGaka.domain.location.dto.LocationDTO;
import com.okagaka.OkaGaka.domain.vehicle.dto.VehicleLocationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper; // JSON 파싱을 위해 필요

import javax.xml.stream.Location;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;

@Service
public class LocationCacheService {

    private static final String LOCATION_KEY = "group:%d:user:%d:location";
    private static final long LOCATION_TTL_MINUTES = 1440; // 위치 정보 유지 시간 (예: 30분)
    private static final String VEHICLE_LOCATION_KEY = "group:%d:vehicle:%d:location";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper; // Jackson ObjectMapper 주입

    /**
     * 사용자의 현재 위치를 Redis에 저장
     * 키: group:{groupId}:user:{userId}:location
     * 값: LocationDTO JSON 문자열
     * TTL: LOCATION_TTL_MINUTES (예: 30분)
     */
    public void saveLocation(LocationDTO dto) {
        try {
            String key = String.format(LOCATION_KEY, dto.getGroupId(), dto.getUserId());
            String locationJson = objectMapper.writeValueAsString(dto);
            redisTemplate.opsForValue().set(key, locationJson, LOCATION_TTL_MINUTES, TimeUnit.MINUTES);
            // set(key, value, timeout, unit) 메서드를 사용하여 TTL 설정
            System.out.println("Location saved for user " + dto.getUserId() + " in group " + dto.getGroupId() + ". Key: " + key);
        } catch (Exception e) {
            System.err.println("Failed to save location to Redis for user " + dto.getUserId() + ": " + e.getMessage());
            // 로깅 또는 예외 처리
        }
    }

//    public void saveLocation(LocationDTO dto) {
//        String key = String.format(LOCATION_KEY, dto.getGroupId(), dto.getUserId());
//        redisTemplate.opsForValue().set(key, dto); // TTL 설정 필요시 set(key, value, Duration)
//    }

    // 차량의 현재 위치/상태를 Redis에 저장
    public void saveVehicleLocation(Long groupId, Long vehicleId, VehicleLocationDTO dto) {
        try {
            String key = String.format(VEHICLE_LOCATION_KEY, groupId, vehicleId);
            String locationJson = objectMapper.writeValueAsString(dto);
            // 차량 정보는 더 자주 업데이트될 수 있으므로 TTL을 적절히 조절 (예: 10분)
            redisTemplate.opsForValue().set(key, locationJson, 10, TimeUnit.MINUTES); // TTL 10분으로 설정
        } catch (Exception e) {
            // 로깅
            System.err.println("Failed to save vehicle location to Redis: " + e.getMessage());
        }
    }

    // 특정 차량의 위치를 Redis에서 가져옴
    public Optional<VehicleLocationDTO> getVehicleLocation(Long groupId, Long vehicleId) {
        String key = String.format(VEHICLE_LOCATION_KEY, groupId, vehicleId);
        String locationJson = redisTemplate.opsForValue().get(key);
        if (locationJson != null) {
            try {
                VehicleLocationDTO dto = objectMapper.readValue(locationJson, VehicleLocationDTO.class);
                return Optional.of(dto);
//                return objectMapper.readValue(locationJson, VehicleLocationDTO.class);
            } catch (Exception e) {
                System.err.println("Failed to parse vehicle location from Redis: " + e.getMessage());
                return Optional.empty();
            }
        }
        return Optional.empty();
    }


    /**
     * 특정 그룹에 속한 모든 멤버의 가장 최근 위치 정보를 Redis에서 가져옴
     * (String 값으로 저장된 각 키를 multiGet으로 조회)
     *
     * @param groupId 조회할 그룹의 ID (필수)
     * @param memberIds 해당 그룹에 속한 멤버들의 ID 리스트
     * @return 해당 멤버들의 LocationDTO 리스트
     */
    public List<LocationDTO> getAllGroupLocations(Long groupId, List<Long> memberIds) { // groupId 파라미터 추가
        if (memberIds == null || memberIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 각 멤버의 위치 정보 키 생성
        List<String> keysToFetch = memberIds.stream()
                .map(userId -> String.format(LOCATION_KEY, groupId, userId))
                .collect(Collectors.toList());

        // Redis String 값들을 한 번에 가져오기
        List<String> locationJsons = redisTemplate.opsForValue().multiGet(keysToFetch);

        List<LocationDTO> locations = new ArrayList<>();
        if (locationJsons != null) {
            for (String json : locationJsons) {
                if (json != null) {
                    try {
                        locations.add(objectMapper.readValue(json, LocationDTO.class));
                    } catch (Exception e) {
                        System.err.println("Failed to parse location from Redis: " + e.getMessage());
                    }
                }
            }
        }
        return locations;
    }

//    public List<LocationDTO> getAllGroupLocations(Long groupId, List<Long> userIds){
//        return userIds.stream()
//                .map(userId -> getLocation(groupId, userId))
//                .filter(Objects::nonNull)
//                .collect(Collectors.toList());
//    }

    /**
     * 특정 유저의 위치를 Redis에서 가져옴 (String 값으로 저장된 단일 키 조회)
     */
    public Optional<LocationDTO> getUserLocation(Long groupId, Long userId) { // groupId 파라미터 추가
        String key = String.format(LOCATION_KEY, groupId, userId);
        String locationJson = redisTemplate.opsForValue().get(key);
        if (locationJson != null) {
            try {
                LocationDTO dto = objectMapper.readValue(locationJson, LocationDTO.class);
                return Optional.of(dto);
            } catch (Exception e) {
                System.err.println("Failed to parse single user location from Redis for key " + key + ": " + e.getMessage());
            }
        }
        return Optional.empty();
    }

//    public LocationDTO getLocation(Long groupId, long userId) {
//        String key = String.format(LOCATION_KEY, groupId, userId);
//        return (LocationDTO) redisTemplate.opsForValue().get(key);
//    }

}
