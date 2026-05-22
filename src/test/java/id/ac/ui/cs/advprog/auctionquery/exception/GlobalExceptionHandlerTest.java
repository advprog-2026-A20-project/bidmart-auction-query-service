package id.ac.ui.cs.advprog.auctionquery.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private GlobalExceptionHandler globalExceptionHandler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("GET", "/api/auctions");
    }

    @Test
    void handleBadRequestShouldUseConstraintViolationMessage() {
        Set<ConstraintViolation<InvalidPayload>> violations = validator.validate(new InvalidPayload(""));
        ConstraintViolationException exception = new ConstraintViolationException(violations);

        var response = globalExceptionHandler.handleBadRequest(exception, request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("must not be blank", response.getBody().message());
    }

    @Test
    void handleBadRequestShouldUseMethodArgumentNotValidMessage() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new InvalidPayload(""), "payload");
        bindingResult.addError(new ObjectError("payload", "payload is invalid"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
            new MethodParameter(GlobalExceptionHandlerTest.class.getDeclaredMethod("sampleMethod", String.class), 0),
            bindingResult
        );

        var response = globalExceptionHandler.handleBadRequest(exception, request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("payload is invalid", response.getBody().message());
    }

    @Test
    void handleBadRequestShouldFallbackToExceptionMessage() {
        IllegalArgumentException exception = new IllegalArgumentException("invalid request");

        var response = globalExceptionHandler.handleBadRequest(exception, request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("invalid request", response.getBody().message());
    }

    @Test
    void handleGenericExceptionShouldReturnUnexpectedServerError() {
        Exception exception = new RuntimeException("boom");

        var response = globalExceptionHandler.handleGenericException(exception, request);

        assertEquals(500, response.getStatusCode().value());
        assertEquals("Unexpected server error", response.getBody().message());
    }

    private void sampleMethod(String payload) {
    }

    private record InvalidPayload(@NotBlank String value) {
    }
}
