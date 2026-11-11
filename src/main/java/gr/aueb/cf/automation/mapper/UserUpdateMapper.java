package gr.aueb.cf.automation.mapper;

import gr.aueb.cf.automation.dto.UserUpdateDto;
import gr.aueb.cf.automation.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserUpdateMapper {

    public void updateEntity(UserUpdateDto dto, UserEntity entity) {

        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setFatherName(dto.getFatherName());
        entity.setMotherName(dto.getMotherName());
        entity.setAge(dto.getAge());

    }
}