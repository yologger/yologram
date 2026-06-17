package link.yologram.api.v1.domain.cms.enum

import link.yologram.api.v1.domain.cms.exception.InvalidSectionException

enum class Section {
    TECH,
    INVEST,
    POLITICS,
    ;

    companion object {
        fun fromPath(path: String): Section {
            return values().firstOrNull { it.name.equals(path, ignoreCase = true) }
                ?: throw InvalidSectionException()
        }
    }
}
