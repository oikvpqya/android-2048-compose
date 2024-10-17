package com.alexjlockwood.twentyfortyeight.domain

import kotlinx.serialization.Serializable

@Serializable
data class Artifact(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val name: String? = null,
    val spdxLicenses: Set<SpdxLicense> = emptySet(),
    val unknownLicenses: Set<UnknownLicense> = emptySet(),
    val scm: Scm? = null,
) {
    @Serializable
    data class SpdxLicense(
        val identifier: String,
        val name: String,
        val url: String,
    )

    @Serializable
    data class UnknownLicense(
        val name: String?,
        val url: String?,
    )

    @Serializable
    data class Scm(
        val url: String,
    )
}
