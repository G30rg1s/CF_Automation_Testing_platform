package gr.aueb.cf.automation.mapper;

import gr.aueb.cf.automation.dto.UserResponseDto;
import gr.aueb.cf.automation.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserResponseMapper {

    public UserResponseDto toDto(UserEntity entity) {
        UserResponseDto dto = new UserResponseDto();

        dto.setId(entity.getId());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setFatherName(entity.getFatherName());
        dto.setMotherName(entity.getMotherName());
        dto.setAge(entity.getAge());

        return dto;
    }
}