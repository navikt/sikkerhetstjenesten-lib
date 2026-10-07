package no.nav.sikkerhetstjenesten.felles.rest

import no.nav.boot.conditionals.ConditionalOnNotProd
import no.nav.sikkerhetstjenesten.felles.NoCoverageAnalysis
import org.springframework.core.annotation.AliasFor
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.tags.Tag
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS


@Target(CLASS)
@Retention(RUNTIME)
@ConditionalOnNotProd
@RestController
@RequestMapping
@Tag(name = "")
@NoCoverageAnalysis
annotation class DevController(
    @get:AliasFor(annotation = RequestMapping::class, attribute = "value")
    val value: Array<String>,
    @get:AliasFor(annotation = Tag::class, attribute = "name")
    val name: String = "",
    @get:AliasFor(annotation = Tag::class, attribute = "description")
    val description: String = ""
)