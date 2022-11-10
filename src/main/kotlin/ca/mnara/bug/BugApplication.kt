package ca.mnara.bug

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.*

@SpringBootApplication
@RestController
class BugApplication(@Autowired private val jdbc: NamedParameterJdbcTemplate) {
    private val limit = 20

    @GetMapping("/")
    fun index(@RequestParam("cursor") rawCursor: String?): IdeaCollection {
        val cursor = rawCursor?.let { Cursor.fromString(it) }
        val ideas = getIdeas(cursor).getOrThrow()
        val newCursor = if (ideas.size > limit) {
            ideas.lastOrNull()?.let { Cursor(it.id, it.createdAt, true).toString() }
        } else null

        return IdeaCollection(
            data = ideas.take(limit),
            cursor = newCursor
        )
    }

    @GetMapping("bug")
    fun recreateBug(): Idea? {
        var ideas = getIdeas(null).getOrThrow()

        while (ideas.size > limit) {
            val cursor = if (ideas.size > limit) {
                ideas.lastOrNull()?.let { Cursor(it.id, it.createdAt, true) }
            } else null

            ideas = getIdeas(cursor).getOrThrow()

            if (ideas.lastOrNull()?.createdAt == LocalDateTime.parse("2000-01-01T00:00:00")) {
               return ideas.lastOrNull()
            }
        }

        return null
    }

    private fun getIdeas(cursor: Cursor?): Result<List<Idea>> = kotlin.runCatching {
        val paginationCondition = cursor?.let { "WHERE (created_at, id) < (:date, :id)" } ?: ""
        val query = "SELECT * FROM idea $paginationCondition ORDER BY created_at DESC, id DESC LIMIT ${limit + 1}"
        val params = cursor?.let { mapOf("date" to it.time, "id" to it.id) } ?: mapOf()

        jdbc.query(query, params) { it, _ ->
            Idea(
                id = UUID.fromString(it.getString("id")),
                name = it.getString("name"),
                createdAt = it.getTimestamp("created_at").toLocalDateTime()
            )
        }
    }
}

data class Idea(
    val id: UUID,
    val name: String,
    val createdAt: LocalDateTime,
)

data class IdeaCollection(
    val data: List<Idea>,
    val cursor: String?
)

data class Cursor(val id: UUID, val time: LocalDateTime, val descending: Boolean) {
    companion object {
        fun fromString(source: String): Cursor {
            val parts = String(
                Base64
                    .getDecoder()
                    .decode(source)
            ).split("::")

            if (parts.size != 4) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor.")
            }

            return Cursor(
                id = UUID.fromString(parts[1]),
                time = LocalDateTime.parse(parts[2]),
                descending = parts.last() == "0"
            )
        }
    }

    override fun toString(): String {
        val direction = if (descending) 0 else 1
        return Base64.getEncoder().encodeToString("v1::$id::$time::$direction".toByteArray())
    }
}

fun main(args: Array<String>) {
    runApplication<BugApplication>(*args)
}
