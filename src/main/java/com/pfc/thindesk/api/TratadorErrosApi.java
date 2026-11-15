package com.pfc.thindesk.api;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;

/** Converte as exceções dos controladores REST em respostas JSON padronizadas. */
@RestControllerAdvice(annotations = org.springframework.web.bind.annotation.RestController.class)
public class TratadorErrosApi {

    private static final Logger log = LoggerFactory.getLogger(TratadorErrosApi.class);

    @ExceptionHandler(RequisicaoInvalidaException.class)
    public ResponseEntity<ErroApi> invalida(RequisicaoInvalidaException e, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, e.getMessage(), e.campo(), req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroApi> tipoInvalido(MethodArgumentTypeMismatchException e, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro '" + e.getName() + "'.", e.getName(), req);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroApi> parametroAusente(MissingServletRequestParameterException e, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente: " + e.getParameterName() + ".",
                e.getParameterName(), req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroApi> corpoInvalido(HttpMessageNotReadableException e, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição ausente ou com JSON inválido.", null, req);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroApi> naoEncontrado(RecursoNaoEncontradoException e, HttpServletRequest req) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage(), null, req);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErroApi> bancoIndisponivel(DataAccessException e, HttpServletRequest req) {
        log.error("Falha ao acessar o MongoDB em {}", req.getRequestURI(), e);
        return resposta(HttpStatus.SERVICE_UNAVAILABLE, "Banco de dados indisponível. Tente novamente em instantes.", null, req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroApi> inesperado(Exception e, HttpServletRequest req) {
        log.error("Erro inesperado em {}", req.getRequestURI(), e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno ao processar a requisição.", null, req);
    }

    private static ResponseEntity<ErroApi> resposta(HttpStatus status, String mensagem, String campo, HttpServletRequest req) {
        var corpo = new ErroApi(Instant.now(), status.value(), status.getReasonPhrase(), mensagem, campo, req.getRequestURI());
        return ResponseEntity.status(status).body(corpo);
    }
}
