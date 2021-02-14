package cn.wildfirechat.app;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN, reason="Forbidden")
public class ForbiddenException extends RuntimeException {
}
