package gr.aueb.cf.automation.mapper;

import gr.aueb.cf.automation.dto.UserLoginResponseDto;
import gr.aueb.cf.automation.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserLoginMapper {

    public UserLoginResponseDto toDto(String accessToken, UserEntity entity) {
        UserLoginResponseDto dto = new UserLoginResponseDto();

        dto.setAccessToken(accessToken);
        dto.setTokenType("Bearer");
        dto.setEmail(entity.getEmail());

        return dto;
    }
}