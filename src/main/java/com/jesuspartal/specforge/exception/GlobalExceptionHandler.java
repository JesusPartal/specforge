package com.jesuspartal.specforge.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SpecNotFoundException.class)
    public ProblemDetail handleSpecNotFound(SpecNotFoundException ex, WebRequest request) {
        log.error("Spec not found" , ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Spec Not Found");
        pd.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        return pd;
    }

    @ExceptionHandler(InvalidGitHubUrlException.class)
    public ProblemDetail handleInvalidUrl(InvalidGitHubUrlException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Invalid GitHub URL");
        pd.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + "; " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation Failed");
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setTitle("Validation Error");
        pd.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        return pd;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParam(MissingServletRequestParameterException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Missing Request Parameter");
        pd.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        return pd;
    }

    @ExceptionHandler(SpecParseException.class)
    public ProblemDetail handleSpecParse(SpecParseException ex, WebRequest request) {
        log.error("Failed to parse spec", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Spec Parse Error");
        pd.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        return pd;
    }

    @ExceptionHandler(GitHubApiException.class)
    public ProblemDetail handleGitHubApi(GitHubApiException ex, WebRequest request) {
        log.error("GitHub Api error", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
        pd.setTitle("GitHub Api Error");
        pd.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleAll(Exception ex, WebRequest request) {
        log.error("Unhandled exception", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        pd.setTitle("Internal Server Error");
        pd.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        return pd;
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ProblemDetail handleRateLimit(RateLimitExceededException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        pd.setTitle("Rate Limit Exceeded");
        pd.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        return pd;
    }
}
