package com.okagaka.OkaGaka.domain.signup.service;

import com.okagaka.OkaGaka.common.exception.CustomException;
import com.okagaka.OkaGaka.common.exception.ErrorCode;
import com.okagaka.OkaGaka.common.external.tmap.Coordinate;
import com.okagaka.OkaGaka.common.s3.S3Service;
import com.okagaka.OkaGaka.domain.familygroup.entity.FamilyGroup;
import com.okagaka.OkaGaka.domain.familygroup.repository.FamilyGroupRepository;
import com.okagaka.OkaGaka.domain.signup.dto.FamilyCreateResponse;
import com.okagaka.OkaGaka.domain.signup.dto.FamilySearchResponse;
import com.okagaka.OkaGaka.domain.signup.dto.ZoneRequest;
import com.okagaka.OkaGaka.domain.signup.entity.SignupTemp;
import com.okagaka.OkaGaka.domain.signup.entity.TempZone;
import com.okagaka.OkaGaka.domain.signup.repository.SignupTempRepository;
import com.okagaka.OkaGaka.domain.signup.repository.TempZoneRepository;
import com.okagaka.OkaGaka.domain.user.entity.User;
import com.okagaka.OkaGaka.domain.user.entity.UserFaceImage;
import com.okagaka.OkaGaka.domain.user.repository.UserFaceImageRepository;
import com.okagaka.OkaGaka.domain.user.repository.UserRepository;
import com.okagaka.OkaGaka.domain.vehicle.entity.Vehicle;
import com.okagaka.OkaGaka.domain.vehicle.repository.VehicleRepository;
import com.okagaka.OkaGaka.domain.zone.entity.Zone;
import com.okagaka.OkaGaka.domain.zone.repository.ZoneRepository;
import com.okagaka.OkaGaka.domain.signup.dto.ZoneResponse;
import com.okagaka.OkaGaka.domain.signup.dto.SignupCompleteResponse;
import com.okagaka.OkaGaka.common.external.tmap.TmapGeocodingClient;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SignupService {

    private final SignupTempRepository signupTempRepository;
    private final FamilyGroupRepository familyGroupRepository;
    private final UserRepository userRepository;
    private final UserFaceImageRepository userFaceImageRepository;
    private final VehicleRepository vehicleRepository;
    private final ZoneRepository zoneRepository;
    private final TempZoneRepository tempZoneRepository;
    private final S3Service s3Service;
    private final TmapGeocodingClient tmapGeocodingClient;

    public Long saveName(String name) {
        SignupTemp signup = SignupTemp.builder()
                .name(name)
                .build();
        return signupTempRepository.save(signup).getId();
    }

    @Transactional
    public void savePhone(Long tempId, String phoneNumber) {
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new CustomException(ErrorCode.USER_ALREADY_EXISTS);
        }

        SignupTemp signup = getSignupTemp(tempId);
        signup.setPhoneNumber(phoneNumber);
    }

    @Transactional
    public List<String> saveFaceImages(Long tempId, List<MultipartFile> faceImages) {

        if (faceImages == null || faceImages.size() != 4) {
            throw new CustomException(ErrorCode.IMAGE_INVALID_REQUEST);
        }

        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile image : faceImages) {
            try {
                imageUrls.add(s3Service.uploadImage(image));
            } catch (IOException e) {
                throw new RuntimeException("S3 업로드 실패", e);
            }
        }

        SignupTemp signup = getSignupTemp(tempId);
        signup.setFaceImages(imageUrls);
        signupTempRepository.save(signup);

        return imageUrls;  // 업로드된 이미지 URL 리스트 반환
    }


    @Transactional
    public ZoneResponse saveZone(Long tempId, ZoneRequest request) {

        System.out.println("[DEBUG] saveZone 시작: tempId=" + tempId + ", name=" + request.getName());


        // 1. SignupTemp 조회
        SignupTemp signup = getSignupTemp(tempId);
        if (signup == null) {
            throw new RuntimeException("tempId에 해당하는 SignupTemp를 찾을 수 없습니다. tempId=" + tempId);
        }

        // 2. 주소 -> 좌표 변환
        Coordinate coordinate = tmapGeocodingClient.getCoordinates(
                request.getCityDo(), request.getGuGun(), request.getDong(), request.getBunji()
        );

        // 3. 좌표 null 체크
        if (coordinate == null
                || coordinate.getLat() == null
                || coordinate.getLat().isEmpty()
                || coordinate.getLon() == null
                || coordinate.getLon().isEmpty()) {
            throw new RuntimeException("좌표 변환 실패: " + request.getCityDo() + " " + request.getGuGun() + " " + request.getDong() + " " + request.getBunji());
        }

        double latitude;
        double longitude;
        try {
            latitude = Double.parseDouble(coordinate.getLat());
            longitude = Double.parseDouble(coordinate.getLon());
        } catch (NumberFormatException e) {
            throw new RuntimeException("좌표 파싱 실패: lat=" + coordinate.getLat() + ", lon=" + coordinate.getLon(), e);
        }

        // 4. TempZone 객체 생성
        TempZone tempZone = TempZone.builder()
                .name(request.getName())
                .latitude(latitude)
                .longitude(longitude)
                .signupTemp(signup)
                .build();

        System.out.println("[DEBUG] 생성된 TempZone: " + tempZone);

        // 5. SignupTemp에 추가 후 저장
        signup.getTempZones().add(tempZone);
        SignupTemp savedSignup = signupTempRepository.save(signup);

        System.out.println("[DEBUG] SignupTemp 저장 완료, tempZones 수: " + savedSignup.getTempZones().size());

        // 6. 응답 반환
        return new ZoneResponse(
                request.getName(),
                latitude,
                longitude
        );
    }

    @Transactional
    public FamilySearchResponse searchFamilyGroup(Long tempId, String familyName) {
        boolean exists = familyGroupRepository.existsByName(familyName);
        Long familyId = null;

        if (exists) {
            FamilyGroup family = familyGroupRepository.findByName(familyName)
                    .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));
            familyId = family.getId();

            SignupTemp signup = getSignupTemp(tempId);
            signup.setFamilyId(familyId);
            signupTempRepository.save(signup);
        }

        return FamilySearchResponse.builder()
                .familyId(familyId)
                .exists(exists)
                .familyName(familyName)
                .build();
    }



    @Transactional
    public FamilyCreateResponse createFamilyGroup(Long tempId, String familyName, String vehicleModel,
                                                  String cityDo, String guGun, String dong, String bunji) {

        // 주소 -> 좌표 변환
        Coordinate coordinate = tmapGeocodingClient.getCoordinates(cityDo, guGun, dong, bunji);


        double latitude = Double.parseDouble(coordinate.getLat());
        double longitude = Double.parseDouble(coordinate.getLon());

        Vehicle vehicle = vehicleRepository.save(
                Vehicle.builder().model(vehicleModel).build()
        );

        FamilyGroup family = familyGroupRepository.save(
                FamilyGroup.builder()
                        .name(familyName)
                        .vehicle(vehicle)
                        .homeLatitude(latitude)
                        .homeLongitude(longitude)
                        .build()
        );

        SignupTemp signup = getSignupTemp(tempId);
        signup.setFamilyId(family.getId());
        signupTempRepository.save(signup);

        return FamilyCreateResponse.builder()
                .familyId(family.getId())
                .familyName(familyName)
                .vehicleModel(vehicleModel)
                .homeLatitude(latitude)
                .homeLongitude(longitude)
                .build();
    }

    @Transactional
    public SignupCompleteResponse finalizeSignup(Long tempId) {
        SignupTemp signup = getSignupTemp(tempId);

        FamilyGroup family = familyGroupRepository.findById(signup.getFamilyId())
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        User user = userRepository.save(
                User.builder()
                        .name(signup.getName())
                        .phoneNumber(signup.getPhoneNumber())
                        .familyGroup(family)
                        .build()
        );

        List<String> imageUrls = new ArrayList<>();
        if (signup.getFaceImages() != null) {
            for (String url : signup.getFaceImages()) {
                userFaceImageRepository.save(
                        UserFaceImage.builder()
                                .user(user)
                                .imageUrl(url)
                                .build()
                );
                imageUrls.add(url);
            }
        }

        // zone은 첫 번째 TempZone만 저장
        TempZone tempZone = signup.getTempZones().isEmpty() ? null : signup.getTempZones().get(0);
        Zone zone = null;
        if (tempZone != null) {
            zone = zoneRepository.save(
                    Zone.builder()
                            .name(tempZone.getName())
                            .latitude(tempZone.getLatitude())
                            .longitude(tempZone.getLongitude())
                            .user(user)
                            .build()
            );
        }

        signupTempRepository.deleteById(tempId);

        return SignupCompleteResponse.builder()
                .userId(user.getId())
                .userName(user.getName())
                .phoneNumber(user.getPhoneNumber())
                .imageUrls(imageUrls)
                .familyId(family.getId())
                .zoneId(zone != null ? zone.getId() : null)
                .zoneName(zone != null ? zone.getName() : null)
                .zoneLatitude(zone != null ? zone.getLatitude() : null)
                .zoneLongitude(zone != null ? zone.getLongitude() : null)
                .build();
    }


    private SignupTemp getSignupTemp(Long tempId) {
        return signupTempRepository.findById(tempId)
                .orElseThrow(() -> new CustomException(ErrorCode.SIGNUP_TEMP_NOT_FOUND));
    }
}
