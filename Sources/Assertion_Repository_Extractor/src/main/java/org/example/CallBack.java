package org.example;

/**
 * a templated functional interface having onSuccess method which can be invoked when a particular one-argument function
 * needs to be invoked in a particular circumstance.
 *
 * @param <T>
 */
interface CallBack<T> {
    void onSuccess(T arg);
}
