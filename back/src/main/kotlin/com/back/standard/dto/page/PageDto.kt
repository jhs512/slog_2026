import com.back.standard.dto.page.PageableDto
import org.springframework.data.domain.Page

data class PageDto<T : Any>(
    val content: List<T>,
    val pageable: PageableDto
) {
    constructor(page: Page<T>) : this(
        page.content,
        PageableDto(
            page.pageable.pageNumber + 1,
            page.pageable.pageSize,
            page.pageable.offset,
            page.totalElements,
            page.totalPages,
            page.numberOfElements,
            page.pageable.sort.isSorted
        )
    )
}