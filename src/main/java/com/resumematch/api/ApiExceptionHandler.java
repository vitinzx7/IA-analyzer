package com.resumematch.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import com.resumematch.analysis.AnalysisException;
import com.resumematch.pdf.PdfExtractionException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(AnalysisException.class)
    public ProblemDetail handleAnalysis(AnalysisException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Requisicao invalida", exception.getMessage());
    }

    @ExceptionHandler(PdfExtractionException.class)
    public ProblemDetail handlePdf(PdfExtractionException exception) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "PDF nao processavel", exception.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleUploadSize(MaxUploadSizeExceededException exception) {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "Upload muito grande",
                "O arquivo enviado passa do limite permitido.");
    }

    @ExceptionHandler(MultipartException.class)
    public ProblemDetail handleMultipart(MultipartException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Upload invalido",
                "Nao foi possivel ler o formulario enviado.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleMediaType(HttpMediaTypeNotSupportedException exception) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formato nao suportado",
                "Use multipart/form-data ou application/json.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethod(HttpRequestMethodNotSupportedException exception) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "Metodo nao permitido",
                "Este endpoint nao aceita o metodo HTTP usado.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        LOG.error("Erro inesperado na API", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Nao foi possivel concluir a analise.");
    }

    private ProblemDetail problem(HttpStatusCode status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
