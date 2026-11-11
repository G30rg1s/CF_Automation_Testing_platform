package gr.aueb.cf.automation.exception.handlers;

import gr.aueb.cf.automation.dto.documents.CategoryDeleteResponseDto;
import gr.aueb.cf.automation.exception.exceptions.CategoryInUseException;
import gr.aueb.cf.automation.exception.exceptions.CategoryNotFoundException;
import gr.aueb.cf.automation.exception.exceptions.DefaultCategoryProtectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CategoryExceptionHandler {


    @ExceptionHandler(CategoryInUseException.class)
    public ResponseEntity<CategoryDeleteResponseDto> handleCategoryInUse(
            CategoryInUseException exception
    ) {

        CategoryDeleteResponseDto response =
                new CategoryDeleteResponseDto(
                        "CATEGORY_IN_USE",
                        exception.getCategoryId(),
                        exception.getCategoryName(),
                        exception.getMessage()
                );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }


    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<CategoryDeleteResponseDto> handleCategoryNotFound(
            CategoryNotFoundException exception
    ) {

        CategoryDeleteResponseDto response =
                new CategoryDeleteResponseDto(
                        "CATEGORY_NOT_FOUND",
                        exception.getCategoryId(),
                        null,
                        exception.getMessage()
                );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }


    @ExceptionHandler(DefaultCategoryProtectedException.class)
    public ResponseEntity<CategoryDeleteResponseDto> handleDefaultCategoryProtected(
            DefaultCategoryProtectedException exception
    ) {

        CategoryDeleteResponseDto response =
                new CategoryDeleteResponseDto(
                        "DEFAULT_CATEGORY_PROTECTED",
                        exception.getCategoryId(),
                        exception.getCategoryName(),
                        exception.getMessage()
                );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }
}