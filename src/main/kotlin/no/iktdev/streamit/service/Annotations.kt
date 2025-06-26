package no.iktdev.streamit.service

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.lang.annotation.ElementType
import java.lang.annotation.RetentionPolicy

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@RestController
annotation class ApiRestController


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@RestController
annotation class ContentRestController

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@RestController
annotation class AssetRestController