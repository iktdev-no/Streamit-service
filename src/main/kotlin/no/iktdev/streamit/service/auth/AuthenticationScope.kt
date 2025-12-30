package no.iktdev.streamit.service.auth

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresAuthentication(val withScope: Scope = Scope.AuthorizedRead)


enum class Scope {
    None,
    AuthorizedRead, // Allows request if not on lan

    MediaRead, // Allows read of all media
    MediaWrite,

    ProgressRead,
    ProgressWrite,

    CatalogRead,
    CatalogWrite,

    UserRead,
    UserWrite,

    AuthorizationPermit,
    AuthorizationCreate,
    DeviceRegistryRead
}

enum class ScopeKey {
    Authorized,
    Media,
    Progress,
    Catalog,
    User,
    Authorization,
    DeviceRegistry
}

fun impliedScopes(scope: Scope): List<Scope> = when (scope) {
    Scope.MediaWrite -> listOf(Scope.MediaRead)
    Scope.ProgressWrite -> listOf(Scope.ProgressRead)
    Scope.CatalogWrite -> listOf(Scope.CatalogRead)
    Scope.UserWrite -> listOf(Scope.UserRead)
    else -> emptyList()
}

fun userDefaultScope(): Map<String,List<String>> {
    return mapOf(
        ScopeKey.Authorized to listOf(Scope.AuthorizedRead),
        ScopeKey.Media to listOf(Scope.MediaRead),
        ScopeKey.Progress to listOf(
            Scope.ProgressWrite,
            Scope.ProgressRead
        ),
        ScopeKey.Catalog to listOf(
            Scope.CatalogRead
        ),
        ScopeKey.User to listOf(
            Scope.UserRead,
            Scope.UserWrite
        ),
        ScopeKey.Authorization to listOf(
            Scope.AuthorizationCreate,
            Scope.AuthorizationPermit
        )
    ).toClaims()
}

fun castScope(): Map<String, List<String>> {
    return mapOf(
        ScopeKey.Authorized to listOf(Scope.AuthorizedRead),
        ScopeKey.Media to listOf(Scope.MediaRead),
        ScopeKey.Catalog to listOf(
            Scope.CatalogRead
        )
    ).toClaims()
}

fun Map<ScopeKey, List<Scope>>.toClaims(): Map<String, List<String>> {
    return this.mapKeys { (key, _) -> key.name }
        .mapValues { (_, scopes) -> scopes.map { it.name } }
}

fun scopesFromClaims(claims: Map<String, List<String>>): Map<ScopeKey, List<Scope>> {
    return claims.mapNotNull { (keyStr, scopeStrs) ->
        val key = ScopeKey.entries.find { it.name == keyStr } ?: return@mapNotNull null
        val scopes = scopeStrs.mapNotNull { scopeName ->
            Scope.entries.find { it.name == scopeName }
        }
        key to scopes
    }.toMap()
}