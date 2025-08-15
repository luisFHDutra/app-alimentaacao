package com.example.alimentaacao.util;

/**
 * Wrapper simples para representar sucesso/erro em operações assíncronas.
 */
public class Result<T> {
    public final boolean isSuccess;
    public final T data;
    public final String error;

    private Result(boolean isSuccess, T data, String error) {
        this.isSuccess = isSuccess;
        this.data = data;
        this.error = error;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(true, data, null);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(false, null, message);
    }
}
