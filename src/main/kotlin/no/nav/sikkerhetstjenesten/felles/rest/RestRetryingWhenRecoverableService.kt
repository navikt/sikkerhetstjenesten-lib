package no.nav.sikkerhetstjenesten.felles.rest


import glide.api.models.exceptions.TimeoutException
import org.springframework.core.annotation.AliasFor
import org.springframework.dao.QueryTimeoutException
import org.springframework.resilience.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.ResourceAccessException
import java.lang.annotation.Inherited
import java.net.SocketTimeoutException
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.reflect.KClass

@Retryable
@Target(FUNCTION, CLASS)
@Retention(RUNTIME)
@Inherited
@MustBeDocumented
@Service
annotation class RestRetryingWhenRecoverableService(
    @get:AliasFor(annotation = Retryable::class) val value: Array<KClass<out Throwable>> = [RecoverableRestException::class, SocketTimeoutException::class, ResourceAccessException::class, TimeoutException::class, QueryTimeoutException::class])