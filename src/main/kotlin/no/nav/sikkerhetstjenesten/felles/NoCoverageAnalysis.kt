package no.nav.sikkerhetstjenesten.felles

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.CONSTRUCTOR
import kotlin.annotation.AnnotationTarget.FUNCTION

@Retention(BINARY)
@Target(FUNCTION, CONSTRUCTOR, CLASS)
annotation class Generated

typealias NoCoverageAnalysis = Generated
