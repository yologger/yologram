package link.yologram.api.v1.domain.news.tech.service

import link.yologram.api.v1.domain.news.tech.entity.TechNewsSource
import link.yologram.api.v1.domain.news.tech.exception.TechNewsSourceDuplicateException
import link.yologram.api.v1.domain.news.tech.exception.TechNewsSourceNotFoundException
import link.yologram.api.v1.domain.news.tech.model.AdminTechNewsSourceCreateRequest
import link.yologram.api.v1.domain.news.tech.model.AdminTechNewsSourceResponse
import link.yologram.api.v1.domain.news.tech.model.AdminTechNewsSourceUpdateRequest
import link.yologram.api.v1.domain.news.tech.repository.TechNewsSourceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminTechNewsSourceService(
    private val techNewsSourceRepository: TechNewsSourceRepository,
) {

    @Transactional(readOnly = true)
    fun getSources(): List<AdminTechNewsSourceResponse> {
        return techNewsSourceRepository.findAllByOrderByIdAsc()
            .map { AdminTechNewsSourceResponse.from(it) }
    }

    @Transactional
    fun create(request: AdminTechNewsSourceCreateRequest): AdminTechNewsSourceResponse {
        if (techNewsSourceRepository.existsByUrl(request.url)) {
            throw TechNewsSourceDuplicateException()
        }

        val saved = techNewsSourceRepository.save(
            TechNewsSource(
                name = request.name,
                url = request.url,
                isActive = request.isActive,
            )
        )
        return AdminTechNewsSourceResponse.from(saved)
    }

    @Transactional
    fun update(id: Long, request: AdminTechNewsSourceUpdateRequest): AdminTechNewsSourceResponse {
        val source = techNewsSourceRepository.findById(id)
            .orElseThrow { TechNewsSourceNotFoundException() }

        request.url?.let {
            if (techNewsSourceRepository.existsByUrlAndIdNot(it, id)) {
                throw TechNewsSourceDuplicateException()
            }
            source.url = it
        }
        request.name?.let { source.name = it }
        request.isActive?.let { source.isActive = it }

        // @LastModifiedDate는 flush 시점에 갱신되므로 응답에 반영되도록 즉시 flush
        val saved = techNewsSourceRepository.saveAndFlush(source)
        return AdminTechNewsSourceResponse.from(saved)
    }

    /**
     * hard delete — 수집 중지는 isActive=false가 담당하고, 삭제는 목록에서 완전 제거.
     * tech_news가 sourceId를 무FK로 참조하지만 뉴스 표시는 비정규화된 sourceName 스냅샷만 사용(TechNews·TechNewsResponse)이라
     * 소스를 지워도 기존 뉴스 노출에 영향 없음.
     */
    @Transactional
    fun delete(id: Long) {
        val source = techNewsSourceRepository.findById(id)
            .orElseThrow { TechNewsSourceNotFoundException() }
        techNewsSourceRepository.delete(source)
    }
}
