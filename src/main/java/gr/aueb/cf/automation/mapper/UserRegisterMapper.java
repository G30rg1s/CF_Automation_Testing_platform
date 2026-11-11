package gr.aueb.cf.automation.mapper;

import gr.aueb.cf.automation.dto.UserRegisterDto;
import gr.aueb.cf.automation.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserRegisterMapper {

    public UserEntity toEntity(UserRegisterDto dto, String passwordHash) {
        UserEntity entity = new UserEntity();

        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setFatherName(dto.getFatherName());
        entity.setMotherName(dto.getMotherName());
        entity.setAge(dto.getAge());
        entity.setPasswordHash(passwordHash);

        return entity;
    }
}