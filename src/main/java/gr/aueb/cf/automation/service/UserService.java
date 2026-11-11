package gr.aueb.cf.automation.service;

import gr.aueb.cf.automation.dto.UserLoginDto;
import gr.aueb.cf.automation.dto.UserLoginResponseDto;
import gr.aueb.cf.automation.dto.UserRegisterDto;
import gr.aueb.cf.automation.dto.UserResponseDto;
import gr.aueb.cf.automation.dto.UserUpdateDto;
import gr.aueb.cf.automation.entity.UserEntity;
import gr.aueb.cf.automation.exception.exceptions.DuplicateEmailException;
import gr.aueb.cf.automation.exception.exceptions.InvalidCredentialsException;
import gr.aueb.cf.automation.exception.exceptions.NotFoundException;
import gr.aueb.cf.automation.exception.exceptions.UserAlreadyExistsException;
import gr.aueb.cf.automation.mapper.UserLoginMapper;
import gr.aueb.cf.automation.mapper.UserRegisterMapper;
import gr.aueb.cf.automation.mapper.UserResponseMapper;
import gr.aueb.cf.automation.mapper.UserUpdateMapper;
import gr.aueb.cf.automation.repository.UserRepository;
import gr.aueb.cf.automation.security.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UserRegisterMapper userRegisterMapper;
    private final UserUpdateMapper userUpdateMapper;
    private final UserResponseMapper userResponseMapper;
    private final UserLoginMapper userLoginMapper;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            UserRegisterMapper userRegisterMapper,
            UserUpdateMapper userUpdateMapper,
            UserResponseMapper userResponseMapper,
            UserLoginMapper userLoginMapper) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.userRegisterMapper = userRegisterMapper;
        this.userUpdateMapper = userUpdateMapper;
        this.userResponseMapper = userResponseMapper;
        this.userLoginMapper = userLoginMapper;
    }

    @Transactional
    public UserResponseDto register(UserRegisterDto request) {
        UserEntity existingUser = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (existingUser != null) {
            if (hasSameRegistrationData(request, existingUser)) {
                throw new UserAlreadyExistsException("User already exists");
            }

            throw new DuplicateEmailException("Email is already registered");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());

        UserEntity user = userRegisterMapper.toEntity(request, passwordHash);
        UserEntity savedUser = userRepository.save(user);

        return userResponseMapper.toDto(savedUser);
    }

    @Transactional(readOnly = true)
    public UserLoginResponseDto login(UserLoginDto request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String accessToken = tokenService.create(user.getId());

        return userLoginMapper.toDto(accessToken, user);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getById(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return userResponseMapper.toDto(user);
    }

    @Transactional
    public UserResponseDto update(Long userId, UserUpdateDto request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        boolean emailBelongsToAnotherUser =
                userRepository.existsByEmailAndIdNot(request.getEmail(), userId);

        if (emailBelongsToAnotherUser) {
            throw new DuplicateEmailException("Email is already registered");
        }

        userUpdateMapper.updateEntity(request, user);
        UserEntity updatedUser = userRepository.save(user);

        return userResponseMapper.toDto(updatedUser);
    }

    //-------------------------------------HELPERS----------------------------------------------------------------------

    private boolean hasSameRegistrationData(
            UserRegisterDto request,
            UserEntity existingUser) {

        return Objects.equals(request.getFirstName(), existingUser.getFirstName())
                && Objects.equals(request.getLastName(), existingUser.getLastName())
                && Objects.equals(request.getEmail(), existingUser.getEmail())
                && Objects.equals(request.getPhone(), existingUser.getPhone())
                && Objects.equals(request.getFatherName(), existingUser.getFatherName())
                && Objects.equals(request.getMotherName(), existingUser.getMotherName())
                && Objects.equals(request.getAge(), existingUser.getAge());

    }
}