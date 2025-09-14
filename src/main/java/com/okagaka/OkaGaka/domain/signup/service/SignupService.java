package com.okagaka.OkaGaka.domain.signup.service;

import com.okagaka.OkaGaka.common.exception.CustomException;
import com.okagaka.OkaGaka.common.exception.ErrorCode;
import com.okagaka.OkaGaka.common.external.embedding.EmbeddingApiClient;
import com.okagaka.OkaGaka.common.external.tmap.Coordinate;
import com.okagaka.OkaGaka.common.s3.S3Service;
import com.okagaka.OkaGaka.domain.familygroup.entity.FamilyGroup;
import com.okagaka.OkaGaka.domain.familygroup.repository.FamilyGroupRepository;
import com.okagaka.OkaGaka.domain.signup.dto.*;
import com.okagaka.OkaGaka.domain.signup.entity.SignupTemp;
import com.okagaka.OkaGaka.domain.signup.entity.TempZone;
import com.okagaka.OkaGaka.domain.signup.repository.SignupTempRepository;
import com.okagaka.OkaGaka.domain.signup.repository.TempZoneRepository;
import com.okagaka.OkaGaka.domain.user.entity.User;
import com.okagaka.OkaGaka.domain.user.entity.UserFaceImage;
import com.okagaka.OkaGaka.domain.user.repository.UserFaceImageRepository;
import com.okagaka.OkaGaka.domain.user.repository.UserRepository;
import com.okagaka.OkaGaka.domain.user.service.FaceEmbeddingService;
import com.okagaka.OkaGaka.domain.vehicle.entity.Vehicle;
import com.okagaka.OkaGaka.domain.vehicle.repository.VehicleRepository;
import com.okagaka.OkaGaka.domain.zone.entity.Zone;
import com.okagaka.OkaGaka.domain.zone.repository.ZoneRepository;
import com.okagaka.OkaGaka.common.external.tmap.TmapGeocodingClient;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final FaceEmbeddingService faceEmbeddingService;
    private final EmbeddingApiClient embeddingApiClient;

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
            System.out.println("[ERROR] tempId에 해당하는 SignupTemp를 찾을 수 없습니다. tempId=" + tempId);
            throw new RuntimeException("tempId에 해당하는 SignupTemp를 찾을 수 없습니다. tempId=" + tempId);
        }
        System.out.println("[DEBUG] SignupTemp 조회 완료: " + signup);


        Coordinate coordinate;
        try {
            coordinate = tmapGeocodingClient.getCoordinates(
                    request.getCityDo(), request.getGuGun(), request.getDong(), request.getBunji()
            );
            System.out.println("[DEBUG] 좌표 변환 완료: lat=" + coordinate.getLat() + ", lon=" + coordinate.getLon());
        } catch (Exception e) {
            System.out.println("[ERROR] 좌표 변환 중 예외 발생: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("좌표 API 호출 실패", e);
        }

        // 3. 좌표 null 체크
        if (coordinate == null
                || coordinate.getLat() == null
                || coordinate.getLat().isEmpty()
                || coordinate.getLon() == null
                || coordinate.getLon().isEmpty()) {
            System.out.println("[ERROR] 좌표 변환 결과가 유효하지 않음");
            throw new RuntimeException("좌표 변환 실패: " + request.getCityDo() + " " + request.getGuGun() + " " + request.getDong() + " " + request.getBunji());
        }

        double latitude;
        double longitude;
        try {
            latitude = Double.parseDouble(coordinate.getLat());
            longitude = Double.parseDouble(coordinate.getLon());
            System.out.println("[DEBUG] 좌표 파싱 완료: latitude=" + latitude + ", longitude=" + longitude);
        } catch (NumberFormatException e) {
            System.out.println("[ERROR] 좌표 파싱 실패: lat=" + coordinate.getLat() + ", lon=" + coordinate.getLon());
            e.printStackTrace();
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
    public FamilyCreateResponse createFamilyGroup(Long tempId, String familyName,
                                                  String cityDo, String guGun, String dong, String bunji) {

        // 주소 -> 좌표 변환
        Coordinate coordinate = tmapGeocodingClient.getCoordinates(cityDo, guGun, dong, bunji);


        double latitude = Double.parseDouble(coordinate.getLat());
        double longitude = Double.parseDouble(coordinate.getLon());

//        Vehicle vehicle = vehicleRepository.save(
//                Vehicle.builder().model(vehicleModel).build()
//        );

        FamilyGroup family = familyGroupRepository.save(
                FamilyGroup.builder()
                        .name(familyName)
//                        .vehicle(vehicle)
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
//                .vehicleModel(vehicleModel)
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

        // UserFaceImage 1차 저장 (imageId 생성)
        List<UserFaceImage> savedFaceImages = new ArrayList<>(); // 실제 엔티티 리스트
        List<FaceImageInfo> faceImageInfosForApi = new ArrayList<>(); // API 요청용 DTO 리스트

        if (signup.getFaceImages() != null) {
            for (String url : signup.getFaceImages()) {
                UserFaceImage savedImage = userFaceImageRepository.save(
                        UserFaceImage.builder()
                                .user(user)
                                .imageUrl(url)
                                .build()
                );
                savedFaceImages.add(savedImage); // 업데이트를 위해 엔티티 저장
                faceImageInfosForApi.add(new FaceImageInfo(savedImage.getId(), savedImage.getImageUrl())); // API 요청에 사용할 정보 저장
            }
        }

        System.out.println("UserFaceImage 1차 저장 완료");

        // Embedding API 호출 및 결과 업데이트 로직
        if(!savedFaceImages.isEmpty()){
            faceEmbeddingService.createAndSaveEmbeddings(user.getId(), faceImageInfosForApi);
//            // Embedding API 호출
//            EmbeddingRequestDto embeddingRequest = new EmbeddingRequestDto(user.getId(), faceImageInfosForApi);
//            EmbeddingResponseDto embeddingResponse = embeddingApiClient.getEmbeddings(embeddingRequest);
//            System.out.println("Embedding API 호출 결과: " + embeddingResponse);
//
//            // 응답 결과를 바탕으로 UserFaceImage에 embeddingUrl 업데이트
//            // 빠른 조회를 위해 Map으로 변환 (Key: imageId, Value: embeddingUrl)
//            Map<Long, String> embeddingUrlMap = embeddingResponse.getData().getItems().stream()
//                    .collect(Collectors.toMap(EmbeddingItem::getId, EmbeddingItem::getEmbeddingUrl));
//
//            // 저장했던 이미지 엔티티들을 순회하며 URL 업데이트
//            savedFaceImages.forEach(image -> {
//                String embeddingUrl = embeddingUrlMap.get(image.getId());
//                if (embeddingUrl != null) {
//                    image.updateEmbeddingImageUrl(embeddingUrl);
//                }
//            });
        }

//        List<FaceImageInfo> faceImageInfos = new ArrayList<>(); // DTO 리스트를 생성
//        if (signup.getFaceImages() != null) {
//            for (String url : signup.getFaceImages()) {
//                // save() 메서드는 DB에 저장된 후 ID가 부여된 엔티티를 반환합니다.
//                UserFaceImage savedImage = userFaceImageRepository.save(
//                        UserFaceImage.builder()
//                                .user(user)
//                                .imageUrl(url)
//                                .build()
//                );
//                // 반환된 엔티티에서 ID와 URL을 꺼내 DTO를 만들어 리스트에 추가합니다.
//                faceImageInfos.add(new FaceImageInfo(savedImage.getId(), savedImage.getImageUrl()));
//            }
//        }

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
//                .faceImages(faceImageInfos)
                .faceImages(faceImageInfosForApi)
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
